/**
 * 
 */
package com.dappit.Dapper.parser;

import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMEntityReference;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author Ohad Serfaty
 * 
 * A class for building DOM documents from mozilla's content sink instructions
 * 
 * supported operations are :
 *	OpenNode <tag name>
 *	CloseNode <tag name>
 *	AddText <content>
 *	AddLeaf <tag name>
 *	WriteAttributeKey <key>	 - in pair with the next op :
 *	WriteAttributeValue <value>	
 *	CloseLead
 *	AddComment
 *	AddEntity
 *
 *	Unsupported ( fot the time being ) : 
 *	AddInstruction
 *	AddTitle
 *	
 *
 * Note that this class is reusable , you can use reset() to clear the content of the dom.
 * 
 */
public class DomDocumentBuilder 
{

	private static final String String32 = new String(new byte[]{ 32 });
	private static final String String0 = new String(new byte[]{ 0 });
	private static final String String1 = new String(new byte[]{ 0x1 });
	private static final String String14 = new String(new byte[]{ 0x14 });
	private static final String String1d = new String(new byte[]{ 0x1d });
	private static final String String0xf = new String(new byte[]{ 0xf });
	private static final String String0x1A = new String(new byte[]{ 0x1A });
	private static final String String0x12 = new String(new byte[]{ 0x12 });
	private static final String String0x8 = new String(new byte[]{ 0x8 });
	private static final String String0x1f = new String(new byte[]{ 0x1f });
	private static final String String0x2 = new String(new byte[]{ 0x2 });
	private static final String String0x7 = new String(new byte[]{ 0x7 });
	private static final String String0x18 = new String(new byte[]{ 0x18 });
	private static final String String0x19 = new String(new byte[]{ 0x19 });
	private static final String String0x1B = new String(new byte[]{ 0x1B });
	private static final String String0x1C = new String(new byte[]{ 0x1C });
	private static final String String0x11 = new String(new byte[]{ 0x11 });
	private static final String String0x10 = new String(new byte[]{ 0x10 });
	private static final String String0x13 = new String(new byte[]{ 0x13 });
	
	Vector<String> operations = new Vector<String>();
	Vector<String> arguments = new Vector<String>();
	
	/**
	 *  reset the builder. can be reused after creating a document.
	 */
	public void reset()
	{
		operations.clear();
		arguments.clear();
	}
	
	/**
	 * Add a content sink instruction with an argument
	 * 
	 * @param domOperation
	 * @param domArgument
	 */
	public void addInstruction(String domOperation , String domArgument){
		this.operations.add(domOperation);
		this.arguments.add(domArgument);
//		System.out.println(domOperation+" " + domArgument);
	}
	
	public static String getCDATASection(String domArgument)
	{
		if (!domArgument.contains("CDATA"))
			return null;
		Pattern pat = Pattern.compile("(.*)\\<\\!(\\s*)\\[CDATA(.*)\\]\\]\\>(.*)",Pattern.DOTALL + Pattern.MULTILINE);
        Matcher mat = pat.matcher(domArgument);
        if (mat.find()) 
        {
        	String group3 = mat.group(3);
        	if (group3.startsWith("["))
        		group3 = group3.replaceFirst("\\[", "");
        	String result = mat.group(1) + group3 +mat.group(4) ;
        	return result;
        }
		return null;
	}
	
	/**
	 * Finalize and build the dom document.
	 * 
	 * @return
	 */
	public Document buildDocument()
	{
//		System.out.println("building document...");
		DOMDocument resultDocument = new DOMDocument();
		Iterator<String> i = this.operations.iterator();
		Iterator<String> j = this.arguments.iterator();
		Element currentElement = null;
		boolean isInLeaf = false;
		boolean closeHtml = true;
		
		while (i.hasNext())
		{
			String domOperation = i.next();
			String domArgument = j.next();
//			System.out.println("Operation :" + domOperation+" Arg:" + domArgument);
			if (domOperation.equalsIgnoreCase("OpenNode"))
			{
				closeHtml=true;
				Element childNode = resultDocument.createElement(domArgument.toLowerCase());
				if (currentElement == null)
				{
					resultDocument.setRootElement((org.dom4j.Element) childNode);
					currentElement = childNode;
				}
				else
				{
					if (!domArgument.equalsIgnoreCase("html"))
					{
							currentElement.appendChild(childNode);
							currentElement = childNode;
					}
					else
						closeHtml = false;
				}
				
			}
			else
			if (domOperation.equalsIgnoreCase("CloseNode")){
				if (currentElement== null)
				{
					System.err.println("Error : Close Node where no OpenNode was called. trying to fix..." );
//					this.dump();
				}
				else
					if (closeHtml)
						currentElement =  (Element)currentElement.getParentNode();
				
			}
			else
			// check : may be problematic for cases of script or style 
			if (domOperation.equalsIgnoreCase("AddText") || domOperation.equalsIgnoreCase("AddContent"))
			{
//				System.out.println(currentElement.getNodeName() +" : Adding text :" + domArgument);
				// check : try and resolve this with a <newline> from mozilla instead :
				boolean script = false;
				boolean style = false;
				
				if (currentElement.getNodeName().equalsIgnoreCase("script")  /*|| currentElement.getNodeName().equalsIgnoreCase("style")*/ )
					script = true;
				else
					if (currentElement.getNodeName().equalsIgnoreCase("style"))
					style=true;
				else
					domArgument = DomDocumentBuilder.fixText(domArgument);
				
//					System.out.println("Body content :" + domArgument);
				
//				System.out.println("AddText  "+domArgument);
				if (domArgument.length() >=1)
				{
					if (!script && !style)
					{
						Text textNode = resultDocument.createTextNode(domArgument);
						currentElement.appendChild(textNode);
					}
					else
					{
						domArgument = domArgument.trim();
						String cdata = getCDATASection(domArgument);
						
						if (cdata!=null)
						{
							if(script)
								cdata = DomDocumentBuilder.fixText(cdata);
							else
								cdata = DomDocumentBuilder.fixText(domArgument);
							CDATASection cdataSection = resultDocument.createCDATASection(cdata);
							currentElement.appendChild(cdataSection);
						}
						else
						{
							domArgument = DomDocumentBuilder.fixText(domArgument);
							Text textNode = resultDocument.createTextNode(domArgument);
							currentElement.appendChild(textNode);
						}
					}
				}
			}
			else
			if (domOperation.equalsIgnoreCase("AddLeaf"))
			{
				Element leafElement = resultDocument.createElement(domArgument);
				currentElement.appendChild(leafElement);
				currentElement=leafElement;
				isInLeaf=true;
			}
			else
			if (domOperation.equalsIgnoreCase("WriteAttributeKey"))
			{
				// add an attribute with the next lookahead operation :
				domOperation = i.next();	// Fetch the next operation , must be WriteAttributeValue
				String value = j.next();	// Feth the attributes value.
				if (!domArgument.toLowerCase().trim().equalsIgnoreCase("_moz-userdefined"))
					currentElement.setAttribute(domArgument.toLowerCase(), DomDocumentBuilder.fixText(value));
			}
			else
			if (domOperation.equalsIgnoreCase("CloseLeaf"))
			{
				if (isInLeaf)
				{
					currentElement =  (Element)currentElement.getParentNode();
					isInLeaf=false;
				}
				
			}
			else
			if (domOperation.equalsIgnoreCase("AddEntity"))
			{
				DOMEntityReference entity = (DOMEntityReference) resultDocument.createEntityReference(domArgument);
//				 a bugfix for a c++ problem in the mozilla parser:
				if (!Character.isDigit(domArgument.charAt(0)))
					entity.setText("&"+domArgument+";");
				else
					entity.setText("");
				currentElement.appendChild(entity);
			}
			else
			if (domOperation.equalsIgnoreCase("AddComment"))
			{
				Comment comment = resultDocument.createComment(domArgument);
				currentElement.appendChild(comment);
			}
			else
				if (domOperation.equalsIgnoreCase("SetTitle"))
				{
					Element titleNode = resultDocument.createElement("title");
					titleNode.appendChild(resultDocument.createTextNode(domArgument));
					NodeList headElements = resultDocument.getElementsByTagName("head");
					// Add the title with the new text :
					if (headElements.getLength() > 0)
						headElements.item(0).appendChild(titleNode);
				}
			
		}
		return resultDocument;
	}
	
	public void dump(){
		Iterator<String> i2 = this.operations.iterator();
		Iterator<String> j2 = this.arguments.iterator();
		while (i2.hasNext())
			System.err.println(i2.next() +" : " + j2.next());
	}
	
	public static String fixText(String text)
	{
		String fixedText = new String(text);
		fixedText = fixedText.replaceAll("&#10;|&#9;&#160;", "");
		fixedText = fixedText.replaceAll("&#194;|&quot;", "\"");
		fixedText = fixedText.replaceAll("&lt;", "<");
		fixedText = fixedText.replaceAll("&gt;", ">");
		fixedText = fixedText.replaceAll("&amp;", "&");
		fixedText = fixedText.replaceAll(String32, " ");
		fixedText = fixedText.replaceAll("["+String0+String1+String14+String1d+String0xf + String0xf+
				String0x1A + String0x12 + String0x8 + String0x1f+ String0x2+String0x7+String0x18+String0x19+String0x1B+String0x1C+
				String0x11+String0x10+String0x13+"]" , "");
		return fixedText;
	}

	/**
	 * @return
	 */
	public Vector<String> getInstructions() {
		return operations;
	}
	
	
}
