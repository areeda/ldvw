#ifndef _IPC_H_
#define _IPC_H_

#if MOZILLAPARSER_USE_IPC

#include <sys/types.h>
#include <stdint.h>

/** The size of the shared-memory buffer */
#define BUFFER_SIZE (1024 * 1024)

/**
 * The minimum buffer size to use shared memory.  If BUFFER_SIZE is less than
 * this value, we'll just use pipes.
 */
#ifdef PIPE_SIZE
#define MIN_BUFFER_SIZE PIPE_SIZE
#else
#define MIN_BUFFER_SIZE 4096
#endif

/**
 * Helper methods for communicating between processes
 */
class IPC
{
public:
	enum Result { OK=0, FAIL };

	/**
	 * Encapsulates a message consisting of an integer and a character array.
	 */
	struct Message
	{
		// Parent->Child: Sending parameter part of request.
		//   arg1: One of the PARAM_* macros
		//   arg2: Depends on the value of arg1
		static const uint32_t TYPE_REQUEST_PARAMS          = 0x01;

		// Parent->Child: Sending data part of request.  No more TYPE_REQUEST_*
		// messages will be sent until a TYPE_RESPONSE_FINISHED_* response is
		// received.
		//   arg1: Ignored
		//   arg2: The raw data to parse.  It is decoded using the encoding
		//         specified via a PARAM_ENCODING parameter (if unspecified,
		//         defaults to "ISO-8859-1")
		static const uint32_t TYPE_REQUEST_DATA            = 0x02;

		// Child->Parent: Sending some response data.  The worker will send
		// zero or more of these responses before sending exactly one
		// TYPE_RESPONSE_FINISHED_* message.
		//   arg1: Parser instruction
		//   arg2: Data for the parser instruction, encoded in UTF-16
		static const uint32_t TYPE_RESPONSE_DATA           = 0x03;

		// Child->Parent: Reset the parse instruction stack.  This message is
		// sent when the parse must be interrupted and restarted because a
		// <META> encoding is detected.
		//   arg1: Ignored
		//   arg2: Ignored
		static const uint32_t TYPE_RESPONSE_RESET          = 0x04;

		// Child->Parent: Base for "finished" responses (these messages will be
		// flushed immediately)
		static const uint32_t TYPE_RESPONSE_AUTOFLUSH_BASE  = 0x08;

		// Child->Parent: Worker finished successfully and is ready for the
		// next TYPE_REQUEST_PARAMS or TYPE_REQUEST_DATA message.
		//   arg1: Ignored
		//   arg2: Ignored
		static const uint32_t TYPE_RESPONSE_FINISHED_OK    =
			TYPE_RESPONSE_AUTOFLUSH_BASE | 0x00;

		// Child->Parent: Worker finished with an error and is ready for the
		// next TYPE_REQUEST_PARAMS or TYPE_REQUEST_DATA message.
		//   arg1: Error code (optional)
		//   arg2: Error message, encoded in UTF-16 (optional)
		static const uint32_t TYPE_RESPONSE_FINISHED_ERROR =
			TYPE_RESPONSE_AUTOFLUSH_BASE | 0x01;

		// Any->Any: Base for internally used message types
		static const uint32_t TYPE_INTERNAL_BASE           = 0x10;

		// For messages of type TYPE_REQUEST_PARAMS: specify the encoding
		// scheme (charset) to use for decoding the data received in the next
		// TYPE_REQUEST_DATA message.
		//   arg2: The character set (encoded in ASCII / ISO-8859-1 with NULL
		//         termination)
		static const uint32_t PARAM_ENCODING               = 0x01;

		// For messages of type TYPE_REQUEST_PARAMS: specify the default
		// encoding scheme (charset) to use for decoding the data received in
		// the next TYPE_REQUEST_DATA message.  This differs from
		// PARAM_ENCODING in that a <META> tag in the HTML body can override
		// the character set (whereas an encoding specified via
		// PARAM_ENCODING will take precedence).
		//   arg2: The character set (encoded in ASCII / ISO-8859-1 with NULL
		//         termination)
		static const uint32_t PARAM_DEFAULT_ENCODING       = 0x02;

		// (TESTING ONLY)
		// For messages of type TYPE_REQUEST_PARAMS: tell the worker to
		// simulate a parser crash.
		//   arg2: The low bits specify the number of TYPE_RESPONSE_DATA
		//         messages to send before the crash.  If the high bit
		//         (0x80000000) is set, this will cause an actual segmentation
		//         fault; otherwise, the child process will simply exit
		//         unexpectedly.
		static const uint32_t PARAM_CRASH                  = 0x08;

		uint32_t type;
		uint32_t arg1;
		uint32_t arg2Len;
		char arg2[0];
	};

	/**
	 * A handle identifying an endpoint of an IPC connection
	 */
	struct Handle
	{
		int reader;
		int writer;
		bool isAlive() const;
	};

	/**
	 * Create a new IPC handle for communicating between a pair of processes.
	 * Before using the handle (and after the fork()), you must call
	 * BindHandle() from both sides to complete initialization.
	 */
	static Handle *NewHandle();

	/**
	 * Free a previously created handle.
	 */
	static void FreeHandle( Handle *aHandle );

	/**
	 * Bind the handle to the child or parent process.
	 *
	 * @param aHandle   The handle to bind
	 * @param isParent  Indicates whether this is the parent or child process.
	 */
	static void BindHandle( Handle *aHandle, bool isParent );

	/**
	 * Generate the argv array to pass to a child process so that it can
	 * resurrect the Handle.  The caller is responsible for calling free() on
	 * the returned pointer.
	 */
	static char *const *FreezeHandle( const Handle *aHandle );

	/**
	 * Given the argv array of a child process, reconstruct the Handle object.
	 * Must be called after the BindHandle() call (which in turn must be called
	 * before the exec() call).
	 */
	static Handle *ThawHandle( int argc, const char *const *argv );

	/**
	 * Construct a new message.
	 */
	static Message *NewMessage( uint32_t msgType,
	                            uint32_t arg1,
	                            const void *arg2,
	                            uint32_t arg2Len );

	/**
	 * Free a message previous constructed with NewMessage() or Receive()
	 */
	static void FreeMessage( Message *msg );

	/**
	 * Send the specified message using aHandle.
	 */
	static Result Send( Handle *aHandle, const Message *message );

	/**
	 * Construct a message (msgType, arg1, arg2, arg2Len); send it to aHandle;
	 * then immediately free the message.  Returns FAIL if either message
	 * construction or sending fails.
	 */
	static Result Send( Handle *aHandle,
	                    uint32_t msgType,
	                    uint32_t arg1,
	                    const void *arg2,
	                    uint32_t arg2Len );

	/**
	 * Helper function: send the specified error message using aHandle.
	 * Uses printf syntax.
	 */
	static Result SendError( Handle *aHandle, const char *fmt, ... );

	/**
	 * Helper function: generate an error message.  Uses printf syntax.
	 */
	static Message *NewErrorMessage( const char *fmt, ... );

	/**
	 * Receive a message using aHandle.  Returns NULL if an error occurs
	 * (message is corrupted or underlying connection is closed).  The
	 * resulting message should be freed with free().
	 */
	static Message *Receive( Handle *aHandle );
};

#endif // MOZILLAPARSER_USE_IPC

#endif // _IPC_H_
