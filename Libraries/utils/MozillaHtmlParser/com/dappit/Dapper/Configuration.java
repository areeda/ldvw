package com.dappit.Dapper;

import java.io.File;

import com.dappit.Dapper.parser.EnviromentController;

/* This file should be renamed to Configuration.java and configured */
public class Configuration {
  

/**
 * @return
 */
public static String getMozillaComponentsPath() {
	try 
	{
//		File distBinDirectory = new File("mozilla.dist.bin."+EnviromentController.getOperatingSystemName());
		File distBinDirectory = new File("C:\\dapper\\mozilla\\dist\\bin");
		return distBinDirectory.getAbsolutePath();
	} 
	catch (Exception e) 
	{
		e.printStackTrace();
		return null;
	}
}

}
