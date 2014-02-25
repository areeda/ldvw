/*
 * File:   Channels.h
 * Author: joe
 *
 * Created on March 3, 2012, 5:07 AM
 */

#ifndef CHANNELS_H
#define	CHANNELS_H
#include <stddef.h>
#include <string>
using std::string;

#include <map>
using std::map;

#include <vector>
using std::vector;


extern "C"
{
//#include "daq_config.h"
#include "daqc.h"
#include "channel.h"

}
#include "NDSConnection.h"
class Channels
{
public:
   
    Channels();
    Channels(const Channels& orig);
    virtual ~Channels();

    void dumpList();
    int getChanCount(chantype_t chant);
    void getCounts();
    int getChanList(chantype_t ctyp);
    int getChanHash(chantype_t ctyp, time_t gps);
    string getListCSV(int strt, int stop);
    int howMany();
    
    void setConn(NDSConnection *c){ conn=c;};
    
    bool str2chantype(const char * str, chantype_t *chantyp);
    

private:
    
    NDSConnection *conn;                         ///< connection we're using for our 
    string sigConv2str(signal_conv_t s);        ///< format calibration factors for printing (tab separated)
    
    
    
    map<chantype_t, string>  chanTypes;         ///< enum to text description of channel types
    map<daq_data_t, string>  dataTypes;         ///< enum to text description of data types (int, double...)
    
    
    map<chantype_t, int> chCounts;              ///< count of channels for each type
    daq_channel_t* channels;                    ///<  all available channels (for type requested)
    int chanCount;                              ///< number of channels allocated for that list
};

#endif	/* CHANNELS_H */

