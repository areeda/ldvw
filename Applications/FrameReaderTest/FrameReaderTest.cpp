/* 
 * File:   FrameReaderTest.cpp
 * Author: joe
 *
 * Created on July 27, 2013, 8:01 AM
 */

#include <cstdlib>

#include <stdlib.h>
#include <iostream>
#include <iomanip>
#include <fstream>
#include <boost/regex.hpp>
#include <boost/algorithm/string.hpp>
#include <boost/algorithm/string/trim.hpp>
#include <boost/lexical_cast.hpp>
#include <getopt.h>
#include <time.h>

#include <FrameL.h>

#include "FrameReaderTest.h"

using namespace std;
using namespace boost;

vector<string> fname;
vector<string> chanNames;
vector<string> trends;

long plotStart = 0;
bool isGpsTime;
double lastPoint;
bool addTimeToFilename;
bool raw=false;
bool useInt=false;
bool verbose=false;

double timeFact;
string outDir;
long totData=0;

/*
 * 
 */
int main( int argc, char** argv )
{
    time_t pstart = time(NULL);
    
    trends.push_back(".min");
    trends.push_back(".mean");
    trends.push_back(".max");
    setup(argc, argv);
    
    string matcher = ".*\\-([0-9]+)\\-([0-9]+).gwf";
    regex expression( matcher);
    int dur = 0;

    // find duration of the frames
    for (int f = 0; f < fname.size(); f++) 
    {
        cmatch what;
        if (regex_match(fname[f].c_str(), what, expression)) 
        {
            string strtString = what[1];
            string durString = what[2];
            long tStart = lexical_cast<long> (strtString);

            if (plotStart == 0) 
            {
                plotStart = tStart;
                lastPoint = tStart;
            }
            dur += lexical_cast<int> (durString);
        }
    }
    ostringstream timeStr;
    timeStr << plotStart << "-" << dur;
    
    // create the output files
    int nChans = chanNames.size();
    ofstream *oFiles[nChans];
    if (outDir.length() >0 && outDir[outDir.length()-1] != '/')
    {
        outDir += string("/");
    }
    for ( int c = 0; c < chanNames.size(); c++ )
    {
        string oname = outDir + chanNames[c];
        if (addTimeToFilename)
        {
            oname += "-" + timeStr.str();
        }
        oname += string(".dat");
        oFiles[c] = new ofstream(oname.c_str(),ios::out | ios::binary);
    }
    for(int f=0;f < fname.size(); f++)
    {
        cmatch what;
        if ( regex_match( fname[f].c_str(), what, expression ) )
        {
            string strtString = what[1];
            string durString = what[2];
            long tStart = lexical_cast<long> (strtString);
            
            if (plotStart == 0)
            {
                plotStart = tStart;
            }
            
            long tLength = lexical_cast<long> (durString);
        
            if (verbose)
            {
                cout << "Working on frame " ;
                cout << f + 1 << " of " << fname.size() << " gps: " << tStart ;
                cout << " cnt: ";
            }
            FrFile *iFile = FrFileINew( (char*)fname[f].c_str() );
            int nVect = raw ? 1 : trends.size();
            FrVect *vects[nVect];
            int npts = 0;
            FrVect *frInfo = FrFileIGetFrameInfo( iFile, tStart, tLength );
            for(int c=0;c < chanNames.size(); c++)
            {
                string name = chanNames[c];
                
                if (raw)
                {
                    vects[0] = FrFileIGetVectD( iFile, (char *)name.c_str(), tStart, tLength );
                    if ( vects[0] != NULL )
                    {
                        totData += vects[0]->nData;
                        npts += vects[0]->nData;
                    }
                }
                else
                {
                    for(int t=0; t < trends.size(); t++)
                    {
                        string chanName = name + trends[t];

                        vects[t] = FrFileIGetVectD( iFile, (char *)chanName.c_str(), tStart, tLength );
                        if ( vects[t] != NULL )
                        {
                            totData += vects[t]->nData;
                            npts += vects[t]->nData;
                        }
                    }
                }
                if (useInt)
                {
                    writeIVect(oFiles[c],vects[0]);
                }
                else
                {
                    writeVects(oFiles[c], vects, nVect);
                }
                
                
                
            }
            FrFileIEnd(iFile);
            if ( verbose )
            {
                cout << npts << endl;
            }
        }
    }
    for ( int c = 0; c < chanNames.size(); c++ )
    {
        oFiles[c]->close( );
    }
    double elapsed = difftime(time(NULL), pstart);
    double trate = totData/elapsed;
    cout << "Elapsed: " << setiosflags(ios::fixed) << setprecision(1) << elapsed << " sec, samples: " << totData;
    cout << ", xfer rate: " << setiosflags(ios::fixed) << setprecision(1) << trate << " samples/sec" << endl;
    return 0;
}

void setup( int argc, char** argv )
{
    static struct option long_options[] =
    {
                                          /* These options set a flag. */
        {"help",    no_argument, 0, 'h'},
        {"int",     no_argument, 0, 'i'},
        {"raw",     no_argument, 0, 'r'},
        {"time",    no_argument, 0, 't'},
        {"verbose", no_argument, 0, 'v'},
                                          /* These options don't set a flag.
           We distinguish them by their indices. */
        {"chan",  required_argument, 0, 'c'},
        {"frame", required_argument, 0, 'f'},
        {"outdir",required_argument, 0, 'o'},
        {"unit",  required_argument, 0, 'u'},
        {0, 0, 0, 0}
    };
    int c;
    int idx=0;
    
    string frameList;
    string chanList;
    string unit;
    bool doHelp = false;
    timeFact = 1;
    
    while ( ( c = getopt_long( argc, argv, "f:c:rihv", long_options, &idx  ) ) != -1 )
    {
        switch ( c )
        {
            case 'c':
                chanList = string(optarg);
                break;
                
            case 'f':
                frameList = string(optarg);
                break;
            
            case 'h':
                doHelp = true;
                break;
                
            case 'i':
                useInt = true;
                break;
                
            case 'o':
                outDir = string(optarg);
                break;
                
            case 'r':
                raw = true;
                break;
                
            case 't':
                addTimeToFilename = true;
                break;
                
            case 'u':
                unit = string(optarg);
                break;
                
            case 'v':
                verbose = true;
                break;
                
            case 0:
                /* getopt_long() set a variable, just keep going */
                break;
                
        }
    }
    if (!doHelp)
    {
        if (frameList.length() > 0 && chanList.length() > 0)
        {
            fname = getList(frameList);
            chanNames = getList(chanList);
        }
        else
        {
            doHelp = true;
        }
        isGpsTime = false;
        if (unit.length() > 0)
        {
            switch (unit[0])
            {
                case 's':
                case 'g':
                    timeFact = 1.;
                    isGpsTime = true;
                    break;
                case 'm':
                    timeFact = 60.;
                    break;
                case 'h':
                    timeFact = 3600.;
                    break;
                case 'd':
                    timeFact = 24 * 3600.;
                    break;
                case 'w':
                    timeFact = 7 * 24 * 3600.;
                    break;
                default:
                    cerr << "Unknown time units: " << unit << endl;
                    doHelp = true;
            }
        }
    }
    if (chanNames.size() == 0 || fname.size() == 0 )
    {
        cout << "Empty channel list or frame file list" << endl;
        cout << "framereadertest [--help] [--verbose] [--raw] --frame <file> --chan <file>" << endl;
        cout << "                [--int (output value as long] [--unit <gsmhdw> (gps,sec, min, hr, day, week)]" << endl;
        cout << "                [--outdir <path>] [--unit <gsmhdw> (gps,sec, min, hr, day, week)]" << endl;
        cout << "   unit: s: sec, m: min, h: hr, d: day, w: week" << endl;
        exit(2);
    }
}
vector<string> getList(string fname)
{
    vector<string> list;
    
    ifstream cin( fname.c_str( ) );
    if ( cin.is_open( ) )
    {
        string line;
        while ( !cin.eof( ) )
        {
            getline( cin, line );
            if (line.length() > 0)
            {
                list.push_back( line );
            }
        }
    }
    return list;
}
/**
 * Write one or more floating point vectors to the file
 * @param oFile the output stream
 * @param vects array of vectors 0=time 1-n = data
 * @param nvect number of vectors (including time ie n+1)
 */
void writeVects(ofstream *oFile,FrVect **vects, int nvect )
{
    float outData[nvect+1];
    bool gotData= true;
    for(int t=0;t<nvect; t++)
    {
        gotData &= vects[t] != NULL;
    }
    if (gotData)
    {
        int nData = vects[0]->nData;
        double dx = *(vects[0]->dx);
        double strt = vects[0]->GTime;

        for(int x=0; x<nData; x++)
        {
            for(int i=0; i<nvect; i++)
            {
                if (i==0)
                {   // one time value per row
                    outData[0]=(strt+x*dx);
                    outData[0] -= isGpsTime ? 0 : plotStart;
                    outData[0] /= timeFact;
                }
                outData[i+1] = vects[i]->dataD[x];
            }
            oFile->write((char*)&outData, (nvect+1) * sizeof(float));
        }
        for ( int i = 0; i < nvect; i++ )
        {
            FrVectFree(vects[i]);
        }
    }
}

void writeIVect(ofstream *oFile, FrVect *vect)
{
    if (vect != NULL)
    {
        int nData = vect->nData;
        double dx = *(vect->dx);
        double strt = vect->GTime;
        double t;
        long v;
        for ( int x = 0; x < nData; x++ )
        {
            t = ( strt + x * dx );
            t -= isGpsTime ? 0 : plotStart;
            t /= timeFact;
            v = (long)vect->dataD[x];
            oFile->write( (char*) &t, sizeof (double) );
            oFile->write( (char*) &v, sizeof(long));
        }
        FrVectFree( vect);
    }
}
