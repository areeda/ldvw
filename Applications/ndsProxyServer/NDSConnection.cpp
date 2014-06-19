/* 
 * File:   NDSConnection.cpp
 * Author: joe
 * 
 * Created on March 19, 2012, 7:52 AM
 */

#include "NDSConnection.h"
#include <string.h>
#include <time.h>
#include <errno.h>
#include <iostream>
using std::cerr;

NDSConnection::NDSConnection( )
{
    port = 31200;   // default port number
}

NDSConnection::NDSConnection( const NDSConnection& orig )
{
    this->serverUrl = orig.serverUrl;
}

NDSConnection::~NDSConnection( )
{
}

void NDSConnection::setUrl(string url)
{
    this->serverUrl = url;
}

void NDSConnection::setPort(int port)
{
    this->port = port;
}

/**
 * Establish a connection with the specified server.  This performs the Kerberos authentication
 * an exception is thrown it fails
 */
void NDSConnection::connect()
{
    
    int rc = daq_startup();
    if (rc != 0)
    {
        throw NDSException(rc,"Global init failed");
    }
    memset(&this->daqd,0,sizeof(this->daqd));
    rc = daq_connect(&this->daqd, this->serverUrl.c_str(), port, nds_try);
    if (rc != 0)
    {
        daq_destroy(&this->daqd);
        throw NDSException(rc);
    }
}
void NDSConnection::disconnect()
{
    try
    {
        daq_destroy(&this->daqd); 
        //int rc2 = daq_disconnect(&daqd);
//        if ( rc2 < 0 )
//        {
//            cerr << " Error on close client connection (daq_disconnect)";
//            cerr << errno << ". " << strerror( errno );
//        }

        memset(&daqd,0,sizeof(daqd));
    }
    catch(...)
    {
        // well we tried.
    }
}
