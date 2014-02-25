/* -*- Mode: C++; tab-width: 2; indent-tabs-mode: nil; c-basic-offset: 2 -*- */
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
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

#ifndef JAVA_CONTENT_SINK_H__
#define JAVA_CONTENT_SINK_H__

#include "nsCOMPtr.h"
#include "nsIHTMLContentSink.h"
#include "JavaWebShellServices.h"
#include "nsIParser.h"
#include "nsIParserService.h"
#include "ParserInstruction.h"

#include "IPC.h"
#include <jni.h>
#include <stdlib.h>

class JavaContentSink : public nsIHTMLContentSink
{
public:
#if MOZILLAPARSER_USE_IPC
	JavaContentSink();
#else // !MOZILLAPARSER_USE_IPC
	JavaContentSink( JNIEnv *env, jobject mozillaParserObj );
#endif // MOZILLAPARSER_USE_IPC
	virtual ~JavaContentSink();

	void SetProxySink(nsIHTMLContentSink *aSink)
	{
		mSink=aSink;    
		NS_ADDREF(mSink);
	}

	void ReleaseProxySink()
	{
		NS_IF_RELEASE(mSink);
		mSink=0;
	}

	// nsISupports
	NS_DECL_ISUPPORTS

	// nsIContentSink
	NS_IMETHOD WillBuildModel();
	NS_IMETHOD DidBuildModel();
	NS_IMETHOD WillInterrupt();
	NS_IMETHOD WillResume();
	NS_IMETHOD SetParser(nsIParser* aParser);
	NS_IMETHOD OpenContainer(const nsIParserNode& aNode);
	NS_IMETHOD CloseContainer(const nsHTMLTag aTag);
	NS_IMETHOD AddHeadContent(const nsIParserNode& aNode);
	NS_IMETHOD AddLeaf(const nsIParserNode& aNode);
	NS_IMETHOD AddComment(const nsIParserNode& aNode);
	NS_IMETHOD AddProcessingInstruction(const nsIParserNode& aNode);
	NS_IMETHOD AddDocTypeDecl(const nsIParserNode& aNode);
	virtual void FlushPendingNotifications(mozFlushType aType) { }
	NS_IMETHOD SetDocumentCharset(nsACString& aCharset) { 
		printf("***************************** setting document charset *********************");
		return NS_OK; }
		virtual nsISupports *GetTarget() { return nsnull; }

	// nsIHTMLContentSink
	NS_IMETHOD SetTitle(const nsString& aValue);
	NS_IMETHOD OpenHTML(const nsIParserNode& aNode);
	NS_IMETHOD CloseHTML();
	NS_IMETHOD OpenHead(const nsIParserNode& aNode);
	NS_IMETHOD CloseHead();
	NS_IMETHOD OpenBody(const nsIParserNode& aNode);
	NS_IMETHOD CloseBody();
	NS_IMETHOD OpenForm(const nsIParserNode& aNode);
	NS_IMETHOD CloseForm();
	NS_IMETHOD OpenMap(const nsIParserNode& aNode);
	NS_IMETHOD CloseMap();
	NS_IMETHOD OpenFrameset(const nsIParserNode& aNode);
	NS_IMETHOD CloseFrameset();
	NS_IMETHOD IsEnabled(PRInt32 aTag, PRBool* aReturn)
		{ /* Take the largest possible feature set. */ NS_ENSURE_ARG_POINTER(aReturn); *aReturn = PR_TRUE; return NS_OK; }
	NS_IMETHOD NotifyTagObservers(nsIParserNode* aNode);
	NS_IMETHOD_(PRBool) IsFormOnStack() { return PR_FALSE; }

	NS_IMETHOD BeginContext(PRInt32 aPosition);
	NS_IMETHOD EndContext(PRInt32 aPosition);
	NS_IMETHOD WillProcessTokens(void) { return NS_OK; }
	NS_IMETHOD DidProcessTokens(void) { return NS_OK; }
	NS_IMETHOD WillProcessAToken(void) { return NS_OK; }
	NS_IMETHOD DidProcessAToken(void) { return NS_OK; }

	nsresult OpenNode( const nsAString &aKind, const nsIParserNode& aNode );
	nsresult CloseNode( const nsAString &aKind );
	nsresult LeafNode( const nsIParserNode &aNode );
	nsresult WriteAttributes( const nsIParserNode &aNode );

	void reset();
	nsresult callback( int arg1, const nsAString &arg2 );
	void setParserVersion(double aParserVersion){ parserVersion = aParserVersion; }
#if MOZILLAPARSER_USE_IPC
	void setIPCHandle( IPC::Handle *aHandle ) { this->ipcHandle = aHandle; }
	/**
	 * Simulate a crash after aCrashInterval callbacks have been made.  If
	 * aCrashHard is true, this will result in a segmentation fault; otherwise,
	 * the process will simply exit quietly.
	 */
	void setCrashInterval( size_t aCrashInterval, bool aCrashHard )
	{
		this->_crashInterval = aCrashInterval;
		this->_crashHard     = aCrashHard;

		fprintf( stderr, "setCrashInterval(%lu, %s)\n", aCrashInterval, aCrashHard ? "true" : "false" );
	}

#endif // MOZILLAPARSER_USE_IPC

	// debug function :
	static void setDebug(bool aDebug) { doDebug = aDebug; }
	static bool isDebug() { return doDebug; }
	JavaWebShellServices *getWebShell() const { return mWebShell; }

protected:
#if MOZILLAPARSER_USE_IPC
	IPC::Handle *ipcHandle;
	size_t _callbackCount;
	size_t _crashInterval;
	bool _crashHard;
#else // !MOZILLAPARSER_USE_IPC
	JNIEnv *_env;
	jobject _mozillaParserObj;
	jmethodID _callbackMethod;
	jmethodID _resetMethod;
#endif // !MOZILLAPARSER_USE_IPC

	nsIHTMLContentSink   *mSink;
	nsIParser            *mParser;
	JavaWebShellServices *mWebShell;
	nsCOMPtr<nsIObserverEntry> mObservers;

	double parserVersion;

	static bool doDebug;
};

#endif // JAVA_CONTENT_SINK_H__

