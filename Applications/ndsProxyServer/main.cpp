/* 
 * File:   main.cpp
 * Author: joe
 *
 * Created on March 6, 2012, 5:48 AM
 */

#include <cstdlib>
#include <stdio.h>
#include <string.h>
#include <cstddef>
#include <iostream>
#include <getopt.h>
#include <map>

#include <sys/time.h>


#include "Channels.h"
#include "NDSConnection.h"
#include "NDSException.h"
#include "NDSData.h"
#include "ProxyServer.h"
#include "Utils.h"

using namespace std;

/*
 * 
 */
int main(int argc, char** argv) 
{
    int verbose=2;
    string server = "nds.ligo.caltech.edu";
    chantype_t chanType=cUnknown;
    bool dumpChanList = false;
    
    static struct option long_options[] =
             {
               /* These options set a flag. */
               {"verbose", no_argument,       &verbose, 2},
               {"brief",   no_argument,       &verbose, 0},
               
               {"server",           required_argument,  0, 's'},
               {"type",             required_argument,  0, 't'},
               {"dump-channels",    no_argument,        0, 'd'},
               {"gps-start",        required_argument,  0, 'g'},
               {"xfer-rate",        required_argument,  0, 'r'},
               {"proxy",            optional_argument,  0, 'p'},
               {"nothreads",        no_argument,        0, 'n'},
               {"help",             no_argument,        0, 'h'},
               {0, 0, 0, 0}
             };
    static string explanations[] =      // these are parallel to long_options
    {
        "Add more information to output",
        "Brief use minimum output, errors only",
        "Specify server by uri or ip address",
        "Channel type: raw, online, RDS, second-trend, minute-trend, test-point, or static",
        "Print list of channels",
        "Start time in gps",
        "Measure the transfer rate",
        "Act as a proxy server",
        "run proxy server for 1 connection (debugging)",
        "Show this message",
    };
    char c;
    string argerr = "";
    bool wantHelp = false;
    string chanName;
    bool xferRateTest = false;
    bool beProxy = false;
    bool noThreads = false;
    string proxyAddress = "";
    string cType = "";
    
    while(( c = getopt_long(argc,argv,"s:t:h",long_options,NULL)) != EOF)
    {
        switch(c)
        {
            case 'd':
                dumpChanList = true;
                break;
                
            case 'r':
                chanName = optarg;
                xferRateTest = true;
                break;
                
            case 'n':
                noThreads = true;
                break;
                        
            case 'p':
                beProxy = true;
                if (optarg != NULL)
                    proxyAddress = optarg;
                break;
                
            case 's':
                server=optarg;
                break;
                
            case 't':
                cType = optarg;
                break;
                
            case 'h':
                wantHelp = true;
                break;
                
            case '\000':
                break;          // I don't know what zero means but we sometimes get it
                
            default:
                argerr = "Unknown argument ";
                argerr += ((char)c);
                argerr += "\n";
        }
    }
    if (argerr.length() > 0 || wantHelp)
    {
        cout << "NDS transfers" << endl;
        cout << argerr << endl;
        for(int i=0; long_options[i].name != NULL;i++)
        {
            if (long_options[i].val > 'a')
            {
                cout << "-" << (char)long_options[i].val << " or ";
            }
            cout << "--" << (char *) long_options[i].name ;
            if (long_options[i].has_arg == required_argument)
                cout << "=value";
            else if (long_options[i].has_arg == optional_argument)
                cout << "[=value]";
                        
            cout << " " << explanations[i] <<endl;
        }
        return 2;
    }
    NDSConnection *conn = NULL;
    Channels *chan = NULL;
    NDSData *dat = NULL;
    
    try
{
        conn = new NDSConnection();
        chan = new Channels();

        if (dumpChanList || xferRateTest)
        {
            conn->setUrl(server);
            chan->setConn(conn);
        }
        
        if (dumpChanList)
        {
            chan->getCounts( );
            chan->getChanList(chanType);
            chan->dumpList();
        }
        if (xferRateTest)
        {
            dat = new NDSData();
            dat->setVerbose(true);
            dat->setConnection(conn);
            dat->reqChanData(chanName.c_str(), cOnline,256, 0, 0, 32);
            while(1)
            {
                struct timespec stime,etime;
                gettime(&stime);
                double *buf=NULL;
                int n=0;
                time_t start = 0;
                dat->recvNextBufferDouble(&buf, &n, &start);
                gettime(&etime);
                float elap = etime.tv_sec - stime.tv_sec + (etime.tv_nsec - stime.tv_nsec)*1e-9;
                cout << elap << endl;
            }
        }
        if (beProxy)
        {
            try
            {
                ProxyServer ps;
                ps.setVerbose(verbose!=0);
                ps.setNoThreads(noThreads);
                ps.Listen();
            }
            catch (NDSException const& ex)
            {
                cerr<<"NDS library error: "<<ex.what()<< endl;
            }
        }
        
    }
    catch (NDSException const& ne)
    {
        cerr << getCurTime() <<"NDS library error: "<<ne.what()<< endl;
    }
    catch (exception const& e)
    {
        cerr << "Error:" << e.what() << endl;
    }
    if (conn != NULL)
    {
        delete conn;
    }
    if (chan != NULL)
    {
        delete chan;
    }
    if (dat != NULL)
    {
        delete dat;
    }
    printf("\n%sProxy server exited.\n",getCurTime().c_str());
    return 0;
}

