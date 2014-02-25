package com.dappit.Dapper.parser;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.List;
import java.util.Vector;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.ParserConfigurationException;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;

/**
 * @author Ohad Serfaty
 *
 * A Mozilla native Html Parser
 *
 */
public class MozillaParser implements HTMLParser
{
	static boolean isInitialized = false;
	DomDocumentBuilder domBuilder = new DomDocumentBuilder();
	InstructionsPool instructionsPool;
	private static String MozillaInitializedJvmProperty = "MozillaParser.Initialized";
	private static String MESSAGE_CHARSET = "UTF-16LE";

	private final int htmlSizeLimit;

	/**
	 * Create a new parser instance with no limit on the input size.
	 * Automatically calls init() if necessary.
	 */
	public MozillaParser()
		throws ParserInitializationException
	{
		this(Integer.MAX_VALUE);
	}

	/**
	 * Create a new parser instance, limiting input to htmlSizeLimit bytes.
	 * Automatically calls init() if necessary.
	 */
	public MozillaParser(int htmlSizeLimit)
		throws ParserInitializationException
	{
		this.htmlSizeLimit = htmlSizeLimit;
		this.init();
	}

	/**
	 * Initialize the mozilla XPCOM embedded components with the proper
	 * components base directory.
	 * 
	 * @param componentBase
	 *            mozilla's components directory (e.g
	 *            /ohad/mozilla/dist/bin).  May be <tt>null</tt>, in which
	 *            case the default path (set at compile-time) is used.
	 */
	private synchronized static native void initXPCOM(String componentBase)	throws ParserInitializationException;

	/**
	 * Parse an html function using mozilla's html parser, populating the
	 * instruction pool.
	 * 
	 * @param htmlBytes       Raw bytes to parse
	 * @param htmlEncoding    The character set (e.g. <tt>UTF-8</tt>) for
	 *                        decoding <i>htmlBytes</i>.  If <tt>null</tt>
	 *                        is passed, the default HTTP encoding
	 *                        (<tt>ISO-8559-1</tt>) is used, unless the
	 *                        character set is overridden by a
	 *                        <tt>&lt;META&gt;</tt> tag in the HTML body.
	 *                        If a non-null value is passed, any such
	 *                        <tt>&lt;META&gt;</tt> tag is ignored.
	 * @param forceAcceptMeta If <tt>true</tt>, then accept a character set
	 *                        specified by a <tt>&lt;META&gt;</tt> tag even if
	 *                        <i>htmlEncoding</i> is non-null.
	 *
	 * @throws ParserException
	 */
	private native void parseHtml( byte[] htmlBytes, String htmlEncoding, boolean forceAcceptMeta )
		throws ParserException;

	/**
	 * Called from native code: add the specified parser instruction to the
	 * instruction pool.
	 *
	 * @param domOperation
	 * @param domArgument
	 */
	private void callback(int domOperation, byte[] domArgument) 
		throws UnsupportedEncodingException
	{
		this.instructionsPool.addInstruction( domOperation, new String(domArgument, 0, domArgument.length, MESSAGE_CHARSET) );
	}

	/**
	 * Called from native code: reset the instruction pool.
	 */
	private void resetInstructionPool()
	{
		this.instructionsPool.reset();
	}

	/**
	 * Parse an html function using mozilla's html parser, populating the
	 * instruction pool.
	 * 
	 * @param htmlBytes       Raw bytes to parse
	 * @param htmlEncoding    The character set (e.g. <tt>UTF-8</tt>) for
	 *                        decoding <i>htmlBytes</i>.  If <tt>null</tt>
	 *                        is passed, the default HTTP encoding
	 *                        (<tt>ISO-8559-1</tt>) is used, unless the
	 *                        character set is overridden by a
	 *                        <tt>&lt;META&gt;</tt> tag in the HTML body.
	 *                        If a non-null value is passed, any such
	 *                        <tt>&lt;META&gt;</tt> tag is ignored.
	 *
	 * @throws ParserException
	 * @throws DocumentException
	 */
	public void callNativeHtmlParser( byte[] htmlBytes, String htmlEncoding )
		throws ParserException, DocumentException
	{
		callNativeHtmlParser( htmlBytes, htmlEncoding, false );
	}

	/**
	 * Parse an html function using mozilla's html parser, populating the
	 * instruction pool.
	 * 
	 * @param html            A string to parse.  If the body contains a
	 *                        <tt>&lt;META&gt;</tt> tag specifying a character
	 *                        set, it will override the existing encoding.  Note
	 *                        that this conversion may be flawed, since the
	 *                        string will first be encoded as UTF-8 and then
	 *                        re-decoded using the character set specified by
	 *                        the <tt>&lt;META&gt;</tt> tag.
	 *
	 * @deprecated            Because of the problems involved in transcoding
	 *                        the string multiple times, the preferred method to
	 *                        use is callNativeHtmlParser(byte[], String).
	 *
	 * @throws ParserException
	 * @throws DocumentException
	 */
	@Deprecated
	public void callNativeHtmlParser( String html )
		throws ParserException, DocumentException
	{
		byte[] bytes;
		try
		{
			bytes = html.getBytes("UTF-8");
		}
		catch( UnsupportedEncodingException e )
		{
			throw new Error( "No support for UTF-8", e );
		}

		callNativeHtmlParser( bytes, "UTF-8", true );
	}

	/**
	 * Parse an html function using mozilla's html parser, populating the
	 * instruction pool.
	 * 
	 * @param htmlBytes       Raw bytes to parse
	 * @param htmlEncoding    The character set (e.g. <tt>UTF-8</tt>) for
	 *                        decoding <i>htmlBytes</i>.  If <tt>null</tt>
	 *                        is passed, the default HTTP encoding
	 *                        (<tt>ISO-8559-1</tt>) is used, unless the
	 *                        character set is overridden by a
	 *                        <tt>&lt;META&gt;</tt> tag in the HTML body.
	 *                        If a non-null value is passed, any such
	 *                        <tt>&lt;META&gt;</tt> tag is ignored.
	 * @param forceAcceptMeta If <tt>true</tt>, then accept a character set
	 *                        specified by a <tt>&lt;META&gt;</tt> tag even if
	 *                        <i>htmlEncoding</i> is non-null.
	 *
	 * @throws ParserException
	 * @throws DocumentException
	 */
	private void callNativeHtmlParser( byte[] htmlBytes, String htmlEncoding, boolean forceAcceptMeta )
		throws ParserException, DocumentException
	{
		if (htmlBytes.length > htmlSizeLimit)
		{
			throw new DocumentException("Html too long:" + htmlBytes.length +">" + this.htmlSizeLimit);
		}

		//html = html.replaceAll("<\\s*(STYLE|style|script|SCRIPT)\\s*>", "<$1 harmless=''> ");

		this.instructionsPool = new InstructionsPool(16 + (htmlBytes.length / 10));
		try
		{
			this.parseHtml( htmlBytes, htmlEncoding, forceAcceptMeta );
		}
		catch( ParserException e )
		{
			throw e;
		}
		catch( Throwable e )
		{
			System.err.println(Thread.currentThread() + "Warning: could not parse html :" + e.toString());
			throw new DocumentException(e);
		}
	}

	/**
	 * Parse an html document, returning a DOM document.
	 *
	 * @deprecated in favor of #parse(byte[], String)
	 */
	@Deprecated
	public Document parse(String html) throws DocumentException, ParserException, ParserConfigurationException
	{
		this.callNativeHtmlParser( html );
		return this.domBuilder.buildDocument( instructionsPool );
	}

	/**
	 * Parse an html document, returning a DOM document.
	 *
	 * @param htmlBytes       Raw bytes to parse
	 * @param htmlEncoding    The character set (e.g. <tt>UTF-8</tt>) for
	 *                        decoding <i>htmlBytes</i>.  If <tt>null</tt>
	 *                        is passed, the default HTTP encoding
	 *                        (<tt>ISO-8559-1</tt>) is used, unless the
	 *                        character set is overridden by a
	 *                        <tt>&lt;META&gt;</tt> tag in the HTML body.
	 *                        If a non-null value is passed, any such
	 *                        <tt>&lt;META&gt;</tt> tag is ignored.
	 */
	public Document parse( byte[] htmlBytes, String htmlEncoding ) throws DocumentException, ParserException, ParserConfigurationException
	{
		this.callNativeHtmlParser( htmlBytes, htmlEncoding );
		return this.domBuilder.buildDocument( instructionsPool );
	}

	/**
	 * Initialize the mozilla html parser with a DLL to load and a mozilla
	 * component base
	 * 
	 * @param parserLibrary   The full path to the MozillaParser library,
	 *                        or null to search for a library named "MozillaParser"
	 * @param componentsBase  The path to the directory containing the
	 *                        Mozilla binaries, or null to use the value
	 *                        set at compile time.
	 *
	 * @throws ParserInitializationException
	 */
	public static void init(String parserLibrary, String componentsBase)
		throws ParserInitializationException
	{
		if( isInitialized )
		{
			return;
		}

		synchronized( Runtime.getRuntime() )
		{
			String initialized = System.getProperty(MozillaInitializedJvmProperty);
			if( initialized != null )
			{
				return;
			}

			try
			{
				if( parserLibrary == null )
				{
					System.loadLibrary( "MozillaParser" );
				}
				else
				{
					System.load( parserLibrary );
				}
			}
			catch (Throwable e)
			{
				String msg = null;

				if( parserLibrary == null )
				{
					msg = "Could not load MozillaParser library.  Make sure the library-path environment variable (windows:PATH, Linux: LD_LIBRARY_PATH, macosx: DYLD_LIBRARY_PATH) is set to include both the MozillaParser library and the Mozilla binary components, or explicitly initialize the Mozilla parser library by calling MozillaParser.init().";
				}

				throw new ParserInitializationException(msg, e);
			}

			initXPCOM(componentsBase);
			System.setProperty(MozillaInitializedJvmProperty, "true");
			isInitialized = true;
		}
	}

	public static void init()
		throws ParserInitializationException
	{
		init( null, null );
	}

	public InstructionsPool getInstructionsPool()
	{
		return this.instructionsPool;
	}
}
