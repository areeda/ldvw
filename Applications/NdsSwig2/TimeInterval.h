/* 
 * File:   TimeInterval.h
 * Author: joe
 *
 * Created on July 9, 2014, 8:26 AM
 */

#ifndef TIMEINTERVAL_H
#define	TIMEINTERVAL_H

#include <time.h>
#include <string>
using namespace std;
        
class TimeInterval
{
public:
    TimeInterval();
    TimeInterval(string epoch) ;
    TimeInterval(long start, long stop);
    TimeInterval(const TimeInterval& orig);
    virtual ~TimeInterval();
    long getDuraton();
    long getStartGps();
    long getStopGps();
    string getEpochName();
private:
    time_t startGps;
    time_t stopGps;
    string epochName;
};

#endif	/* TIMEINTERVAL_H */

