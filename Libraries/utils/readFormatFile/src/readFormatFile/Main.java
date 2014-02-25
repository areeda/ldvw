package readFormatFile;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;




public class Main
{
	public static Logger logger = Logger.getLogger(Main.class.getName());

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		Options opt = new Options();
		opt.addOption("v", "verbose", true, "verbosity 0=errors only 5=all the boring stuff");
		opt.addOption("t", "recordType", true, "Record type, eg APT, required");
		opt.addOption("h", "help", false, "Print this message.");
		CommandLineParser parser = new PosixParser();
		try
		{
			Boolean doit = true;
			Integer verbose=1;
			String recType = "";
			
			CommandLine cmd = parser.parse( opt, args);
			if (cmd.hasOption("v"))
			{
				String vStr = cmd.getOptionValue("v");
				verbose = Integer.parseInt(vStr);
			}
			Level lev = Level.WARNING;
			if (verbose > 6)verbose=6;
			switch (verbose)
			{
				case 0: lev = Level.SEVERE; break;
				case 1: lev = Level.WARNING; break;
				case 2: lev = Level.INFO; break;
				case 3: lev = Level.FINE; break;
				case 4: lev = Level.FINER; break;
				case 5: lev = Level.FINEST; break;
				case 6: lev = Level.ALL; break;
			}
			Logger.getLogger(Main.class.getName()).setLevel(lev);
			for (Handler handler : Logger.getLogger("").getHandlers()) 
			{
			    handler.setLevel(Level.FINE);
			}

			String[] argList = cmd.getArgs();
			
			if (argList.length == 0)
			{
				System.err.println(" Command is missing input file\n");
				doit = false;
			}
			if (cmd.hasOption("t"))
			{
				recType = cmd.getOptionValue("t");
			}
			else
			{
				recType = "";
			}
			
			if (cmd.hasOption("h") || !doit)
			{
				// automatically generate the help statement
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp( "AptDBCreate [options] <input file>", opt );
				doit = false;
			}
			
			if (doit)
			{
				
				for(String inFile : argList)
				{
					FileProcessor fp = new FileProcessor(inFile);
					Integer stat = fp.readIt(recType);
					if (stat == 0 && verbose > 0)
						System.out.println("File: " + inFile + " processed.");
					else if (stat > 0)
						System.err.println("File: " + inFile + " had errors.");
				}
			}
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
