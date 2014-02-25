/* 
 * File:   NDSConnection.h
 * Author: joe
 *
 * Created on March 19, 2012, 7:52 AM
 */

#ifndef NDSCONNECTION_H
#define	NDSCONNECTION_H

#include <string>
using std::string;

#include <cstdio>

#include "NDSException.h"

extern "C"
{
//#include "daq_config.h"
#include "daqc.h"
#include "channel.h"
}

class NDSConnection
{
public:
    NDSConnection();
    NDSConnection(const NDSConnection& orig);
    virtual ~NDSConnection();
    void setUrl(string url);
    void setPort(int port);
    daq_t* getDaqd(){ return &daqd;};    ///< the daqd is the descriptor need for network calls
    
    void connect();
    void disconnect();
    
private:
    string serverUrl;   ///< which server this object is communicating with
    int port;           ///< port number for server
    daq_t daqd;         ///< the nds2 descriptor of the connection

    

};

#endif	/* NDSCONNECTION_H */

