/* 
 * File:   ServerThread.h
 * Author: joe
 *
 * Created on March 26, 2012, 8:49 PM
 */

#ifndef SERVERTHREAD_H
#define	SERVERTHREAD_H

#include <string>
#include <map>
#include <vector>

using namespace std;

#include "NDSConnection.h"
#include "Channels.h"
#include "NDSData.h"

#define MAX_SOURCE_LIST 262144

/**
 * Spawned as a pthread, this class handles the socket communications for the NDS library interface
 */
class ServerThread
{

    
private:

    string prompt;
    string hello;
    string version;
    int fd;
    
    enum Command {UNKNOWN, ATIM, BSTS, CHCOUNT, CHHASH, CHLIST, CONNECT, DATA, DISCONNECT, 
                  EMPTY, HELP, NEXT, SOURCEDATA, SOURCELIST, VERSION, BYE};
    map<string, Command> cmdList;
    
    NDSConnection *conn;
    Channels *chan;
    NDSData *data;
    bool connected;
    
    bool checkConn();
    void cmdErr(const char* msg);               ///< error decoding commands or arguments send em a message
    void disconnect();
    void dispatcher(void* id);
    Command getCmd(const char *buf, vector<string> &args);
    static string getCurTime();
    void sayBye();
    void sendBytes( const void* msg, int n, int size );   ///< send a buffer as binary
    void sendRcMsg(int rc);
    void sendStr(const char *str);              ///< send a string to client
    void sendStr(string str);                       ///< send a string to client
    void sendSuccess();                         ///< send success message
    
    // the commands
    void getAvailableTimes(vector<string> args);    ///< find when these channels have data
    void getSourceData( vector<string> args );      ///< return times from source frames
    void getSourceList( vector<string> args );  ///< get source list
    void connect(vector<string> args);              ///< establish connect to the server
    void disconnect(vector<string> args);           ///< close connection to server but not client
    void nextBuf(vector<string> args);              ///< send next buffer of data previously requested
    void reqData(vector<string> args);              ///< set up a data request
    void sendBufStatus(vector<string> args);       ///< send information on the status of received data
    void sendChanCount(vector<string> args);        ///< send list of channels matching spec
    void sendChanHash(vector<string> args);         ///< send hash of channel list of an optional type
    void sendChanList(vector<string> args);         ///< send list of channels matching spec
    void sendHelp();                                ///< send a summary of commands for interactive use
    
 public:
    static bool verbose;
    static bool noThreads;

    ServerThread();
    ServerThread(const ServerThread& orig);
    virtual ~ServerThread();
    static void setNoThreads(bool nt);
    static void setVerbose(bool v);
    static void *threadHelper(void *port);   
    
};

#endif	/* SERVERTHREAD_H */

