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
#include "JavaContentSink.h"
#include "nsHTMLTags.h"
#include "nsString.h"
#include "nsReadableUtils.h"
#include "prprf.h"
#include "nsStaticAtom.h"
#include "nsAString.h"

static NS_DEFINE_IID(kIContentSinkIID, NS_ICONTENT_SINK_IID);
static NS_DEFINE_IID(kIHTMLContentSinkIID, NS_IHTML_CONTENT_SINK_IID);
static NS_DEFINE_IID(kILoggingSinkIID, NS_ILOGGING_SINK_IID);
static NS_DEFINE_IID(kISupportsIID, NS_ISUPPORTS_IID);

// list of tags that have skipped content
static const char gSkippedContentTags[] = {
  eHTMLTag_style,
  eHTMLTag_script,
  eHTMLTag_server,
  eHTMLTag_title,
  0
};



#include "JavaContentSinkHack.h"

bool JavaContentSink::doDebug=false;

JavaContentSink::JavaContentSink() {
// printf("initializing JavaContentSink\n");
  mLevel=-1;
  mSink=0;
  mParser=0;
}

JavaContentSink::~JavaContentSink() { 
  mSink=0;
}

NS_IMETHODIMP_(nsrefcnt) JavaContentSink::AddRef(){ return 0; }
NS_IMETHODIMP_(nsrefcnt) JavaContentSink::Release(){ return 0; }

nsresult
JavaContentSink::QueryInterface(const nsIID& aIID, void** aInstancePtr)
{
 //printf("in QueryInterface");
  NS_PRECONDITION(nsnull != aInstancePtr, "null ptr");
  if (nsnull == aInstancePtr) {                                            
    return NS_ERROR_NULL_POINTER;                                        
  }                                                                      
  if (aIID.Equals(kISupportsIID)) {
    nsISupports* tmp = this;
    *aInstancePtr = (void*) tmp;
  }
  else if (aIID.Equals(kIContentSinkIID)) {
    nsIContentSink* tmp = this;
    *aInstancePtr = (void*) tmp;
  }
  else if (aIID.Equals(kIHTMLContentSinkIID)) {
    nsIHTMLContentSink* tmp = this;
    *aInstancePtr = (void*) tmp;
  }
  else if (aIID.Equals(kILoggingSinkIID)) {
    nsILoggingSink* tmp = this;
    *aInstancePtr = (void*) tmp;
  }
  else {
    *aInstancePtr = nsnull;
    return NS_NOINTERFACE;
  }
  return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::SetOutputStream(PRFileDesc *aStream,PRBool autoDeleteOutput) {
  return NS_OK;
}

static
void WriteTabs(PRFileDesc * out,int aTabCount) {
  int tabs;
  for(tabs=0;tabs<aTabCount;++tabs)
    PR_fprintf(out, "  ");
}


NS_IMETHODIMP
JavaContentSink::WillBuildModel() {
  
  if (doDebug)
	  printf("<begin>\n");
  return NS_OK;
}

NS_IMETHODIMP
JavaContentSink::DidBuildModel() {
  
 if (doDebug)
   printf( "</begin>\n");

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::WillInterrupt() {
  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::WillResume() {
  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::SetParser(nsIParser* aParser)  
{
	//printf("-------> Setting parser : %x\n" , aParser);fflush(stdout);
  nsresult theResult=NS_OK;
  
  NS_IF_RELEASE(mParser);
  
  mParser = aParser;

  return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenContainer(const nsIParserNode& aNode) {

  OpenNode("container", aNode); //do the real logging work...

  nsresult theResult=NS_OK;
  return theResult;

}


NS_IMETHODIMP
JavaContentSink::CloseContainer(const nsHTMLTag aTag) {

  nsresult theResult=NS_OK;
  nsHTMLTag nodeType = nsHTMLTag(aTag);
  if ((nodeType >= eHTMLTag_unknown) &&
      (nodeType <= nsHTMLTag(NS_HTML_TAG_MAX))) {
    const PRUnichar* tag = GetStringValue(nodeType);
    theResult = CloseNode(NS_ConvertUCS2toUTF8(tag).get());
  }
  else theResult= CloseNode("???");
  return theResult;

}

NS_IMETHODIMP
JavaContentSink::AddHeadContent(const nsIParserNode& aNode) {
	
  eHTMLTags type = (eHTMLTags)aNode.GetNodeType();

  if (type == eHTMLTag_title) {
    nsCOMPtr<nsIDTD> dtd;
    mParser->GetDTD(getter_AddRefs(dtd));
    NS_ENSURE_TRUE(dtd, NS_ERROR_FAILURE);
    
    nsString theString;
    PRInt32 lineNo = 0;
	
    dtd->CollectSkippedContent(type, theString, lineNo);
    SetTitle(theString);
  }
  else {
    LeafNode(aNode);
  }

  nsresult theResult=NS_OK;
  return theResult;

}

NS_IMETHODIMP
JavaContentSink::AddLeaf(const nsIParserNode& aNode) {
  LeafNode(aNode);

  nsresult theResult=NS_OK;
  return theResult;

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
JavaContentSink::AddProcessingInstruction(const nsIParserNode& aNode){
#ifdef VERBOSE_DEBUG
  DebugDump("<",aNode.GetText(),(mNodeStackPos)*2);
#endif
 char* text = nsnull;
    GetNewCString(aNode.GetText(), &text);
    if(text) 
    {
      printf("AddProcessingInstruction %s" , text);
      fflush(stdout);
      callback("AddProcessingInstruction" , text);
      nsMemory::Free(text);
    }
  nsresult theResult=NS_OK;
  return theResult;
}

/**
 *  This gets called by the parser when it encounters
 *  a DOCTYPE declaration in the HTML document.
 */

NS_IMETHODIMP
JavaContentSink::AddDocTypeDecl(const nsIParserNode& aNode) {
#ifdef VERBOSE_DEBUG
  DebugDump("<",aNode.GetText(),(mNodeStackPos)*2);
#endif
  nsresult theResult=NS_OK;
  return theResult;

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
JavaContentSink::AddComment(const nsIParserNode& aNode){
#ifdef VERBOSE_DEBUG
  DebugDump("<",aNode.GetText(),(mNodeStackPos)*2);
#endif
  char* text = nsnull;
    GetNewCString(aNode.GetText(), &text);
    if(text) 
    {
      callback("AddComment" , text);
      nsMemory::Free(text);
    }
  
  nsresult theResult=NS_OK;
  return theResult;

}


NS_IMETHODIMP
JavaContentSink::SetTitle(const nsString& aValue) {
  char* tmp = nsnull;
  GetNewCString(aValue, &tmp);
  if(tmp) {
     callback("SetTitle" , tmp );
    //printf( "<title value=\"%s\"/>\n", tmp);
    nsMemory::Free(tmp);
  }
  --mLevel;
  nsresult theResult=NS_OK;
  return theResult;

}


NS_IMETHODIMP
JavaContentSink::OpenHTML(const nsIParserNode& aNode) {
  OpenNode("html", aNode);

  nsresult theResult=NS_OK;
  return theResult;

}

NS_IMETHODIMP
JavaContentSink::CloseHTML() {
  CloseNode("html");

  nsresult theResult=NS_OK;
  return theResult;

}

NS_IMETHODIMP
JavaContentSink::OpenHead(const nsIParserNode& aNode) {
  OpenNode("head", aNode);

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::CloseHead() {
  CloseNode("head");

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenBody(const nsIParserNode& aNode) {
  OpenNode("body", aNode);

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::CloseBody() {
  CloseNode("body");

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenForm(const nsIParserNode& aNode) {
  OpenNode("form", aNode);

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::CloseForm() {
  CloseNode("form");

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenMap(const nsIParserNode& aNode) {
  OpenNode("map", aNode);

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::CloseMap() {
  CloseNode("map");

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::OpenFrameset(const nsIParserNode& aNode) {
  OpenNode("frameset", aNode);

  nsresult theResult=NS_OK;
  return theResult;
}

NS_IMETHODIMP
JavaContentSink::CloseFrameset() {
  CloseNode("frameset");

  nsresult theResult=NS_OK;
  return theResult;
}


nsresult
JavaContentSink::OpenNode(const char* aKind, const nsIParserNode& aNode) {
  if (doDebug) printf("<open container=");

  nsHTMLTag nodeType = nsHTMLTag(aNode.GetNodeType());
  if ((nodeType >= eHTMLTag_unknown) &&
      (nodeType <= nsHTMLTag(NS_HTML_TAG_MAX))) {
    const PRUnichar* tag = GetStringValue(nodeType);
    if (doDebug) printf( "\"%s\"", NS_ConvertUCS2toUTF8(tag).get());
    char container[100];
    sprintf(container , "%s", NS_ConvertUCS2toUTF8(tag).get());
    callback("OpenNode" , container ); 
  }
  else {
    char* text = nsnull;
    GetNewCString(aNode.GetText(), &text);
    if(text) {
      if (doDebug) printf( "\"%s\"", text);
      callback("OpenNode" , text);
      nsMemory::Free(text);
    }
  }

  if (WillWriteAttributes(aNode)) {
    if (doDebug) printf( ">\n");
    WriteAttributes(aNode);
    if (doDebug) printf("</open>\n");
  }
  else 
  {
    if (doDebug) printf( ">\n");
  }
  return NS_OK;
}

nsresult
JavaContentSink::CloseNode(const char* aKind) {
  callback("CloseNode" , (char*)aKind );
  if (doDebug) printf( "</open container=\"%s\">\n", aKind);
  return NS_OK;
}


nsresult
JavaContentSink::WriteAttributes(const nsIParserNode& aNode) {
  nsAutoString tmp;
  PRInt32 ac = aNode.GetAttributeCount();
  for (PRInt32 i = 0; i < ac; ++i) {
    char* key=nsnull;
    char* value=nsnull;
    const nsAString& k = aNode.GetKeyAt(i);
    const nsAString& v = aNode.GetValueAt(i);

    GetNewCString(k, &key);
    if(key) 
    {
      if (doDebug) printf(" <attr key=\"%s\" value=\"", key);
      callback("WriteAttributeKey" , key ); 
      nsMemory::Free(key);
    }
 
    tmp.Truncate();
    tmp.Append(v);
    if(!tmp.IsEmpty()) {
      PRUnichar first = tmp.First();
      if ((first == '"') || (first == '\'')) {
        if (tmp.Last() == first) {
          tmp.Cut(0, 1);
          PRInt32 pos = tmp.Length() - 1;
          if (pos >= 0) {
            tmp.Cut(pos, 1);
          }
        } else {
          // Mismatched quotes - leave them in
        }
      }
      GetNewCString(tmp, &value);

      if(value) 
      {
        if (doDebug) printf( "%s\"/>\n", value);
        callback("WriteAttributeValue" , value );
        nsMemory::Free(value);
      }
    }
    else
  {
        callback("WriteAttributeValue" , "" );
        if (doDebug) printf( "\"/>\n");
  }

  }

  if (0 != strchr(gSkippedContentTags, aNode.GetNodeType())) {
    nsCOMPtr<nsIDTD> dtd;
    mParser->GetDTD(getter_AddRefs(dtd));
    NS_ENSURE_TRUE(dtd, NS_ERROR_FAILURE);
    
    nsString theString;
    PRInt32 lineNo = 0;

    dtd->CollectSkippedContent(aNode.GetNodeType(), theString, lineNo);
    char* content = nsnull;
    GetNewCString(theString, &content);
    if(content) 
    {
      if (doDebug) printf( " <content value=\"");
      if (doDebug) printf( "%s\"/>\n", content) ;
      callback("AddContent" , content );
      nsMemory::Free(content);
    }
  }
  
  return NS_OK;
}

PRBool
JavaContentSink::WillWriteAttributes(const nsIParserNode& aNode)
{
  PRInt32 ac = aNode.GetAttributeCount();
  if (0 != ac) {
    return PR_TRUE;
  }
  if (0 != strchr(gSkippedContentTags, aNode.GetNodeType())) {
    nsCOMPtr<nsIDTD> dtd;
    mParser->GetDTD(getter_AddRefs(dtd));
    NS_ENSURE_TRUE(dtd, NS_ERROR_FAILURE);
    
    nsString content;
    PRInt32 lineNo = 0;
    dtd->CollectSkippedContent(aNode.GetNodeType(), content, lineNo);
    if (!content.IsEmpty()) {
      return PR_TRUE;
    }
  }
  return PR_FALSE;
}

nsresult
JavaContentSink::LeafNode(const nsIParserNode& aNode)
{
	
	nsHTMLTag				nodeType  = nsHTMLTag(aNode.GetNodeType());

  if ((nodeType >= eHTMLTag_unknown) &&
      (nodeType <= nsHTMLTag(NS_HTML_TAG_MAX))) {
    const PRUnichar* tag = GetStringValue(nodeType);

  if(tag)
  {
	if (doDebug) printf("<leaf tag=\"%s\"", NS_ConvertUCS2toUTF8(tag).get());
	char leafTag[100];
	sprintf(leafTag , "%s" , NS_ConvertUCS2toUTF8(tag).get());
	callback("AddLeaf" , leafTag);
  }
    else
  {
	if (doDebug) printf( "<leaf tag=\"???\"");
  }

    if (WillWriteAttributes(aNode)) {
			if (doDebug) printf(">\n");
		        WriteAttributes(aNode);
			if (doDebug) printf( "</leaf>\n");
    }
    else {
			if (doDebug) printf( "/>\n");
    }
  }
  else 
  {
    PRInt32 pos;
    nsAutoString tmp;
    char* str = nsnull;
    switch (nodeType) 
    {
			case eHTMLTag_whitespace:
			case eHTMLTag_text:
        GetNewCString(aNode.GetText(), &str);
        if(str) 
	 	{
		  if (doDebug) printf( "<text value=\"%s\"/>\n", str);
		  callback("AddText" , str );
	      nsMemory::Free(str);
	    }
				break;

			case eHTMLTag_newline:
				callback("AddText" , "\n" );
				break;

			case eHTMLTag_entity:
				tmp.Append(aNode.GetText());
				pos = tmp.Length();
				if (pos >= 0) {
					tmp.Cut(pos-1, 1);
				}
				if (doDebug) printf("<entity value=\"%s\"/>\n", NS_LossyConvertUCS2toASCII(tmp).get());
				char entityValue[100];
				sprintf(entityValue , "%s" ,  NS_LossyConvertUCS2toASCII(tmp).get());
				callback("AddEntity" , entityValue);
				break;

			default:
				printf("Error : Unsupported Node type : %d" , nodeType);
//				NS_NOTREACHED("unsupported leaf node type");
		}//switch
  }
  if (doDebug) printf("</leaf>\n");
  callback("CloseLeaf" , "");
  return NS_OK;
}

nsresult 
JavaContentSink::QuoteText(const nsAString& aValue, nsString& aResult) {
  aResult.Truncate();
    /*
      if you're stepping through the string anyway, why not use iterators instead of forcing the string to copy?
     */
  const nsPromiseFlatString& flat = PromiseFlatString(aValue);
  const PRUnichar* cp = flat.get();
  const PRUnichar* end = cp + aValue.Length();
  while (cp < end) {
    PRUnichar ch = *cp++;
    if (ch == '"') {
      aResult.AppendLiteral("&quot;");
    }
    else if (ch == '&') {
      aResult.AppendLiteral("&amp;");
    }
    else if ((ch < 32) || (ch >= 127)) {
//      aResult.AppendLiteral("&#");
//      printf("char:%d ... \n",ch);fflush(stdout);
//      aResult.AppendInt(PRInt32(ch), 10);
//      aResult.Append(PRUnichar(';'));
      aResult.Append(ch);
    }
    else {
      aResult.Append(ch);
    }
  }
  return NS_OK;
}

/**
 * Use this method to convert nsString to char*. 
 * REMEMBER: Match this call with nsMemory::Free(aResult);
 * 
 * @update 04/04/99 harishd
 * @param aValue - The string value
 * @param aResult - String coverted to char*.
 */
nsresult
JavaContentSink::GetNewCString(const nsAString& aValue, char** aResult)
{
  nsresult result=NS_OK;
  nsAutoString temp;
  result=QuoteText(aValue,temp);
  if(NS_SUCCEEDED(result)) {
    *aResult = temp.IsEmpty() ? nsnull : ToNewCString(temp);
  }
  return result;
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

void JavaContentSink::callback(char* arg1 , char* arg2 /*, char* arg3*/){
	jstring string1 = env->NewStringUTF(arg1);
	jstring string2 = env->NewStringUTF(arg2);
	env->CallVoidMethod(mozillaParserObject , callbackMid , string1 , string2 );
}

void JavaContentSink::setJavaEnviroment(JNIEnv *aEnv, jobject aMozillaParserObject){
	env = aEnv;
	if (doDebug) printf("Setting java enviroment...");
	mozillaParserObject = aMozillaParserObject;
	
	jclass cls = env->GetObjectClass(mozillaParserObject);
	callbackMid = env->GetMethodID(cls , "callback" , "(Ljava/lang/String;Ljava/lang/String;)V");
	
}
