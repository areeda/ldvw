/**
 * 
 */
package com.dappit.Dapper.parser.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ccil.cowan.tagsoup.Parser;
import org.dom4j.DocumentException;
import org.dom4j.io.DOMWriter;
import org.dom4j.io.SAXReader;
import org.w3c.dom.Document;

import com.dappit.Dapper.parser.MozillaParser;
import com.dappit.Dapper.parser.profiler.SimpleTimeProfiler;
import com.dappit.Dapper.parser.test.util.ProgressLogger;

/**
 * @author Ohad Serfaty
 *
 */
public class ParserComparator 
{
	
	private static volatile double mozillaParsingTime;
	private static volatile double tagsoupParsingTime;

	public static byte[] fileGetContentsInBytes(File file) throws FileNotFoundException, IOException {
	    FileInputStream fIS = new FileInputStream(file);
	    ByteArrayOutputStream bIS = new ByteArrayOutputStream();
	    byte[] temp = new byte[256];
	    int bytesRead = 0;
	    while ((bytesRead = fIS.read(temp)) != -1) {
	      bIS.write(temp,0,bytesRead);
	    }
	    fIS.close();
	    bIS.close();
	    
	    return bIS.toByteArray();
	  }

	/**
	 * @param youTubeContent
	 * @throws DocumentException 
	 * @throws NetworkErrorException 
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
	private static void compareMozillaAndTagsoup(String content) throws Exception {
		
		SimpleTimeProfiler profiler = new SimpleTimeProfiler();
		
		// profile mozilla :
		profiler.start();
//		System.out.println("Parsing content : "+ content);
		MozillaParser parser = new MozillaParser();
		System.out.println("Mozilla Parsing...");
		parser.parse(content);
		mozillaParsingTime += profiler.report("Mozilla:");

		profiler = new SimpleTimeProfiler();
		// profile tagsoup :
		System.out.println("Tagsoup Parsing...");
		profiler.start();
		tagSoupParse(content);
	    tagsoupParsingTime+= profiler.report("tagsoup:");
	}
	
	private static Document tagSoupParse(String content) {
		Parser htmlParser = new Parser();

		SAXReader saxReader = new SAXReader(htmlParser);
		saxReader.setMergeAdjacentText(true);
		DOMWriter domWriter = new DOMWriter();
		try 
		{
			return domWriter.write(saxReader.read(new StringReader(content)));
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return null;
	}
	
	private static void testZippedContent() throws Exception
	{
		ZipInputStream zippedInputStream = new ZipInputStream(new FileInputStream("./test.content.zip"));
		int counter = 0;
		int maxCount = 1000;
		ProgressLogger progressLogger = new ProgressLogger(maxCount);
		while (counter++ < maxCount)
			{
			
				ZipEntry nextZippedEntry = zippedInputStream.getNextEntry();
				if (nextZippedEntry == null)
					break;
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				System.out.println("Reading zipped file :" + nextZippedEntry.getName());
				byte[] buf = new byte[1024];
		        int len;
		        while ((len = zippedInputStream.read(buf)) > 0) {
		            bos.write(buf, 0, len);
		        }
		        String content = new String(bos.toByteArray());
//		        System.out.println("Content : "+ content);
		        bos.close();
		        compareMozillaAndTagsoup(content);
				
		        progressLogger.incrementCount();
			}
		System.out.println("Mozilla Parsing time :" + mozillaParsingTime +" sec");
		System.out.println("Tagsoup Parsing time :" + tagsoupParsingTime +" sec");
		
	}
	
	public static class ZipFileReader {
		
		private final String fileName;
		private ZipInputStream zippedInputStream;

		public ZipFileReader(String fileName) throws FileNotFoundException{
			this.fileName = fileName;
			zippedInputStream = new ZipInputStream(new FileInputStream(this.fileName));
		}
		
		public synchronized String nextContent() throws Exception
		{
			ZipEntry nextZippedEntry = zippedInputStream.getNextEntry();
			if (nextZippedEntry == null)
				return null;
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			System.out.println("Reading zipped file :" + nextZippedEntry.getName());
			byte[] buf = new byte[1024];
	        int len;
	        while ((len = zippedInputStream.read(buf)) > 0) {
	            bos.write(buf, 0, len);
	        }
	        String content = new String(bos.toByteArray());
//	        System.out.println("Content : "+ content);
	        bos.close();
	        return content;
		}
		
	}
		
	private static void testZippedContentMultithreaded() throws Exception
	{
		int maxThreads = 10;
		ExecutorService mozillThreadPool = Executors.newFixedThreadPool(maxThreads);
		ExecutorService tagsoupThreadPool = Executors.newFixedThreadPool(maxThreads);
		
		ZipFileReader mozillaFileReader = new ZipFileReader("./test.content.zip");
		ZipFileReader tagsoupFileReader = new ZipFileReader("./test.content.zip");
		int counter = 0;
		int maxCount =530;
		
		SimpleTimeProfiler mozillaProfiler = new SimpleTimeProfiler();
		mozillaProfiler.start();
		// first have Mozilla : 
		while (counter++ < maxCount)
		{
			mozillThreadPool.execute(new MozillaParsingThread(mozillaFileReader));
		}
		mozillThreadPool.shutdown();
		mozillThreadPool.awaitTermination(10000, TimeUnit.SECONDS);
		double mozillaTime = mozillaProfiler.report("Mozilla total time");
		
		counter = 0;
		// then have tagsoup :
		SimpleTimeProfiler tagsoupProfiler = new SimpleTimeProfiler();
		tagsoupProfiler .start();
		while (counter++ < maxCount)
		{
			tagsoupThreadPool.execute(new TagsoupParsingThread(tagsoupFileReader));	
		}
		tagsoupThreadPool.shutdown();
		tagsoupThreadPool.awaitTermination(10000, TimeUnit.SECONDS);
		
		double tagsoupTime = tagsoupProfiler.report("Tagsoup total time");
		
		System.out.println("Mozilla Parsing multithreaded time :" + mozillaParsingTime +" sec");
		System.out.println("Tagsoup Parsing multithreaded time :" + tagsoupParsingTime +" sec");
		
		System.out.println("Mozilla Parsing Total time :" + mozillaTime +" sec");
		System.out.println("Tagsoup Parsing Total time :" + tagsoupTime +" sec");
		
	}
	
	public static class MozillaParsingThread extends Thread 
	{
		
		private final ZipFileReader mozillaFileReader;
		private boolean synchronize;
		private static Object SynchronizationObject = new Object();
		private static Hashtable<String, Document> documentHashTable = new Hashtable<String, Document>();

		/**
		 * @param tagsoupFileReader
		 */
		public MozillaParsingThread(ZipFileReader tagsoupFileReader) {
			this(tagsoupFileReader,false);
		}

		/**
		 * @param tagsoupFileReader2
		 * @param b
		 */
		public MozillaParsingThread(ZipFileReader tagsoupFileReader, boolean synchronize) {
			this.synchronize = synchronize;
			this.mozillaFileReader = tagsoupFileReader;
		}
		
		public void run()
		{
			String content;
			try 
			{
				content = mozillaFileReader.nextContent();
				SimpleTimeProfiler profiler = new SimpleTimeProfiler();
				profiler.start();
				MozillaParser parser = new MozillaParser();
				org.dom4j.Document document;
				if (this.synchronize)
				{
					synchronized(SynchronizationObject)
					{
						document = (org.dom4j.Document) parser.parse(content);
					}
				}
				else
				{
					document = (org.dom4j.Document) parser.parse(content);
				}
				
				mozillaParsingTime += profiler.report("Mozilla");
//				org.dom4j.Document document2 = (org.dom4j.Document) parser.parse(content);
//				if (!document2.asXML().equals(document.asXML()))
//				{
//					System.err.println("------------------------->>> content not equals ????");
//				}
				
				documentHashTable.put(content.hashCode()+Boolean.toString(synchronize), (Document) document);
			}
			catch (Exception e) 
			{
				e.printStackTrace();
			}
			
		}
		
		public static Hashtable<String, Document> getDocumentsHashTable(){
			return documentHashTable;
		}
		
	}
	
	public static class TagsoupParsingThread extends Thread {
		
		private final ZipFileReader tagsoupFileReader;
		private final boolean synchronize;
		private static Object SynchronizationObject = new Object();

		/**
		 * @param tagsoupFileReader
		 */
		public TagsoupParsingThread(ZipFileReader tagsoupFileReader) {
			this(tagsoupFileReader,false);
		}

		/**
		 * @param tagsoupFileReader2
		 * @param b
		 */
		public TagsoupParsingThread(ZipFileReader tagsoupFileReader, boolean synchronize) {
			this.synchronize = synchronize;
			this.tagsoupFileReader = tagsoupFileReader;
		}

		public void run()
		{
			try 
			{
				String content = tagsoupFileReader.nextContent();
				SimpleTimeProfiler profiler = new SimpleTimeProfiler();
				profiler.start();
				if (synchronize)
				{
					synchronized(SynchronizationObject)
					{
						tagSoupParse(content);
					}
				}
				else
					tagSoupParse(content);
				tagsoupParsingTime += profiler.report("Tagsoup");
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
		}
		
	}
	
	public static void main(String[] args) throws Exception 
	{
		TestMozillaParser.initTestingXPCOM();
		
		// Scheme 1 :
		testZippedContentMultithreaded();
		
		// Scheme 2 :
//		testTagsoupSynchronizedParsing();
		
		// Scheme 3 :
//		testMozillaSynchronizedParsing();
//		System.out.println( MozillaParsingThread.getDocumentsHashTable());
//		Hashtable<String, Document> documentHashTable = MozillaParsingThread.getDocumentsHashTable();
//		for (String contentType:documentHashTable.keySet())
//		{
//			if (contentType.endsWith("true"))
//			{
//				org.dom4j.Document synchronizedDocumentResult = (org.dom4j.Document) documentHashTable.get(contentType);
//				System.out.println(contentType +"->" + synchronizedDocumentResult);
//				
//				String parralelScontent = contentType.replace("true", "false");
//				org.dom4j.Document unsynchronizedDocumentResult = (org.dom4j.Document) documentHashTable.get(parralelScontent);
//				System.out.println( parralelScontent+"->" +unsynchronizedDocumentResult );
//				if (!unsynchronizedDocumentResult.asXML().equals(synchronizedDocumentResult.asXML()))
//					System.err.println("Not Good : "  + contentType);
//			}
//		}
//		
	}

	/**
	 * @throws Exception 
	 * 
	 */
	private static void testTagsoupSynchronizedParsing() throws Exception {
		tagsoupMultithreadedParse(true , "Tagsoup Synchronized ");
		tagsoupMultithreadedParse(false, "Tagsoup Parallel ");
	}
	
	/**
	 * @throws Exception 
	 * 
	 */
	private static void testMozillaSynchronizedParsing() throws Exception {
		mozillaMultithreadedParse(true , "Mozilla Synchronized ");
		mozillaMultithreadedParse(false, "Mozilla Parallel ");
	}

	/**
	 * @throws FileNotFoundException 
	 * @throws Exception 
	 * 
	 */
	private static void mozillaMultithreadedParse(final boolean synchronize , String reportString) throws Exception
	{
		int maxThreads = 30;
		ExecutorService mozillaThreadPool = Executors.newFixedThreadPool(maxThreads);
		mozillaParsingTime=0;
		ZipFileReader tagsoupFileReader = new ZipFileReader("./test.content.zip");
		int counter = 0;
		int maxCount =530;
		
		// then have tagsoup :
		SimpleTimeProfiler mozillaProfiler = new SimpleTimeProfiler();
		mozillaProfiler .start();
		while (counter++ < maxCount)
		{
			mozillaThreadPool.execute(new MozillaParsingThread(tagsoupFileReader , synchronize));	
		}
		mozillaThreadPool.shutdown();
		mozillaThreadPool.awaitTermination(10000, TimeUnit.SECONDS);
		
		double mozillaTime = mozillaProfiler.report("Tagsoup synchronized total time");
		
		System.out.println(reportString + " time :" + mozillaParsingTime +" sec");
		System.out.println(reportString + " Total time :" + mozillaTime +" sec");
		
	}
	
	/**
	 * @throws FileNotFoundException 
	 * @throws Exception 
	 * 
	 */
	private static void tagsoupMultithreadedParse(final boolean synchronize , String reportString) throws Exception
	{
		int maxThreads = 10;
		ExecutorService tagsoupThreadPool = Executors.newFixedThreadPool(maxThreads);
		tagsoupParsingTime=0;
		ZipFileReader tagsoupFileReader = new ZipFileReader("./test.content.zip");
		int counter = 0;
		int maxCount =530;
		
		// then have tagsoup :
		SimpleTimeProfiler tagsoupProfiler = new SimpleTimeProfiler();
		tagsoupProfiler .start();
		while (counter++ < maxCount)
		{
			tagsoupThreadPool.execute(new TagsoupParsingThread(tagsoupFileReader , synchronize));	
		}
		tagsoupThreadPool.shutdown();
		tagsoupThreadPool.awaitTermination(10000, TimeUnit.SECONDS);
		
		double tagsoupTime = tagsoupProfiler.report("Tagsoup synchronized total time");
		
		System.out.println(reportString + " time :" + tagsoupParsingTime +" sec");
		System.out.println(reportString + " Total time :" + tagsoupTime +" sec");
		
	}
	

}
