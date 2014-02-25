/* 
 * File:   ProxyServer.cpp
 * Author: joe
 * 
 * Created on March 26, 2012, 3:57 PM
 */
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <errno.h>
#include <unistd.h>
#include <signal.h>

#include <iostream>

#include "ProxyServer.h"
#include "ServerThread.h"

ProxyServer::ProxyServer()
{
    verbose = true;
    curThread = 0;
    noThreads=false;
}

ProxyServer::ProxyServer(const ProxyServer& orig)
{
}

ProxyServer::~ProxyServer()
{
    freeaddrinfo(servinfo); // free the linked-list
}

void ProxyServer::Listen()
{
    int status=0;
    struct addrinfo hints;
    signal(SIGPIPE, SIG_IGN);
    
    memset(&hints, 0, sizeof hints); // make sure the struct is empty
    hints.ai_family = AF_UNSPEC; // don't care IPv4 or IPv6
    hints.ai_socktype = SOCK_STREAM; // TCP stream sockets
    hints.ai_flags = AI_PASSIVE; // fill in my IP for me
    string ermsg = "";
    if ((status = getaddrinfo("127.0.0.1", "31300", &hints, &servinfo)) != 0)
    {
        ermsg = gai_strerror(status);
    }

    if (status == 0)
    {
        // servinfo now points to a linked list of 1 or more struct addrinfos
        sock = socket(servinfo->ai_family, servinfo->ai_socktype, servinfo->ai_protocol);
        if (sock == -1)
        {
            status = errno;
            ermsg = strerror(errno);
        }
        int on = 1;
        int ret = setsockopt( sock, SOL_SOCKET, SO_REUSEADDR, &on, sizeof (on ) );
        if (ret < 0)
        {
            status = errno;
            ermsg = strerror( errno );            
        }
    }
    if (status == 0)
    {
        status = bind(sock,servinfo->ai_addr, servinfo->ai_addrlen);
        if (status == -1)
        {
            status = errno;
            if ( status == EADDRINUSE )
            { // if the socket listener dies at an inoportune time it is locked for a while
                for ( int tries = 0; tries < 10 && status == EADDRINUSE; tries++ )
                {
                    cout << "Socket in use will retry in 10 seconds." << endl;
                    sleep( 10 );
                    status = bind(sock,servinfo->ai_addr, servinfo->ai_addrlen);
                    if (status != 0)
                        status = errno;
                }
            }
            if (status != 0)
            ermsg = strerror(errno);
        }
    }
    if (status == 0)
    {
        status = listen(sock, maxThreads);
        
        if (status == -1)
        {
            status = errno;
            ermsg = strerror(errno);
        }
    }
    if (status == 0)
    {
        serveItUp(sock);
    }

    else
    {
        printf("Error: %d - %s", status, ermsg.c_str());
        return;
    } 

}
void ProxyServer::serveItUp(int sock) throw(NDSException)
{
    
    cout << getCurTime() << "Proxy is accepting connections." << endl;
    while(1)
    {
        struct sockaddr addr;
        socklen_t addrLen = sizeof(addr);
        int fd = accept(sock, &addr, &addrLen);
        if (fd == -1)
        {
            throw NDSException(errno, strerror(errno));
        }
        char peer[INET6_ADDRSTRLEN];
        memset(peer,0,sizeof(peer));
        
        inet_ntop(addr.sa_family, get_in_addr(&addr), peer, sizeof(peer));
        int in_port = get_in_port(&addr);
        
        if (verbose) 
            cout << getCurTime() << "Connected to " <<  peer << " on port " << in_port <<  " as fd: " << fd << endl;
        
        void *pport = &fd;
        int rc;
        ServerThread::setVerbose(verbose);
        if (noThreads)
        {
            ServerThread::setNoThreads(true);
            ServerThread::threadHelper(pport);
            break;
        }
        else
        {
            ServerThread::setNoThreads(false);
            rc = pthread_create(&threads[curThread], NULL, &ServerThread::threadHelper,  pport);
        }
        if (rc)
        {
            cerr << getCurTime() << rc << strerror(rc) << endl;
        }
        
    }
}
// get sockaddr, IPv4 or IPv6:

void *ProxyServer::get_in_addr(struct sockaddr *sa)
{
    void *ret = &(((struct sockaddr_in6*) sa)->sin6_addr);
    if (sa->sa_family == AF_INET)
        ret = &(((struct sockaddr_in*) sa)->sin_addr);
    
    return ret;
}

unsigned short ProxyServer::get_in_port(struct sockaddr *sa)
{
    unsigned short ret = (((struct sockaddr_in6*) sa)->sin6_port);
    if (sa->sa_family == AF_INET)
        ret = (((struct sockaddr_in*) sa)->sin_port);
    
    ret = ntohs(ret);

    return ret;
}

string ProxyServer::getCurTime()
{
    time_t now;
    time(&now);
    char nowstr[64];
    memset(nowstr, 0, sizeof (nowstr));
    strftime(nowstr, sizeof (nowstr) - 1, "%a %b %d, %Y %H:%M:%S %Z - ", localtime(&now));

    return string(nowstr);
}