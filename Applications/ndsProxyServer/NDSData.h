/* 
 * File:   NDSData.h
 * Author: joe
 *
 * Created on March 19, 2012, 9:35 PM
 */

#ifndef NDSDATA_H
#define	NDSDATA_H

#include <string>
using std::string;

#include "NDSConnection.h"
#include <daqc.h>

class NDSData
{
public:
    NDSData();
    NDSData(const NDSData& orig);
    virtual ~NDSData();
    
    int reqChanData(const char *name, chantype_t ctype, double rate, time_t start, time_t end, time_t dt );
    int recvNextBufferDouble(double **buf, int *n, time_t *start);
    void setConnection(NDSConnection *c);
    void setVerbose(bool v){verbose = v;};
    
private:
    NDSConnection *conn;
    string chanName;
    time_t start;
    time_t end;
    time_t dt;
    daq_data_t dtype;
    chan_req_t* req;
    bool verbose;

};

#endif	/* NDSDATA_H */

