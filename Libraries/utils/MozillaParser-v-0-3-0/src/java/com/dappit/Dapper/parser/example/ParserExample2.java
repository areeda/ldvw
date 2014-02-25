/**
 * 
 */
package com.dappit.Dapper.parser.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;

import org.dom4j.DocumentException;
import org.w3c.dom.Document;

import com.dappit.Dapper.parser.EnviromentController;
import com.dappit.Dapper.parser.MozillaParser;
import com.dappit.Dapper.parser.ParserInitializationException;

/**
 * @author Ohad Serfaty
 *
 */
public class ParserExample2 {
	
	public static void main(String[] args) throws Exception 
	{
		// parser library :
		
		File parserLibraryFile = new File("./native/bin/MozillaParser" + EnviromentController.getSharedLibraryExtension());
		String parserLibrary = parserLibraryFile.getAbsolutePath();
		System.out.println("Loading Parser Library :" + parserLibrary);
		//	mozilla.dist.bin directory :
		final File mozillaDistBinDirectory = new File("mozilla.dist.bin."+EnviromentController.getOperatingSystemName());
		MozillaParser.init(parserLibrary,mozillaDistBinDirectory.getAbsolutePath());		
		MozillaParser parser = new MozillaParser();
		BufferedReader reader = new BufferedReader(new FileReader(new File("./testParser.html")));
		String line;
		StringBuilder text=new StringBuilder();
		while((line = reader.readLine())!= null)
		{
			text.append(line.replace((char)162, ' '));
			text.append('\n');
		}
		
		parser.parse(text.toString());
				
	}
				

}
