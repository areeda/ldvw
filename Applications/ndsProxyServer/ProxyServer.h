/* 
 * File:   ProxyServer.h
 * Author: joe
 *
 * Created on March 26, 2012, 3:57 PM
 */

#ifndef PROXYSERVER_H
#define	PROXYSERVER_H

#include <pthread.h>
#include "NDSException.h"

class ProxyServer
{
public:
    ProxyServer();
    ProxyServer(const ProxyServer& orig);
    virtual ~ProxyServer();

    static string getCurTime();
    void Listen();
    void setNoThreads(bool n) { noThreads = n; };
    void setVerbose(bool v){verbose=v;};
    
private:
    struct addrinfo *servinfo; // Address information on our server port
    int sock;   // The socket we're serving
    bool verbose;
    bool noThreads;
    static const int maxThreads = 5;
    pthread_t threads[maxThreads];
    int curThread;
    
    
    void *get_in_addr(struct sockaddr *sa);
    unsigned short get_in_port(struct sockaddr *sa);
    void serveItUp(int fd) throw(NDSException);
};

#endif	/* PROXYSERVER_H */

