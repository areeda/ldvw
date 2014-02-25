/**
 * 
 */
package com.dappit.Dapper.parser.example;

import java.io.File;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;

import com.dappit.Dapper.parser.EnviromentController;
import com.dappit.Dapper.parser.MozillaParser;
import com.dappit.Dapper.parser.ParserInitializationException;

/**
 * @author Ohad Serfaty
 *
 */
public class ParserExample {
	
	public static void main(String[] args) throws Exception 
	{
		// parser library :
		
		File parserLibraryFile = new File("./native/bin/MozillaParser" + EnviromentController.getSharedLibraryExtension());
		String parserLibrary = parserLibraryFile.getAbsolutePath();
		System.out.println("Loading Parser Library :" + parserLibrary);
		//	mozilla.dist.bin directory :
		final File mozillaDistBinDirectory = new File("mozilla.dist.bin."+EnviromentController.getOperatingSystemName());
		
		
		MozillaParser.init(parserLibrary,mozillaDistBinDirectory.getAbsolutePath());		
	
		Thread thread1 = new Thread()
		{
			
			public void run(){
				try 
				{
					
				MozillaParser parser = new MozillaParser();
				
					String html = "<html><body>";
					for (int i=0; i<100; i++)
						html += "<li><table><tr><td>1111111111111111</table>\n";
					html += "</body></html>";
					Document document = parser.parse(html);
					System.out.println("Generated document :" + ((org.dom4j.Document)document).asXML());
				}
				 catch (DocumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("done...");
			}
		};
		
		Thread thread2 = new Thread(){
			
			public void run(){
				try 
				{
				//MozillaParser.initXPCOM(mozillaDistBinDirectory.getAbsolutePath());
				MozillaParser parser = new MozillaParser();
				System.out.println("parsing...");
				
					String html = "<html><body>";
					for (int i=0; i<100; i++)
						html += "<li><table><tr><td>222222222222222</table>\n";
					html += "</body></html>";
					Document document = parser.parse(html);
					System.out.println("Generated document :" + ((org.dom4j.Document)document).asXML());
				}
				catch (Exception e) 
				{
					e.printStackTrace();
				}
				System.out.println("done...");
			}
		};
		
		thread1.start();
		thread2.start();
		thread1.join();
		thread2.join();
		
//		// start the mozilla parser with the right library/components directory :
//		MozillaParser.init(parseLibrary, mozillaDistBinDirectory.getAbsolutePath());
//		
//		// parse the document :
//		Document domDocument = MozillaParser.getInstance().parse("<html>Hello world!<html>");
//		
//		// stop the parser ( essential for a clean exit ).
//		MozillaParser.getInstance().stopRunning();
	}

}
