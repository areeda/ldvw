/**
 * 
 */
package com.dappit.Dapper.parser.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.dappit.Dapper.parser.EnviromentController;
import com.dappit.Dapper.parser.MozillaParser;



/**
 * @author Ohad Serfaty
 *
 */
public class CustomTests
{

	
	public static void main(String[] args) throws Exception
	{
		File testFile = new File("test.html");
		BufferedReader reader = new BufferedReader(new FileReader(testFile));
		
		String line = null;
		StringBuilder string = new StringBuilder();
		while ((line = reader.readLine()) != null)
		{
			string.append(line);
			string.append('\n');
		}
		
		System.out.println(string.toString());
		
		File parserLibraryFile = new File("./native/bin/MozillaParser" + EnviromentController.getSharedLibraryExtension());
		String parserLibrary = parserLibraryFile.getAbsolutePath();
		System.out.println("Loading Parser Library :" + parserLibrary);
		//	mozilla.dist.bin directory :
		final File mozillaDistBinDirectory = new File("mozilla.dist.bin."+EnviromentController.getOperatingSystemName());
		MozillaParser.init(parserLibrary,mozillaDistBinDirectory.getAbsolutePath());	
		
		MozillaParser parser = new MozillaParser();
		parser.parse(string.toString());
	}
	
}

