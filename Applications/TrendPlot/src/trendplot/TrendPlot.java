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

package trendplot;

import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.viewerplugin.ExternalProgramManager;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * Manage long term trend plots from frames
 * Most of the heavy lifting is done by external programs
 * This application provides an easy interface
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TrendPlot
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        TrendPlot me = new TrendPlot();
        me.doit(args);
    }
    private File datDir = null;
    private boolean hasData;
    private String timeUnitName;
    private double tMax;
    private boolean verbose;    
    
    private ArrayList<String> chanNames;
    private String server;
    private boolean useRaw = false;
    private long startGps;
    private int duration;
    private ArrayList<String> flist;
    private String timeUnit;
    private final String version = "0.0.2";
    private final String programName = "TrendPlot";

    private void doit(String[] args)
    {
        if (processArgs(args))
        {
            try
            {
                if (!hasData)
                {
                    getFileList();
                    transferData();
                }
                plotData();
            }
            catch (WebUtilException | IOException ex)
            {
                Logger.getLogger(TrendPlot.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    private boolean processArgs(String[] args)
    {
        
        boolean ret = true;

        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("verbose", "print lots of progress messages"));

        options.addOption(OptionBuilder.withArgName("out").hasArg().withDescription("output filename").create("outfile"));
        options.addOption(OptionBuilder.withArgName("geometry").hasArg().withDescription("image size <X>x<Y> [default=1280x768]").create("geom"));

        options.addOption(OptionBuilder.withArgName("chan").hasArgs().withDescription("channel name multiple allowed").create("chan"));
        options.addOption(OptionBuilder.withArgName("start").hasArg().withDescription("start time").create("start"));
        options.addOption(OptionBuilder.withArgName("end").hasArg().withDescription("end time (or use duration)").create("end"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("LDR server default=use system default").create("server"));
        options.addOption(OptionBuilder.withArgName("duration").hasArg().withDescription("duration (or use end time)").create("duration"));
        options.addOption(OptionBuilder.withArgName("datdir").hasArg().withDescription("use all prefetched data in specified directory").create("datdir"));
        

        CommandLineParser parser = new GnuParser();

        boolean wantHelp = true;
        CommandLine line;
        try
        {
            // parse the command line arguments
            line = parser.parse(options, args);
        }
        catch (ParseException exp)
        {
            // oops, something went wrong
            System.err.println("Command parsing failed.  Reason: " + exp.getMessage());
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
            verbose = line.hasOption("verbose");
            String[] chans = line.getOptionValues("chan");
            chanNames = new ArrayList<>();
            hasData = false;
            if (line.hasOption("datdir"))
            {
                String datDirPath = line.getOptionValue("datdir");
                
                datDir = new File(datDirPath);
                if (!datDir.exists() || ! datDir.isDirectory())
                {
                    System.err.format("Data directory [%1$s] does not exist.%n", datDirPath);
                    wantHelp = true;
                }
                else
                {
                    Pattern datFilePat = Pattern.compile("^([A-Z]+[0-9]:.+)\\.dat$");
                    String[] dirList = datDir.list();
                    if (dirList != null)
                    {
                        for (String f : dirList)
                        {
                            Matcher m = datFilePat.matcher(f);
                            if (m.find())
                            {
                                chanNames.add(m.group(1));
                            }
                        }
                    }
                    if (chanNames.size() > 0)
                    {
                        hasData = true;
                    }
                }
            }
            if (chans == null || chans.length == 0)
            {
                if (!hasData)
                {
                    System.err.println("No channels specified. Nothing to do.");
                    wantHelp=true;
                }
            }
            else
            {
                chanNames.clear();
                chanNames.addAll(Arrays.asList(chans));
            }
            
            String it = line.getOptionValue("start");
            if (it == null || it.isEmpty())
            {
                System.err.println("Start value not specified.");
                wantHelp=true;
            }
            else
            {
                startGps = TimeAndDate.getGPS(it);
            }
            
            it = line.getOptionValue("duration");
            if (it == null || it.isEmpty())
            {
                System.err.println("Duration not specified.");
                wantHelp = true;
            }
            else
            {
                duration = (int) TimeAndDate.getDuration(it);
            }
            
            it = line.getOptionValue("server");
            if (it == null || it.isEmpty())
            {
                server = "ligo.caltech.edu";
            }
            else
            {
                server = it;
            }
            
        }
        if (wantHelp)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName + " ver: " + version, options);
            ret = false;
        }
        if (verbose)
        {
            System.out.println();
        }
        return ret;
    }

    private void getFileList() throws WebUtilException
    {
        // ligo_data_find --server ldr.ligo.caltech.edu \
        //     -o H -t H1_M -s 1067299216 -e 1067299217 --url-type file
        StringBuilder cmd = new StringBuilder();
        cmd.append("ligo_data_find ");
        if (! server.isEmpty())
        {
            cmd.append("--server ldr.");
            cmd.append(server);
        }
        if (verbose)
        {
            cmd.append(" --verbose ");
        }
        String ftype = useRaw ? "R" : "M";
        String observatory = "";
        for(String ch : chanNames)
        {
            String o = ch.substring(0, 1);
            if (observatory.isEmpty() || observatory.equals(o))
            {
                observatory = o;
            }
            else
            {
                System.err.println("Sorry we can't mix channels form different observatories, yet.");
                throw new WebUtilException("Channels are from more than one observaoty");
            }
        }
        
        
        cmd.append(String.format(" -o %1$s -t %1$s1_M ",observatory));
        cmd.append("-s ").append(Long.toString(startGps));
        cmd.append(" -e ");
        cmd.append(Long.toString(startGps + duration));
        cmd.append(" --url-type file");
        flist = new ArrayList<>();
        
        if (verbose)
        {
            System.out.println(cmd);
            System.out.println();
        }
        ExternalProgramManager epm = new ExternalProgramManager();
        if (epm.runExternalProgram(cmd.toString()))
        {
            Pattern pat = Pattern.compile("file://localhost(.*gwf)");
            
            String output=epm.getStdout();
            String[] files = output.split("\n");
            for(String f : files)
            {
                Matcher m = pat.matcher(f);
                if (m.find())
                {
                    String path = m.group(1);
                    flist.add(path);
                }
            }
            if (flist.isEmpty())
            {
                throw new WebUtilException("No frames were found for this request.");
            }
        }
        else
        {
            System.err.println("Error running ligo_data_find.\nStderr:");
            System.err.println(epm.getStderr());

            System.err.println("\nStdout:");
            System.err.println(epm.getStdout());
            
            throw new WebUtilException("Error running ligo_data_find");
        }
    }

    private void transferData() throws IOException, WebUtilException
    {
        datDir = createTempDirectory();
        File fList = mkTempInputFile("frames", flist, datDir);
        File cList = mkTempInputFile("channels", chanNames, datDir);
        
        String cmd = String.format("/home/areeda/bin/framereadertest --frame %1$s --chan %2$s --outdir %3$s",
                                   fList.getAbsolutePath(), cList.getAbsolutePath(), datDir.getAbsolutePath());
        
        if (duration < 1000)
        {
            timeUnit="s";
            timeUnitName = "Seconds";
            tMax = duration;
        }
        else if (duration < 7200)
        {
            timeUnit="m";
            timeUnitName = "Minutes";
            tMax = duration/60.;
        }
        else if (duration < 48 * 3600)
        {
            timeUnit="h";
            timeUnitName = "hours";
            tMax = duration / 3600.;
        }
        else if (duration < 40 * 7 * 24 * 3600)
        {
            timeUnit = "d";
            timeUnitName = "days";
            tMax = duration / (24. * 3600.);
        }
        else
        {
            timeUnit = "w";
            timeUnitName = "Weeks";
            tMax = duration / (7. * 24. * 3600.);
        }
        cmd += " --unit " + timeUnit;
        if (verbose)
        {
            System.out.println(cmd);
        }
        ExternalProgramManager epm = new ExternalProgramManager();
        if (!epm.runExternalProgram(cmd))
        {
            System.err.println("Error running framereadertest.");
            System.err.println(epm.getStderr());
            throw new WebUtilException("Error running framereadertest");
        }
    }
    public File mkTempInputFile(String name, ArrayList<String> vals, File dir) throws IOException
    {
        File out = File.createTempFile(name, ".lst", dir);
        try (FileWriter fw = new FileWriter(out))
        {
            for (String it : vals)
            {
                fw.append(it).append("\n");
            }
        }
        return out;
    }
    public File createTempDirectory() throws IOException
    {
        final File temp;

        temp = File.createTempFile("trendPlot", "_dir", new File("/tmp"));

        if (!(temp.delete()))
        {
            throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
        }

        if (!(temp.mkdir()))
        {
            throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
        }

        return (temp);
    }

    private void plotData() throws IOException, WebUtilException
    {
        
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        
        String strtDate = TimeAndDate.gpsAsUtcString(startGps);
        String endDate = TimeAndDate.gpsAsUtcString(startGps + duration);

        for(String cName : chanNames)
        {
            String datFile = datDir.getAbsolutePath() + "/" + cName + ".dat";            
            File outFile = File.createTempFile(cName, ".png", datDir);

            StringBuilder gnuCmds = new StringBuilder();
            String[] gnuEnv =
            {
                "GDFONTPATH=/usr/share/fonts/liberation/:/usr/share/fonts/dejavu/"
            };
            gnuCmds.append("set macros\n\n");
            gnuCmds.append("set terminal png size 1600,800 enhanced font 'LiberationSerif-Bold, 14'\n");
            gnuCmds.append("set output '").append(outFile.getAbsolutePath()).append("'\n");
            
            gnuCmds.append("set title ");
            String cNameEscaped = cName.replaceAll("_", "\\\\_");
            gnuCmds.append("'Trend of ").append(cNameEscaped).append(" from ");
            gnuCmds.append(strtDate).append(" to ").append(endDate).append("' font ',24'").append("\n");
            
            gnuCmds.append("set xlabel '").append(timeUnitName).append(" since ");
            gnuCmds.append(strtDate).append("' font ',18'\n");
            
            int xtics = (int) Math.ceil(tMax/40);
            gnuCmds.append(String.format("set xtics %d%n", xtics));
            
            gnuCmds.append("set ylabel 'Counts' font ',18'\n");
            
            gnuCmds.append("set key inside bottom horizontal\n");
            gnuCmds.append("set grid xtics ytics\n");
            gnuCmds.append("filespec =\" binary filetype=raw format='%float%float%float%float'\"\n");
            gnuCmds.append("styleMin=\"using 1:2 lt 1  lw 1 with lines title 'Min'\"\n");
            gnuCmds.append("styleMean=\"using 1:3 lt -1 lw 2 with lines title 'Mean'\"\n");
            gnuCmds.append("styleMax=\"using 1:4 lt 3 lw 1 with lines title 'Max'\"\n");
            gnuCmds.append("file=\"'").append(datFile).append("'\"\n");
            gnuCmds.append("plot @file @filespec @styleMin, ");
            gnuCmds.append("@file @filespec @styleMean, @file @filespec @styleMax\n");

            System.out.println();
            System.out.println(gnuCmds.toString());
            
            // save the gnuplot commands
            File gnuFile = File.createTempFile(cName, ".gnuPlot", datDir);
            FileWriter gnuWriter = new FileWriter(gnuFile);
            gnuWriter.append(gnuCmds);
            gnuWriter.append("\n");
            gnuWriter.close();;
            
            ExternalProgramManager epm = new ExternalProgramManager();
            epm.addEnv(gnuEnv);
            String gnuplot = "gnuplot";
            if (System.getProperty("os.name").contains("Mac OS"))
            {
                gnuplot = "/opt/local/bin/gnuplot";
            }
            if (!epm.runExternalProgram(gnuplot, gnuCmds.toString()))
            {
                System.err.println("Error running gnuplot.");                
                System.err.println(epm.getStderr());
            }
            
        }
    }
}
