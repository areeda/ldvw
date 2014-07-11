/* 
 * File:   NdsException.h
 * Author: joe
 *
 * Created on July 9, 2014, 7:13 PM
 */

#ifndef NDSEXCEPTION_H
#define	NDSEXCEPTION_H

#include <string>
using std::string;
#include <exception>


class NdsException
{
public:
    NdsException();
    NdsException(int rc);
    NdsException(const char* msg);
    NdsException(int rc, const char* msg);
    NdsException(const NdsException& orig);
    virtual ~NdsException() throw ();

    const char* what() const throw ();

private:
    int nds_rc; ///< the error code from an NDS library call or system call
    string errMsg; ///< string translation of the rc
    const char* getMsg(int rc); ///< translate a NDS error number
};


#endif	/* NDSEXCEPTION_H */

