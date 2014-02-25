/**  A fairly random collection of utility functions
 * 
 */

#include "Utils.h"
#include <string.h>



void gettime(timespec *tp)
{


    struct timeval tv;
    gettimeofday(&tv, NULL);
    tp->tv_sec = tv.tv_sec;
    tp->tv_nsec = tv.tv_usec * 1000;

}

string getCurTime()
{
    time_t now;
    time(&now);
    char nowstr[64];
    memset(nowstr, 0, sizeof (nowstr));
    strftime(nowstr, sizeof (nowstr) - 1, "%a %b %d, %Y %H:%M:%S %Z - ", localtime(&now));

    return string(nowstr);
}
