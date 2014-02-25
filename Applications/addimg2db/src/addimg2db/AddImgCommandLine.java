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
package addimg2db;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Decode the command line for addimg2db program
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class AddImgCommandLine
{
    private boolean auto;
    private String user;
    private String group;
    private String description;
    
    // used in parsing

    private boolean wantHelp;
    private boolean ret;
    private CommandLine line;
    private String ermsg = "";
    private String intpat = "^\\d+$";
    //@todo get a better regex for floats
    private String fltpat = "^[\\d\\.]+$";
    private String[] files = null;
    private boolean verbose;
    
    boolean parseCommand(String[] args, String programName, String version)
    {
        ret = true;

        Options options = new Options();


        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("verbose", "print information on what we're doing"));
        options.addOption(new Option("debug", "be very verbose"));
        
        options.addOption(new Option("auto", "if file name ends with <gpstime>-<duration><unit> append to description "));
       
        
        options.addOption(OptionBuilder.withArgName("user").hasArg().withDescription("user name for addition [default=\"system\"]").create("user"));
        options.addOption(OptionBuilder.withArgName("group").hasArg().withDescription("group name [default=no group]").create("group"));
        options.addOption(OptionBuilder.withArgName("desc").hasArg().withDescription("description saved with image [default=empty]").create("desc"));
        
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
            auto = line.hasOption("auto");
            verbose = line.hasOption("verbose");
            
            user = "system";
            if (line.hasOption("user"))
            {
                user = line.getOptionValue("user");
            }
            
            group = "";
            if (line.hasOption("group"))
            {
                group = line.getOptionValue("group");
            }
            
            description = "";
            if (line.hasOption("desc"))
            {
                description = line.getOptionValue("desc");
            }

            files = line.getArgs();
            
            if (line.hasOption("debug"))
            {
                System.out.format("auto:  %1$b%n", auto);
                System.out.format("desc:  %1$s%n", description);
                System.out.format("group: %1$s%n", group);
                System.out.format("user:  %1$s%n", user);
                for(String file : files)
                {
                    System.out.format("file:  %1$s%n",file);
                }
            }
            
        }
        if (files == null || files.length == 0)
        {
            ermsg += "No input files specified.\n";
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
            String syntax = "java -jar "+ programName + ".jar [options] imageFilePath...";
            formatter.printHelp(syntax, options);
            ret = false;
        }
        
        return ret;
    }

    public boolean isAuto()
    {
        return auto;
    }

    public String getUser()
    {
        return user;
    }

    public String getGroup()
    {
        return group;
    }

    public String getDescription()
    {
        return description;
    }

    public String[] getFiles()
    {
        return files;
    }
    
}
