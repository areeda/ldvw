#include <jni.h>
#include "MozillaParser.h"
#include "JavaContentSink.h"
#include "nsXPCOM.h"
#include "nsIComponentManager.h"
#include "nsParserCIID.h"
#include "nsIParser.h"
#include "nsILoggingSink.h"
#include "nsIInputStream.h"
#include "nsILocalFile.h" 
#include "nsEmbedString.h"
#include "nsIProxyObjectManager.h"
#include "nsParser.h"



// Class IID's
static NS_DEFINE_CID(kParserCID, NS_PARSER_CID);
static NS_DEFINE_CID(kLoggingSinkCID, NS_LOGGING_SINK_CID);
static NS_DEFINE_CID(kNavDTDCID, NS_CNAVDTD_CID);
static NS_DEFINE_CID(kProxyObjectManagerCID,NS_PROXYEVENT_MANAGER_CID);

// JAVA Exception constants :
const char *PARSER_EXCEPTION="com/dappit/Dapper/parser/ParserException";
const char *PARSER_INITIALIZATION_EXCEPTION="com/dappit/Dapper/parser/ParserInitializationException";

// Java helper function :Generate an exception :
void generateException(JNIEnv * env , const char *exceptionName , char *message){
	env->ExceptionDescribe();
	env->ExceptionClear();

	jclass newExcCls = env->FindClass(exceptionName);
	if (newExcCls == 0) /* Unable to find the new exception class, give up. */
	{
		return;
	}
	env->ThrowNew( newExcCls, message);
}

void generateException(JNIEnv * env ,  char *message ){
	generateException(env,PARSER_EXCEPTION,message);
}
// Have a static boolean to prevent being initialized twice :
static bool wasInitialized = false;

JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_initXPCOM
  (JNIEnv *env, jclass , jstring componentBaseString)
{ 
	if (wasInitialized)
	{
		printf("Warning : Attempt to initialize XPCOM again was blocked.\n"); 
		fflush(stdout);
		return;
	}
	wasInitialized=true;

	jboolean iscopy;
	const char *componentBaseInput = env->GetStringUTFChars(componentBaseString , &iscopy);
	int inputBaseLength = (int)env->GetStringLength(componentBaseString);
	
	printf("Initializing XPCOM from location : %s...\n" , componentBaseInput);
	nsresult rv;
	nsCOMPtr<nsILocalFile> file;
	nsString componentBaseNsString;
	componentBaseNsString.AppendWithConversion(componentBaseInput , inputBaseLength );
	rv = NS_NewLocalFile(nsEmbedString(componentBaseNsString), PR_FALSE,
                     getter_AddRefs(file));
	rv = NS_InitXPCOM2(nsnull, file , nsnull);
	env->ReleaseStringUTFChars(componentBaseString , componentBaseInput);
	if (NS_FAILED(rv)) 
	{
		printf("Strting XPCOM failed...\n");
		char errMessage[100];
		sprintf(errMessage , "%x" , rv );
		generateException(env , PARSER_INITIALIZATION_EXCEPTION , "Starting XPCOM Failed : %s");		
  	}
  	
  	nsresult result = NS_OK;
	 
}


JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_setDebug
  (JNIEnv *, jobject, jboolean aDoDebug)
  {
	  JavaContentSink::setDebug((bool)aDoDebug);
  }


JNIEXPORT void JNICALL Java_com_dappit_Dapper_parser_MozillaParser_parseHtml
  (JNIEnv *env, jobject mozillaParserObject, jstring stringToParse)
{ 
	nsresult result;
	jboolean iscopy;
	const char *parserInput = env->GetStringUTFChars(stringToParse, &iscopy);
	int htmlLength = (int)env->GetStringLength(stringToParse);

  nsCOMPtr<nsIParser> parser(do_CreateInstance(kParserCID, &result));
  if (NS_FAILED(result)) {
    printf("\nUnable to create a parser : %x \n" , result);
    return;
  }


  // Create a JavaContentSink object and set the java enviroment to point the MozillaParser java object
nsIContentSink* sink;
JavaContentSink javaContentSink;
javaContentSink.setJavaEnviroment(env , mozillaParserObject);
  sink = &javaContentSink;



//   Create a CNavDTD dtd object and register the DTD to the parser :
  nsCOMPtr<nsIDTD> dtd(do_CreateInstance(kNavDTDCID, &result));
  if(NS_FAILED(result)) {
    printf("Unable to create a dtd\n");
    return;
  }
   parser->RegisterDTD(dtd);

   // Create a string stream and put the html into it :
  nsString stream;
  stream.AppendWithConversion(parserInput , htmlLength);

	// Set a content sink ( the javaContentSink for that matter )
  parser->SetContentSink(sink);

  // Parse and release : 
 parser->Parse(stream, 0, NS_LITERAL_CSTRING("text/html"), PR_FALSE, PR_TRUE);
 env->ReleaseStringUTFChars(stringToParse , parserInput);

}



