/*
 * File:   NDSException.h
 * Author: joe
 *
 * Created on March 4, 2012, 6:51 AM
 */

#ifndef NDSEXCEPTION_H
#define	NDSEXCEPTION_H
#include <string>
using std::string;
#include <exception>
using std::exception;


class NDSException: public exception
{
public:
    NDSException();
    NDSException(int rc);
    NDSException(int rc, const char* msg);
    NDSException(const NDSException& orig);
    virtual ~NDSException() throw();

    const char* what() const throw();

private:
    int nds_rc;   ///< the error code from an NDS library call or system call
    string errMsg;  ///< string translation of the rc
    const char* getMsg(int rc); ///< translate a NDS error number
};

#endif	/* NDSEXCEPTION_H */

