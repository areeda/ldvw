package readFormatFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileProcessor
{
	String fName;
	String rti = "APT";
	String lastErrorMsg="";

    private Character just='L';
    private String fmt = "AN";
    private Integer len = 0;
    private Integer strtPos =0;
    private String elNum = "";
    private String comment = "";

	public FileProcessor(String inFile)
	{
		fName = inFile;
	}

	public Integer readIt(String recType)
	{
		rti = recType;
		Integer ret = 0;	// assume no error
		lastErrorMsg = "";
		File in = new File(fName);
		Pattern strtPat = Pattern.compile("([L/R])\\s+(\\w+)\\s+0*(\\d+)\\s+0*(\\d+)\\s+(\\S+)\\s+(.+)$");
		
        BufferedReader input;
		try
		{
			input = new BufferedReader(new FileReader(in));
	        String line = null; //not declared within while loop
	        int state = 0;		// 0 state is looking for record type indicator
	        
	        while ((line = input.readLine()) != null && ret == 0)
	        {
	        	// first thing we look for is 
	        	// L AN 0003 00001  N/A     RECORD TYPE INDICATOR.
                // APT: BASIC LANDING FACILITY DATA
	        	Matcher m = strtPat.matcher(line);
	        	if (m.find())
	        	{
	        		if (state == 0)
	        		{	// start of a field descriptor
	        			initRec(m);
	        			state = 1;		// appending comments
	        		}
	        		else if (state == 1)
        			{	// we're looking for the start of a particular record
        				if (comment.contains("RECORD")  && comment.contains("TYPE") && comment.contains("INDICATOR"))
        				{ 
        					if (comment.contains(rti) && rti.length() != 0	)
	        				{
	        					Main.logger.info(rti + ": record start found.");
	        					state = 3;	// we're looking for a field
	        				}
        					else if (state == 3)
        					{
        						Main.logger.fine("End of record type " + rti + " by start of new record type\n" + line);
        						state = 0;
        					}
        					else if (comment.contains(rti) && rti.length() == 0)
        					{
        						state = 0;
        						Main.logger.info(rti + ": record start found.  " + "(" + strtPos + "," + len + ") - " + comment);
        					}
        				}
        				initRec(m);		// either way we're starting a new record
        			}
        			else if (state == 3)
        			{	// we're in a record, output this field
        				Main.logger.fine("(" + strtPos + "," + len + ") - " + comment);
        				
        				// and start a new one, we stay in state 3
        				initRec(m);
        			}
	        	}
	        	else if (state == 1 || state == 3)
        		{	// append comment
        			String c = line.trim();
        			if (c.length() > 0)
        				comment += " | " + c;
        		}

	        	
	        }
		}
		catch (FileNotFoundException e)
		{
			lastErrorMsg = "File: " + fName + " not found.";
			ret = 1;
		}
		catch (IOException e)
		{
			
		}
		return ret;
	}
	private void initRec(Matcher m)
	{
		just = m.group(1).trim().charAt(0);	// justification L/R
		fmt = m.group(2);
		len = Integer.parseInt(m.group(3));
		strtPos = Integer.parseInt(m.group(4));
		elNum = m.group(5);
		comment = m.group(6);

	}
}
