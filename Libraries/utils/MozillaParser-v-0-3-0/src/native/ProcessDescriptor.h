#ifndef _PROCESS_DESCRIPTOR_H_
#define _PROCESS_DESCRIPTOR_H_

#if MOZILLAPARSER_USE_IPC

#include "IPC.h"
#include <sys/types.h>

/**
 * Describes the state of a worker process.
 */
class ProcessDescriptor
{
public:
	/**
	 * Fork a new child process.  The parent will get back a ProcessDescriptor
	 * pointer, whereas the child will start in WorkerProcess::run().
	 */
	static ProcessDescriptor *fork();

	/**
	 * Is the child idle?  Note that initially after fork(), the child is set
	 * as not idle.
	 */
	bool isAvailable() const;

	/**
	 * Change the child's idle status.
	 *
	 * @param isAvailable  If true, the child is considered idle; if false, the
	 *                     child is considered busy
	 *
	 * When this method is called with isAvailable==false, the job count is
	 * incremented.
	 */
	void setAvailable( bool isAvailable );

	/**
	 * Returns the number of jobs that this process has been given, i.e. the
	 * number of times that setAvailable(false) has been called.
	 */
	size_t getJobCount() const;

	/**
	 * Is the child alive?  This is tested by writing zero bytes to the writer
	 * file descriptor; if the write fails, the child is considered dead.
	 */
	bool isAlive() const;

	/**
	 * Terminate the child process.  This is achieved by closing the pipe used
	 * to communicate with it.
	 */
	void terminate();

	/** The process id of the child */
	const pid_t pid;

	/** The IPC handle for communicating with the child process */
	IPC::Handle *ipcHandle;

	/**
	 * The destructor should only be called after terminate()
	 */
	~ProcessDescriptor();

private:
	ProcessDescriptor( pid_t pid, IPC::Handle *aHandle );
	bool _isAvailable;
	size_t _jobCount;
};

#endif // MOZILLAPARSER_USE_IPC

#endif // _PROCESS_DESCRIPTOR_H_
