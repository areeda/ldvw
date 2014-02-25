/**
 * 
 */
package com.dappit.Dapper.parser.profiler;

/**
 * @author Ohad Serfaty
 *
 * this class is not working properly. please try another way...
 *
 */
@SuppressWarnings("unused")
public class SimpleMemoryProfiler extends SimpleProfiler {

	
	
	private long startFreeMem;
	
	private long startTotalMem;
	private long startMaxMemory;

	/* (non-Javadoc)
	 * @see com.dappit.Dapper.parser.profiler.SimpleProfiler#report(java.lang.String)
	 */
	@Override
	public double report(String reportPrefix) {
		System.gc();
		long currentFreeMem = Runtime.getRuntime().freeMemory();
		long currentTotalMem = Runtime.getRuntime().totalMemory();
		long currentMaxMemory = Runtime.getRuntime().maxMemory();
		
		System.out.println("free :" + currentFreeMem);
//		System.out.println("total : " + currentTotalMem);
//		System.out.println("max : " + currentMaxMemory);
		if (reportPrefix != null)
			System.err.println(reportPrefix +": " + (currentFreeMem - startFreeMem));
		return currentFreeMem - startFreeMem;
	}

	/* (non-Javadoc)
	 * @see com.dappit.Dapper.parser.profiler.SimpleProfiler#start()
	 */
	@Override
	public void start() 
	{
		System.gc();
		startFreeMem = Runtime.getRuntime().freeMemory();
		startTotalMem =  Runtime.getRuntime().totalMemory();
		startMaxMemory = Runtime.getRuntime().maxMemory();
		
		System.out.println("free :" + startFreeMem);
//		System.out.println("total : " + startTotalMem);
//		System.out.println("max : " + startMaxMemory);
	}

}
