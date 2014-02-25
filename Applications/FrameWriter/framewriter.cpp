/* 
 * File:   main.cpp
 * Author: joe
 *
 * Created on July 26, 2013, 8:52 PM
 */

#include <cstdlib>
#include <FrameL.h>

using namespace std;

/*
 * 
 */
int main( int argc, char** argv )
{
    FrFile *oFile;

    FrameH *frame;

    FrProcData *proc;

    double sampleRate;

    long nData, i, j;



    frame = FrameNew( "demo" ); /*----------------------create a 10s long frame--*/

    frame->dt = 10;



    sampleRate = 16384; /*-----add a 16384Hz 32bits float proc channel---*/

    nData = sampleRate * frame->dt;

    proc = FrProcDataNew( frame, "Channel_Name", sampleRate, nData, -32 );



    /*-------- open output file; compression type 9, 1000 seconds per file ----*/



    oFile = FrFileONewM( "test", 9, "FrFull", 1000 );

    if ( oFile == NULL )
    {

        printf( "Open file error (%s)\n", FrErrorGetHistory( ) );

        return (0 );
    }



    for ( i = 0; i < 10; i++ )
    { /*---------------------produce 10 frames ---*/

        frame->GTimeS = frame->GTimeS + frame->dt;



        for ( j = 0; j < proc->data->nData; j++ )
        { /*----- update channel content--*/

            proc->data->dataF[j] = j;
        } /*-- change this to your need---*/



        if ( i < 2 ) FrameDump( frame, stdout, 2 ); /*------------- just for debug---*/



        if ( FrameWrite( frame, oFile ) != FR_OK )
        {

            printf( "Write error; last error:%s\n", FrErrorGetHistory( ) );

            return (0 );
        }

    }



    FrFileOEnd( oFile ); /*------------------------------ close the output file---*/



    FrameFree( frame );



    return (0 );

}

