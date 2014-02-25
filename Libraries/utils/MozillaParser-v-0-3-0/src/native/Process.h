#ifndef _PROCESS_H_
#define _PROCESS_H_

#if MOZILLAPARSER_USE_IPC

#include "IPC.h"

class Process
{
public:
	/**
	 * The main loop of the child process.
	 *
	 * @param aHandle  The handle to use for communicating with the parent
	 *                 process
	 *
	 * @return ignored
	 */
	static int run( IPC::Handle *aHandle );
};

#endif // MOZILLAPARSER_USE_IPC

#endif // _PROCESS_H_
