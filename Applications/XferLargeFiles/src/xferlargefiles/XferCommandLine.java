/*
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package xferlargefiles;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class XferCommandLine
{
    private boolean showProgressBar;
    private String ofileName;
    
    // input specification
    private String channelName;
    private String server;
    private String cType;               // chan type eg raw, rds, ...
    
    private Integer startGPS = 0;
    private Integer duration;


    // used in parsing
    private boolean wantHelp;
    private boolean ret;
    private CommandLine line;
    private String ermsg = "";
    private String intpat = "^\\d+$";
    //@todo get a better regex for floats
    private String fltpat = "^[\\d\\.]+$";

    public boolean parseCommand(String[] args, String programName, String version)
    {
        ret = true;

        Options options = new Options();


        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("progress", "show graphical progress bar"));
        
        options.addOption(OptionBuilder.withArgName("out").hasArg().withDescription("output filename").create("outfile"));

        options.addOption(OptionBuilder.withArgName("channel").hasArg().withDescription("channel <name,type>").create("chan"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("server <URL> [default=best match]").create("server"));
        options.addOption(OptionBuilder.withArgName("ctype").hasArg().withDescription("channel type eg (raw, rds, minute-trend [default=best match]").create("ctype"));

        options.addOption(OptionBuilder.withArgName("start").hasArg().withDescription("GPS start time").create("start"));
        options.addOption(OptionBuilder.withArgName("duration").hasArg().withDescription("duration (seconds)").create("dur"));

        CommandLineParser parser = new GnuParser();

        wantHelp = false;
        try
        {
            // parse the command line arguments
            line = parser.parse(options, args);
        }
        catch (ParseException exp)
        {
            // oops, something went wrong
            System.err.println("Command parsing failed.  Reason: " + exp.getMessage());
            wantHelp = true;
            line = null;
        }


        if (line != null)
        {
            if (line.hasOption("version"))
            {
                System.out.println(programName + " - version " + version);
                ret = false;
            }

            wantHelp = line.hasOption("help");

            showProgressBar = line.hasOption("progress");
            startGPS = getIntegerOpt("start");
            duration = getIntegerOpt("dur");

            if (line.hasOption("outfile"))
            {
                ofileName = line.getOptionValue("outfile");
            }
            else
            {
                ermsg += "No output file specified.\n";
            }
            
            if (line.hasOption("chan"))
            {
                channelName = line.getOptionValue("chan");
            }
            else
            {
                ermsg += "No channel specified.\n";
            }
            if (line.hasOption("server"))
            {
                server = line.getOptionValue("server");
            }

            if (line.hasOption("ctype"))
            {
                cType = line.getOptionValue("ctype");
            }
            else
            {
                ermsg += "No channel type specified.\n";
            }

        }
        if (!ermsg.isEmpty())
        {
            System.out.println("Command error:\n" + ermsg);
            wantHelp = true;
            ret = false;
        }
        if (wantHelp)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName, options);
            ret = false;
        }

        return ret;
    }  
    /**
     * check if a parameter was specified,convert it. Sets fields erMsg, wantsHelp and ret if a bad
     * value was entered
     *
     * @param param name of the parameter
     * @return float value of the parameter or null if not specified
     */
    private Integer getIntegerOpt(String param)
    {
        Integer retval = null;
        if (line.hasOption(param))
        {
            String val = line.getOptionValue(param);
            if (val.matches(intpat))
            {
                retval = Integer.parseInt(val);
            }
            else
            {
                ermsg += "can't parse " + param + " value (" + val + ")\n";
                wantHelp = true;
                ret = false;
            }
        }
        return retval;
    }

    //==============Getters===========================
    public boolean isShowProgressBar()
    {
        return showProgressBar;
    }

    public String getOfileName()
    {
        return ofileName;
    }

    public String getChannelName()
    {
        return channelName;
    }

    public String getServer()
    {
        return server;
    }

    public String getcType()
    {
        return cType;
    }

    public Integer getStartGPS()
    {
        return startGPS;
    }

    public Integer getDuration()
    {
        return duration;
    }
    
}
