/* 
 * File:   ServerThread.cpp
 * Author: joe
 * 
 * Created on March 26, 2012, 8:49 PM
 */

#include "ServerThread.h"

#include <errno.h>
#if __APPLE__
#include <err.h>
#define __LITTLE_ENDIAN 1234
#define __BYTE_ORDER __LITTLE_ENDIAN
#else
#include <error.h>
#include <bits/byteswap.h>
#endif


#include <cstdio>
#include <cstddef>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <signal.h>


#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/tokenizer.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/predicate.hpp>


#include <iostream>
#include <channel.h>

#include "NDSException.h"
#include "Utils.h"


using namespace std;
using namespace boost;

typedef tokenizer< escaped_list_separator<char> > Tokenizer;

bool ServerThread::verbose;
bool ServerThread::noThreads;

ServerThread::ServerThread()
{
    conn = NULL;
    chan = NULL;
    data = NULL;

    version = "0.0.10";             // something to send to user to know who we are
    prompt = "> ";
    hello = "NDS proxy, help works\n";
    cmdList["atim"] = ATIM;         // available data
    cmdList["bsts"] = BSTS;         // buffer status
    cmdList["bye"]  = BYE;          // close connection to server and client
    cmdList["chls"] = CHLIST;       // list of channels
    cmdList["chsh"] = CHHASH;       // get hash of channel list
    cmdList["chct"] = CHCOUNT;      // get channel count
    cmdList["conn"] = CONNECT;      // establish server connection
    cmdList["data"] = DATA;         // specify what data to transfer
    cmdList["dcon"] = DISCONNECT;   // close connection to server but not client
    cmdList["exit"] = BYE;          // close connection to server and client
    cmdList["help"] = HELP;         // send silly command list
    cmdList["next"] = NEXT;         // get next data buffer
    cmdList["srcd"] = SOURCEDATA;   // Get detailed list of available frame times
    cmdList["srcl"] = SOURCELIST;   // get source frame list
    cmdList["quit"] = BYE;          // close connection to server and client
    cmdList["ver"] = VERSION;       // send back server version
}

ServerThread::ServerThread(const ServerThread& orig)
{
    throw NDSException(128, "Cannot copy a Server Thread object.");
}

ServerThread::~ServerThread()
{
}

void ServerThread::setVerbose(bool v)
{
    verbose = v;
}

void ServerThread::setNoThreads( bool nt)
{
    noThreads=nt;
}
/**
 * We can be called with pthread for threaded operation or directly for debuggin
 * We are passed an open socket to be passed to command dispatcher
 * @param port open socket to our client
 * @return status
 */
void* ServerThread::threadHelper(void *port)
{
    ServerThread *st = new ServerThread();
    
    try
    {
        st->dispatcher(port);
    }
    catch ( NDSException ex )
    {
        printf( "%s Server thread exception: %s ", getCurTime( ).c_str( ), ex.what( ) );
        if ( errno != 0 )
        {
            printf( "System error number: %d - %s", errno, strerror( errno ) );
        }
        printf( "\n" );

        if ( st->conn != NULL )
        {
            st->sayBye( );
            st->conn = NULL;
        }
    }
    catch (std::exception const& e)
    {
        string t = getCurTime();
        fprintf(stderr, "%s Server exception: %s\n",t.c_str(), e.what());
    }
    if (st->conn != NULL)
    {
        st->sayBye();
        st->conn = NULL;
    }
    delete st;
    if (noThreads) 
    {
        return NULL;
    } else 
    {
        pthread_exit(0);
    }
    
}

void ServerThread::dispatcher(void* id)
{
    signal(SIGPIPE, SIG_IGN);
    fd = *((int*) id);
    connected = true;      /// connected to our client
    cout.setf(ios::fixed, ios::floatfield);
    cout.precision(3);
    while (connected)
    {
        sendStr(hello);
        const int buflen = 262144;
        char buf[buflen];
        char obuf[120];
        try
        {
            while (connected)
            {
                sendStr(prompt);
                
                memset(buf, 0, buflen);
                int cnt = recv(fd, buf, buflen, 0);
                if (cnt < 0)
                {
                    string t = getCurTime();
                    fprintf(stderr, "%s Recv err: %d - %s\n",t.c_str(), errno, strerror(errno));
                    connected = false;
                    if (conn != NULL)
                        delete conn;
                    conn = NULL;

                }
                else if (cnt > 0)
                {
                    timespec start, end;
                    double elap;
                    gettime(&start);
                    
                    
                    vector<string> args;
                    Command cmd = getCmd(buf, args);
                    string emsg = "";
                    switch (cmd)
                    {
                        case UNKNOWN:
                            emsg = string("Unknown command: ") + args[0];
                            cmdErr(emsg.c_str());
                            break;

                        case ATIM:
                            if (checkConn())
                            {
                                getAvailableTimes(args);
                                sendStr("OK\n");
                            }
                            break;
                            
                        case BSTS:
                            if (checkConn())
                            {
                                sendBufStatus(args);
                            }
                            break;

                        case BYE:
                            sayBye( );
                            connected = false;
                            break;

                        case CHCOUNT:
                            if ( checkConn( ) )
                            {
                                sendChanCount(args);
                                sendStr("OK\n");
                            }
                            break;
                            
                        case CHHASH:
                            if ( checkConn( ) )
                            {
                                sendChanHash(args);
                                sendStr("OK\n");
                            }
                            break;

                        case CHLIST:
                            if (checkConn())
                            {
                                sendChanList( args );
                                sendStr( "OK\n" );
                            }
                            break;

                        case CONNECT:
                            if (conn == NULL)
                                connect(args);
                            else
                                cmdErr("Attempt to connect but a connection is already active");
                            break;

                        case DATA:
                            if (checkConn())
                            {
                                reqData(args);
                            }
                            break;

                        case DISCONNECT:
                            if (conn != NULL)
                                disconnect(args);
                            
                            sendStr( "OK\n" );
                            break;

                        case EMPTY:
                            // we can just ignore this
                            break;

                        case HELP:
                            sendHelp();
                            break;

                        case NEXT:
                            if ( checkConn( ) )
                            {
                                nextBuf(args);
                            }
                            break;
                            
                        case SOURCEDATA:
                            if ( checkConn( ) )
                            {
                                getSourceData(args);
                                sendStr( "OK\n" );
                            }
                            break;
                            
                        case SOURCELIST:
                            if ( checkConn())
                            {
                                getSourceList(args);
                                sendStr( "OK\n" );
                            }
                            break;
                            
                        case VERSION:
                            sendStr(version + string("\nOK\n"));
                            break;
                    }
                    if (ServerThread::verbose)
                    {
                        gettime(&end);
                        elap = end.tv_sec - start.tv_sec + (end.tv_nsec - start.tv_nsec) / 1e9;

                        memset(obuf,0,sizeof(obuf));
                        strncpy(obuf,buf,110);
                        cout << getCurTime() << "recv:(" << fd << ") [" << elap << "s] " << obuf;
                        if (! (strchr(obuf,'\n') != NULL))
                            cout << endl;
                        cout.flush();
                    }
                }
                else
                {
                    if (fd > 0)
                        close(fd);

                    if (ServerThread::verbose) 
                        printf("%s Connection %d closed.\n", getCurTime().c_str(), fd);
                    disconnect();

                    break;
                }
            }
        }
        catch (NDSException ex)
        {
            if (verbose)
            {
                printf("%s Connection %d closed on catch.\n", getCurTime().c_str(), fd);
                printf("Exception: %s - %s \n",ex.what(), strerror(errno));
            }
            sayBye();
            connected = false;
        }
        catch (std::exception ex)
        {
            if (ServerThread::verbose)
            {
                printf("%s Connection %d closed on catch.\n", getCurTime().c_str(), fd);
                printf("Exception: %s ",ex.what());
                if (errno != 0)
                {
                    printf("System error number: %d - %s", errno, strerror(errno));
                }
                printf("\n");
            }
            sayBye();
            connected = false;
        }
    }
    
}

ServerThread::Command ServerThread::getCmd(const char *buf, vector<string> &args)
{
    ServerThread::Command ret = EMPTY;
    args.clear();
    if (buf != NULL)
    {
        string in = buf;
        if (in.length() > 0)
        {
            Tokenizer tok(in);
            for (Tokenizer::iterator it(tok.begin()), end(tok.end()); it != end; ++it)
            {
                string t = *it;
                trim(t);
                args.push_back(t);
            }
        }
    }
    if (args.size() > 0)
    {
        string cmd = boost::to_lower_copy(args[0]);
        ret = cmdList[cmd];
    }
    return ret;
}
/**
 * Send an error message to client
 * @param msg the meat of the message we add constant stuff to make it easy to identify
 */
void ServerThread::cmdErr(const char* msg)
{
    string erMsg = string("Error: ") + string(msg) + string("\n");
    sendStr(erMsg);
}
void ServerThread::sendStr(string str)
{
    sendStr(str.c_str());
}
void ServerThread::sendStr(const char* msg)
{
#ifdef __APPLE__
#define MSG_NOSIGNAL 0x0
#endif
    try
    {
        ssize_t nsent = send(fd, msg, strlen(msg),  MSG_NOSIGNAL);
        if (nsent == -1)
        {
            if (errno == EPIPE)
            {
                cout << "Error sending message, (broken pipe) remote disconnected." << endl;
                vector<string> args;
                disconnect(args);
                throw NDSException(EPIPE, "Error sending message, remote disconnected.");
            }
            throw NDSException(errno, "Error sending message");
        }
    } 
    catch ( std::exception const& e )
    {
        conn = NULL;
        cout << "Error sending message "<< strlen(msg) << " bytes long. " << e.what( ) << endl;
        throw e;
    }
}

void ServerThread::sendBytes( const void* msg, int n, int size=1 )
{
#ifdef __APPLE__
#define MSG_NOSIGNAL 0x0
#endif
    try
    {
        ssize_t nsent=0;
        if ( __BYTE_ORDER == __LITTLE_ENDIAN  && size!=1)
        {
            char *t = (char *)malloc(size);
            char *msgb = (char *)msg;
            for(int i=0;i<n;i++)
            {
                for(int j=0;j<size;j++)
                {
                   t[size-j-1] = msgb[i*size+j]; 
                }
                ssize_t tn = send( fd, t, size, MSG_NOSIGNAL );
                if (tn == -1)
                {
                    nsent = -1;
                    break;
                }
                nsent += tn;
            }
            free(t);
        }
        else
        {
            nsent = send( fd, msg, n, MSG_NOSIGNAL );
        }
        if ( nsent == -1 )
        {
            if ( errno == EPIPE )
            {
                cout << "Error sending message, remote disconnected." << endl;
                vector<string> args;
                sayBye();
                throw NDSException( EPIPE, "Error sending message, remote disconnected." );
            }
            throw NDSException( errno, "Error sending message" );
        }
    }
    catch ( std::exception const& e )
    {
        conn = NULL;
        cout << "Error sending message " << n << " bytes long. " << e.what( ) << endl;
        throw e;
    }
}
void ServerThread::connect(vector<string> args)
{
    if (conn != NULL)
    {
        disconnect(args);
    }
    if (args.size() > 1)
    {
        try
        {
            conn = new NDSConnection();
            string server = args[1];
            conn->setUrl(server);
            if (args.size() > 2)
            {
                int port = lexical_cast<int>(args[2]);
                conn->setPort(port);
            }
            conn->connect();
        }
        catch (NDSException const& e)
        {
            if (conn != NULL)
            {
                delete conn;
            }
            conn = NULL;
            cerr << getCurTime() << e.what() << endl;
            cmdErr(e.what());
        }
        catch (std::exception const& e)
        {
            if (conn != NULL) {
                delete conn;
            }
            conn = NULL;
            cmdErr(e.what());
        }
        if (conn != NULL)
            sendStr("OK\n");
    }
    else
    {
        cmdErr("You must specify a server for the connect command.");
    }
}
void ServerThread::disconnect()
{
    vector<string> args;
    disconnect(args);
}
void ServerThread::disconnect(vector<string> args)
{
    if (conn != NULL)
    {
        conn->disconnect();
        delete conn;
        conn = NULL;
    }
    if (chan != NULL)
    {
        delete chan;
        chan = NULL;
    }
    if (data != NULL)
    {
        delete data;
        data = NULL;
    }
}
/**
 * Get the source list from the server this is less detailed than source data
 * 
 * @param args list of channel names
 * @see ServerThread::getSourceData
 */
void ServerThread::getAvailableTimes(vector<string> args)
{
    unsigned int i;
    unsigned int n = args.size();
    if (n > 1)
    {
        daq_channel_t chan[n];
        int rc = 0;
        for (i = 1; i < args.size() && rc == 0; ++i)
        {
            if (args[i].length() < 64)
            {
                memset(&(chan[i-1]),0,sizeof(daq_channel_t));
                daq_init_channel(&(chan[i-1]), args[i].c_str(), cUnknown, 0.0, _undefined);
                rc = daq_request_channel_from_chanlist(conn->getDaqd(), &(chan[i-1]));
                if (rc != 0)
                {
                    string ermsg = string("daq_request_channel_from_chanlist:") + string(daq_strerror(rc));
                    cmdErr(ermsg.c_str());
                }
            }
        }
        if (rc == 0)
        {
            char* list = (char *)calloc(1,(size_t) MAX_SOURCE_LIST);
            long src_len;
            time_t gps = 0;
            rc = daq_recv_source_list(conn->getDaqd(), list, (size_t) MAX_SOURCE_LIST, gps,
                                    &src_len);
            if (rc)
            {
                cmdErr(daq_strerror(rc));
            }
            else
            {
                long inx;
                sendSuccess();
                for (inx = 0; inx < src_len; inx++)
                {
                    if (list[inx] == ' ') continue;
                    char* cbrace = strchr(list + inx, '}') + 1;
                    *cbrace = 0;
                    string ans =  string(list + inx) + string("\n");
                    sendStr(ans.c_str());
                    inx = cbrace - list;
                }
                daq_clear_channel_list(conn->getDaqd());
            }
            free(list);
        }
        
    }
    else
    {
        cmdErr("No channel names specified.");
    }
}

/**
 * Get the source data from the server this more detailed than available times
 * 
 * Channel types are optional and their presence is determined by matching against the list
 * @param args list of channel names, [<channel type>]
 * @see ServerThread::getSourceData
 * @see Channels::str2chantype
 */
void ServerThread::getSourceData( vector<string> args )
{
    unsigned int i;
    unsigned int n = args.size( );
    if ( n > 1 )
    {
        if ( chan == NULL )
        {
            chan = new Channels( );
        }
        chan->setConn( conn );
        
        daq_channel_t daq_chan[n];
        for(i=0;i<n;i++)
        {
            memset( &( daq_chan[i] ), 0, sizeof (daq_channel_t ) );
        }
        int ochan=0;
        
        int rc = 0;
        i = 1;
        while ( i < args.size( ) && rc == 0 )
        {
            if ( args[i].length( ) < 64 )
            {
                chantype_t ctype = cUnknown;
                string cname = args[i];
                
                if ( i < args.size() - 1 )
                {
                    if (chan->str2chantype(args[i+1].c_str(), &ctype))
                    {
                        i++;
                    }
                }
                i++;
                daq_init_channel( &( daq_chan[ochan] ), cname.c_str( ), ctype , 0.0, _undefined );
                rc = daq_request_channel_from_chanlist( conn->getDaqd( ), &( daq_chan[ochan] ) );
                if ( rc != 0 )
                {
                    string ermsg = string( "daq_request_channel_from_chanlist:" ) + string( daq_strerror( rc ) );
                    cmdErr( ermsg.c_str( ) );
                }
                ochan++;
            }
        }
        if ( rc == 0 )
        {
            char* list = (char *) calloc( 1, (size_t) MAX_SOURCE_LIST );
            long src_len;
            time_t gps = 0;
            rc = daq_recv_source_data( conn->getDaqd( ), list, (size_t) MAX_SOURCE_LIST, gps,
                                       &src_len );
            if ( rc )
            {
                cmdErr( daq_strerror( rc ) );
            }
            else
            {
                long inx;
                sendSuccess( );
                for ( inx = 0; inx < src_len; inx++ )
                {
                    if ( list[inx] == ' ' ) continue;
                    char* cbrace = strchr( list + inx, '}' ) ;
                    if (cbrace != NULL)
                    {
                        cbrace ++;
                        *cbrace = 0;
                        string ans = string( list + inx ) + string( "\n" );
                        sendStr( ans.c_str( ) );
                        inx = cbrace - list;
                    }
                    else
                    {
                        sendStr("\n");
                    }
                }
                daq_clear_channel_list( conn->getDaqd( ) );
            }
            free( list );
        }

    }
    else
    {
        cmdErr( "No channel names specified." );
    }
}

/**
 * Get the source list from the server for more detailed than available times
 * 
 * Channel types are optional and their presence is determined by matching against the list
 * @param args list of channel names, [<channel type>]
 * @see ServerThread::getSourceData
 * @see Channels::str2chantype
 */
void ServerThread::getSourceList( vector<string> args )
{
    unsigned int i;
    unsigned int n = args.size( );
    if ( n > 1 )
    {
        if ( chan == NULL )
        {
            chan = new Channels( );
        }
        chan->setConn( conn );

        daq_channel_t daq_chan[n];
        int rc = 0;
        i = 1;
        while ( i < args.size( ) && rc == 0 )
        {
            if ( args[i].length( ) < 64 )
            {
                chantype_t ctype = cUnknown;
                string cname = args[i];
                memset( &( daq_chan[i - 1] ), 0, sizeof (daq_channel_t ) );
                if ( i < args.size( ) - 1 )
                {
                    if ( chan->str2chantype( args[i + 1].c_str( ), &ctype ) )
                    {
                        i++;
                    }
                }
                i++;
                daq_init_channel( &( daq_chan[i - 1] ), cname.c_str( ), ctype, 0.0, _undefined );
                rc = daq_request_channel_from_chanlist( conn->getDaqd( ), &( daq_chan[i - 1] ) );
                if ( rc != 0 )
                {
                    string ermsg = string( "daq_request_channel_from_chanlist:" ) + string( daq_strerror( rc ) );
                    cmdErr( ermsg.c_str( ) );
                }
            }
        }
        if ( rc == 0 )
        {
            char* list = (char *) calloc( 1, (size_t) MAX_SOURCE_LIST );
            long src_len;
            time_t gps = 0;
            rc = daq_recv_source_list( conn->getDaqd( ), list, (size_t) MAX_SOURCE_LIST, gps,
                                       &src_len );
            if ( rc )
            {
                cmdErr( daq_strerror( rc ) );
            }
            else
            {
                long inx;
                sendSuccess( );
                for ( inx = 0; inx < src_len; inx++ )
                {
                    if ( list[inx] == ' ' ) continue;
                    char* cbrace = strchr( list + inx, '}' ) + 1;
                    *cbrace = 0;
                    string ans = string( list + inx ) + string( "\n" );
                    sendStr( ans.c_str( ) );
                    inx = cbrace - list;
                }
                daq_clear_channel_list( conn->getDaqd( ) );
            }
            free( list );
        }

    }
    else
    {
        cmdErr( "No channel names specified." );
    }
}
/**
 * Retrieve the count for a channel type and send to client
 * @param args optional channel type as first arg
 */
void ServerThread::sendChanCount( vector<string> args )
{
    if ( chan == NULL )
    {
        chan = new Channels( );
    }
    chan->setConn( conn );
    chantype_t chanType = cUnknown;
    if ( args.size( ) > 1 )
    {
        if ( !chan->str2chantype( args[1].c_str( ), &chanType ) )
        {
            string erMsg = string( "Unknown channel type:" ) + args[1];
            cmdErr( erMsg.c_str( ) );
            return;
        }
    }
    int cnt = chan->getChanCount(chanType);
    sendStr("success\n");
    string result = boost::lexical_cast<string>( cnt ) + string("\n");
    sendStr(result.c_str());
}
/**
 * Retrieve the hash of all channels of optionally specified type and send to client
 * 
 * @param args optional type as first argument
 */
void ServerThread::sendChanHash(vector<string> args)
{
    if (chan == NULL)
    {
        chan = new Channels();
    }
    chan->setConn(conn);
    chantype_t chanType = cUnknown;
    if (args.size() > 1)
    {
        if (!chan->str2chantype(args[1].c_str(), &chanType))
        {
            string erMsg = string("Unknown channel type:") + args[1];
            cmdErr(erMsg.c_str());
            return;
        }
    }
    int hash = chan->getChanHash(chanType, (time_t) 0);
    sendSuccess();
    char result[100];
    memset(result,0,sizeof(result));
    sprintf(result,"%x\n", hash);
    sendStr(result);
}
/**
 * Retrieve and send a list of all channels of optionally specifiedtype
 * @param args first argument is type
 */
void ServerThread::sendChanList( vector<string> args )
{
    if ( chan == NULL )
    {
        chan = new Channels( );
    }
    chan->setConn( conn );
    chantype_t chanType = cUnknown;
    if ( args.size( ) > 1 )
    {
        if ( !chan->str2chantype( args[1].c_str( ), &chanType ) )
        {
            string erMsg = string( "Unknown channel type:" ) + args[1];
            cmdErr( erMsg.c_str( ) );
            return;
        }
    }
    chan->getChanList( chanType );
    int nChans = chan->howMany( );
    sendSuccess( );
    int chunksize = 100;
    for ( int idx = 0; idx < nChans; idx += chunksize )
    {
        string result = chan->getListCSV( idx, chunksize );
        sendStr( result.c_str( ) );
    }
}
void ServerThread::sendBufStatus(vector<string> args)
{
    if ( data == NULL )
    {
        cmdErr( "Invalid request for Buffer Status.  Did you call DATA before BSTS?" );
        return;
    }
    string status = data->recvNextBufferInfo();
    sendSuccess();
    sendStr(status);
    if (!boost::starts_with(status, "Error"))
    {
        sendStr("OK\n");
    }
        
}
void ServerThread::nextBuf(vector<string> args)
{
    bool isAlpha = true;
    if (args.size() > 1)
    {
        isAlpha = !iequals(args[1], string("binary"));
    }
    if (data == NULL)
    {
        cmdErr("Invalid request for Next Buffer.  Did you call DATA before NEXT?");
        return;
    }
    double *buf=NULL;
    int n;
    time_t start;
    int rc = data->recvNextBufferDouble(&buf, &n, &start);
    if (rc != 0)
    {
        NDSException ex = NDSException(rc);
        string ermsg = "Error: ";
        ermsg += ex.what();
        ermsg += "\n";
        sendStr(ermsg.c_str());
    }
    else if (buf==0 || n == 0)
    {
        sendStr("Error: no data available");
    }
    else
    {
        sendStr("OK\n");
        if (isAlpha)
        {
            char abuf[500];
            memset(abuf, 0, sizeof (abuf));
            sprintf(abuf, "%d, %ld\n", n, start);
            sendStr(abuf);
            for (int i = 0; i < n; i += 8)
            {
                string line = "";
                for (int j = i; j < n && j < i + 8; j++)
                {
                    float t = buf[j];
                    if (line.length() > 0)
                        line += string(",");
                    line += lexical_cast<string > (t);
                }
                line += string("\n");
                sendStr(line.c_str());
            }
        }
        else
        {
            sendBytes(&start,1,sizeof(start));
            sendBytes(&n,1,sizeof(n));
            sendBytes(buf,n,sizeof(double));
        }
        free(buf);
    }
}

void ServerThread::reqData(vector<string> args)
{
    int argc = args.size();
    if (argc < 5 || argc > 7)
    {
        cmdErr("Invalid call to request data.  You need 5, 6, or 7 arguments");
        return;
    }

    if (chan == NULL)
    {
        chan = new Channels();
        chan->setConn(conn);
    }
    if (data == NULL)
    {
        data = new NDSData();
        data->setVerbose(ServerThread::verbose);
        data->setConnection(conn);
    }
    string chName = args[1];
    chantype_t chanType = cUnknown;
    if (!chan->str2chantype(args[2].c_str(), &chanType))
    {
        string erMsg = string("Unknown channel type:") + args[1];
        cmdErr(erMsg.c_str());
        return;
    }
    time_t start = atol(args[3].c_str());
    time_t end = atol(args[4].c_str());
    time_t dt = 0;
    double rate = 0;
    if (argc > 5)
        dt = atol(args[5].c_str());
    if (argc > 6)
        rate = atof(args[6].c_str());
    // add channel to request list
    int rc;
    if (dt == 0)
    {
        cmdErr("Stride cannot be zero.");
        return;
    }
    rc = data->reqChanData(chName.c_str(), chanType, rate, start, end, dt);
    
    if (rc == 0)
        sendStr("ok\n");
    else
    {
        sendRcMsg(rc);
    }
}

void ServerThread::sendRcMsg(int rc)
{
    NDSException ex = NDSException(rc); // we're not throwing it, just using it to get the error message
    string ermsg =  string("Error: ");
    ermsg += ex.what()+ string("\n");
    sendStr(ermsg.c_str() );
    cerr << getCurTime() << ermsg;
}
/**
 * A success message is sent before commands that that send back variable number of lines
 * so the client can consume the first line to test for error/success.
 */
void ServerThread::sendSuccess()
{
    sendStr("success\n");
}
void ServerThread::sendHelp()
{
    string helpMsg = "All commands have the form:\n<command>, [<argument> [,<argument>...]\n";
    helpMsg += "Commands are not case sensitive.\n";
    helpMsg += "Quotes (\"), and commas (,) should be escaped with a backslash(\\)\n";
    helpMsg += "\n";
    helpMsg += "ATIM, <channel name>, [<channel name>...] - get times of available data\n";
    helpMsg += "BSTS - buffer status info (after DATA command): name, gps start, rate(Hz), size (bytes), data type\n";
    helpMsg += "BYE or EXIT or QUIT - close this connection, disconnect from NDS server if necessary\n";
    helpMsg += "CONN, <server name or ip address>[,<port>] - connect to a NDS server, must have valid ticket\n";
    helpMsg += "CHLS, [<channel type>] - return all channels of that type as a CSV list, default=all\n";
    helpMsg += "CHSH, [<channel type>] - return a hash (hex) of the corresponding channel list, default=all\n";
    helpMsg += "CHCT, [<channel type>] - return count of channels of that type, default=all\n";
    helpMsg += "DATA, <channel name>, <channel type>, <gps start time>, <gps end time> [,<buffer size>[,<rate>]] \n";
    helpMsg += "      - REQUEST data to be transferred, use NEXT to get the data\n";
    helpMsg += "DCON -  disconnect from NDS server but leave this connection open\n";
    helpMsg += "SRCD, <channel name>, [<channel type>] [,<channel name>, [<channel type>] ... ] - detailed source data of frames\n" ;
    helpMsg += "SRCL, <channel name>, [<channel type>] [,<channel name>, [<channel type>] ... ] - source list of frames\n" ;
    helpMsg += "NEXT, [ALPHA|BINARY] - get next buffer, default is alpha. Only one channel can be specified.\n";

    helpMsg += "\n";
    helpMsg += "Known channel types are: unknown, online, raw, RDS, second-trend, minute-trend, test-point, static.\n";
    sendStr(helpMsg.c_str());
}
string ServerThread::getCurTime()
{
    time_t now;
    time(&now);
    char nowstr[64];
    memset(nowstr, 0, sizeof (nowstr));
    strftime(nowstr, sizeof (nowstr) - 1, "%a %b %d, %Y %H:%M:%S %Z - ", localtime(&now));

    return string(nowstr);
}

void ServerThread::sayBye()
{
    sendStr( "So long.\n" );
    vector<string> args;
    disconnect( args );
    connected = false;
    if ( verbose )
    {
        cout << getCurTime( ) << " Connection " << fd << " closed." << endl;
    }
    if (fcntl(fd, F_GETFD) != -1 && errno != EBADF)
    {
        int crc = close( fd );
        if ( crc < 0 )
        {
            cerr << getCurTime( ) << " Error on close client connection ";
            cerr << errno << ". " << strerror( errno );
        }
    }
    int rc2 = shutdown( fd, SHUT_RDWR );
    if (rc2 < 0 )
    {
        cerr << getCurTime( ) << " Error on shutdown client connection ";
        cerr << errno << ". " << strerror( errno );   
    }
    close( fd );
}
bool ServerThread::checkConn()
{
    bool ret = true;
    if ( conn == NULL  || !connected )
    {
        cmdErr( "You must connect to a server before issuing that command." );
        ret = false;;
    }
    return ret;
}