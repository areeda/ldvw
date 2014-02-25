#include "Semaphore.h"

Semaphore::Semaphore( JNIEnv *env, int permits )
{
	jclass semClass = env->FindClass( "java/util/concurrent/Semaphore" );
	jmethodID semCtor = env->GetMethodID( semClass, "<init>", "(I)V" );
	_semAcquire = env->GetMethodID( semClass, "acquire", "()V" );
	_semRelease = env->GetMethodID( semClass, "release", "()V" );

	jobject semaphore = env->NewObject( semClass, semCtor, permits );

	_semaphore = env->NewGlobalRef( semaphore );
}

void
Semaphore::destroy( JNIEnv *env )
{
	env->DeleteGlobalRef( _semaphore );
}

void
Semaphore::acquire( JNIEnv *env )
{
	env->CallVoidMethod( _semaphore, _semAcquire );
}

void
Semaphore::release( JNIEnv *env )
{
	env->CallVoidMethod( _semaphore, _semRelease );
}
