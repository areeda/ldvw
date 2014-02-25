package com.dappit.Dapper.parser;

import java.util.Vector;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;

/**
 * @author Ohad Serfaty
 *
 * A Mozilla native Html Parser
 *
 */
public class MozillaParser 
{

	boolean isParsing = false;
	static boolean isInitialized = false;
	DomDocumentBuilder domBuilder = new DomDocumentBuilder();
	private static String MozillaInitializedJvmProperty = "MozillaParser.Initialized";

	/**
	 * initialize the mozilla XPCOM embedded components with the proper
	 * components base directory
	 * 
	 * @param componentBase
	 *            mozilla's components directory (e.g
	 *            /home/ohad/mozilla/dist/bin )
	 */
	private synchronized static native void initXPCOM(String componentBase)	throws ParserInitializationException;

	/**
	 * Native function. parse an html function using mozilla's html parser and
	 * make callbacks to the java local sink ( DomDocumentBuilder for that
	 * matter)
	 * 
	 * @param html
	 *            HTML to parse.
	 * @throws ParserInitializationException
	 */
	public native void parseHtml(String html)	throws ParserInitializationException;

	/**
	 * 
	 * A callback is being made from native code to this function.
	 * 
	 * @param domOperation
	 * @param domArgument
	 */
	public synchronized void callback(String domOperation, String domArgument) 
	{
		// System.out.println("called back with :"+domOperation +" " + domArgument );
		domBuilder.addInstruction(domOperation, domArgument);
	}

	public Document parse(String html) throws DocumentException
	{
		html = html.replaceAll("<\\s*style\\s*>", "<style harmless=''> ");
		html = html.replaceAll("<\\s*script\\s*>", "<script harmless=''> ");
		this.domBuilder.reset();
		try
		{
			this.parseHtml(html);
		}
		catch (Throwable e)
		{
			System.err.println("Warning: could not parse html :" + e.getMessage());
			throw new DocumentException(e);
		}
		return this.domBuilder.buildDocument();
	}

	public void dump() {
		this.domBuilder.dump();
	}

	/**
	 * Initialize the mozilla html parser with a DLL to load and a mozilla
	 * component base
	 * 
	 * @param dllToLoad
	 * @param componentsBase
	 * @throws ParserInitializationException
	 */
	public static void init(String parserLibrary, String componentsBase) throws Exception 
	{
		String initialized = System.getProperty(MozillaInitializedJvmProperty);
		if (initialized == null) 
		{
			System.setProperty(MozillaInitializedJvmProperty, "true");
		}
		else 
		{
			System.err.println("Warning : MozillaParser detected an additional attempt to initialize XPCOM. operation ignored.");
			return;
		}
		try
		{
			System.load(parserLibrary);
		}
		catch (Throwable e)
		{
			System.err.println("Warning:Could not load library "+parserLibrary +" Possible reason : " +
					"You have to include both mozilla.dist.bin." + EnviromentController.getOperatingSystemName() 
					+" And mozilla.dist.bin."+ EnviromentController.getOperatingSystemName() + " " +
							"In the right environment variable (windows:PATH , Linux: LD_LIBRARY_PATH , macosx: DYLD_LIBRARY_PATH )") ;
			throw new ParserInitializationException(e);
		}
		initXPCOM(componentsBase);
	}

	/**
	 * @return
	 */
	public Vector<String> getDomBuilderArguments() {
		return this.domBuilder.getInstructions();

	}

}
