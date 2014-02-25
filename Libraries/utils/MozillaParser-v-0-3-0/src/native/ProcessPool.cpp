#if MOZILLAPARSER_USE_IPC

#include "ProcessPool.h"
#include "ProcessDescriptor.h"
#include "Lock.h"
#include "Semaphore.h"

ProcessPool::ProcessPool( size_t minSize, size_t maxJobs, Semaphore &semaphore, Lock &lock )
	: _minSize( minSize )
	, _maxJobs( maxJobs )
	, _semaphore( semaphore )
	, _lock( lock )
{
}

const ProcessDescriptor *
ProcessPool::spawn( JNIEnv *env )
{
	ProcessDescriptor *pd = NULL;

	_semaphore.acquire( env );

	SYNCHRONIZED( env, _lock )
	{
		// Check if we have an available worker
		for( ProcessTable::iterator it = _workers.begin() ;
		     it != _workers.end() ;
		     /* ++it */ )
		{
			ProcessDescriptor *pd = it->second;
			if( pd->isAvailable() )
			{
				// Make sure it's not dead!
				if( pd->isAlive() )
				{
					pd->setAvailable( false );
					return pd;
				}
				else
				{
					ProcessTable::iterator ppd = it++;
					delete pd;
					_workers.erase( ppd );
					continue;
				}
			}

			++it;
		}

		// We need to create a new worker
		pd = ProcessDescriptor::fork();
		if( pd != NULL )
		{
			_workers[pd->pid] = pd;
		}
	}

	if( pd == NULL )
	{
		_semaphore.release( env );
	}

	return pd;
}

void
ProcessPool::release( JNIEnv *env, const ProcessDescriptor *pd )
{
	SYNCHRONIZED( env, _lock )
	{
		// Look up the entry in our ProcessTable
		ProcessTable::iterator ppd = _workers.find( pd->pid );
		ProcessDescriptor *pd2 = ppd->second;

		// Have we exceeded the minimum pool size?
		// Or, has this worker exceeded the maximum number of jobs per lifetime?
		if( _workers.size() > _minSize || pd2->getJobCount() >= _maxJobs )
		{
			pd2->terminate();
			delete pd2;
			_workers.erase( ppd );
		}
		else
		{
			pd2->setAvailable( true );
		}
	}

	_semaphore.release( env );
}

#endif // MOZILLAPARSER_USE_IPC
