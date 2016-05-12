/*
 * File:   NDSException.cpp
 * Author: joe
 *
 * Created on March 4, 2012, 6:51 AM
 */


#include <map>
#include "daqc_response.h"
#include "daqc.h"
#include "NDSException.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

using namespace std;


NDSException::NDSException()
{
    nds_rc = 0; // don't know what error they are reporting
}

NDSException::NDSException(int rc)
{
    nds_rc = rc; // that's the error return code form an NDS call
    getMsg(rc);
}

NDSException::NDSException(int rc, const char* msg)
{
    nds_rc = rc; // that's the error return code form an NDS call
    errMsg = msg;
    if (!(errMsg.find("\n") == errMsg.npos))
        errMsg += string("\n");
}
NDSException::NDSException(const NDSException& orig)
{

    nds_rc = orig.nds_rc;
    errMsg = orig.errMsg;
}

NDSException::~NDSException() throw()
{
    
}


const char* NDSException::what() const throw()
{
    return errMsg.c_str();
}
const char* NDSException::getMsg(int rc)
{
    map<int, string> erMsgList;
    erMsgList[DAQD_OK]="Successful completion";
    erMsgList[DAQD_ERROR]="Generic error code";
    erMsgList[DAQD_NOT_CONFIGURED]="Daqd is not configured.\nUsually daq_connect was not called or failed.";
    erMsgList[DAQD_INVALID_IP_ADDRESS]="Attempt to get host ip address from hostname failed.";
    erMsgList[DAQD_INVALID_CHANNEL_NAME]="Invalid channel name";
    erMsgList[DAQD_SOCKET]="Client failed to get socket";
    erMsgList[DAQD_SETSOCKOPT]="Unable to set client socket options";
    erMsgList[DAQD_CONNECT]="Unable to connect to server. The server address or port may be incorrectly specified or there may be no path to the server.";
    erMsgList[DAQD_BUSY]="NDS server is overloaded";
    erMsgList[DAQD_MALLOC]="Insufficient memory for allocation";
    erMsgList[DAQD_WRITE]="Error occurred trying to write to socket";
    erMsgList[DAQD_VERSION_MISMATCH]="Communication protocol version mismatch";
    erMsgList[DAQD_NO_SUCH_NET_WRITER]=" No such net writer (nds1)";
    erMsgList[DAQD_NOT_FOUND]="Requested data were not found";
    erMsgList[DAQD_GETPEERNAME]="Could not get client's IP address";
    erMsgList[DAQD_DUP]="Error in dup() (obsolete)";
    erMsgList[DAQD_INVALID_CHANNEL_DATA_RATE]="Requested data rate is invalid for channel";
    erMsgList[DAQD_SHUTDOWN]="Shutdown request failed.";
    erMsgList[DAQD_NO_TRENDER]="Trend data are not available (nds1)";
    erMsgList[DAQD_NO_MAIN]="Full channel data are not available (nds1)";
    erMsgList[DAQD_NO_OFFLINE]="No offline data (nds1)";
    erMsgList[DAQD_THREAD_CREATE]="Unable to create thread (obsolete)";
    erMsgList[DAQD_TOO_MANY_CHANNELS]="Too many channels requested";
    erMsgList[DAQD_COMMAND_SYNTAX]="Command syntax error";
    erMsgList[DAQD_SASL]="Request sasl authentication protocol (nds2 only)";
    erMsgList[DAQD_NOT_SUPPORTED]="Requested feature is not supported";

    string ermsg;
    char rc_str[16];
    memset(rc_str,0,sizeof(rc_str));
    sprintf(rc_str,"%d ",rc);
    ermsg = string("Unknown NDS error: ") + string(rc_str);
    string er1 = daq_strerror(rc);
    if (erMsgList.find(rc) != erMsgList.end())
    {
        ermsg = string("NDS error: ") + string(rc_str) + erMsgList[rc];
    }
    errMsg = string(ermsg) + string(" - ") + er1 + string("\n");
    return errMsg.c_str();
}
