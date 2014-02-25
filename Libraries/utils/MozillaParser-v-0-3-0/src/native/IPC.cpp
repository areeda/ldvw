#include "IPC.h"
#include "util.h"

#if MOZILLAPARSER_USE_IPC

#ifdef WINDOWS
#include <windows.h>
#endif

#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <assert.h>

#include <sys/ipc.h>
#include <sys/shm.h>

static char buf[100];

#define TYPE_STR(type) \
	(type == IPC::Message::TYPE_REQUEST_PARAMS ? "TYPE_REQUEST_PARAMS" : \
	(type == IPC::Message::TYPE_REQUEST_DATA ? "TYPE_REQUEST_DATA" : \
	(type == IPC::Message::TYPE_RESPONSE_DATA ? "TYPE_RESPONSE_DATA" : \
	(type == IPC::Message::TYPE_RESPONSE_RESET ? "TYPE_RESPONSE_RESET" : \
	(type == IPC::Message::TYPE_RESPONSE_FINISHED_OK ? "TYPE_RESPONSE_FINISHED_OK" : \
	(type == IPC::Message::TYPE_RESPONSE_FINISHED_ERROR ? "TYPE_RESPONSE_FINISHED_ERROR" : \
	(sprintf(buf,"UNKNOWN (0x%x)", (type)),buf) ))))))

struct Buffer
{
	size_t bytesUsed;
	char data[0];
};

struct HandleImpl : IPC::Handle
{
	int fds[4];
	int shmid;
	Buffer *buffer;
	char *bufReadPtr;
	char *bufWritePtr;
	char *bufEnd;
};

static bool doRecv( int fd, void *msg, ssize_t size );
static bool doSend( int fd, const void *msg, ssize_t size );
static IPC::Message * PipeReceive( HandleImpl *handle );
static IPC::Result PipeSend( const HandleImpl *handle, uint32_t msgType, uint32_t arg1, const void *arg2, uint32_t arg2Len );

static const uint32_t TYPE_INTERNAL_FLUSH =
	IPC::Message::TYPE_INTERNAL_BASE | 0x01;
static const uint32_t TYPE_INTERNAL_FLUSH_OK =
	IPC::Message::TYPE_INTERNAL_BASE | 0x02;
static const uint32_t TYPE_INTERNAL_PING =
	IPC::Message::TYPE_INTERNAL_BASE | 0x03;

bool
IPC::Handle::isAlive()
const
{
	HandleImpl *handle = (HandleImpl *) this;
	return (OK == PipeSend( handle, TYPE_INTERNAL_PING, 0, NULL, 0 ));
}

IPC::Handle *
IPC::NewHandle()
{
	HandleImpl *handle = (HandleImpl *) malloc( sizeof(*handle) );
	memset( handle, 0, sizeof(*handle) );

	/* Initialize the file descriptors to be used */

	if( pipe( &handle->fds[0] ) )
	{
		perror( "pipe() failed" );
		FreeHandle( handle );
		return NULL;
	}

	if( pipe( &handle->fds[2] ) )
	{
		perror( "pipe() failed" );
		FreeHandle( handle );
		return NULL;
	}

	/* Create the shared buffer */

	handle->reader = -1;
	handle->writer = -1;

	if( BUFFER_SIZE < MIN_BUFFER_SIZE )
	{
		handle->shmid  = -1;
		return handle;
	}

	handle->shmid = shmget( IPC_PRIVATE, BUFFER_SIZE, 0600 );
	if( handle->shmid == -1 )
	{
		perror( "shmget() failed; continuing without shared memory" );
		return handle;
	}

	handle->buffer = (Buffer *) shmat( handle->shmid, 0, 0 );
	if( handle->buffer == (void *)(-1) )
	{
		perror( "shmat() failed; continuing without shared memory" );
		int result = shmctl( handle->shmid, IPC_RMID, 0 );
		if( result )
		{
			fprintf( stderr, "Failed to free shared memory segment %d: %s", handle->shmid, strerror(errno) );
		}
		handle->shmid  = -1;
		handle->buffer = NULL;
		return handle;
	}

	handle->buffer->bytesUsed = 0;
	handle->bufEnd = ((char *) handle->buffer) + BUFFER_SIZE;

	return handle;
}

void
IPC::FreeHandle( Handle *aHandle )
{
	int i;
	int result;

	HandleImpl *handle = (HandleImpl *) aHandle;

	for( i = 0 ; i < 4 ; ++i )
	{
		if( handle->fds[i] )
		{
			result = close( handle->fds[i] );
			if( result )
			{
				perror( "IPC::FreeHandle(): close()" );
			}
		}
	}

	if( handle->buffer )
	{
		shmdt( handle->buffer );

		int result = shmctl( handle->shmid, IPC_RMID, 0 );
		if( result )
		{
			fprintf( stderr, "%s:%d: Failed to free shared memory segment %d: %s\n", __FILE__, __LINE__, handle->shmid, strerror(errno) );
		}
	}

	memset( handle, 0, sizeof(*handle) );
	free( handle );
}

void
IPC::BindHandle( Handle *aHandle, bool isParent )
{
	HandleImpl *handle = (HandleImpl *) aHandle;

	if( isParent )
	{
		close( handle->fds[0] );
		handle->fds[0] = 0;
		close( handle->fds[3] );
		handle->fds[3] = 0;
		handle->reader = handle->fds[2];
		handle->writer = handle->fds[1];
	}
	else
	{
		close( handle->fds[1] );
		handle->fds[1] = 0;
		close( handle->fds[2] );
		handle->fds[2] = 0;
		handle->reader = handle->fds[0];
		handle->writer = handle->fds[3];

		/* Child writes to the buffer */
		if( handle->buffer )
		{
			handle->bufWritePtr = handle->buffer->data;
			handle->buffer->bytesUsed = 0;
		}
	}
}

char *const *
IPC::FreezeHandle( const Handle *aHandle )
{
	const HandleImpl *handle = (const HandleImpl *) aHandle;

	char **argv = (char **) malloc( 5 * sizeof(char *) );
	asprintf( &argv[0], "%s", "MozillaParser" );
	asprintf( &argv[1], "%d", handle->fds[0] );
	asprintf( &argv[2], "%d", handle->fds[3] );
	asprintf( &argv[3], "%d", handle->shmid );
	argv[4] = NULL;

	return argv;
}

IPC::Handle *
IPC::ThawHandle( int argc, const char *const *argv )
{
	if( argc != 4 )
	{
		fprintf( stderr, "IPC::ThawHandle(): Invalid number of arguments: expected %d, got %d\n", 3, argc-1 );
		return NULL;
	}

	HandleImpl *handle = (HandleImpl *) malloc( sizeof(*handle) );
	memset( handle, 0, sizeof(*handle) );

	handle->reader = atoi( argv[1] );
	handle->writer = atoi( argv[2] );
	handle->shmid  = atoi( argv[3] );

	/* Make sure to save the file descriptors so they'll be closed by FreeHandle() */
	handle->fds[0] = handle->reader;
	handle->fds[1] = handle->reader;

	if( handle->shmid != -1 )
	{
		handle->buffer = (Buffer *) shmat( handle->shmid, 0, 0 );
		if( handle->buffer == (void *)(-1) )
		{
			fprintf( stderr, "%s:%d: shmat(%d) failed; continuing without shared memory: %s\n", __FILE__, __LINE__, handle->shmid, strerror(errno) );
			handle->buffer = NULL;
			return handle;
		}

		int result = shmctl( handle->shmid, IPC_RMID, 0 );
		if( result )
		{
			fprintf( stderr, "%s:%d: Failed to free shared memory segment %d: %s", __FILE__, __LINE__, handle->shmid, strerror(errno) );
		}

		handle->bufWritePtr = handle->buffer->data;
		handle->buffer->bytesUsed = 0;
		handle->bufEnd = ((char *) handle->buffer) + BUFFER_SIZE;
	}

	return handle;
}

IPC::Message *
IPC::NewMessage( uint32_t msgType,
                 uint32_t arg1,
                 const void *arg2,
                 uint32_t arg2Len )
{
	if( arg2 == NULL )
	{
		arg2Len = 0;
	}

	IPC::Message *msg = (IPC::Message *) malloc( sizeof(*msg) + arg2Len );

	msg->type = msgType;
	msg->arg1 = arg1;
	msg->arg2Len = arg2Len;

	if( arg2Len != 0 )
	{
		memcpy( msg->arg2, arg2, arg2Len );
	}

	return msg;
}

void
IPC::FreeMessage( Message *msg )
{
	free(msg);
}

IPC::Result
IPC::Send( Handle *aHandle, const Message *msg )
{
	HandleImpl *handle = (HandleImpl *) aHandle;

	size_t len = sizeof(*msg) + msg->arg2Len;

	// If there's room in the buffer, use it
	if( (handle->bufWritePtr != NULL) &&
	    !(msg->type & Message::TYPE_RESPONSE_AUTOFLUSH_BASE) &&
	    ((handle->bufWritePtr + len) < handle->bufEnd) )
	{
		//fprintf( stderr, "Writing message to buffer\n" );
		memcpy( handle->bufWritePtr, msg, len );
		handle->bufWritePtr += len;
		handle->buffer->bytesUsed += len;
		return OK;
	}

	// Otherwise, we'll send it through the pipe.  First, though, flush the
	// buffer if necessary.

	if( handle->bufWritePtr != NULL )
	{
		// Tell the other side to process data in the shared buffer
		if( PipeSend( handle, TYPE_INTERNAL_FLUSH, 0, NULL, 0 ) != OK )
		{
			fprintf( stderr, "[pid %d] Failed to send TYPE_INTERNAL_FLUSH!\n", getpid() );
			return FAIL;
		}

		// Wait until the buffer is clear for us to clobber again
		Message *response = PipeReceive( handle );
		if( !response || response->type != TYPE_INTERNAL_FLUSH_OK )
		{
			fprintf( stderr, "[pid %d] Failed to receive TYPE_INTERNAL_FLUSH_OK! (response->type = 0x%x)\n", getpid(), response ? response->type : 0xdeadbeef );
			if( response )
			{
				FreeMessage( response );
			}

			return FAIL;
		}

		// Reset the buffer pointer
		handle->bufWritePtr = handle->buffer->data;
		handle->buffer->bytesUsed = 0;
	}

	if( !doSend( handle->writer, msg, len ) )
	{
		fprintf( stderr, "[pid %d] Pipe send failed\n", getpid() );
		return FAIL;
	}

	return OK;
}

IPC::Result
IPC::Send( Handle *aHandle, 
           uint32_t msgType,
           uint32_t arg1,
           const void *arg2,
           uint32_t arg2Len )
{
	Message *msg = NewMessage( msgType, arg1, arg2, arg2Len );
	if( !msg )
	{
		return FAIL;
	}

	IPC::Result result = IPC::Send( aHandle, msg );
	IPC::FreeMessage( msg );

	return result;
}

IPC::Result
IPC::SendError( Handle *aHandle, const char *fmt, ... )
{
	char *buf;
	jchar *utf16;
	int length;

	va_list args;
	va_start( args, fmt );
	vasprintf( &buf, fmt, args );

	utf16 = convertASCIItoUTF16( buf, &length );

	IPC::Result result =
		IPC::Send( aHandle,
		           IPC::Message::TYPE_RESPONSE_FINISHED_ERROR,
		           1,
		           utf16,
		           length * sizeof(*utf16) );

	free( buf );
	free( utf16 );

	return result;
}

IPC::Message *
IPC::NewErrorMessage( const char *fmt, ... )
{
	char *buf;
	jchar *utf16;
	int length;

	va_list args;
	va_start( args, fmt );
	vasprintf( &buf, fmt, args );

	utf16 = convertASCIItoUTF16( buf, &length );

	Message *message =
		NewMessage( IPC::Message::TYPE_RESPONSE_FINISHED_ERROR,
		            1,
		            utf16,
		            length * sizeof(*utf16) );

	free( buf );
	free( utf16 );

	return message;
}

IPC::Message *
IPC::Receive( Handle *aHandle )
{
	HandleImpl *handle = (HandleImpl *) aHandle;

	// See if there's anything for us in the buffer
	if( handle->bufReadPtr != NULL )
	{
		// If we're done reading from the buffer, reset it and get the next
		// message from the pipe
		if( handle->bufReadPtr == handle->buffer->data + handle->buffer->bytesUsed )
		{
			handle->bufReadPtr = NULL;
			if( PipeSend( handle, TYPE_INTERNAL_FLUSH_OK, 0, NULL, 0 ) != OK )
			{
				return NULL;
			}

			return PipeReceive( handle );
		}

		// Otherwise, fetch the next message from the buffer

		const Message *origMsg = (Message *) handle->bufReadPtr;
		assert( handle->bufReadPtr < handle->bufEnd );
		assert( handle->bufReadPtr + sizeof(*origMsg) < handle->bufEnd );
		size_t len = sizeof(*origMsg) + origMsg->arg2Len;
		assert( handle->bufReadPtr + len < handle->bufEnd );

		Message *msg = (IPC::Message *) malloc( len );
		memcpy( msg, origMsg, len );
		handle->bufReadPtr += len;

		return msg;
	}

	return PipeReceive( handle );
}

static IPC::Message *
PipeReceive( HandleImpl *handle )
{
	IPC::Message hdr;

	if( !doRecv( handle->reader, &hdr, sizeof(hdr) ) )
	{
		return NULL;
	}

	IPC::Message *msg = (IPC::Message *) malloc( sizeof(*msg) + hdr.arg2Len );

	memcpy( msg, &hdr, sizeof(hdr) );

	if( !doRecv( handle->reader, msg->arg2, hdr.arg2Len ) )
	{
		free(msg);
		return NULL;
	}

	// If we received a TYPE_INTERNAL_FLUSH message, set the read pointer and
	// continue reading
	if( msg->type == TYPE_INTERNAL_FLUSH )
	{
		free(msg);
		handle->bufReadPtr = handle->buffer->data;
		return IPC::Receive( handle );
	}

	// If we received a TYPE_INTERNAL_PING message, ignore it and continue reading
	if( msg->type == TYPE_INTERNAL_PING )
	{
		free(msg);
		return PipeReceive( handle );
	}

	return msg;
}

static IPC::Result
PipeSend( const HandleImpl *handle, 
          uint32_t msgType,
          uint32_t arg1,
          const void *arg2,
          uint32_t arg2Len )
{
	IPC::Message *msg = IPC::NewMessage( msgType, arg1, arg2, arg2Len );
	if( !msg )
	{
		return IPC::FAIL;
	}

	bool isOk = doSend( handle->writer, msg, sizeof(*msg) + arg2Len );
	IPC::FreeMessage( msg );

	if( !isOk )
	{
		return IPC::FAIL;
	}

	return IPC::OK;
}

static bool doRecv( int fd, void *msg, ssize_t len )
{
	char *data = (char *) msg;
	ssize_t nBytes = 0;
	while(nBytes < len)
	{
		ssize_t thisBytes = read(fd, data, len-nBytes);
		if(thisBytes > 0)
		{
			data += thisBytes;
			nBytes += thisBytes;
		}
		else if(thisBytes < 0)
		{
			return false;
		}
		else
		{
			break;
		}
	}

	return ( nBytes == len );
}

static bool doSend( int fd, const void *msg, ssize_t len )
{
	char *data = (char *) msg;
	ssize_t nBytes = 0;
	while(nBytes < len)
	{
		ssize_t thisBytes = write(fd, data, len-nBytes);
		if(thisBytes > 0)
		{
			data += thisBytes;
			nBytes += thisBytes;
		}
		else if(thisBytes < 0)
		{
			return false;
		}
		else
		{
			break;
		}
	}

	return ( nBytes == len );
}

#endif // MOZILLAPARSER_USE_IPC
