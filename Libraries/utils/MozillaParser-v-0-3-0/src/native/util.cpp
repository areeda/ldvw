#include "util.h"

#include <stdlib.h>
#include <string.h>

char *convertUTF16toASCII( const jchar *uniChars, int length )
{
	int i;
	char *result = (char *) malloc( sizeof(*result) * (length+1) );

	for( i=0; i<length; ++i )
	{
		result[i] = (char) uniChars[i];
	}

	result[i] = '\0';

	return result;
}

jchar *convertASCIItoUTF16( const char *chars, int *length )
{
	int i;
	int len = *length = strlen( chars );
	jchar *result = (jchar *) malloc( sizeof(*result) * len );

	for( i=0; i<len; ++i )
	{
		result[i] = (jchar) chars[i];
	}

	return result;
}
