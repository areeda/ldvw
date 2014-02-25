#include <jni.h>
#include <stdlib.h>
#include "EnviromentController.h"



JNIEXPORT void Java_com_dappit_Dapper_parser_EnviromentController_setenv
  (JNIEnv *env , jclass, jstring variableName , jstring value )
{ 
	
	char envSet[1024];
	char envSet2[1024];
	jboolean iscopy;
	const char *variableNameStr = env->GetStringUTFChars(variableName , &iscopy);
	const char *variableValueStr = env->GetStringUTFChars(value , &iscopy);
	sprintf(envSet , "%s=%s" , variableNameStr , variableValueStr);
	printf("Changing variable name : %s to : %s...\n" , variableNameStr ,variableValueStr );
	putenv(envSet);
	printf("Putenv : %s\n" , envSet);
	env->ReleaseStringUTFChars(variableName , variableNameStr);
	env->ReleaseStringUTFChars(value , variableValueStr);
	 
}

JNIEXPORT jstring Java_com_dappit_Dapper_parser_EnviromentController_getenv
  (JNIEnv *env , jclass, jstring variableName  ){
  	char *envSet;
  	jboolean iscopy;
  	const char *variableNameStr = env->GetStringUTFChars(variableName , &iscopy);
  	envSet = getenv(variableNameStr);
  	env->ReleaseStringUTFChars(variableName , variableNameStr);
  	jstring string1 = env->NewStringUTF(envSet);
  	return string1;
  }


