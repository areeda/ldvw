/* 
 * File:   NDSData.cpp
 * Author: joe
 * 
 * Created on March 19, 2012, 9:35 PM
 */


#include "NDSData.h"
//#include "daqc_internal.h"
#include <cstdio>
#include <string.h>
#include <math.h>
#include <stdlib.h>


NDSData::NDSData()
{
    conn = NULL;
}

NDSData::NDSData(const NDSData& orig)
{
    conn = orig.conn;
    chanName = orig.chanName;
    start = orig.start;
    end = orig.end;
    dt = orig.dt;
    dtype = orig.dtype;
    req = orig.req;
}

NDSData::~NDSData()
{
}

/**
 * Initialize our connection
 * @param c - connection object, must have been open and authorization completed
 */
void NDSData::setConnection(NDSConnection *c)
{
    conn = c;
}

int NDSData::reqChanData(const char *name, chantype_t ctype, double rate, time_t start, time_t end, time_t dt)
{
    int rc = daq_request_channel(conn->getDaqd(), name, ctype, rate);
    if (rc == 0)
    {
        time_t stop = end == 0 ? dt : end;
        rc = daq_request_data(conn->getDaqd(), start, stop, dt);
    }
    if (rc==0)
        chanName = name;
    return rc;
}

int NDSData::recvNextBufferDouble(double **buf, int *n, time_t *start)
{
    int rc;
    rc = daq_recv_next(conn->getDaqd());

    if (rc == 0)
    {
        daq_t *daq = conn->getDaqd();
        chan_req_t* stat = daq_get_channel_status(daq, chanName.c_str());
        
        
        time_t gps = daq_get_block_gps(daq);
        
        int size = daq_get_data_length(daq, chanName.c_str());


        const void *b =  daq_get_channel_addr(daq, chanName.c_str());
        
        
        *start = gps;
        double *dp;
        const short *shortp;
        const int *intp;
        const float *floatp;
        const long *longp;
        const double *doublep;
        
        switch(stat->data_type)
        {
            case _undefined:
                *buf = NULL;
                *n = 0;
                dp = 0;
                break;

            /** 16-bit (short) integer data. */
            case _16bit_integer:
                *n = size/2;
                dp = (double*)calloc(*n,sizeof(double));
                shortp = (short*)b;
                for(int i=0;i<*n;i++)
                    dp[i] = (double)shortp[i];
                break;

            /** 32-bit (int) integer data. */
            case _32bit_integer:
                *n = size / 4;
                dp = (double*)calloc(*n,sizeof(double));
                if (chanName.find("ODC") == string::npos)
                {
                    intp = (int*) b;
                    for(int i=0;i<*n;i++)
                        dp[i] = (double) intp[i];
                }
                else
                {

                    unsigned *uintp;
                    uintp = (unsigned *)b;
                    for(int i=0; i < *n; i++)
                        dp[i]=(double) uintp[i];
                }
                break;

            /** 64-bit (long) integer data. */
            case _64bit_integer:
                *n = size / 8;
                dp = (double*)calloc(*n,sizeof(double));
                longp = (long*) b;
                for(int i=0;i<*n;i++)
                    dp[i] = (double) longp[i];
                                break;

            /** 32-bit (float) floating point data. */
            case _32bit_float:
                *n = size/4;
                dp = (double*)calloc(*n,sizeof(double));
                floatp = (float*)b;
                for(int i=0;i<*n;i++)
                    dp[i] = floatp[i];
                break;

            /** 64-bit (double) floating point data. */
            case _64bit_double:
                *n = size / 8;
                dp = (double*)calloc(*n,sizeof(double));
                doublep = (double*) b;
                for(int i=0;i<*n;i++)
                    dp[i] = doublep[i];
                break;

            /** Complex data from two 32-bit floats {re, im}. */
            case _32bit_complex:
                *n = size / 8;
                dp = (double*)calloc(*n,sizeof(double));
                floatp = (float*) b;
                for (int i = 0; i<*n; i+=2)
                    dp[i] = (double) sqrt(floatp[i] * floatp[i] + floatp[i+1] * floatp[i+1]);
                break;
        }
        *buf = dp;
        if (verbose && rc == 0 && dp != NULL)
        {
            printf("buf: at %ld, size: %d bytes, %d values ", gps, stat->status, *n);
            
            float mean = 0;
            int i;
            for (i = 0; i < *n; i++)
                mean += dp[i];
            mean /= *n;
            printf("mean: %.3f - ", mean);
            for (i = 0; i < 5; i++)
                printf("%.8e, ", dp[i]);
            printf("\n");
        }
        if (dp == 0)
        {
            rc = -1;
        }
    }
    
    return rc;
}
