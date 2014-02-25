#ifndef _LOCK_H_
#define _LOCK_H_

#include <jni.h>

// A cute preprocessor macro that lets us mimic Java's synchronized keyword.
// Effectively equivalent to:
//
// {
//    Lock::Handle lh = lock.aquire();
//    (CODE)
// }
#define SYNCHRONIZED(env, lock) \
	for( Lock::Handle lh = lock.acquire(env), *lhPtr = &lh ; lhPtr ; lhPtr = NULL )

/**
 * A lock based on JVM monitors.
 */
class Lock
{
public:
	/**
	 * A handle on the lock.  This lets the caller easily acquire the lock and
	 * automatically release it when the handle goes out of scope.
	 */
	class Handle
	{
	public:
		~Handle();
	protected:
		Handle( Lock &lock, JNIEnv *env );
		Lock &_lock;
		JNIEnv *_env;
		friend class Lock;
	};

	/**
	 * Create a new lock.
	 */
	Lock( JNIEnv *env );

	/**
	 * Destroy the lock.
	 */
	void destroy( JNIEnv *env );

	/**
	 * Acquire the lock.  The lock is released when the handle is destroyed.
	 */
	Handle acquire( JNIEnv *env );

protected:
	jobject _obj;
};

#endif // _LOCK_H_
