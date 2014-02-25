/* 
 * File:   LdvwFilter.cpp
 * Author: joe
 * 
 * Created on April 12, 2013, 12:36 AM
 */

#include "LdvwFilter.h"

#include <ios>

#include <boost/program_options.hpp>



namespace po = boost::program_options;


LdvwFilter::LdvwFilter( )
{
    filtTypes["high"] = kHighPass;
    filtTypes["low"] = kLowPass;
    filtTypes["band"] = kBandPass;
    filtTypes["notch"] = kBandStop;
}

LdvwFilter::LdvwFilter( const LdvwFilter& orig )
{
    
}

LdvwFilter::~LdvwFilter( )
{
}

int LdvwFilter::processArgs(int argc, char **argv)
{
    int opt;
    po::options_description desc("Allowed options");
    desc.add_options()
            ("help", "produce this message")
            ("in", po::value< string >(), "input file default = stdin")
            ("out", po::value< string >(), "output file default = stdout")
            ("bin", "read/write binary doubles, default is ascii one per line")
            ("filt", po::value< string >(), "filter type (high, low, band, notch)")
            ("cutoff", po::value< vector< float > >(), "cutoff value(s) in Hz")
            ("order",  po::value< int >(), "order for butterworth default = 5")
            ("fs",po::value< float >(), "sample frequency" )
            ;
    
    po::variables_map vm;
    po::store( po::parse_command_line( argc, argv, desc ), vm );
    po::notify( vm );

    string erMsg = "";
    bin = vm.count("bin") > 0;
    if (vm.count("in"))
    {
        
        
        inFile = vm["in"].as<string>();
        ios::openmode mode = bin ? ios::in | ios::binary : ios::in ;
        
        ins.open(inFile.c_str(), mode);
        if (ins == NULL)
        {
            erMsg += "Input file (" + inFile + ") does not exist.\n";
        }
        
    }
    
    if (vm.count("out"))
    {
        
        outFile = vm["out"].as<string>();
        ios::openmode mode = bin ? ios::out | ios::binary : ios::out ;
        outs.open(outFile.c_str(), mode);
        if (outs == NULL)
        {
            erMsg += "Can't open output file (" + outFile + ") for writing.\n";
        }
        
    }
    
    if (vm.count("filt"))
    {
        filtTypeStr = vm["filt"].as<string>();
        if ( filtTypes.count( filtTypeStr ) == 0)
        {
            erMsg += "Unknown filter type (" + filtTypeStr + ")\n";
        }
        else
        {
            filtType = filtTypes[filtTypeStr];
        }
    }
    else
    {
        erMsg += "No filter type specified.\n";
    }
    if (vm.count("cutoff"))
    {
        cutoff = vm["cutoff"].as< vector <float> >();
    }
    else
    {
        erMsg += "No cutoff values specified.\n";
    }
    if (vm.count("order"))
    {
        order = vm["order"].as<int>();
    }
    else
    {
        order = 5;
    }
    if (vm.count("fs"))
    {
        fs = vm["fs"].as<float>();
    }
    else
    {
        erMsg += "No sample frequency (fs) specified.\n";
    }
    int ret = 0;
    if ( vm.count( "help" ) || erMsg.length() > 0 )
    {
        if (erMsg.length() > 0)
        {
            cout << "Error: " << erMsg << "\n\n";
        }
        cout << argv[0] << " is a command line filter tool \n";
        cout << desc << "\n";
        ret = 1;
    }
    return ret;
}

int LdvwFilter::filterFile()
{
    readData();
    filterIt();
    writeData();
}

int LdvwFilter::readData()
{
    if (bin)
    {
        
    }
    else
    {
        double val;
        string line;
        if (inFile.length() > 0)
        {
            double val;
            while ( ins >> val )
            {
                inData.push_back( val );
            }
        }
        else
        {
            double val;
            while(cin >> val)
            {
                inData.push_back(val);
            }
        }
    }
}

int LdvwFilter::writeData( )
{
    if ( bin )
    {

    }
    else
    {
        for(long i=0;i<outData.size(); i++)
        {
            cout << outData[i] << "\n";
        }
    }
}

int LdvwFilter::filterIt()
{
    long n = inData.size();
    long gps = 0;
    long usec = 0;
    
    TSeries::size_type ns = n;
    Time *time = new Time( gps, usec );

    double *data = &inData[0];
    double dlen = n/fs;
    long sec = (int)dlen;
    usec = (int) ((dlen-sec)*1000000);
    Interval *interval = new Interval( sec, usec );
    
    TSeries *ts = new TSeries( *time, *interval, ns, data );

    FilterDesign *fd = new FilterDesign( fs, "test filter" );
    fd->butter( filtType, 4, 100 );

    FSeries coef;
    int tfSize = fs > 100 ? fs/2 : 128;
    
    
    double tf[tfSize];
    fd->Xfer( coef, 0., fs / 2, 1 );
    fComplex tfVal[tfSize];
    coef.getData( tfSize, tfVal );

    for ( int i = 0; i < tfSize; i++ )
    {
        tf[i] = tfVal[i].Mag( );
    }
    Pipe *f = fd->release( );

    TSeries t2 = f->apply( *ts );

    double *dataOut = (double *) calloc(n,sizeof(double));
    t2.getData( n, dataOut );
    
    for(long i=0;i<n;i++)
    {
        outData.push_back(dataOut[i]);
    }
}