/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class MozillaParser */

#ifndef _Included_MozillaParser
#define _Included_MozillaParser
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     MozillaParser
 * Method:    initXPCOM
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_initXPCOM
  (JNIEnv *, jclass, jstring);

/*
 * Class:     MozillaParser
 * Method:    parseHtml
 * Signature: ([BLjava/lang/String;Z)V
 */
JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_parseHtml
  (JNIEnv *, jobject, jbyteArray, jstring, jboolean);

#ifdef __cplusplus
}
#endif
#endif
