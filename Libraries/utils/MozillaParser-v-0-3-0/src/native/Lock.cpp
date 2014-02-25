#include "Lock.h"

Lock::Lock( JNIEnv *env )
{
	jclass objClass = env->FindClass( "java/lang/Object" );
	jmethodID objCtor = env->GetMethodID( objClass, "<init>", "()V" );
	jobject obj = env->NewObject( objClass, objCtor );
	_obj = env->NewGlobalRef( obj );
}

void
Lock::destroy( JNIEnv *env )
{
	env->DeleteGlobalRef( _obj );
}

Lock::Handle
Lock::acquire( JNIEnv *env )
{
	return Handle( *this, env );
}

Lock::Handle::Handle( Lock &lock, JNIEnv *env )
	: _lock( lock )
	, _env( env )
{
	_env->MonitorEnter( _lock._obj );
}

Lock::Handle::~Handle()
{
	_env->MonitorExit( _lock._obj );
}
