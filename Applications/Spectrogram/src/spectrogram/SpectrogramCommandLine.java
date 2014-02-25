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
package spectrogram;

import edu.fullerton.viewerplugin.SpectrumCalc;
import edu.fullerton.viewerplugin.WindowGen;
import edu.fullerton.viewerplugin.WindowGen.Window;
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
public class SpectrogramCommandLine
{
    // all the class fields have getter classes the easiest way to figure out which is what
    // is to run --help as that description has to be up to date
    
    // run time options
    private boolean showProgressBar;
    private boolean useTestData;
    
    private boolean interp;
    private boolean logFreq;
    private boolean logIntensity;
    private boolean norm;
    private boolean detrend;
    private boolean smooth;
    
    private SpectrumCalc.Scaling scaling;
    private WindowGen.Window window;
    private String color;
    
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
    private Float up,lo;                // in percentile

    private String ofileName;
    
    // input specification
    private String channelName;
    private String server;
    private String cType;               // chan type eg raw, rds, ...
    private Float sampleRate;
    
    
    private String testDataFile=null;
    
    // prefilter specification
    private String filtType;
    private Float cutoff;
    private Integer order;
    
    // used in parsing
    private boolean wantHelp;
    private boolean ret;
    private CommandLine line;
    private String ermsg = "";
    private final String intpat = "^\\d+$";
    //@todo get a better regex for floats
    private final String fltpat = "^[\\d\\.]+$";
    
    public SpectrogramCommandLine()
    {
        
    }
    public boolean parseCommand(String[] args, String programName, String version)
    {
        ret = true;
        
        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("progress", "show graphical progress bar"));
        
        options.addOption(new Option("interp", "interpolate resulting image"));
        options.addOption(new Option("logfreq", "use logarithmic frequncy access"));
        options.addOption(new Option("logintensity", "scale intensities by log of amplitude or power"));
        options.addOption(new Option("nodetrend", "Do not detrend (subtract linear fit) the data before transform."));
        options.addOption(new Option("nodata", "don't get data, show lables only"));
        options.addOption(new Option("norm", "divide each fft by mean fft"));
        options.addOption(new Option("smooth", "low pass filter the spectrograms in frequency an time"));

        options.addOption(OptionBuilder.withArgName("out").hasArg().withDescription("output filename").create("outfile"));
        options.addOption(OptionBuilder.withArgName("geometry").hasArg().withDescription("image size <X>x<Y> [default=1280x768]").create("geom"));
        
        options.addOption(OptionBuilder.withArgName("channel").hasArg().withDescription("channel <name,type>").create("chan"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("server <URL> [default=best match]").create("server"));
        options.addOption(OptionBuilder.withArgName("ctype").hasArg().withDescription("channel type eg (raw, rds, minute-trend [default=best match]").create("ctype"));
        options.addOption(OptionBuilder.withArgName("rate").hasArg().withDescription("sample rate (only required if more than one)").create("rate"));        
        
        options.addOption(OptionBuilder.withArgName("start").hasArg().withDescription("GPS start time").create("start"));
        options.addOption(OptionBuilder.withArgName("duration").hasArg().withDescription("duration (seconds)").create("dur"));
        options.addOption(OptionBuilder.withArgName("xticks").hasArg().withDescription("tick marks/grid lines on time axis").create("xticks"));
        options.addOption(OptionBuilder.withArgName("yticks").hasArg().withDescription("tick marks/grid lines on freq axis").create("yticks"));
        options.addOption(OptionBuilder.withArgName("secpfft").hasArg().withDescription("seconds per fft [default=1]").create("secpfft"));
        options.addOption(OptionBuilder.withArgName("fmin").hasArg().withDescription("min frequency to plot [default=1/secpfft]").create("fmin"));
        options.addOption(OptionBuilder.withArgName("fmax").hasArg().withDescription("max frequency to plot [default=<sample rate>/2]").create("fmax"));
        options.addOption(OptionBuilder.withArgName("overlap").hasArg().withDescription("fft overlap (0-.9) [default=0.5]").create("overlap"));
        
        options.addOption(OptionBuilder.withArgName("scale").hasArg().withDescription("spectrum scaling (AS, ASD, PS, PSD) [default=asd]").create("scale"));
        options.addOption(OptionBuilder.withArgName("color").hasArg().withDescription("color table name (hot, jet, bw) [default=hot]").create("color"));  
        options.addOption(OptionBuilder.withArgName("window").hasArg().withDescription("Anti-ringing window (NONE, BLACKMAN, HAMMING, HANNING, FLATTOP) [default=HAMMING]").create("window"));
        options.addOption(OptionBuilder.withArgName("lo").hasArg().withDescription("Min pixel value in percentile (0 <= lo < 1) [default=0").create("lo"));  
        options.addOption(OptionBuilder.withArgName("up").hasArg().withDescription("Max pixel value in percentile (0 < up <= 1) [default=1").create("up"));  

        options.addOption(OptionBuilder.withArgName("filt").hasArg().withDescription("Filter type (high low) [default=none").create("filt"));
        options.addOption(OptionBuilder.withArgName("cutoff").hasArg().withDescription("Filter cutoff frequency (Hz)[default=150 ").create("cutoff"));
        options.addOption(OptionBuilder.withArgName("order").hasArg().withDescription("Order of butterworth [default=4").create("order"));
        
        options.addOption(OptionBuilder.withArgName("testdata").hasArg().withDescription("Use data from csv file <filename>").create("testdata"));
        
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
            useTestData = line.hasOption("nodata");
            logFreq = line.hasOption("logfreq");
            logIntensity = line.hasOption("logintensity");
            detrend = ! line.hasOption("nodetrend");
            smooth = line.hasOption("smooth");
            interp = line.hasOption("interp");

            startGPS = getIntegerOpt("start");
            duration = getIntegerOpt("dur");
            xTicks = getIntegerOpt("xticks");
            yTicks = getIntegerOpt("yticks");

            secPerFFT = getFloatOpt("secpfft");
            overlap = getFloatOpt("overlap");
            fmin = getFloatOpt("fmin");
            fmax = getFloatOpt("fmax");

            lo = getFloatOpt("lo");
            up = getFloatOpt("up");
            
            norm = line.hasOption("norm");

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
                channelName = line.getOptionValue("chan");
            }
            if (line.hasOption("server"))
            {
                server = line.getOptionValue("server");
            }
            
            if (line.hasOption("ctype"))
            {
                cType = line.getOptionValue("ctype");
            }
            
            sampleRate = 0.f;
            if (line.hasOption("rate"))
            {
                sampleRate = getFloatOpt("rate");
            }

            if (line.hasOption("scale"))
            {
                val = line.getOptionValue("scale").toUpperCase();
                try
                {
                    scaling = SpectrumCalc.Scaling.valueOf(val.toUpperCase());
                }
                catch (IllegalArgumentException ex)
                {
                    ermsg += "Unknown scale (" + val + ")\n";
                }
            }
            else
            {
                scaling = SpectrumCalc.Scaling.ASD;
            }
            
            if (line.hasOption("window"))
            {
                val = line.getOptionValue("window").toUpperCase();
                try
                {
                    window = WindowGen.Window.valueOf(val);
                }
                catch(IllegalArgumentException ex)
                {
                    ermsg += "Unknown Window (" + val + ")\n";
                }
            }
            else
            {
                window = WindowGen.Window.HAMMING;
            }
            if (line.hasOption("color"))
            {
                val = line.getOptionValue("color").toLowerCase();
                if (val.equals("hot") || val.equals("jet") || val.equals("bw"))
                {
                    color = val;
                }
                else
                {
                    ermsg += "Unknown color scheme (" + val + ")\n";
                }
            }

            if (line.hasOption("testdata"))
            {
                useTestData = true;
                testDataFile = line.getOptionValue("testdata");
            }
            
            // filter parameters
            if (line.hasOption("filt"))
            {
                filtType = line.getOptionValue("filt");
                if (!filtType.equalsIgnoreCase("high") && !filtType.equalsIgnoreCase("low"))
                {
                    ermsg += "Unknown filter type [" + filtType + "]";
                }
                else
                {
                    if (line.hasOption("cutoff"))
                    {
                        val = line.getOptionValue("cutoff");
                        if (val.matches(fltpat))
                        {
                            up = Float.parseFloat(val);
                        }
                        else
                        {
                            ermsg += "can't parse cutoff value (" + val + ")\n";
                            wantHelp = true;
                            ret = false;
                        }
                    }
                    else
                    {
                        ermsg += "filter specified but no cutoff value\n";
                    }
                    if (line.hasOption("order"))
                    {
                        val = line.getOptionValue("order");
                        if (val.matches(intpat))
                        {
                            order = Integer.parseInt(val);
                        }
                        else
                        {
                            ermsg += "can't parse order value (" + val + ")";
                            wantHelp = true;
                            ret = false;
                        }
                    }
                    else
                    {
                        order = 4;
                    }
                }
            }
        }
        if (!ermsg.isEmpty())
        {
            System.out.println("Command error:\n" + ermsg);
            wantHelp = true;
            ret = false;
        }
        if (wantHelp )
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName, options);
            ret = false;
        }
        return ret;
    }

    //=============  Getters  ===========
    public boolean isShowProgressBar()
    {
        return showProgressBar;
    }

    public boolean isUseTestData()
    {
        return useTestData;
    }

    public boolean isLogFreq()
    {
        return logFreq;
    }

    public boolean isLogIntensity()
    {
        return logIntensity;
    }

    public int getOutX()
    {
        return outX == null ? 0 : outX;
    }

    public int getOutY()
    {
        return outY == null ? 0 : outY;
    }

    public int getStartGPS()
    {
        return startGPS == null ? 0 : startGPS;
    }

    public int getDuration()
    {
        return duration == null ? 0 : duration;
    }

    public int getxTicks()
    {
        return xTicks == null ? 0 : xTicks;
    }

    public int getyTicks()
    {
        return yTicks == null ? 0 : yTicks;
    }

    public float getSecPerFFT()
    {
        return secPerFFT == null || secPerFFT <= 0 ? 1 : secPerFFT;
    }

    public String getOfileName()
    {
        return ofileName == null ? "" : ofileName;
    }

    public String getChannelName()
    {
        return channelName == null ? "" : channelName;
    }

    public String getServer()
    {
        return server == null ? "" : server;
    }

    public Float getSampleRate()
    {
        return sampleRate;
    }

    public boolean isNorm()
    {
        return norm;
    }

    public SpectrumCalc.Scaling getScaling()
    {
        return scaling;
    }

    public String getColor()
    {
        return color == null ? "" : color;
    }

    public Float getOverlap()
    {
        return overlap  == null ? -1 : overlap;
    }

    public Float getFmin()
    {
        return fmin;
    }

    public Float getFmax()
    {
        return fmax;
    }

    public boolean isDetrend()
    {
        return detrend;
    }

    public Window getWindow()
    {
        return window;
    }

    public String getTestDataFile()
    {
        return testDataFile;
    }

    public boolean isSmooth()
    {
        return smooth;
    }

    public Float getUp()
    {
        up = up == null ? 1.0f : up;
        return up;
    }

    public Float getLo()
    {
        lo = lo == null ? 0 : lo;
        return lo;
    }

    public boolean isInterp()
    {
        return interp;
    }

    public String getFiltType()
    {
        String ret = filtType == null ? "" : filtType;
        
        return ret;
    }

    public float getCutoff()
    {
        cutoff = cutoff == null ? 0 : cutoff;
        return cutoff;
    }

    public int getOrder()
    {
        order = order == null ? 0 : order;
        return order;
    }

    public String getcType()
    {
        return cType;
    }

    
    /**
     * check if a parameter was specified,convert it.  Sets fields erMsg, wantsHelp and ret
     * if a bad value was entered
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
                ermsg += "can't parse " + param +  " value (" + val + ")\n";
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
}
