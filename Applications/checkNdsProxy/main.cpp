/* 
 * File:   main.cpp
 * Author: joe
 *
 * Created on May 24, 2013, 6:35 AM
 */

#include <cstdlib>

using namespace std;

/*
 * 
 */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 

void error(const char *msg)
{
    perror(msg);
    exit(2);
}

int main(int argc, char *argv[])
{
    bool verbose=false;
    
    int sockfd, portno, n;
    struct sockaddr_in serv_addr;
    struct hostent *server;

    char buffer[256];
    portno = 31300;
    
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) 
    {
        error("ERROR opening socket");
    }
    struct timeval timeout;
    timeout.tv_sec = 10;
    timeout.tv_usec = 0;

    if ( setsockopt( sockfd, SOL_SOCKET, SO_RCVTIMEO, (char *) &timeout,
                     sizeof (timeout ) ) < 0 )
        error( "setsockopt timeout failed\n" );

    if ( setsockopt( sockfd, SOL_SOCKET, SO_SNDTIMEO, (char *) &timeout,
                     sizeof (timeout ) ) < 0 )
        error( "setsockopt timeout failed\n" );
    server = gethostbyname("localhost");
    if (server == NULL) 
    {
        fprintf(stderr,"ERROR, no such host\n");
        exit(1);
    }
    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;
    bcopy((char *)server->h_addr, 
         (char *)&serv_addr.sin_addr.s_addr,
         server->h_length);
    serv_addr.sin_port = htons(portno);
    if (connect(sockfd,(struct sockaddr *) &serv_addr,sizeof(serv_addr)) < 0) 
    {
        error("ERROR connecting");
    }
    
    bzero(buffer,256);
    strcpy(buffer,"bye");
    n = write(sockfd,buffer,strlen(buffer));
    if (n < 0) 
    {
         error("ERROR writing to socket");
    }
    bzero(buffer,256);
    n = read(sockfd,buffer,255);
    if (n < 0) 
    {
         error("ERROR reading from socket");
    }
    close(sockfd);
    if (verbose)
    {
        printf("OK\n");
    }
    exit(0);
}

