/**
 * 
 */
package com.dappit.Dapper.parser.profiler;

/**
 * @author Ohad Serfaty
 *
 */
public abstract class SimpleProfiler {

	  public abstract void start() ;
	  
	  public abstract double report(String reportPrefix);
	  
}
