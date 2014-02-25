#ifndef _SEMAPHORE_H_
#define _SEMAPHORE_H_

#include <jni.h>

/**
 * A wrapper around a Java semaphore.
 */
class Semaphore
{
public:
	/**
	 * Create a new Semaphore.
	 *
	 * @param permits Number of initial permits
	 */
	Semaphore( JNIEnv *env, int permits );

	/**
	 * Destroy the semaphore.
	 */
	void destroy( JNIEnv *env );

	/**
	 * Acquire the semaphore (P).
	 */
	void acquire( JNIEnv *env );

	/**
	 * Release the semaphore (V).
	 */
	void release( JNIEnv *env );

protected:
	jobject _semaphore;
	jmethodID _semAcquire;
	jmethodID _semRelease;
};

#endif // _SEMAPHORE_H_
