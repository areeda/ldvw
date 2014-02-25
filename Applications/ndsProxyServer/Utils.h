/* 
 * File:   Utils.h
 * Author: areeda
 *
 * Created on June 1, 2012, 12:37 PM
 */

#ifndef UTILS_H
#define	UTILS_H
#ifdef __APPLE__
#include <sys/stat.h>
#endif
#include <sys/time.h>
#include <string>
using std::string;

void gettime(timespec *tp);
string getCurTime();

#endif	/* UTILS_H */

