/* 
 * File:   main.cpp
 * Author: joe
 *
 * Created on April 12, 2013, 12:34 AM
 */

#include <cstdlib>

#include "LdvwFilter.h"

using namespace std;

/*
 * 
 */
int main( int argc, char** argv )
{
    int status = 1; // assume an error, only if filter returns 0 do we get success
    
    LdvwFilter me;
    if (me.processArgs(argc, argv) == 0)
    {
        status = me.filterFile();    // do it
    }
    
    return status;
}

