/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
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

package edu.fullerton.timeseriesapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
public class TimeSeriesCommandLine
{
 
    private boolean logX;
    private boolean logY;
    private boolean detrend;


    private Integer outX;
    private Integer outY;
    private Integer startGPS = 0;
    private Integer duration;
    private Integer xTicks;
    private Integer yTicks;

    private Float secPerFFT;
    private Float overlap;
    private Float fmin;
    private Float fmax;

    private String ofileName;

    // input specification
    private String[] channelNames;
    private String server;
    private String cType;               // chan type eg raw, rds, ...

    // used in parsing
    private boolean wantHelp;
    private boolean ret;
    private CommandLine line;
    private String ermsg = "";
    private final String intpat = "^\\d+$";

    private final String fltpat = "^[-+]?[0-9]*\\.?[0-9]+$";

    ;
    private String timeAxis;
    
    
    boolean parseCommand(String[] args, String programName, String version)
    {
        ret = true;

        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));

        options.addOption(new Option("semilog", "log scale Y-axis"));
        options.addOption(new Option("nodetrend", "Do not detrend (subtract linear fit) the data before transform."));

        options.addOption(OptionBuilder.withArgName("out").hasArg().withDescription("output filename").create("outfile"));
        options.addOption(OptionBuilder.withArgName("geometry").hasArg().withDescription("image size <X>x<Y> [default=640x480]").create("geom"));

        options.addOption(OptionBuilder.withArgName("channel").hasArgs().withDescription("channel <name,type> multiples accepted").create("chan"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("server <URL> [default=best match]").create("server"));
        options.addOption(OptionBuilder.withArgName("ctype").hasArg().withDescription("channel type eg (raw, rds, minute-trend [default=best match]").create("ctype"));

        options.addOption(OptionBuilder.withArgName("start").hasArg().withDescription("GPS start time").create("start"));
        options.addOption(OptionBuilder.withArgName("duration").hasArg().withDescription("duration (seconds)").create("dur"));
        options.addOption(OptionBuilder.withArgName("xticks").hasArg().withDescription("tick marks/grid lines on time axis").create("xticks"));
        options.addOption(OptionBuilder.withArgName("yticks").hasArg().withDescription("tick marks/grid lines on freq axis").create("yticks"));
        options.addOption(OptionBuilder.withArgName("timeaxis").hasArg().withDescription("time axis label (utc, gps, dt [default=dt]").create("timeaxis"));

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
            logX = line.hasOption("logfreq");
            logY = line.hasOption("logpower");
            detrend = !line.hasOption("nodetrend");

            startGPS = getIntegerOpt("start");
            duration = getIntegerOpt("dur");
            xTicks = getIntegerOpt("xticks");
            yTicks = getIntegerOpt("yticks");

            secPerFFT = getFloatOpt("secpfft");
            overlap = getFloatOpt("overlap");
            fmin = getFloatOpt("fmin");
            fmax = getFloatOpt("fmax");

            if (line.hasOption("outfile"))
            {
                ofileName = line.getOptionValue("outfile");
            }
            String val;

            Matcher m;

            if (line.hasOption("geom"))
            {
                val = line.getOptionValue("geom");
                Pattern gp = Pattern.compile("(\\d+)x(\\d+)");
                m = gp.matcher(val);
                if (m.find())
                {
                    String sx = m.group(1);
                    String sy = m.group(2);
                    if (sx.matches(intpat) && sy.matches(intpat))
                    {
                        outX = Integer.parseInt(sx);
                        outY = Integer.parseInt(sy);
                    }
                    else
                    {
                        ermsg += "can't parse geometry value (" + val + ")";
                        wantHelp = true;
                        ret = false;
                    }
                }
                else
                {
                    ermsg += "can't parse geometry value (" + val + ")";
                    wantHelp = true;
                    ret = false;
                }
            }

            if (line.hasOption("chan"))
            {
                channelNames = line.getOptionValues("chan");
            }
            if (line.hasOption("server"))
            {
                server = line.getOptionValue("server");
            }

            if (line.hasOption("ctype"))
            {
                cType = line.getOptionValue("ctype");
            }
            if (line.hasOption("timeaxis"))
            {
                timeAxis = line.getOptionValue("timeaxis");
            }
            else
            {
                timeAxis = "dt";
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
    private Float getFloatOpt(String param)
    {
        Float retval = null;
        if (line.hasOption(param))
        {
            String val = line.getOptionValue(param);
            if (val.matches(fltpat))
            {
                retval = Float.parseFloat(val);
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

    public boolean isLogX()
    {
        return logX;
    }

    public boolean isLogY()
    {
        return logY;
    }

    public boolean isDetrend()
    {
        return detrend;
    }



    public Integer getOutX()
    {
        outX = outX == null ? 640 : outX;
        return outX;
    }

    public Integer getOutY()
    {
        outY = outY == null ? 480 : outY;
        return outY;
    }

    public Integer getStartGPS()
    {
        startGPS = startGPS == null ? 0 : startGPS;
        return startGPS;
    }

    public Integer getDuration()
    {
        duration = duration == null ? 0 : duration;
        return duration;
    }

    public Integer getxTicks()
    {
        xTicks = xTicks == null ? 0 : xTicks;
        return xTicks;
    }

    public Integer getyTicks()
    {
        yTicks = yTicks == null ? 0 : yTicks;
        return yTicks;
    }

    public Float getSecPerFFT()
    {
        secPerFFT = secPerFFT == null ? -1 : secPerFFT;
        return secPerFFT;
    }

    public Float getOverlap()
    {
        return overlap;
    }

    public Float getFmin()
    {
        return fmin;
    }

    public Float getFmax()
    {
        return fmax;
    }

    public String getOfileName()
    {
        ofileName = ofileName == null ? "" : ofileName;
        return ofileName;
    }

    public String[] getChannelNames()
    {
        return channelNames;
    }

    public String getServer()
    {
        return server;
    }

    public String getcType()
    {
        return cType;
    }

    public String getTimeAxis()
    {
        return timeAxis;
    }

   
}
