/* 
 * File:   LdvwFilter.h
 * Author: joe
 *
 * Created on April 12, 2013, 12:36 AM
 */

#ifndef LDVWFILTER_H
#define	LDVWFILTER_H

#include <string>
#include <iostream>
#include <istream>
#include <ostream>
#include <fstream>
#include <vector>

#include <boost/unordered_map.hpp>

#include "gds/TSeries.hh"
#include "gds/FilterDesign.hh"
#include "gds/Interval.hh"
#include "gds/FilterIO.hh"
#include "gds/Time.hh"
#include "gds/FSeries.hh"
#include "gds/Complex.hh"

using namespace std;

class LdvwFilter
{
public:
    LdvwFilter();
    LdvwFilter(const LdvwFilter& orig);
    virtual ~LdvwFilter();
    
    int filterFile();
    int processArgs(int argc, char **argv);
private:
    bool bin;                   /// input/output is binary
    string inFile;
    string outFile;
    boost::unordered_map<string,Filter_Type> filtTypes;
    string filtTypeStr;            /// see allowable types
    Filter_Type filtType;
    vector<float> cutoff;
    int order;                  /// order of butterworth filter
    float fs;                   /// sample frequency
    
    ifstream ins;
    ofstream outs;
    vector<double> inData;      /// data as read
    vector<double> outData;     /// filtered data
    
    int filterIt();
    int readData();
    int writeData();
};

#endif	/* LDVWFILTER_H */

