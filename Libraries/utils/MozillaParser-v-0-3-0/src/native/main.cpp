#include <unistd.h>
#include <stdio.h>
#include "IPC.h"
#include "Process.h"

#include "init.h"

int main( int argc, char **argv )
{
	IPC::Handle *handle = IPC::ThawHandle( argc, argv );
	if( initXPCOM() != 0 )
	{
		return 1;
	}

	Process::run( handle );

	/* Should never be reached */
	return 1;
}
