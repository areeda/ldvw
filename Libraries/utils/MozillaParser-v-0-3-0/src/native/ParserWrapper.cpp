#include <jni.h>
#include "MozillaParser.h"
#include "JavaContentSink.h"
#include "nsXPCOM.h"
#include "nsParserCIID.h"
#include "nsMetaCharsetCID.h"
#include "nsICategoryManager.h"
#include "nsServiceManagerUtils.h"

#include "ParserWrapper.h"

#include <unistd.h>
#include <stdarg.h>
#include "asprintf.h"

// Class IID's
static NS_DEFINE_CID(kParserCID, NS_PARSER_CID);
static NS_DEFINE_CID(kLoggingSinkCID, NS_LOGGING_SINK_CID);
static NS_DEFINE_CID(kNavDTDCID, NS_CNAVDTD_CID);
static NS_DEFINE_CID(kMetaCharsetCID,NS_META_CHARSET_CID);

static nsresult decode( nsIUnicodeDecoder *decoder, const char *inBuffer, uint32_t htmlNumBytes, nsAString &aOutBuffer );

ParserWrapper::ParserWrapper()
	: _errorMessage(        strdup( "No error" )           )
	, decoder(              NULL                           )
	, charsetSource(        kCharsetUninitialized          )
	, defaultDecoder(       NULL                           )
	, defaultCharset(       "ISO-8859-1"                   )
	, defaultCharsetSource( kCharsetFromWeakDocTypeDefault )
{
}

ParserWrapper::~ParserWrapper()
{
	free( _errorMessage );
	_errorMessage = NULL;
}

int ParserWrapper::init()
{
	nsresult result;

	// Register nsMetaCharsetObserver.  Note: we should be using
	// nsChardetModule, but this isn't yet available as of 2.0.0.14
	// (and nsI18nModule seems like overkill)
	nsCOMPtr<nsICategoryManager>
		categoryManager(do_GetService( NS_CATEGORYMANAGER_CONTRACTID, &result));
	if( NS_FAILED(result) )
	{
		return _setErrorMessage( "Unable to load the category manager: %x", result );
	}

	result = categoryManager->AddCategoryEntry( "parser-service-category",
	                                            "Meta Charset Service",
	                                            NS_META_CHARSET_CONTRACTID,
	                                            PR_TRUE, PR_TRUE, nsnull );

	if( NS_FAILED(result) )
	{
		return _setErrorMessage( "Failed to load Meta Charset Service: %x", result );
	}

	ccm = do_GetService(NS_CHARSETCONVERTERMANAGER_CONTRACTID, &result);
	if( NS_FAILED(result) )
	{
		return _setErrorMessage( "Failed to instantiate nsICharsetConverterManager: %x", result );
	}

	result = ccm->GetUnicodeDecoderRaw( "ISO-8859-1", &defaultDecoder );
	if( NS_FAILED(result) )
	{
		return _setErrorMessage( "Failed to get unicode decoder for charset ISO-8859-1: %x", result );
	}

	return 0;
}

void ParserWrapper::setContentSink( nsCOMPtr<JavaContentSink> contentSink )
{
	this->contentSink = contentSink;
}

void
ParserWrapper::setEncoding( const char *encoding, bool allowOverride )
{
	charset.Assign( encoding );
	if( allowOverride )
	{
		charsetSource = kCharsetFromUserDefault;
	}
	else
	{
		charsetSource = kCharsetFromChannel;
	}
}

int
ParserWrapper::parse( const char *htmlBytes, uint32_t htmlNumBytes )
{
	int result = this->_parse( htmlBytes, htmlNumBytes );

	/* Reset the original state */
	decoder = NULL;
	charset.Truncate();
	charsetSource = kCharsetUninitialized;

	return result;
}

const char *
ParserWrapper::getLastErrorMessage()
{
	return _errorMessage;
}

int
ParserWrapper::_parse( const char *htmlBytes, uint32_t htmlNumBytes )
{
	nsresult result;

	// We may have to try parsing twice, if decoder is null AND the document contains a <meta> tag
	while( true )
	{
		if( charset.IsEmpty() )
		{
			decoder = defaultDecoder;
			charset.Assign( defaultCharset );
			charsetSource = defaultCharsetSource;
		}
		else
		{
			result = ccm->GetUnicodeDecoder( charset.get(), &decoder );
			if( NS_FAILED(result) )
			{
				return _setErrorMessage( "Failed to get unicode decoder for charset %s: %x", charset.get(), result );
			}
		}

		nsString streamUTF16;
		if( NS_FAILED( result = decode( decoder, htmlBytes, htmlNumBytes, streamUTF16 ) ) )
		{
			return _setErrorMessage( "Failed to decode HTML: 0x%08x", result );
		}

		nsCOMPtr<nsIParser> parser(do_CreateInstance(kParserCID, &result));
		if( NS_FAILED(result) )
		{
			return _setErrorMessage( "Unable to create a parser: 0x%08x", result );
		}

		// Create a JavaContentSink object and set the file descriptor where
		// we want responses sent
		//contentSink->setParserVersion(parserVersion);

		// Create a CNavDTD dtd object and register the DTD to the parser :
		nsCOMPtr<nsIDTD> dtd(do_CreateInstance(kNavDTDCID, &result));
		if(NS_FAILED(result))
		{
			return _setErrorMessage( "Unable to create a dtd: %x", result );
		}

		parser->RegisterDTD(dtd);
		parser->SetDocumentCharset( charset, charsetSource );
		parser->SetContentSink( contentSink );

		// Parse
		result = parser->Parse(streamUTF16, 0, NS_LITERAL_CSTRING("text/html"), PR_FALSE, PR_TRUE);

		// See if we got interrupted; if so, try again with the new character set
		if( contentSink->getWebShell()->wasStopped() &&
			contentSink->getWebShell()->getNewCharset() != NULL &&
			contentSink->getWebShell()->getNewCharsetSource() > charsetSource )
		{
			charset.Assign( contentSink->getWebShell()->getNewCharset() );
			charsetSource = contentSink->getWebShell()->getNewCharsetSource();
			contentSink->getWebShell()->reset();
			contentSink->reset();
		}
		else
		{
			break;
		}
	}

	return 0;
}

int
ParserWrapper::_setErrorMessage( const char *fmt, ... )
{
	va_list args;
	va_start( args, fmt );

	free( _errorMessage );
	vasprintf( &_errorMessage, fmt, args );

	return 1;
}

nsresult
decode( nsIUnicodeDecoder *decoder, const char *inBuffer, uint32_t htmlNumBytes, nsAString &aOutBuffer )
{
	// Mostly taken from nsXMLHttpRequest.cpp

	nsresult rv;
	PRInt32 outBufferLength;
	PRInt32 dataLen = htmlNumBytes;

	rv = decoder->GetMaxLength(inBuffer, dataLen, &outBufferLength);
	if( NS_FAILED(rv) )
	{
		return rv;
	}

	PRUnichar *outBuffer = (PRUnichar *) nsMemory::Alloc( (outBufferLength+1) * sizeof(PRUnichar) );

	if( !outBuffer )
	{
		return NS_ERROR_OUT_OF_MEMORY;
	}

	PRInt32 totalChars = 0,
	        outBufferIndex = 0,
	        outLen = outBufferLength;

	do
	{
		PRInt32 inBufferLength = dataLen;
		rv = decoder->Convert( inBuffer,
		                       &inBufferLength,
		                       &outBuffer[outBufferIndex],
		                       &outLen );
		totalChars += outLen;
		if( NS_FAILED(rv) )
		{
			// We consume one byte, replace it with U+FFFD
			// and try the conversion again.
			outBuffer[outBufferIndex + outLen++] = (PRUnichar)0xFFFD;
			outBufferIndex += outLen;
			outLen = outBufferLength - (++totalChars);

			decoder->Reset();

			if((inBufferLength + 1) > dataLen) {
				inBufferLength = dataLen;
			} else {
				inBufferLength++;
			}

			inBuffer = &inBuffer[inBufferLength];
			dataLen -= inBufferLength;
		}
	} while ( NS_FAILED(rv) && (dataLen > 0) );

	aOutBuffer.Assign(outBuffer, totalChars);
	nsMemory::Free(outBuffer);

	return NS_OK;
}
