#ifndef _ASPRINTF_H_
#define _ASPRINTF_H_

#ifdef WINDOWS

#include <stdarg.h>
#include <stdio.h>

static int vasprintf( char **ret, const char *format, va_list args )
{
	int len, result;
	char *str;

	len = strlen( format ) * 2;

	while(1)
	{
		str = (char *) malloc( len+1 );
		result = _vsnprintf( str, len, format, args );
		str[len] = '\0';

		if( (result >= 0) && (result <= len) )
			break;

		free( str );
		len = len * 2;
	}

	*ret = str;
	return result;
}

#endif // WINDOWS

#endif // _ASPRINTF_H_
