/* 
 * File:   TimeInterval.cpp
 * Author: joe
 * 
 * Created on July 9, 2014, 8:26 AM
 */

#include "TimeInterval.h"
#include <boost/algorithm/string.hpp>
#include "NdsException.h"

TimeInterval::TimeInterval( )
{
    startGps = 0;
    stopGps=0;
    epochName="";
}

TimeInterval::TimeInterval( const TimeInterval& orig )
{
    startGps = orig.startGps;
    stopGps = orig.stopGps;
    epochName = orig.epochName;
}

TimeInterval::~TimeInterval( )
{
}

TimeInterval::TimeInterval(string epoch)
{
    string ep=epoch;
    boost::algorithm::to_upper(ep);
    if (ep.compare("S5") == 0)
    {
        startGps = 815153408;
        stopGps = 880920032;
    }
    else if (ep.compare("s6") == 0)
    {
        startGps = 930960015;
        stopGps = 971654415;
    }
    else if (ep.compare("S6a") == 0 )
    {
        startGps = 930960015;
        stopGps = 935798415;
    }
    else if (ep.compare("S6b") == 0)
    {
        startGps = 937785615;
        stopGps = 947203215;
    }
    else if (ep.compare("S6c") == 0)
    {
        startGps = 947635215;
        stopGps = 961545615;
    }
    else if (ep.compare("S6d") == 0)
    {
        startGps = 961545615;
        stopGps = 971654415;
    }
    else if (ep.compare("ER2") == 0)
    {
        startGps = 1025636416;
        stopGps = 1028563232;
    }
    else if (ep.compare("ER3") == 0)
    {
        startGps = 1042934416;
        stopGps = 1045353616;
    }
    else if (ep.compare("ER4") == 0)
    {
        startGps = 1057881616;
        stopGps = 1061856016;
    }
    else if (ep.compare("ER5") == 0)
    {
        startGps = 1073606416;
        stopGps = 1078790416;
    }
    else
    {
        throw NdsException("Unknown epoch specified");
    }
    epochName = epoch;
}
TimeInterval::TimeInterval(long start, long stop)
{
    startGps = start;
    stopGps = stop;
    epochName = "";
}
long TimeInterval::getDuraton( )
{
    return stopGps - startGps;
}
long TimeInterval::getStartGps( )
{
    return startGps;
}
long TimeInterval::getStopGps( )
{
    return stopGps;
}
string TimeInterval::getEpochName( )
{
    return epochName;
}

