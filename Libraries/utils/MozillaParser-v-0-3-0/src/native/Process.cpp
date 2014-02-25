#if MOZILLAPARSER_USE_IPC

#include <jni.h>
#include "MozillaParser.h"
#include "JavaContentSink.h"
#include "ParserWrapper.h"

#include "Process.h"
#include "IPC.h"

#include "nsXPCOM.h"

#include <unistd.h>

int
Process::run( IPC::Handle *aHandle )
{
	IPC::Message *message = NULL;
	IPC::Message *errorMessage = NULL;

	// Close all files other than the IPC file descriptors and stdout/stderr
	for( int i=getdtablesize()-1 ; i>=0 ; --i )
	{
		if( (i != aHandle->reader) && (i != aHandle->writer) && (i != 1) && (i != 2) )
		{
			close( i );
		}
	}

	ParserWrapper parser;
	nsCOMPtr<JavaContentSink> contentSink = new JavaContentSink();
	contentSink->setIPCHandle( aHandle );

	if( parser.init() != 0 )
	{
		/* Postpone the error until it's our turn to send */
		errorMessage = IPC::NewErrorMessage( "%s", parser.getLastErrorMessage() );
	}

	parser.setContentSink( contentSink );

	for( message = IPC::Receive(aHandle) ;
	     message != NULL ;
	     IPC::FreeMessage(message), message = IPC::Receive(aHandle) )
	{
		if( message->type == IPC::Message::TYPE_REQUEST_PARAMS )
		{
			if( errorMessage != NULL )
			{
				continue;
			}

			switch( message->arg1 )
			{
				case IPC::Message::PARAM_ENCODING:
					parser.setEncoding( message->arg2, false );
					break;
				case IPC::Message::PARAM_DEFAULT_ENCODING:
					parser.setEncoding( message->arg2, true );
					break;
				case IPC::Message::PARAM_CRASH:
					if( message->arg2Len == sizeof(uint32_t) )
					{
						uint32_t tmp = *((uint32_t *) message->arg2);
						contentSink->setCrashInterval( tmp & ~0x80000000, tmp & 0x80000000 );
					}
					else
					{
						errorMessage = IPC::NewErrorMessage( "Invalid arg2 passed for PARAM_CRASH message" );
					}
					break;
				default:
					errorMessage = IPC::NewErrorMessage( "Unknown parameter id %d", message->arg1 );
			}

			continue;
		}
		else if( message->type != IPC::Message::TYPE_REQUEST_DATA )
		{
			return IPC::SendError( aHandle, "Invalid message type received: %d", message->type );
		}

		if( errorMessage != NULL )
		{
			IPC::Result result = IPC::Send( aHandle, errorMessage );
			IPC::FreeMessage( errorMessage );
			return result;
		}

		const char *htmlBytes = message->arg2;
		uint32_t htmlNumBytes = message->arg2Len;

		if( parser.parse( htmlBytes, htmlNumBytes ) == 0 )
		{
			IPC::Result msgResult =
				IPC::Send( aHandle,
				           IPC::Message::TYPE_RESPONSE_FINISHED_OK,
				           0,
				           NULL,
				           0 );

			if( msgResult != IPC::OK )
			{
				return NS_ERROR_FAILURE;
			}
		}
		else
		{
			return IPC::SendError( aHandle, "%s", parser.getLastErrorMessage() );
		}
	}

	return NS_OK;
}

#endif // MOZILLAPARSER_USE_IPC
