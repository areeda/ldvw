/**
 * 
 */
package com.dappit.Dapper.parser.profiler;

/**
 * @author Ohad Serfaty
 *
 */
public class SimpleTimeProfiler extends SimpleProfiler {
	  long startStamp;
	  
	  /**
	   * 
	   */
	  public double report(String prefix) 
	  {
	  	long endStamp = System.currentTimeMillis();
	  	double resultSeconds = (double)((double)(endStamp-startStamp)/1000.0);
	  	System.err.println(prefix +" : " + resultSeconds +" sec.");
	  	return resultSeconds;
	  }

	  /**
	   * 
	   */
	  public void start() {
	  	startStamp = System.currentTimeMillis();
	  }

}
