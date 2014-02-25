/**
 * 
 */
package com.dappit.Dapper.parser.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.ccil.cowan.tagsoup.Parser;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMReader;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.HTMLWriter;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.dappit.Dapper.Configuration;
import com.dappit.Dapper.parser.EnviromentController;
import com.dappit.Dapper.parser.MozillaParser;
import com.dappit.Dapper.parser.ParserInitializationException;
import com.dappit.Dapper.parser.profiler.SimpleMemoryProfiler;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * @author Ohad Serfaty
 *
 */
public class TestMozillaParser extends TestCase {

	boolean doTesting = true;
	
	public static void initTestingXPCOM(){
		File mozillaParserLibraryFile;
		try 
		{
			mozillaParserLibraryFile = new File("native/bin/MozillaParser"+ EnviromentController.getSharedLibraryExtension());
		}
		catch (Exception e1) 
		{
			mozillaParserLibraryFile = new File("./native/bin/MozillaParser.dll");
			e1.printStackTrace();
		}
		
		
		String mozillaParserLibrary = mozillaParserLibraryFile.getAbsolutePath();
		String mozillaComponentBasePath = Configuration.getMozillaComponentsPath();
			try 
			{
				System.out.println("Loading and initializing XPCOM from "+ mozillaParserLibrary);
				MozillaParser.init(mozillaParserLibrary , mozillaComponentBasePath);
				System.out.println("done!");
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}	
	}
	
	
	static 
	{
		initTestingXPCOM();
	}
	
	// helper function : get the string of the dom document
	
	public static String serialize(Document document) throws IOException{
		StringWriter stringWriter = new StringWriter();
	    XMLSerializer serializer = new XMLSerializer();
	    serializer.setOutputCharStream(stringWriter);
	    serializer.serialize(document);
	    
	    return stringWriter.toString();
	    
	}
	
	
	private Document parseAndCompare(String html ,String expectedResult) throws Exception{
		//MozillaParser parser = MozillaParser.getInstance();
		MozillaParser parser = new MozillaParser();
		 Document document = parser.parse(html);
		 //System.out.println(serialize(document));
		 if (doTesting)
			 assertEquals(expectedResult, serialize(document));
		 return document;
	}
	
	public void testSimple1() throws Exception{
		 String simple1 = "<html>Hello world!</html>";
		 String expected1 = "<?xml version=\"1.0\"?>\n" +
		 		"<html><body>Hello world!</body></html>";
		 parseAndCompare(simple1 , expected1);
	}
	
	
	
	public void testSimple2() throws Exception{
		 String simple2 = "<html>Hello world!</html>";
		 String expected1 = "<?xml version=\"1.0\"?>\n" +
		 		"<html><body>Hello world!</body></html>";
		 parseAndCompare(simple2 , expected1);
	}

	public void testFonts() throws Exception
	{
		String html = "<p><p><p><font color=\"steelblue\" size=\"4\" FACE=\"Verdana\"><b>Ledger: The Joker's a \"pure anarchist\"</b></font><font size=\"2\"><br><b>Author:</b> <a href=\"bio_jett.html\" target=\"_blank\">Jett</a>  <br><b><font color=\"silver\">Tuesday, December 5, 2006 - 11:32 AM, 8:00 PM:</b></font>  Here's a bit from Heath Ledger about his upcoming turn as The Joker in <i>TDK</i>:<p><font size=\"1\">";
		MozillaParser parser = new MozillaParser();
		 Document document = parser.parse(html);
		 System.out.println(serialize(document));
	}
	
	public void testComment1() throws Exception{
		 String simple2 = "<html><body><p><!-- a comment --></p> <br> Hello world!</html>";
		 String expected1 = "<?xml version=\"1.0\"?>\n"+
		 			"<html><body><p><!-- a comment --></p> <br/> Hello world!</body></html>";
		 parseAndCompare(simple2 , expected1);
//		 System.out.println(serialize(document));
	}
	
	public void testScriptComment1() throws Exception{
		 String simple2 = "<html><body><script language=\"JavaScript\" > document.write('hell');</script> <br> Hello world!</html>";
		 String expected1 = "" +
		 		"<?xml version=\"1.0\"?>\n"+
"<html><body><script language=\"JavaScript\">document.write('hell');</script> <br/> Hello world!</body></html>";
		 parseAndCompare(simple2 , expected1);
//		 System.out.println(serialize(document));
	}
	
	public void testStyleContent() throws Exception{
		 String simple2 = "<html><head><style > <!--  body,td,a,p,.h{font-family:arial,sans-serif} " +
		 		".h{font-size:20px} " +
		 		" .h{color:#3366cc} " +
		 		" .q{color:#00c} " +
		 		" --></style></head><body> <br> Hello world!</html>";
		 String expected1 = "<?xml version=\"1.0\"?>\n"+
		 			"<html><head><style harmless=\"\">&lt;!--  body,td,a,p,.h{font-family:arial,sans-serif} .h{font-size:20px}  .h{color:#3366cc}  .q{color:#00c}  --&gt;</style></head><body> <br/> Hello world!</body></html>";
		 parseAndCompare(simple2 , expected1);
//		 System.out.println(serialize(document));
	}
	
	public void testAmpReplacer(){
		String testString = "&#10;&#10;&#10;";
		String newString = testString.replaceAll("&#10;", "");
		assertEquals("" ,newString );
		
		testString = "&#10;3&#10;1&#10;";
		newString = testString.replaceAll("&#10;", "");
		assertEquals("31" ,newString );
		
	}
	
	public void testStyleReplacer()
	{
		String testString = "< style >";
		String newString = testString.replaceAll("<\\s*style\\s*>", "<style harmless=''> ");
		assertEquals("<style harmless=''> " ,newString );
		
		testString = "< style>";
		newString = testString.replaceAll("<\\s*style\\s*>", "<style harmless=''> ");
		assertEquals("<style harmless=''> " ,newString );
		
		testString = "<style>";
		newString = testString.replaceAll("<\\s*style\\s*>", "<style harmless=''> ");
		assertEquals("<style harmless=''> " ,newString );
		
		testString = "< style defer>";
		newString = testString.replaceAll("<\\s*style\\s*>", "<style harmless=''> ");
		assertNotSame("<style harmless=''> " ,newString );
		
	}
	
	public void testMultithreadedXPCOMInitialization() throws InterruptedException{
		Thread thread1 = new Thread()
		{
			public void run(){
				try 
				{
					initTestingXPCOM();
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		thread1.join();
		Thread.sleep(1000);
		thread1 = new Thread()
		{
			public void run(){
				try 
				{
					initTestingXPCOM();
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
			}
		};
		thread1.start();
		thread1.join();
		
	}
	
	public Document parseRandomHtml(int length) throws ParserInitializationException, DocumentException
	{
		String html = "<html><body>";
		for (int i=0; i<length; i++)
			html += "<div>"+Math.random()+"</div>";
		html += "</body></html>";
		MozillaParser parser = new MozillaParser();
		 return parser.parse(html);
	}
	
	public void testMultithreaded1()
	{
		Thread thread1 = new Thread()
		{
			public void run(){
				try 
				{
					parseRandomHtml(100);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		Thread thread2 = new Thread(){
			public void run(){
				try 
				{
					parseRandomHtml(100);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		thread1.start();
		thread2.start();
		try {
			thread1.join();
			thread2.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	volatile int failed = 0;
	
	public void testMultithreaded2() throws InterruptedException
	{
		
		int NUM_THREADS=50;
		
		final Thread[] threadPool = new Thread[NUM_THREADS];
		final Random random = new Random(0);
		
		for (int i=0; i<NUM_THREADS; i++)
			threadPool[i]= new Thread(){
			public void run(){
				try {
					double randomNumber = random.nextDouble()*100000000.0;
					String html = "<html><body>";
					for (int i=0; i<100; i++)
						html += "<p>"+randomNumber+"</p>";
					html += "</body></html>";
					MozillaParser parser = new MozillaParser();
					 Document document = parser.parse(html);
					 Vector<String> instructions = parser.getDomBuilderArguments();
					 int closeNodeCounter=0;
					 int openNodeCounter=0;
					 for (String instruction:instructions)
					 {
						 if (instruction.equalsIgnoreCase("CloseNode"))
							 closeNodeCounter++;
						 if (instruction.equalsIgnoreCase("OpenNode"))
							 openNodeCounter++;
						 
					 }
//					 System.err.println("Close Node Counter :" + closeNodeCounter);
//					 System.err.println("Open Node Counter :" + openNodeCounter);
					 
					 if (!serialize(document).equals("<?xml version=\"1.0\"?>\n"+html))
					 {
						 synchronized(threadPool)
						 {
							 System.err.println("Html input was  :" + "<?xml version=\"1.0\"?>\n"+html);
							 System.err.println("Failed document :" + serialize(document));
							 parser.dump();
							 System.err.println("Verifying :" + document.getChildNodes().item(0).getChildNodes().item(0).getChildNodes().item(0).getNodeName() );
							 System.err.println("<p number : > : " + document.getChildNodes().item(0).getChildNodes().item(0).getChildNodes().getLength());
							 
							 failed++;
						 }
					 }
					
				}
				catch (Throwable e)
				{
					e.printStackTrace();
					failed++;
				}
			}
		};
		for (int i=0; i<NUM_THREADS; i++)
			threadPool[i].start();
		for (int i=0; i<NUM_THREADS; i++)
			threadPool[i].join();
		assertEquals(0, failed);
	}
	
	
	
	@SuppressWarnings("unchecked")
	public void testEntityDomWriterBug() throws Exception{
		String testString = 
			"<!doctype html public \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">"+
			"<html>" +
			"<body>"+
//			"<a href=\"http://us.ard.yahoo.com/SIG=12ku07d54/M=289534.6742909.7689533.6551553/D=yahoosrch/S=2766679:HLSCH/Y=YAHOO/EXP=1167961934/A=2828626/R=0/SIG=10mgpruen" +
//			"/*http://www.yahoo.com?fr=yfp-t-501\">Yahoo!</a> &nbsp; " +
//			"<a href=\"http://us.ard.yahoo.com/SIG=12ku07d54/M=289534.6742909.7689533.6551553/D=yahoosrch/S=2766679:HLSCH/Y=YAHOO" +
//			"" +
//			"/EXP=1167961934/A=2828626/R=1/SIG=11nbq2pc6/*http://us.rd.yahoo.com/evt=31554/*http://my.yahoo.com?fr=yfp-t-501\">" +
//			"My Yahoo!</a>" +
//			" &nbsp;" +
			" <a href=\"http://us.ard.yahoo.co" +
			"" +
			"m/SIG=12ku07d54/M=289534.6742909.7689533.6551553/D=yahoosrch/S" +
			"" +
			"=2766679:HLSCH/Y=YAHOO/EXP=1167961934/A=2828626/R=2/SIG=10n3m6b64/*http" +
			"://mail.yahoo.com?fr=yfp-t-501\">" +
			"Mail</a> " +
			"&nbsp; &nbsp;" +
			" Welcome, " +
			"<strong>Guest</strong> [" +
			"";
			
			Document document = new MozillaParser().parse(testString);
			
		    ByteArrayOutputStream bs = new ByteArrayOutputStream();
		    OutputStreamWriter oSW = null;
		      oSW = new OutputStreamWriter(bs);
		      
		    OutputFormat format = OutputFormat.createPrettyPrint();
		    format.setXHTML(false);
		    format.setExpandEmptyElements(true);
		    HTMLWriter writer = new HTMLWriter(oSW, format);
		    Set tags = writer.getPreformattedTags();
		    tags.add("STYLE");
		    writer.setPreformattedTags(tags);
		    
		    DOMReader domReader = new DOMReader(); 
		    
//		    System.out.println(" dom serialization : \n "+ serialize(document));
		    
		      writer.write(domReader.read(document));
		      writer.flush();

		      // nhaving no exception means that the test is OK.
			
	}
	
	
	// from dapper : TODO : put this in a UTIL class :
	  private String findEncoding(Element rootElement) {
		    String encoding = "UTF-8";
		    NodeList metas = rootElement.getElementsByTagName("meta");
		    for (int m = 0; m < metas.getLength(); m++) {
		      Element meta = (Element)metas.item(m);
		      // find if we have an http-equiv attribute :
		      boolean hasHttpEquivContentType = false;		// guilty until proven otherwise.
		      boolean hasNameContentType = false;		// guilty until proven otherwise.
		      if (meta.getAttribute("http-equiv").length()>0)
		      {
		    	  hasHttpEquivContentType = meta.getAttribute("http-equiv").toLowerCase().equals("content-type");
		      }
		      else
		    	  if (meta.getAttribute("HTTP-EQUIV").length()>0)
		    	  {
			    	  hasHttpEquivContentType = meta.getAttribute("HTTP-EQUIV").toLowerCase().equals("content-type");
		    	  }
		      
		      if (meta.getAttribute("name").length()>0)
		    	  hasNameContentType = meta.getAttribute("name").toLowerCase().equals("content-type");
		      else
		    	  if (meta.getAttribute("NAME").length()>0)
		    		  hasNameContentType = meta.getAttribute("NAME").toLowerCase().equals("content-type");
		      
		      String contentAttributeStr = null;
		      
		      if ( meta.getAttribute("content").length()>0)
		    	  contentAttributeStr = meta.getAttribute("content") ;
		      else
		    	  if ( meta.getAttribute("CONTENT").length()>0)
			    	  contentAttributeStr = meta.getAttribute("CONTENT") ;
		      
		      if ( (hasHttpEquivContentType || hasNameContentType)  &&  contentAttributeStr != null ) {

		        Pattern pat = Pattern.compile("charset\\s?=\\s?(.+);*",Pattern.CASE_INSENSITIVE);
		        Matcher mat = pat.matcher(contentAttributeStr);
		        if (mat.find()) 
		        {
		          encoding = mat.group(1);
		          break;
		        }
		      }
		    }
		    
		    return encoding;
		  }
	
	  private void printDocumentPreety(Document doc) throws IOException{
		  StringWriter stringWriter = new StringWriter();
		    OutputFormat format = OutputFormat.createPrettyPrint();
		    format.setXHTML(false);
		    format.setEncoding(findEncoding(doc.getDocumentElement()));
		    format.setExpandEmptyElements(true);
		    HTMLWriter writer = new HTMLWriter(stringWriter, format);
		    Set tags = writer.getPreformattedTags();
//		    tags.add("STYLE");
		    tags.clear();
		    writer.setPreformattedTags(tags);    
		    DOMReader domReader = new DOMReader(); 
		    writer.write(domReader.read(doc));
//		    System.out.println("Document:\n" + stringWriter.toString());
	  }
	  
//	/**
//	 * @param youTubeContent
//	 * @throws DocumentException 
//	 * @throws NetworkErrorException 
//	 * @throws IOException 
//	 * @throws MalformedURLException 
//	 */
//	private void displayMozillaAndTagsoupDoms(Cacher cacher , String url) throws Exception {
//		String content = null;
//		try
//		{
//			System.err.println("Fetching content from :" + url);
//			content = cacher.getCache(url);
//		}
//		catch (Exception e)
//		{
//			System.err.println("couldn't find contetn for URL:" + url +". grabbing page from net...");
//			content = Util.urlGetContents(new URL(url));
//			cacher.putCache(url , content);
//		}
//		
//		
//		// profile mozilla :
//		Document document = MozillaParser.getInstance().parse(content);
//		
////		 System.out.println("Mozilla encoding :" + findEncoding(document.getDocumentElement()));
//		
//		 printDocumentPreety(document);
//		 
//		// profile tagsoup :
//		Parser htmlParser       = new Parser();
//	     
//	    SAXReader saxReader     = new SAXReader(htmlParser);
//	    saxReader.setMergeAdjacentText(true);
//	    DOMWriter domWriter     = new DOMWriter();
//	    document                = domWriter.write(saxReader.read(new StringReader(content)));
//	    
////	    System.out.println("Tagsoup encoding :" + findEncoding(document.getDocumentElement()));
//	    
//	    printDocumentPreety(document);
//	 
//////	    System.out.println("title :" + );
////	    String nanaTitle = document.getDocumentElement().getChildNodes().item(0) 
////		.getChildNodes().item(4).getTextContent();
////	    for (int i=0; i<nanaTitle.length(); i++)
////	    	System.out.println((int)nanaTitle.charAt(i));
//	}
//	
//	public void testHebrew(){
//		char dalet = 0xD793;
//		System.out.println(dalet);
//	}
//	
//	
//	// this onw is not a true test , just a debug check..
//	public void testHebrewEncoding() throws Exception 
//	{
//		Cacher contentCacher = new Cacher("ohad.dappit.com");
//		displayMozillaAndTagsoupDoms(contentCacher, "http://www.nana.co.il");
//	}
//	
//	
//	Vector<String> contentList = new Vector<String>();
//	
//	public void addToContentList(Cacher cacher , String url) throws Exception{
//		String content = null;
//		try
//		{
//			System.err.println("Fetching content from :" + url);
//			content = cacher.getCache(url);
//		}
//		catch (Exception e)
//		{
//			System.err.println("couldn't find contetn for URL:" + url +". grabbing page from net...");
//			content = Util.urlGetContents(new URL(url));
//			cacher.putCache(url , content);
//		}
//		contentList.add(content);
//	}
//	
//	public void testMultithreadedPerformance() throws Exception {
//		Cacher contentCacher = new Cacher("ohad.dappit.com");
//		contentCacher.setCacheLifeTime(Integer.MAX_VALUE);
//		addToContentList(contentCacher,"http://www.youtube.com/results?search_query=saddam&search=Search");
//		addToContentList(contentCacher, "http://www.digg.com");
//		addToContentList(contentCacher, "http://www.walla.co.il");
//		addToContentList(contentCacher, "http://www.dappit.com");
//		addToContentList(contentCacher, "http://www.cnn.com");
//		addToContentList(contentCacher, "http://slashdot.org");
//		addToContentList(contentCacher, "http://www.netdimes.org");
//		addToContentList(contentCacher, "http://www.yahoo.com");
//		addToContentList(contentCacher, "http://www.mozilla.org");
//		addToContentList(contentCacher, "http://www.nana.co.il");
//		addToContentList(contentCacher, "http://www.finance.com");
//		addToContentList(contentCacher, "http://www.cnn.co.jp/");
//		addToContentList(contentCacher, "http://www.techcrunch.com/");
//		addToContentList(contentCacher, "http://freshmeat.net/");
//		
//		mozillaParsingTime = 0.0;
//		tagsoupParsingTime = 0.0;
//		
//		System.err.println("Mozilla parsing time :" + mozillaParsingTime +" sec.");
//		System.err.println("Tagsoup parsing time :" + tagsoupParsingTime +" sec.");
//		
//		MozillaParsingThread[] mozillaThreads = new MozillaParsingThread[contentList.size()];
//		TagSoupParsingThread[] tagsoupThreads = new TagSoupParsingThread[contentList.size()];
//		
//		for (int i=0; i<contentList.size(); i++)
//		{
//			mozillaThreads[i] = new MozillaParsingThread(contentList.get(i));
//			tagsoupThreads[i] = new TagSoupParsingThread(contentList.get(i));
//		}
//		
//		
//		// first do the tagsoup threads :
//		for (int i=0; i<contentList.size(); i++)
//			tagsoupThreads[i].start();
//		for (int i=0; i<contentList.size(); i++)
//			tagsoupThreads[i].join();
//		
//		// then do mizlla threads :
//		for (int i=0; i<contentList.size(); i++)
//			mozillaThreads[i].start();
//		
//		for (int i=0; i<contentList.size(); i++)
//			mozillaThreads[i].join();
//		
//		System.err.println("--------------> Mozilla parsing time :" + mozillaParsingTime +" sec.");
//		System.err.println("--------------> Tagsoup parsing time :" + tagsoupParsingTime +" sec.");
//		
//		// assert that mozilla parser works no worse than 1.25 the tagsoup time :
//		assertTrue(1.25*tagsoupParsingTime > mozillaParsingTime);
//	}
//	
//	class MozillaParsingThread extends Thread {
//		
//		private final String content;
//
//		public MozillaParsingThread(String content){
//			this.content = content;
//		}
//		
//		public void run()
//		{
//			SimpleTimeProfiler profiler = new SimpleTimeProfiler();
//			profiler.start();
//			MozillaParser.getInstance().parse(content);
//			mozillaParsingTime += profiler.report("Mozilla:");
//		}
//		
//		
//	}
//	
//class TagSoupParsingThread extends Thread 
//{
//		
//		private final String content;
//
//		public TagSoupParsingThread(String content){
//			this.content = content;
//		}
//		
//		public void run()
//		{
//			SimpleTimeProfiler profiler = new SimpleTimeProfiler();
//			profiler.start();
//			try {
//				tagSoupParse(content);
//			} catch (DocumentException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			tagsoupParsingTime += profiler.report("Tagsoup:");
//		}
//		
//		
//	}
//	
//	
//	
//	public void testPerformance() throws Exception
//	{
//		mozillaParsingTime = 0.0;
//		tagsoupParsingTime = 0.0;
//		
//		Cacher contentCacher = new Cacher("ohad.dappit.com");
//		contentCacher.setCacheLifeTime(Integer.MAX_VALUE);
//		
//		compareMozillaAndTagsoup(contentCacher,"http://www.youtube.com/results?search_query=saddam&search=Search");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.digg.com");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.walla.co.il");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.dappit.com");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.cnn.com");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://slashdot.org");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.netdimes.org");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.yahoo.com");
//		
//		compareMozillaAndTagsoup(contentCacher, "http://www.mozilla.org");
//		compareMozillaAndTagsoup(contentCacher, "http://www.nana.co.il");
//		compareMozillaAndTagsoup(contentCacher, "http://www.finance.com");
//		compareMozillaAndTagsoup(contentCacher, "http://www.cnn.co.jp/");
//		compareMozillaAndTagsoup(contentCacher, "http://www.techcrunch.com/");
//		compareMozillaAndTagsoup(contentCacher, "http://freshmeat.net/");
//		
//		
//		System.err.println("--------------> Mozilla parsing time :" + mozillaParsingTime +" sec.");
//		System.err.println("--------------> Tagsoup parsing time :" + tagsoupParsingTime +" sec.");
//		
//		// assert that mozilla parser works no worse than 1.25 the tagsoup time :
//		assertTrue(1.25*tagsoupParsingTime > mozillaParsingTime);
//		
//	}
//	
	private Document tagSoupParse(String content) throws DocumentException{
		Parser htmlParser       = new Parser();
	     
	    SAXReader saxReader     = new SAXReader(htmlParser);
	    saxReader.setMergeAdjacentText(true);
	    DOMWriter domWriter     = new DOMWriter();
	   return  domWriter.write(saxReader.read(new StringReader(content)));
	}
//
//	public void testCrawler() throws MalformedURLException, IOException, NetworkErrorException, CacheDirectoryException, CacheWriteException, DocumentException{
//		
//		Cacher cacher = new Cacher();
//		cacher.setCacheLifeTime(Integer.MAX_VALUE);
//		for (int i=1; i<20 ; i++)
//		{
//			int start=10*i;
//			String googleUrlString = "http://www.google.co.il/search?q=windows&hl=iw&lr=&start=" +start +  "&sa=N";
//			System.out.println("Fetching :" +googleUrlString);
//			String urlContent = Util.urlGetContents( new URL(googleUrlString));
//			
//			Document googleDoc = tagSoupParse(urlContent);
////			Document googleDoc = MozillaParser.getInstance().parse(urlContent);
//			NodeList anchors = googleDoc.getElementsByTagName("a");
//			System.out.println("number of anchors : " + anchors.getLength());
//			for (int j=0; j<anchors.getLength() ; j++)
//			{
//				Attr hrefAttribute = (Attr)anchors.item(j).getAttributes().getNamedItem("href");
//				if (hrefAttribute!=null)
//				{
//					String attributeValue = hrefAttribute.getValue();
//					if (attributeValue.startsWith("http://") && !attributeValue.endsWith(".pdf"))
//					{
//						System.err.println(i+":"+j+"/"+anchors.getLength()+ " : Fetching from : " + attributeValue);
//						String urlContent2=null;
//						try 
//						{
//							urlContent2 = cacher.getCache(attributeValue);
//						}
//						catch (Exception e)
//						{
//							try
//							{
//								urlContent2 = Util.urlGetContents(new URL(attributeValue));
//							}
//							catch (Exception ex) 
//							{
//								ex.printStackTrace();
//								urlContent2 = "<html>";
//							}
//							cacher.putCache(attributeValue, urlContent2);
//						}
////						tagSoupParse(urlContent2);
//						MozillaParser.getInstance().parse(urlContent2);
//					}	
//				}
//			}
//			
//			
//			
//		}
//		
//		
//	}
//	
//
//	volatile double mozillaParsingTime = 0.0;
//	volatile double tagsoupParsingTime = 0.0;
//	
//	/**
//	 * @param youTubeContent
//	 * @throws DocumentException 
//	 * @throws NetworkErrorException 
//	 * @throws IOException 
//	 * @throws MalformedURLException 
//	 */
//	private void compareMozillaAndTagsoup(Cacher cacher , String url) throws Exception {
//		String content = null;
//		try
//		{
//			System.err.println("Fetching content from :" + url);
//			content = cacher.getCache(url);
//		}
//		catch (Exception e)
//		{
//			System.err.println("couldn't find contetn for URL:" + url +". grabbing page from net...");
//			content = Util.urlGetContents(new URL(url));
//			cacher.putCache(url , content);
//		}
//		
//		SimpleTimeProfiler profiler = new SimpleTimeProfiler();
//		
//		// profile mozilla :
//		profiler.start();
////		System.out.println("Parsing content : "+ content);
//		MozillaParser.getInstance().parse(content);
//		mozillaParsingTime += profiler.report("Mozilla:");
//		
//		// profile tagsoup :
//		profiler.start();
//		tagSoupParse(content);
//	    tagsoupParsingTime+= profiler.report("tagsoup:");
//	}
//	
//	
//	public void testXClarisWindow() throws Exception
//	{
//		
//		// came across this error that crashed the parser :
////		###!!! ASSERTION: unsupported leaf node type: 'Not Reached', file C:\dapper\mozilla\parser\htmlparser\java\JavaContentSink.cpp, line 782
////		Break: at file C:\dapper\mozilla\parser\htmlparser\java\JavaContentSink.cpp, line 782
//		
//		Cacher contentCacher = new Cacher("ohad.dappit.com");
//		contentCacher.setCacheLifeTime(Integer.MAX_VALUE);
//		
//		compareMozillaAndTagsoup(contentCacher," http://www.sdcoe.k12.ca.us/score/cla.html");
//	}
//	
//	
//	// WARNING : THIS TEST IS NOT WORKING AUTOMATICALLY
//	// YOU MUST CHECK THAT THE MEMORY CONSUMPTION IN NOT INCREASING MANUALLY
//	// TODO : FIND A BETTER WAY TO HANDLE THIS
	public void testMemoryLeak() throws Exception
	{
		SimpleMemoryProfiler memoryProfiler = new SimpleMemoryProfiler();
		memoryProfiler.start();
		for (int i=0; i<20000; i++)
		{
			testSimple2();
		}
		//assertTrue("Memory diff is bigger than 20MB. Please check for memory leak" , memoryProfiler.report("Total memory diff") > -100000.0);
	}
	
	
	
}
