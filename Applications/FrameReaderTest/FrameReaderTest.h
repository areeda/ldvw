/* 
 * File:   FrameReaderTest.h
 * Author: joe
 *
 * Created on July 29, 2013, 9:28 PM
 */

#ifndef FRAMEREADERTEST_H
#define	FRAMEREADERTEST_H

#include <string>
#include <vector>
using namespace std;

vector<string> getList(string fname);
void setup( int argc, char** argv );
void writeIVect(ofstream *oFile, FrVect *vect);
void writeVects(ofstream *oFile,FrVect **vects, int nvect );


#endif	/* FRAMEREADERTEST_H */

