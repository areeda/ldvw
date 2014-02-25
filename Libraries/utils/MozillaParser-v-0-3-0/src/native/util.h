#ifndef _UTIL_H_
#define _UTIL_H_

#include <jni.h>

#define STRINGIFY(x) _STRINGIFY(x)
#define _STRINGIFY(x) #x

char *convertUTF16toASCII( const jchar *uniChars, int length );
jchar *convertASCIItoUTF16( const char *chars, int *length );

#endif // _UTIL_H_
