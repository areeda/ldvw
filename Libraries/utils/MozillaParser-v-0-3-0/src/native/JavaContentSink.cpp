/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *+
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Mozilla Communicator client code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1998
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
#include "nsIAtom.h"
#include "nsStringAPI.h"
#include "JavaContentSink.h"
#include "nsString.h"
#include "nsReadableUtils.h"
#include "prprf.h"
#include "nsStaticAtom.h"
#include "nsAString.h"
#include "nsContentUtils.h"
#include <unistd.h>
#include <stdint.h>

#include "JavaWebShellServices.h"
#include "IPC.h"

#ifdef WINDOWS

#include <stdarg.h>

static void JCS_DEBUG(const char *fmt, ...)
{
	va_list args;
	va_start( args, fmt );

	if( JavaContentSink::isDebug() )
		vfprintf( stderr, fmt, args );
}

static void JCS_WARN(const char *fmt, ...)
{
	va_list args;
	va_start( args, fmt );

	vfprintf( stderr, fmt, args );
}

#else // !WINDOWS

#define JCS_DEBUG(args...) do { \
	if(JavaContentSink::isDebug()) \
		fprintf( stderr, args ); \
} while(0)

#define JCS_WARN(args...) do { \
	fprintf( stderr, args ); \
} while(0)

#endif // WINDOWS

#define JCS_CSTR(s) (NS_ConvertUTF16toUTF8((s)).get())

bool JavaContentSink::doDebug = false;

// list of tags that have skipped content
static const char gSkippedContentTags[] = {
  eHTMLTag_style,
  eHTMLTag_script,
  eHTMLTag_server,
  eHTMLTag_title,
  0
};

static nsString
GetStringValue( nsHTMLTag aTag )
{
	nsresult rv;

	nsCOMPtr<nsIParserService> parserService(do_GetService(NS_PARSERSERVICE_CONTRACTID, &rv));

	if( NS_FAILED(rv) )
	{
		JCS_WARN( "Can't get parser service!\n" );
		return NS_LITERAL_STRING("???");
	}

	return nsString( parserService->HTMLIdToStringTag(aTag) );
}

static nsString
GetValidName( const nsAString &oldName )
{
	nsString result;

	nsAString::const_iterator begin, end;

	oldName.BeginReading(begin);
	oldName.EndReading(end);

	for( ; begin != end ; ++begin )
	{
		if( (*begin >= 'a' && *begin <= 'z') ||
		    (*begin >= 'A' && *begin <= 'Z') )
		{
			result += *begin;
		}
	}

	if( result.IsEmpty() )
	{
		result.AssignLiteral( "dummy" );
	}

	return result;
}

static PRInt32
ResolveCharacterEntity( const nsAutoString &entityStr )
{
	nsresult rv;

	nsCOMPtr<nsIParserService> parserService(do_GetService(NS_PARSERSERVICE_CONTRACTID, &rv));

	JCS_DEBUG( "Trying to resolve character entity: [%s]\n", JCS_CSTR(entityStr) );

	if( NS_FAILED(rv) )
	{
		JCS_WARN( "Can't get parser service!\n" );
		return -1;
	}

	PRInt32 entityChar;
	parserService->HTMLConvertEntityToUnicode( entityStr, &entityChar );
	if (entityChar == -1 && 
		!entityStr.IsEmpty() &&
		entityStr.First() == (PRUnichar) '#')
	{
		PRInt32 err = 0;
		entityChar = entityStr.ToInteger( &err, kAutoDetect );  // NCR
	}

	if( entityChar == -1 )
	{
		JCS_DEBUG( "Failed resolving entity [%s]!\n", JCS_CSTR(entityStr) );
	}
	else
	{
		nsAutoString entityCharStr;
		entityCharStr.Append( PRUnichar(entityChar) );
		JCS_DEBUG( "Resolved to: %s\n", JCS_CSTR(entityCharStr) );
	}

	return entityChar;
}

#if MOZILLAPARSER_USE_IPC
JavaContentSink::JavaContentSink()
	: ipcHandle( NULL )
	, _callbackCount( 0 )
	, _crashInterval( 0 )
	, _crashHard( false )
#else // !MOZILLAPARSER_USE_IPC
JavaContentSink::JavaContentSink( JNIEnv *env, jobject mozillaParserObj )
	: _env( env )
	, _mozillaParserObj( mozillaParserObj )
	, _callbackMethod( _env->GetMethodID(_env->GetObjectClass(_mozillaParserObj), "callback", "(I[B)V") )
	, _resetMethod( _env->GetMethodID(_env->GetObjectClass(_mozillaParserObj), "resetInstructionPool", "()V") )
#endif // MOZILLAPARSER_USE_IPC
	, mSink( NULL )
	, mParser( NULL )
	, mWebShell( new JavaWebShellServices() )
	, mObservers( nsnull )
	, parserVersion( 2.0 )
{
	// JCS_DEBUG("initializing JavaContentSink\n");
	//fprintf( stderr, "JavaContentSink CONSTRUCTOR - this = 0x%08x\n", this );

	NS_ADDREF( mWebShell );
}

JavaContentSink::~JavaContentSink()
{
	//fprintf( stderr, "JavaContentSink DESTRUCTOR - this = 0x%08x\n", this );

	NS_IF_RELEASE( mSink );
	NS_IF_RELEASE( mWebShell );

	mSink = 0;
	mParser = 0;
	mWebShell = 0;
}

NS_IMPL_ISUPPORTS2(JavaContentSink, nsIContentSink, nsIHTMLContentSink)

NS_IMETHODIMP
JavaContentSink::WillBuildModel()
{
	JCS_DEBUG("<begin>\n");
	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::DidBuildModel()
{
	JCS_DEBUG( "</begin>\n");
	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::WillInterrupt()
{
	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::WillResume()
{
	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::SetParser(nsIParser* aParser)  
{
	nsresult rv = NS_OK;

	mParser = aParser;

	if( !mObservers )
	{
		nsCOMPtr<nsIParserService> parserService(do_GetService(NS_PARSERSERVICE_CONTRACTID, &rv));

		if( NS_FAILED(rv) )
		{
			JCS_WARN( "Can't get parser service!\n" );
			return rv;
		}

		parserService->GetTopicObservers(NS_LITERAL_STRING("text/html"),
		                                 getter_AddRefs(mObservers));

		//fprintf( stderr, "OK got observers\n" );
	}

	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::OpenContainer(const nsIParserNode& aNode)
{
	return OpenNode( NS_LITERAL_STRING("container"), aNode);
}

NS_IMETHODIMP
JavaContentSink::CloseContainer(const nsHTMLTag aTag)
{
	nsresult theResult=NS_OK;
	nsHTMLTag nodeType = nsHTMLTag(aTag);
	if ((nodeType >= eHTMLTag_unknown) &&
		(nodeType <= nsHTMLTag(NS_HTML_TAG_MAX)))
	{
		theResult = CloseNode( GetStringValue(nodeType) );
	}
	else
	{
		theResult = CloseNode( NS_LITERAL_STRING("???") );
	}
	return theResult;
}

NS_IMETHODIMP
JavaContentSink::AddHeadContent(const nsIParserNode& aNode)
{
	nsresult theResult = NS_OK;

	eHTMLTags type = (eHTMLTags)aNode.GetNodeType();

	if( type == eHTMLTag_title )
	{
		nsCOMPtr<nsIDTD> dtd;
		mParser->GetDTD(getter_AddRefs(dtd));
		NS_ENSURE_TRUE(dtd, NS_ERROR_FAILURE);

		nsString theString;
		PRInt32 lineNo = 0;

		dtd->CollectSkippedContent(type, theString, lineNo);
		theResult = SetTitle(theString);
	}
	else
	{
		theResult = LeafNode(aNode);
	}

	return theResult;
}

NS_IMETHODIMP
JavaContentSink::AddLeaf( const nsIParserNode &aNode )
{
	return LeafNode(aNode);
} 

/**
 *  This gets called by the parser when you want to add
 *  a comment node to the current container in the content
 *  model.
 *  
 *  @updated gess 3/25/98
 *  @param   
 *  @return  
 */
NS_IMETHODIMP
JavaContentSink::AddComment( const nsIParserNode &aNode )
{
	callback( ParserInstruction::AddComment, aNode.GetText() );
	return NS_OK;
}

/**
 *  This gets called by the parser when you want to add
 *  a PI node to the current container in the content
 *  model.
 *  
 *  @updated gess 3/25/98
 *  @param   
 *  @return  
 */
NS_IMETHODIMP
JavaContentSink::AddProcessingInstruction(const nsIParserNode &aNode)
{
	callback( ParserInstruction::AddProcessingInstruction, aNode.GetText() );
	return NS_OK;
}

/**
 *  This gets called by the parser when it encounters
 *  a DOCTYPE declaration in the HTML document.
 */

NS_IMETHODIMP
JavaContentSink::AddDocTypeDecl(const nsIParserNode &aNode)
{
	callback( ParserInstruction::AddDoctypeDecl, aNode.GetText() );
	return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::SetTitle( const nsString &aValue )
{
	callback( ParserInstruction::SetTitle, aValue );

	nsresult theResult=NS_OK;
	return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenHTML( const nsIParserNode &aNode )
{
	return OpenNode( NS_LITERAL_STRING("html"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseHTML()
{
	return CloseNode( NS_LITERAL_STRING("html") );
}

NS_IMETHODIMP
JavaContentSink::OpenHead( const nsIParserNode &aNode )
{
	return OpenNode( NS_LITERAL_STRING("head"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseHead()
{
	return CloseNode( NS_LITERAL_STRING("head") );
}

NS_IMETHODIMP
JavaContentSink::OpenBody( const nsIParserNode &aNode )
{
	return OpenNode( NS_LITERAL_STRING("body"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseBody()
{
	return CloseNode( NS_LITERAL_STRING("body") );
}

NS_IMETHODIMP
JavaContentSink::OpenForm( const nsIParserNode &aNode )
{
	return OpenNode( NS_LITERAL_STRING("form"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseForm()
{
	return CloseNode( NS_LITERAL_STRING("form") );
}

NS_IMETHODIMP
JavaContentSink::OpenMap(const nsIParserNode &aNode)
{
	return OpenNode( NS_LITERAL_STRING("map"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseMap()
{
	return CloseNode( NS_LITERAL_STRING("map") );
}

NS_IMETHODIMP
JavaContentSink::OpenFrameset( const nsIParserNode &aNode )
{
	return OpenNode( NS_LITERAL_STRING("frameset"), aNode );
}

NS_IMETHODIMP
JavaContentSink::CloseFrameset()
{
	return CloseNode( NS_LITERAL_STRING("frameset") );
}

NS_IMETHODIMP
JavaContentSink::NotifyTagObservers( nsIParserNode *aNode )
{
	if( !mObservers )
	{
		return NS_OK;
	}

	return mObservers->Notify( aNode, mParser, mWebShell, 0 );
}

/**
 * This gets called when handling illegal contents, especially
 * in dealing with tables. This method creates a new context.
 * 
 * @update 04/04/99 harishd
 * @param aPosition - The position from where the new context begins.
 */
NS_IMETHODIMP
JavaContentSink::BeginContext(PRInt32 aPosition) 
{
	return NS_OK;
}

/**
 * This method terminates any new context that got created by
 * BeginContext and switches back to the main context.  
 *
 * @update 04/04/99 harishd
 * @param aPosition - Validates the end of a context.
 */
NS_IMETHODIMP
JavaContentSink::EndContext(PRInt32 aPosition)
{
	return NS_OK;
}

nsresult
JavaContentSink::OpenNode( const nsAString &aKind, const nsIParserNode &aNode )
{
	JCS_DEBUG( "<open container=" );

	nsresult theResult = NS_OK;
	nsHTMLTag nodeType = nsHTMLTag(aNode.GetNodeType());

	if( (nodeType >= eHTMLTag_unknown) &&
	    (nodeType <= nsHTMLTag(NS_HTML_TAG_MAX)) )
	{
		nsString tag = GetStringValue(nodeType);
		JCS_DEBUG( "\"%s\"", JCS_CSTR(tag) );
		theResult = callback( ParserInstruction::OpenNode, tag );
	}
	else
	{
		JCS_DEBUG( "\"%s\"", JCS_CSTR(aNode.GetText()) );
		theResult = callback( ParserInstruction::OpenNode, GetValidName(aNode.GetText()) );
	}

	if( theResult != NS_OK )
	{
		return theResult;
	}

	JCS_DEBUG( ">\n" );
	theResult = WriteAttributes( aNode );
	JCS_DEBUG( "</open>\n" );

	return theResult;
}

nsresult
JavaContentSink::CloseNode( const nsAString &aKind )
{
	callback( ParserInstruction::CloseNode, aKind );
	JCS_DEBUG( "</open container=\"%s\">\n", JCS_CSTR(aKind) );
	return NS_OK;
}

nsresult
JavaContentSink::LeafNode( const nsIParserNode &aNode )
{
	nsHTMLTag nodeType = nsHTMLTag(aNode.GetNodeType());

	if( (nodeType >= eHTMLTag_unknown) &&
	    (nodeType <= nsHTMLTag(NS_HTML_TAG_MAX)) )
	{
		nsString tag = GetStringValue(nodeType);

		JCS_DEBUG( "<leaf tag=\"%s\"", JCS_CSTR(tag) );
		callback( ParserInstruction::AddLeaf, tag );

		JCS_DEBUG( ">\n" );
		WriteAttributes( aNode );
		JCS_DEBUG( "</leaf>\n" );

		callback( ParserInstruction::CloseLeaf, EmptyString() );
	}
	else 
	{
		nsAutoString tmp;
		PRInt32 pos = 0;
		PRUnichar last = 0;
		PRInt32 entityChar;

		switch (nodeType) 
		{
			case eHTMLTag_whitespace:
			case eHTMLTag_text:
				JCS_DEBUG( "<text value=\"%s\"/>\n", JCS_CSTR(aNode.GetText()) );
				callback( ParserInstruction::AddText, aNode.GetText() );
				break;

			case eHTMLTag_newline:
				callback( ParserInstruction::AddText, NS_LITERAL_STRING("\n") );
				break;

			case eHTMLTag_entity:
				tmp.Append(aNode.GetText());

				/* Try resolving the character entity */
				entityChar = ResolveCharacterEntity(tmp);
				if( entityChar != -1 )
				{
					tmp.Truncate();
					tmp.Append( PRUnichar(entityChar) );
					JCS_DEBUG( "<text value=\"%s\"/>\n", JCS_CSTR(tmp) );
					callback( ParserInstruction::AddText, tmp );
				}
				else
				{
					/* Trim off the trailing ';' */
					last = tmp.Last();
					pos = tmp.Length();
					if (pos >= 0 && last == ';') {
						tmp.Cut(pos-1, 1);
					}

					JCS_DEBUG( "<entity value=\"%s\"/>\n", JCS_CSTR(tmp) );
					callback( ParserInstruction::AddEntity, tmp );
				}

				break;

			default:
				JCS_WARN( "Error : Unsupported node type : %d", nodeType );
		}//switch
	}

	return NS_OK;
}

nsresult
JavaContentSink::WriteAttributes( const nsIParserNode& aNode )
{
	nsAutoString tmp;
	PRInt32 ac = aNode.GetAttributeCount();

	nsHTMLTag nodeType = nsHTMLTag(aNode.GetNodeType());
	nsString tag = GetStringValue(nodeType);

	JCS_DEBUG( "WriteAttributes: node is %s\n", JCS_CSTR(tag) );

	for( PRInt32 i = 0; i < ac; ++i )
	{
		const nsAString& k = aNode.GetKeyAt(i);
		const nsAString& v = aNode.GetValueAt(i);

		callback( ParserInstruction::WriteAttributeKey, k ); 
		callback( ParserInstruction::WriteAttributeValue, v );
	}

	if( 0 != strchr(gSkippedContentTags, aNode.GetNodeType()) )
	{
		nsCOMPtr<nsIDTD> dtd;
		mParser->GetDTD(getter_AddRefs(dtd));
		NS_ENSURE_TRUE(dtd, NS_ERROR_FAILURE);

		nsString content;
		PRInt32 lineNo = 0;

		dtd->CollectSkippedContent(aNode.GetNodeType(), content, lineNo);
		JCS_DEBUG( "WriteAttributes: Skipped content is [%s]\n", JCS_CSTR(content) );
		JCS_DEBUG( " <content value=\"");
		JCS_DEBUG( "%s\"/>\n", JCS_CSTR(content) );
		callback( ParserInstruction::AddContent, content);
	}

	return NS_OK;
}

void
JavaContentSink::reset()
{
#if MOZILLAPARSER_USE_IPC
	IPC::Send( ipcHandle,
			   IPC::Message::TYPE_RESPONSE_RESET,
			   0,
			   NULL,
			   0 );
#else // !MOZILLAPARSER_USE_IPC
	_env->CallVoidMethod( _mozillaParserObj, _resetMethod );
#endif // MOZILLAPARSER_USE_IPC
}

nsresult
JavaContentSink::callback( int arg1, const nsAString &arg2 )
{
	const PRUnichar *arg2Data;
	PRUint32 arg2Len = NS_StringGetData( arg2, &arg2Data );
	uint32_t arg2Bytes = arg2Len * sizeof(*arg2Data);

	JCS_DEBUG( "arg1: %d, arg2: %s\n", arg1, JCS_CSTR(arg2) );

#if MOZILLAPARSER_USE_IPC

	++_callbackCount;

	if( _crashInterval && _callbackCount > _crashInterval )
	{
		fprintf( stderr, "Crash interval exceeded; simulating crash\n" );
		if( _crashHard )
		{
			*((int *) NULL) = 1;
		}
		else
		{
			exit(1);
		}
	}

	IPC::Result result =
		IPC::Send( ipcHandle,
		           IPC::Message::TYPE_RESPONSE_DATA,
		           arg1,
		           arg2Data,
		           arg2Bytes );

	if( result == IPC::OK )
	{
		return NS_OK;
	}

	return NS_ERROR_FAILURE;

#else // !MOZILLAPARSER_USE_IPC

	jbyteArray byteArray = _env->NewByteArray( arg2Bytes );
	_env->SetByteArrayRegion( byteArray, 0, arg2Bytes, (jbyte *) arg2Data );
	_env->CallVoidMethod( _mozillaParserObj, _callbackMethod, arg1, byteArray );

	return NS_OK;

#endif // MOZILLAPARSER_USE_IPC
}
