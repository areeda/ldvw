#if MOZILLAPARSER_USE_IPC

#include "ProcessDescriptor.h"
#include "Process.h"

#include <sys/types.h>
#include <unistd.h>
#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <string.h>
#include <errno.h>

#include "util.h"

/* Return strdup(orig) if orig is non-null; otherwise return null */
static char *strdup_if( char *orig )
{
	if( orig != NULL )
	{
		return strdup(orig);
	}
	else
	{
		return orig;
	}
}

ProcessDescriptor *
ProcessDescriptor::fork()
{
	// Save the PATH environment variable so we can reset it in the parent
	char *oldPath = strdup_if( getenv("PATH") );

	// This is where we'll save the result of the exec() call if it returns
	volatile int result = 0;

	// Create the handle for communicating with the child process
	IPC::Handle *ipcHandle = IPC::NewHandle();

	signal( SIGCHLD, SIG_IGN );
	pid_t childPid = ::vfork();

	if( childPid < 0 )
	{
		perror( "fork() failed" );
		return NULL;
	}

	if( childPid == 0 )
	{
		/* Child */

		// Run the standalone executable
		setenv( "PATH", getenv(STRINGIFY(LIB_PATH)), 1 );
		execvp( STRINGIFY(CHILD_BIN), IPC::FreezeHandle( ipcHandle ) );
		result = errno;
		_exit( 1 );
	}
	else
	{
		/* Parent */

		// Restore the PATH environment variable
		if( oldPath == NULL )
		{
			unsetenv("PATH");
		}
		else
		{
			setenv( "PATH", oldPath, 1 );
			free( oldPath );
		}

		// If result is non-zero, then execvp() returned in the child process
		if( result != 0 )
		{
			fprintf( stderr, "execvp(\"" STRINGIFY(CHILD_BIN) "\") failed: %s\n", strerror(result) );
			IPC::FreeHandle( ipcHandle );
			return NULL;
		}

		IPC::BindHandle( ipcHandle, true );
		return new ProcessDescriptor( childPid, ipcHandle );
	}
}

bool
ProcessDescriptor::isAvailable()
const
{
	return _isAvailable;
}

void
ProcessDescriptor::setAvailable( bool isAvailable )
{
	this->_isAvailable = isAvailable;
	if( !isAvailable )
	{
		++_jobCount;
	}
}

size_t
ProcessDescriptor::getJobCount()
const
{
	return _jobCount;
}

bool
ProcessDescriptor::isAlive()
const
{
	return this->ipcHandle->isAlive();
}

void
ProcessDescriptor::terminate()
{
	/*
	fprintf( stderr, "Process %d closing descriptors %d and %d\n", getpid(), writer, reader );
	close( writer );
	close( reader );
	*/

	_isAvailable = false;
}

ProcessDescriptor::~ProcessDescriptor()
{
	IPC::FreeHandle( ipcHandle );
	ipcHandle = NULL;
}

ProcessDescriptor::ProcessDescriptor( pid_t __pid, IPC::Handle *aHandle )
	: pid( __pid )
	, ipcHandle( aHandle )
	, _isAvailable( false )
	, _jobCount( 0 )
{
}

#endif // MOZILLAPARSER_USE_IPC
