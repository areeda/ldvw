#include "nsXPCOM.h"
#include "nsILocalFile.h" 
#include "nsString.h"

#include "init.h"
#include "util.h"

#ifndef WINDOWS
#include <stdlib.h>
#include <unistd.h>
#include <assert.h>
#include <sys/stat.h>
#endif // !WINDOWS

static bool fileExists( const char *file );
static void searchLibraryPath( nsAString &path, const char *fileName );

#ifndef WINDOWS

static bool fileExists( const char *file )
{
	static struct stat st;
	if( !stat( file, &st ) )
	{
		return true;
	}
	else
	{
		return false;
	}
}

static void searchLibraryPath( nsAString &path, const char *fileName )
{
	char *dir, *libpath = getenv( STRINGIFY(LIB_PATH) );

	if( !libpath )
	{
		return;
	}

	libpath = strdup(libpath);

	for( char *ptr = libpath ; (dir = strsep(&ptr, STRINGIFY(PATH_SEPARATOR))) != NULL ; )
	{
		char *file;
		asprintf( &file, "%s" STRINGIFY(PATH_DELIMITER) "%s", dir, fileName );
		bool exists = fileExists(file);
		free( file );

		if( exists )
		{
			path.Assign( NS_ConvertASCIItoUTF16(dir) );
			break;
		}
	}

	free(libpath);
}

#else // WINDOWS

static void searchLibraryPath( nsAString &path, const char *fileName )
{
	char buffer[4096];
	char *endptr;

	if( SearchPath( NULL, fileName, NULL, 4095, buffer, &endptr ) > 0 )
	{
		*(endptr-1) = '\0';
		path.Assign( NS_ConvertASCIItoUTF16(buffer) );
	}
}

#endif // WINDOWS

int initXPCOM()
{
	nsAutoString componentBase;
	nsresult rv;
	nsCOMPtr<nsILocalFile> file;

	searchLibraryPath( componentBase, STRINGIFY(LIB_PREFIX) "xpcom" STRINGIFY(LIB_SUFFIX) );

	rv = NS_NewLocalFile( componentBase, PR_FALSE, getter_AddRefs(file) );
	if (NS_FAILED(rv)) 
	{
		fprintf( stderr, "%s:%d: NS_NewLocalFile failed: 0x%08x\n", __FILE__, __LINE__, rv );
		return 1;
  	}

	rv = NS_InitXPCOM2( nsnull, file, nsnull );
	if (NS_FAILED(rv)) 
	{
		fprintf( stderr, "%s:%d: Starting XPCOM failed: 0x%08x\n", __FILE__, __LINE__, rv );
		return 1;
  	}

	return 0;
}
