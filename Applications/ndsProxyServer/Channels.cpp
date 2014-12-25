/*
 * File:   Channels.cpp
 * Author: joe
 *
 * Created on March 3, 2012, 5:07 AM
 */

#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <iostream>
#include <vector>

#include "Channels.h"
#include "NDSException.h"
#include "Utils.h"

using namespace std;

Channels::Channels()
{
    this->conn = NULL;
    this->channels = NULL;
    
    // give some names to the channel types
    chanTypes[cUnknown] = "unknown";
    chanTypes[cOnline] = "online";
    chanTypes[cRaw] = "raw";
    chanTypes[cRDS] = "RDS";
    chanTypes[cSTrend] = "second-trend";
    chanTypes[cMTrend] = "minute-trend";
    chanTypes[cTestPoint] = "test-point";
    chanTypes[cStatic] = "static";

    // give names to data types
    dataTypes[_undefined] = "undef";
    dataTypes[_16bit_integer] = "INT-16";
    dataTypes[_32bit_integer] = "INT-32";
    dataTypes[_32bit_uint] = "UINT-32";
    dataTypes[_64bit_integer] = "INT-64";
    dataTypes[_32bit_float] = "FLT-32";
    dataTypes[_64bit_double] = "FLT-64";
    dataTypes[_32bit_complex] = "CPX-64";

    
}


Channels::Channels(const Channels& orig)
{
    
}

Channels::~Channels()
{
    if (channels != NULL)
    {
        free(channels);
        channels = NULL;
    }
}

/**
 * Print a list of all channels to stdout
 */
void Channels::dumpList()
{
    string list = getListCSV(0,this->howMany());
    cout << list << endl;
}
/**
 * @brief Convert a section of the current channel list to CSV format.
 * First line is column names if start ==0
 * @param strt 0 based first channel number
 * @param count number of channels to return
 * @return one BIG string with all the channels
 */
string Channels::getListCSV(int strt,int count)
{
    string ret = "";
    int nChan = howMany();
    char *line = (char *) calloc(512, 1);
    int st = strt;
    
    if (strt == 0)
        ret += "#Name,Rate,Tst pnt,Type,BPS,Data,Gain,Offset,Slope,Units\n";

    for (int i = st; i < strt + count && i < nChan; i++)
    {
        daq_channel_t c = channels[i];
        memset(line, 0, sizeof(char) * sizeof(line));
        sprintf(line, "%s,%.2f,%d,%s,%d,%s,%s\n",
                c.name, c.rate, c.tpnum, chanTypes[c.type].c_str(), c.bps,
                dataTypes[c.data_type].c_str(), sigConv2str(c.s).c_str());
        ret += line;

    }
    free(line);
    return ret;
}

int Channels::howMany()
{
    return this->chanCount;
}

/**
 * Query for the number of channels of each known type
 */
void Channels::getCounts()
{
    timespec start, end;
    double elap;
    gettime(&start);
    /*---  Get the number of channels for each channel type */
    int num_alloc    = 0;
    
    for (map<chantype_t, string>::iterator it=chanTypes.begin(); it != chanTypes.end();it++)
    {
        chantype_t chant = it->first;
        num_alloc = getChanCount(chant);
        chCounts[chant] = num_alloc;
        printf("There are %d channels of type %d - %s.\n",num_alloc, chant,chanTypes[chant].c_str());
    }
    gettime(&end);
    elap = end.tv_sec - start.tv_sec + (end.tv_nsec - start.tv_nsec) / 1e9;
    printf("Get channel count %.2fs\n",elap);
}
/**
 * Query for the number of channels of a specific type
 * @param chant what type
 * @return number of channels available for that type.  Note:  available at any time
 */
int Channels::getChanCount(chantype_t chant)
{
    daq_channel_t* channels = NULL;
    int num_alloc    = 0;
    time_t gps = 0;
    
    int rc = daq_recv_channel_list(conn->getDaqd(), channels, 0, &num_alloc, gps, chant);
    if (rc)
    {
        throw NDSException(rc);
    }
    return num_alloc;
}
/**
 * Query the server for a list of channels and build the internal vector for access by getter fn's
 * @param ctyp - channel type ('online', 'raw', 'minute trend' as the enum
 * @return number of channels retrieved
 */
int Channels::getChanList(chantype_t ctyp)
{
    timespec start, end;
    double elap;
    gettime(&start);
    
    getCounts();
    int ccnt = chCounts[ctyp];  // reported  number of channels
    int ccnt2;                  // received number of channels
    if (ccnt == 0)
    {
        ccnt = chCounts[ctyp] = getChanCount(ctyp);
    }
    if (ccnt > 0)
    {
        channels = (daq_channel_t*)calloc(ccnt,sizeof(daq_channel_t));

        int rc = daq_recv_channel_list(conn->getDaqd(), channels, ccnt, &ccnt2, 0, ctyp);
        if (rc != 0)
        {
            throw NDSException(rc);
        }  
    }
    else
    {
        channels = NULL;
    }
    this->chanCount = ccnt;
    gettime(&end);
    elap = end.tv_sec - start.tv_sec + (end.tv_nsec - start.tv_nsec) / 1e9;
    
    long xRate = ccnt*sizeof(daq_channel_t)/elap;
    printf("Get channel List %.3f s.  %ld bytes/sec\n",elap,xRate);
    return ccnt;
}
int Channels::getChanHash(chantype_t ctyp,time_t gps)
{
    int hash;
    int length = sizeof(hash);

    int rc = daq_recv_channel_hash(conn->getDaqd(), &hash, &length, gps, ctyp);
    if ( rc != 0 )
    {
        throw NDSException( rc );
    }  
    
    return hash;
}
/**
 * Format calibration factors as tab separated string
 * @param s calibration factor struct
 * @return string ready for printing to tsv file
 */
string Channels::sigConv2str(signal_conv_t s)
{
    char *buf = (char *)calloc(256,1);
    string ret="?";
    
    sprintf(buf,"%.3f,%.3f,%.3f,%s",
            s.signal_gain,s.signal_offset,s.signal_slope,s.signal_units);
    
    ret = buf;
    free(buf);
    return ret;
}
/**
 * convert external channel type name to internal enum
 * "unknown", "online", "raw", "RDS", "second-trend", "minute-trend", "test-point", "static".
 * 
 * The list of available types is not case sensitive
 * @param str - external type name
 * @param chantyp - internal channel type enum
 * @return  true if we found a match.  If not match is found chantyp will be set to unknown (same as all)
 */
bool Channels::str2chantype(const char * str, chantype_t *chantyp)
{
    bool ret = false;

    for (map<chantype_t, string>::iterator it = chanTypes.begin(); it != chanTypes.end() && !ret; it++) 
    {
        chantype_t chant = it->first;
        if (strcasecmp(str,chanTypes[chant].c_str())==0)
        {
            *chantyp = chant;
            ret=true;
            
        }
    }
    return ret;
}