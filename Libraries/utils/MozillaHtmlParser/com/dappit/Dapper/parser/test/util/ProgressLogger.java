/**
 * 
 */
package com.dappit.Dapper.parser.test.util;

import java.util.logging.Logger;

/**
 * @author Ohad Serfaty
 *
 * A testing utility for showing the progress in percents to stdout/err.
 *
 */
public class ProgressLogger {


	long percent=0;
	long totalCount=0;
	long currentCount=0;
	private final Logger logger;
	private String logPrefix="";
	
	public ProgressLogger(Logger logger,long totalCount){
		this.logger = logger;
		this.totalCount = totalCount;
	}
	
	public ProgressLogger(long totalCount){
		this.logger = null;
		this.totalCount = totalCount;
	}
	
	public void incrementCount(long countsToIncrement){
		currentCount = currentCount+countsToIncrement;
		long newPrecent = (long)(Math.floor(100.0*(double)currentCount/(double)totalCount));
		
		if (newPrecent > percent)
		{
			percent = newPrecent;
			if (logger!=null)
				logger.info(logPrefix+percent + "%.. ");
			else
				System.out.print(logPrefix+percent + "%.. ");
			if (percent%10 == 0 && logger==null)
				System.out.println();
		}				
	}
	
	public void incrementCount(){
		incrementCount(1);
	}

	public void setLogPrefix(String logPrefix) {
		this.logPrefix = logPrefix;
	}
}

