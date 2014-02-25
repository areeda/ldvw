#include <jni.h>
#include <stdlib.h>
#include "MozillaParser.h"

#include "IPC.h"
#include "ProcessPool.h"
#include "ProcessDescriptor.h"
#include "Semaphore.h"
#include "Lock.h"
#include "asprintf.h"
#include "util.h"

// JAVA Exception constants :
static const char *PARSER_EXCEPTION="com/dappit/Dapper/parser/ParserException";
static const char *PARSER_INITIALIZATION_EXCEPTION="com/dappit/Dapper/parser/ParserInitializationException";

#if MOZILLAPARSER_USE_IPC
static ProcessPool *processPool = NULL;
static Semaphore *semaphore = NULL;
static Lock *lock = NULL;
#endif // MOZILLAPARSER_USE_IPC

// Have a static boolean to prevent being initialized twice :
static bool wasInitialized = false;

static void doParse( JNIEnv *env, jobject mozillaParserObject, size_t dataLen, const char *data, size_t encodingLen, const char *encoding, bool forceAllowMeta );
static void throwException( JNIEnv *env, const char *exceptionName, const char *fmt, ... );

// Java helper function :Generate an exception :
static void throwException( JNIEnv *env,
                            const char *exceptionName,
                            const char *fmt,
                            ... )
{
	char *buf;
	va_list args;
	va_start( args, fmt );
	vasprintf( &buf, fmt, args );

	env->ExceptionDescribe();
	env->ExceptionClear();

	jclass newExcCls = env->FindClass( exceptionName );
	if( newExcCls != NULL )
	{
		env->ThrowNew( newExcCls, buf );
	}

	free( buf );
}

JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_initXPCOM
  (JNIEnv *env, jclass , jstring componentBaseString)
{
#ifdef MOZILLAPARSER_USE_IPC
	semaphore = new Semaphore( env, 20 );
	lock = new Lock( env );
	processPool = new ProcessPool( 20, 10000, *semaphore, *lock );
#endif // MOZILLAPARSER_USE_IPC

	wasInitialized=true;
}

JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_parseHtml
	(JNIEnv *env,
	 jobject mozillaParserObject,
	 jbyteArray dataBytes,
	 jstring dataEncoding,
	 jboolean forceAllowMeta)
{
	/* Get the raw byte array containing the HTML to parse */
	jint dataNumBytes   = env->GetArrayLength( dataBytes );
	jbyte *dataBytesRaw = env->GetByteArrayElements( dataBytes, NULL );

	/* Run the parser (this may throw a Java exception, which will be propagated) */
	if( dataEncoding != NULL )
	{
		const jchar *encodingChars = env->GetStringChars( dataEncoding, NULL );
		int encodingCharsLength    = env->GetStringLength( dataEncoding );
		char *encodingCStr = convertUTF16toASCII( encodingChars, encodingCharsLength );

		doParse( env, mozillaParserObject, dataNumBytes, (const char *) dataBytesRaw, encodingCharsLength, encodingCStr, forceAllowMeta );

		free( encodingCStr );
		env->ReleaseStringChars( dataEncoding, encodingChars );
	}
	else
	{
		doParse( env, mozillaParserObject, dataNumBytes, (const char *) dataBytesRaw, 0, NULL, forceAllowMeta );
	}

	/* Clean up and return */
	env->ReleaseByteArrayElements( dataBytes, dataBytesRaw, 0 );
}

#if MOZILLAPARSER_USE_IPC
static void
doParse( JNIEnv *env, jobject mozillaParserObject, size_t dataLen, const char *data, size_t encodingLen, const char *encoding, bool forceAllowMeta )
{
	IPC::Message *message = NULL;

	/* Get the callback methods */
	jmethodID callbackMethod = env->GetMethodID(env->GetObjectClass(mozillaParserObject), "callback", "(I[B)V");
	if( callbackMethod == NULL )
	{
		throwException( env, PARSER_EXCEPTION, "Failed to find method: MozillaParser.callback()" );
		return;
	}

	jmethodID resetMethod = env->GetMethodID(env->GetObjectClass(mozillaParserObject), "resetInstructionPool", "()V");
	if( resetMethod == NULL )
	{
		throwException( env, PARSER_EXCEPTION, "Failed to find method: MozillaParser.resetInstructionPool()" );
		return;
	}

	/* Get a new child ProcessDescriptor */
	const ProcessDescriptor *child = processPool->spawn( env );

	if( child == NULL )
	{
		fprintf( stderr, "Failed to spawn a child process!\n" );
		throwException( env, PARSER_EXCEPTION, "Failed to spawn a child process" );
		return;
	}

	/* Get the encoding-name string */
	if( encoding != NULL )
	{
		uint32_t msgType;
		if( forceAllowMeta )
		{
			msgType = IPC::Message::PARAM_DEFAULT_ENCODING;
		}
		else
		{
			msgType = IPC::Message::PARAM_ENCODING;
		}

		/* Tell the child what encoding to use */
		IPC::Result result = IPC::Send( child->ipcHandle,
		                                IPC::Message::TYPE_REQUEST_PARAMS,
		                                msgType,
		                                encoding,
		                                encodingLen+1 );

		if( result != IPC::OK )
		{
			processPool->release( env, child );
			throwException( env, PARSER_EXCEPTION, "Failed to send encoding to parser worker" );
			return;
		}
	}

	IPC::Result result = IPC::Send( child->ipcHandle,
	                                IPC::Message::TYPE_REQUEST_DATA,
	                                0,
	                                data,
	                                dataLen );

	if( result != IPC::OK )
	{
		processPool->release( env, child );
		throwException( env, PARSER_EXCEPTION, "Failed to send data to parser worker" );
		return;
	}

	for( message = IPC::Receive( child->ipcHandle ) ;
	     message != NULL ;
	     IPC::FreeMessage(message), message = IPC::Receive( child->ipcHandle ) )
	{
		if( message->type == IPC::Message::TYPE_RESPONSE_DATA )
		{
			jbyteArray byteArray = env->NewByteArray( message->arg2Len );
			if( byteArray == NULL )
			{
				fprintf( stderr, "%s:%d: Failed to allocate Java array of length %d\n", __FILE__, __LINE__, message->arg2Len );
				processPool->release( env, child );
				return;
			}

			env->SetByteArrayRegion( byteArray, 0, message->arg2Len, (jbyte *) message->arg2 );
			env->CallVoidMethod( mozillaParserObject, callbackMethod, message->arg1, byteArray );
		}
		else if( message->type == IPC::Message::TYPE_RESPONSE_RESET )
		{
			env->CallVoidMethod( mozillaParserObject, resetMethod );
		}
		else
		{
			break;
		}
	}

	processPool->release( env, child );

	if( message == NULL )
	{
		/* Child process died prematurely */
		throwException( env, PARSER_EXCEPTION, "Parser crashed" );
		return;
	}

	if( message->type == IPC::Message::TYPE_RESPONSE_FINISHED_ERROR )
	{
		const jchar *errorMsg = (const jchar *) message->arg2;
		const uint32_t errorMsgLen = message->arg2Len / sizeof( *errorMsg );

		char *errorMsgCStr = convertUTF16toASCII( errorMsg, errorMsgLen );
		throwException( env, PARSER_EXCEPTION, "%s", errorMsgCStr );
		free(errorMsgCStr);

		return;
	}

	if( message->type != IPC::Message::TYPE_RESPONSE_FINISHED_OK )
	{
		throwException( env, PARSER_EXCEPTION, "Parser returned unexpected message (type=%d, arg1=%d)", message->type, message->arg1 );
		return;
	}
}

#else // !MOZILLAPARSER_USE_IPC

static void
doParse( JNIEnv *env, jobject mozillaParserObject, size_t dataLen, const char *data, size_t encodingLen, const char *encoding, bool forceAllowMeta )
{
	/* Create the ParserWrapper instance */
	ParserWrapper parser;
	parser.setContentSink( new JavaContentSink( env, mozillaParserObject ) );

	int result = parser.init();
	if( result )
	{
		throwException( env, PARSER_EXCEPTION, "Failed to initialize parser: %s", parser.getLastErrorMessage() );
		return;
	}

	/* Get the encoding-name string */
	if( encoding != NULL )
	{
		parser.setEncoding( encoding, forceAllowMeta );
	}

	result = parser.parse( data, dataLen );
	if( result )
	{
		throwException( env, PARSER_EXCEPTION, "Failed to parse HTML: %s", parser.getLastErrorMessage() );
		return;
	}
}

#endif // MOZILLAPARSER_USE_IPC
