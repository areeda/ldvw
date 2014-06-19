/* 
 * File:   main.cpp
 * Author: joe
 *
 * Created on July 26, 2013, 8:52 PM
 */

#include <cstdlib>
#include <FrameL.h>
#include <math.h>

using namespace std;

/*
 * 
 */
int main( int argc, char** argv )
{
    FrFile *oFile;

    FrameH *frame;

    FrProcData *proc, *wave;

    

    long nData, i, j;

    frame = FrameNew( "NDS-TEST" ); 

    frame->dt = 100;    // length of frame in seconds
    frame->detectProc->longitude = -117.885108333;
    frame->detectProc->latitude = 33.878575;

    double sampleRate = 16384; /*-----add a 16384Hz 32bits float proc channel---*/
    double waveRate = 16384;
    nData = sampleRate * frame->dt;
    long nWaveData = waveRate * frame->dt;

    proc = FrProcDataNew( frame, "TEST:NDS-COUNT_INFRAME", sampleRate, nData, 32 );
    wave = FrProcDataNew( frame, "TEST:NDS-200HZ_TONE", waveRate, nData, 32 );

    double fact = 2 * M_PI * 200 / waveRate;

    /*-------- open output file; compression type 9, 1000 seconds per file ----*/



    oFile = FrFileONewM( "NDS-TEST", 9, "FrFull", 1000 );

    if ( oFile == NULL )
    {

        printf( "Open file error (%s)\n", FrErrorGetHistory( ) );

        return (0 );
    }



    frame->GTimeS = 1072569616;

    float v;
    wave->data->type = FR_VECT_4R;
    proc->data->type = FR_VECT_4R;

    for ( j = 0; j < proc->data->nData; j++ )
    { /*----- update channel content--*/

        v=(float)sin(j*fact);
        wave->data->dataF[j] = v;
        proc->data->dataF[j] = j/sampleRate + frame->GTimeS;
    } 


    FILE *out=fopen("/home/joe/t/test1.list", "w");

    if ( i < 2 ) FrameDump( frame, out, 2 ); /*------------- just for debug---*/



    if ( FrameWrite( frame, oFile ) != FR_OK )
    {

        printf( "Write error; last error:%s\n", FrErrorGetHistory( ) );

        return (0 );
    }

    FrFileOEnd( oFile ); /*------------------------------ close the output file---*/

    FrameFree( frame );

    return (0 );

}

