#ifndef _PROCESS_POOL_H_
#define _PROCESS_POOL_H_

#if MOZILLAPARSER_USE_IPC

#include <sys/types.h>
#include <jni.h>
#include <map>

class Semaphore;
class Lock;
class ProcessDescriptor;

/**
 * Implements a pool of ParserProcess processes with a specified minimum size.
 * An external semaphore is used to control the maximum allowable size of the
 * pool.
 */
class ProcessPool
{
public:
	/**
	 * Create a new process pool.
	 *
	 * @param minSize    The minimum number of children to keep around when
	 *                   there is at least one idle child.
	 * @param maxJobs    The maximum number of jobs to assign to any given child
	 *                   process before euthanizing it
	 * @param semaphore  The semaphore used to control the maximum size of the
	 *                   process pool
	 * @param lock       The Lock object to use to protect critical sections.
	 */
	ProcessPool( size_t minSize, size_t maxJobs, Semaphore &semaphore, Lock &lock );

	/**
	 * Return a ProcessDescriptor for a worker process, creating a new process
	 * if necessary.  This method may block if the current number of child
	 * processes is at least maxSize.
	 */
	const ProcessDescriptor *spawn( JNIEnv *env );

	/**
	 * Inform us that the specified worker is no longer in use.  This method
	 * must be called by the parent process when it has finished interacting
	 * with the worker; otherwise the worker will always be considered busy!
	 */
	void release( JNIEnv *env, const ProcessDescriptor *pd );

private:
	typedef std::map<pid_t, ProcessDescriptor *> ProcessTable;

	size_t _minSize;
	size_t _maxJobs;
	Semaphore &_semaphore;
	Lock &_lock;
	ProcessTable _workers;
};

#endif // MOZILLAPARSER_USE_IPC

#endif // _PROCESS_POOL_H_
