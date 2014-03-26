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

import addimg2db.Addimg2db;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageItemTextLink;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.viewerplugin.ExternalProgramManager;
import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

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
        int ret;
        TrendPlot me = new TrendPlot();
        ret = me.doAll(args);
        System.exit(ret);
    }
    private File datDir = null;
    private boolean hasData;
    private String timeUnitName;
    
    private Float tRange = null;
    private boolean verbose;    
    
    private TreeSet<String> chanNames;
    private String server;
    private boolean useRaw = false;
    private long startGps;
    private int duration;
    private ArrayList<String> flist;
    private String timeUnit;
    private String email = "";
    private Float min, max;             // input data min,max
    private Float minMax, maxMin;       // the other limit of the min & max values
    private String scale = "";
    
    public final String fpRegex = "^[-+]?[0-9]*\\.?[0-9]+$";
    
    private final String version = "0.0.2";
    private final String programName = "TrendPlot";
    private Float tmin = null;
    private Float tmax = null;
    
    private ArrayList<Float> minList;
    private ArrayList<Float> maxList;
    Float wmin, wmax;                   // user requested display min/max
    
    private String user;
    private Database db;
    private String group;
    private ArrayList<String> descriptions;
    private ArrayList<Integer> imgIds;
    private long startTime;
    private long transferStart;
    private long plotStart;
    private long emailStart;
    private boolean gotError;
    private boolean checkOnly;
    private String config;
    // send all output to String buffers so we can email them on error
    private StringWriter errStringWriter;
    private PrintWriter errPrintWriter;
    private StringWriter outStringWriter;
    private PrintWriter outPrintWriter;
    private long qtime;

    private int doAll(String[] args)
    {
        int ret;
        startTime = System.currentTimeMillis();
        // initialize the rest in case an error keeps them from running;
        transferStart = startTime;
        plotStart = startTime;
        emailStart = startTime;
        try
        {
            errStringWriter = new StringWriter();
            errPrintWriter = new PrintWriter(errStringWriter);
            
            outStringWriter = new StringWriter();
            outPrintWriter = new PrintWriter(outStringWriter);

            ret = doit(args);
        }
        catch (ViewConfigException | LdvTableException | SQLException ex)
        {
            errPrintWriter.format("An error occured making plot(s)\n %1$s - %2$s\n", 
                                        ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            ret = 2;
        }
        try
        {
            if (!checkOnly)
            {
                emailResults();
            }
        }
        catch (IOException | WebUtilException ex)
        {
            errPrintWriter.format("An error occured emailing results\n %1$s - %2$s\n",
                                  ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            ret = 3;
        }
        System.out.println(outStringWriter.toString());
        System.err.println(errStringWriter.toString());
        return ret;
    }
    private int doit(String[] args) throws ViewConfigException, LdvTableException, SQLException
    {
        
        descriptions = new ArrayList<>();
        imgIds = new ArrayList<>();
        int ret = 0;
        if (processArgs(args))
        {
            if (!checkOnly)
            {
                try
                {
                    if (!hasData)
                    {
                        getFileList();
                        transferData();
                    }

                    plotData();
                    
                    ret = 0;
                }
                catch (WebUtilException | IOException ex)
                {
                    Logger.getLogger(TrendPlot.class.getName()).log(Level.SEVERE, null, ex);
                    ret = 1;
                }
            }
        }
        else
        {
            ret = 1;
        }
        return ret;
    }

    private boolean processArgs(String[] args)
    {
        gotError = false;
        boolean ret = true;

        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("verbose", "print lots of progress messages"));
        options.addOption(new Option("check", "just check the arguments"));

        options.addOption(OptionBuilder.withArgName("out").hasArg().withDescription("output filename").create("outfile"));
        options.addOption(OptionBuilder.withArgName("geometry").hasArg().withDescription("image size <X>x<Y> [default=1280x768]").create("geom"));

        options.addOption(OptionBuilder.withArgName("chan").hasArgs().withDescription("channel name multiple allowed").create("chan"));
        options.addOption(OptionBuilder.withArgName("start").hasArg().withDescription("start time").create("start"));
        options.addOption(OptionBuilder.withArgName("end").hasArg().withDescription("end time (or use duration)").create("end"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("LDR server default=use system default").create("server"));
        options.addOption(OptionBuilder.withArgName("duration").hasArg().withDescription("duration (or use end time)").create("duration"));
        options.addOption(OptionBuilder.withArgName("datdir").hasArg().withDescription("use all prefetched data in specified directory").create("datdir"));
        
        options.addOption(OptionBuilder.withArgName("min").hasArg().withDescription("Y-axis minimum value").create("min"));
        options.addOption(OptionBuilder.withArgName("max").hasArg().withDescription("Y-axis maximum value").create("max"));
        options.addOption(OptionBuilder.withArgName("scale").hasArg().withDescription("min/max algorithm [abs, percent, percentile").create("scale"));

        options.addOption(OptionBuilder.withArgName("email").hasArg().withDescription("email ldvw link when finished to this address ").create("email"));
        options.addOption(OptionBuilder.withArgName("user").hasArg().withDescription("common name to use for database entry ").create("user"));
        options.addOption(OptionBuilder.withArgName("group").hasArg().withDescription("Image group ").create("group"));
        options.addOption(OptionBuilder.withArgName("config").hasArg().withDescription("Viewer config ").create("config"));
        options.addOption(OptionBuilder.withArgName("qtime").hasArg().withDescription("System time in ms of condor_submit ").create("qtime"));
        options.addOption(OptionBuilder.withArgName("geom").hasArg().withDescription("Plot dimensions XxY").create("geom"));
        
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
            errPrintWriter.println("Command parsing failed.  Reason: " + exp.getMessage());
            line = null;
            gotError = true;
        }

        if (line != null)
        {
            if (line.hasOption("version"))
            {
                outPrintWriter.println(programName + " - version " + version);
                ret = false;
            }
            wantHelp = line.hasOption("help");
            verbose = line.hasOption("verbose");
            checkOnly = line.hasOption("check");
            String[] chans = line.getOptionValues("chan");
            chanNames = new TreeSet<>();
            hasData = false;
            if (line.hasOption("datdir"))
            {
                String datDirPath = line.getOptionValue("datdir");
                
                datDir = new File(datDirPath);
                if (!datDir.exists() || ! datDir.isDirectory())
                {
                    errPrintWriter.format("Data directory [%1$s] does not exist.%n", datDirPath);
                    gotError = true;
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
                    if (!chanNames.isEmpty())
                    {
                        hasData = true;
                    }
                }
            }
            
            if (chans != null && chans.length > 0)
            {
                chanNames.addAll(Arrays.asList(chans));
            }
            if (chanNames.isEmpty())
            {
                errPrintWriter.println("No channels specified. Nothing to do.");
                gotError = true;
                
            }
            String it = line.getOptionValue("start");
            if ((it == null || it.isEmpty()) && datDir == null)
            {
                errPrintWriter.println("Start value not specified.");
                gotError=true;
            }
            else
            {
                startGps = TimeAndDate.getGPS(it);
            }
            duration = 0;
            it = line.getOptionValue("duration");
            if ((it == null || it.isEmpty()) && datDir == null && ! line.hasOption("end"))
            {
                errPrintWriter.println("Duration or end must be specified.");
                gotError = true;
            }
            else
            {
                duration = (int) TimeAndDate.getDuration(it);
            }
            if (duration == 0)
            {
                it = line.getOptionValue("end");
                if ((it == null || it.isEmpty()) && datDir == null)
                {
                    errPrintWriter.println("Duration or end must be specified.");
                    gotError = true;
                }
                else if (it != null && !it.isEmpty())
                {
                    if (it.matches("^\\d+"))
                    {
                        long endGps = Integer.parseInt(it);
                        duration = (int) (endGps - startGps);
                    }
                    else
                    {
                        errPrintWriter.format("Invalid end time (%1$s).", it);
                        gotError = true;
                    }
                }
            }
            it = line.getOptionValue("server");
            if (it == null || it.isEmpty())
            {
                server = "";
            }
            else
            {
                server = it;
            }
            it = line.getOptionValue("email");
            if (it != null && ! it.isEmpty() )
            {
                email = it;
            }
            it = line.getOptionValue("group");
            group = "";
            if (it != null && !it.isEmpty())
            {
                group = it;
            }
            it = line.getOptionValue("user");
            if (it != null && !it.isEmpty())
            {
                user = it;
            }            it = line.getOptionValue("scale");
            if (it != null && ! it.isEmpty())
            {
                scale = it;
            }
            it = line.getOptionValue("min");
            if (it != null && ! it.isEmpty())
            {
                if (it.matches(fpRegex))
                {
                    wmin = Float.parseFloat(it);
                }
                else
                {
                    gotError = true;
                    outPrintWriter.format("Min argument is not a valid floating point number (%1$s)%n", it);
                }
            }
            it = line.getOptionValue("max");
            if (it != null && !it.isEmpty())
            {
                if (it.matches(fpRegex))
                {
                    wmax = Float.parseFloat(it);
                }
                else
                {
                    gotError = true;
                    errPrintWriter.format("Max argument is not a valid floating point number (%1$s)%n", it);
                }
            }
            it = line.getOptionValue("qtime");
            qtime=0;
            if (it != null && !it.isEmpty())
            {
                it=it.trim();
                if (it.matches("^\\d+"))
                {
                    qtime=Long.parseLong(it);
                }
                else
                {
                    gotError = true;
                    errPrintWriter.format("qtime is not a valid long(%1$s)", it);
                }
            }
            it = line.getOptionValue("config");
            config = "";
            if (it != null && !it.isEmpty())
            {
                config = it;
            }
        }
        if (wantHelp || gotError)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName + " ver: " + version, options);
            ret = false;
        }
        if (verbose)
        {
            outPrintWriter.println();
        }
        return ret;
    }

    private void getFileList() throws WebUtilException
    {
        // gw_data_find --server ldr.ligo.caltech.edu \
        //     -o H -t H1_M -s 1067299216 -e 1067299217 --url-type file
        StringBuilder cmd = new StringBuilder();
        cmd.append("gw_data_find ");
        if (! server.isEmpty())
        {
            cmd.append("--server ldr.");
            cmd.append(server);
            cmd.append(":80");
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
                errPrintWriter.println("Sorry we can't mix channels form different observatories, yet.");
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
            outPrintWriter.println("Data find command:");
            outPrintWriter.println(cmd);
            outPrintWriter.println();
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
            errPrintWriter.println("Error running ligo_data_find.\nStderr:");
            errPrintWriter.println(epm.getStderr());

            errPrintWriter.println("\nStdout:");
            errPrintWriter.println(epm.getStdout());
            
            throw new WebUtilException("Error running ligo_data_find");
        }
    }

    private void transferData() throws IOException, WebUtilException
    {
        transferStart=System.currentTimeMillis();
        datDir = createTempDirectory();
        File fList = mkTempInputFile("frames", flist, datDir);
        File cList = mkTempInputFile("channels", chanNames, datDir);
        
        String cmd = String.format("/home/areeda/bin/framereadertest --frame %1$s --chan %2$s --outdir %3$s",
                                   fList.getAbsolutePath(), cList.getAbsolutePath(), datDir.getAbsolutePath());
        
        if (duration < 1000)
        {
            timeUnit="s";
            timeUnitName = "Seconds";
            tRange = duration/1.f;
        }
        else if (duration < 7200)
        {
            timeUnit="m";
            timeUnitName = "Minutes";
            tRange = duration/60.f;
        }
        else if (duration < 48 * 3600)
        {
            timeUnit="h";
            timeUnitName = "hours";
            tRange = duration / 3600.f;
        }
        else if (duration < 40 * 7 * 24 * 3600)
        {
            timeUnit = "d";
            timeUnitName = "days";
            tRange = duration / (24.f * 3600.f);
        }
        else
        {
            timeUnit = "w";
            timeUnitName = "Weeks";
            tRange = duration / (7.f * 24.f * 3600.f);
        }
        cmd += " --unit " + timeUnit;
        if (verbose)
        {
            outPrintWriter.println("Data transfer command:");
            outPrintWriter.println(cmd);
        }
        ExternalProgramManager epm = new ExternalProgramManager();
        if (!epm.runExternalProgram(cmd))
        {
            errPrintWriter.println("Error running framereadertest.");
            errPrintWriter.println(epm.getStderr());
            throw new WebUtilException("Error running framereadertest");
        }
    }
    public File mkTempInputFile(String name, Collection<String> vals, File dir) throws IOException
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

    private void plotData() throws IOException, WebUtilException, ViewConfigException, LdvTableException, SQLException
    { 
        plotStart=System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        
        String strtDate = TimeAndDate.gpsAsUtcString(startGps);
        String endDate = TimeAndDate.gpsAsUtcString(startGps + duration);

        for(String cName : chanNames)
        {
            String datFile = datDir.getAbsolutePath() + "/" + cName + ".dat"; 
            File dfile = new File(datFile);
            if (dfile.length() < 100)
            {
                errPrintWriter.format("%1$s does not have enough data to plot.\n", cName);
                continue;
            }
            scaleData(datFile);
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
            
            timeUnitName = timeUnitName == null ? "" : timeUnitName;
            gnuCmds.append("set xlabel '").append(timeUnitName).append(" since ");
            gnuCmds.append(strtDate).append("' font ',18'\n");
            
            int xtics = (int) Math.ceil(tRange/40);
            gnuCmds.append(String.format("set xtics %d%n", xtics));
            
            gnuCmds.append("set ylabel 'Counts' font ',18'\n");
            
            gnuCmds.append("set key inside bottom horizontal\n");
            gnuCmds.append("set grid xtics ytics\n");
            gnuCmds.append("filespec =\" binary filetype=raw format='%float%float%float%float'\"\n");
            gnuCmds.append("styleMin=\"using 1:2 lt 1  lw 1 with lines title 'Min'\"\n");
            gnuCmds.append("styleMean=\"using 1:3 lt -1 lw 2 with lines title 'Mean'\"\n");
            gnuCmds.append("styleMax=\"using 1:4 lt 3 lw 1 with lines title 'Max'\"\n");
            if (tmin != null && tmax != null)
            {
                gnuCmds.append(String.format("set xrange [ %1$e:%2$e]\n", tmin,tmax));
            }
            if (min != null && max != null)
            {
                gnuCmds.append(String.format("set yrange [ %1$e:%2$e]\n", min,max));
            }
            gnuCmds.append("file=\"'").append(datFile).append("'\"\n");
            gnuCmds.append("plot @file @filespec @styleMin, ");
            gnuCmds.append("@file @filespec @styleMean, @file @filespec @styleMax\n");

            if (verbose)
            {
                outPrintWriter.println();
                outPrintWriter.println(gnuCmds.toString());
            }
            // save the gnuplot commands
            File gnuFile = File.createTempFile(cName, ".gnuPlot", datDir);
            
            try (FileWriter gnuWriter = new FileWriter(gnuFile))
            {
                gnuWriter.append(gnuCmds);
                gnuWriter.append("\n");
            }
            
            ExternalProgramManager epm = new ExternalProgramManager();
            epm.addEnv(gnuEnv);
            String gnuplot = "gnuplot";
            if (System.getProperty("os.name").contains("Mac OS"))
            {
                gnuplot = "/opt/local/bin/gnuplot";
            }
            if (!epm.runExternalProgram(gnuplot, gnuCmds.toString()))
            {
                errPrintWriter.println("Error running gnuplot.");                
                errPrintWriter.println(epm.getStderr());
            }
            else
            {
                imgIds = add2db(outFile.getAbsolutePath(), cName, strtDate, endDate);
            }
            
        }
    }
    private ArrayList<Integer> add2db(String fileName, String chanName, String strtDate, String endDate) 
            throws ViewConfigException, LdvTableException, SQLException
    {
        if (group.isEmpty())
        {
            group = "Trend plots";
        }
        Addimg2db adder = new Addimg2db();
        if ( config != null)
        {
            adder.setConfigFileName(config);
        }
        adder.setAuto(false);
        adder.setGroup(group);
        adder.setUser(user);
        String desc = String.format("Trend plot of %1$s from %2$s to %3$s", chanName, strtDate, endDate);
        adder.setDescription(desc);
        String[] files = new String[1];
        files[0] = fileName;
        adder.setFiles(files);
        ArrayList<Integer> imgIdlst = adder.doit();
        if (!imgIdlst.isEmpty())
        {
            for(Integer imgId : imgIdlst)
            {
                descriptions.add(desc);
                imgIds.add(imgId);
            }
        }
        return imgIdlst;
    }

   

    private void scaleData(String datFile) throws FileNotFoundException, IOException
    {
        findMinMax(datFile);
        switch(scale)
        {
            case "percent":
                float range=max-min;
                min = range * wmin/100.f + min;
                max = range * wmax/100.f + min;
                break;
            case "percentile":
                makeHistogram(datFile);
                if (minList.size() > 0 && maxList.size() > 0)
                {
                    int minIdx = Math.round(minList.size() * wmin/100.f);
                    minIdx = minIdx < 0 ? 0 : minIdx;
                    minIdx = minIdx >= minList.size() ? minList.size() -1 : minIdx;

                    int maxIdx = Math.round(maxList.size() * wmax / 100.f);
                    maxIdx = maxIdx < 0 ? 0 : maxIdx;
                    maxIdx = maxIdx >= maxList.size() ? maxList.size() -1 : maxIdx;
                    min = minList.get(minIdx);
                    max = maxList.get(maxIdx);
                }
                break;
                
            case "abs":
                min = wmin;
                max = wmax;
                break;

            default:
                break;
        }
        tRange = tmax - tmin;
    }

    private void findMinMax(String datFileName) throws FileNotFoundException, IOException
    {
        File datFile = new File(datFileName);
        try (BufferedInputStream ins = new BufferedInputStream(new FileInputStream(datFile)))
        {
            boolean eof = false;
            min = Float.MAX_VALUE;
            minMax = Float.MIN_VALUE;
            max = Float.MIN_VALUE;
            maxMin = Float.MAX_VALUE;
            tmin = Float.MAX_VALUE;
            tmax = Float.MIN_VALUE;
            byte[] b = new byte[Float.SIZE/8];
            int count = 0;
            int nread;
            while (!eof)
            {
                try
                {
                    nread = ins.read(b);
                    float time  = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float inmin = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float mean  = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float inmax = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    if (nread == Float.SIZE/8 * 4)
                    {
                        count++;
                        tmin = Math.min(tmin, time);
                        tmax = Math.max(tmax, time);
                        min = Math.min(min, inmin);
                        minMax = Math.max(minMax, inmin);
                        max = Math.max(max, inmax);
                        maxMin = Math.min(maxMin, inmax);
                    }
                    else
                    {
                        eof = true;
                        ins.close();
                    }
                }
                catch (EOFException ex)
                {
                    eof = true;
                    ins.close();
                }
                catch (IOException ex)
                {
                    ins.close();
                    throw ex;
                }
            }
        }
    }

    private void makeHistogram(String datFileName) throws FileNotFoundException, IOException
    {
        File datFile = new File(datFileName);
        try (BufferedInputStream ins = new BufferedInputStream(new FileInputStream(datFileName)))
        {
            boolean eof = false;
            minList = new ArrayList<>();
            maxList = new ArrayList<>();
            
            byte[] b = new byte[Float.SIZE / 8];
            int nread;
            while (!eof)
            {
                try
                {
                    nread = ins.read(b);
                    float time = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float inmin = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float mean = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    nread += ins.read(b);
                    float inmax = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    if (nread == Float.SIZE / 8 * 4)
                    {
                        minList.add(inmin);
                        maxList.add(inmax);
                    }
                    else
                    {
                        eof = true;
                        ins.close();
                    }
                }
                catch (EOFException ex)
                {
                    eof = true;
                    ins.close();
                }
                catch (IOException ex)
                {
                    ins.close();
                    throw ex;
                }
            }
        }
        Collections.sort(minList);
        Collections.sort(maxList);
    }

    private void emailResults() throws IOException, WebUtilException
    {
        emailStart=System.currentTimeMillis();
        
        ViewerConfig vc = new ViewerConfig();
        if (!config.isEmpty())
        {
            vc.setConfigFileName(config);
        }
        vc.readConfig();
        String host = vc.get("webserver");
        if (host == null || host.isEmpty())
        {
            host = "ldvw.ligo.caltech.edu";
        }
        String servlet = vc.get("servlet");
        if (servlet == null || servlet.isEmpty())
        {
            servlet = "ldvw/view";
        }
        PageItemList msg = new PageItemList();
        
        if (! imgIds.isEmpty())
        {
            msg.add(new PageItemHeader("The following results are available:", 3));
            PageTable tbl = new PageTable();
            for(int img = 0; img < imgIds.size(); img++)
            {
                PageTableRow row = new PageTableRow();
                row.add(descriptions.get(img));
                Integer imgId = imgIds.get(img);
                String url = "https://" + host + "/" + servlet + "?act=getimg&amp;imgId=" + imgId.toString();
                PageItemTextLink link = new PageItemTextLink(url, "link");
                row.add(link);
                tbl.addRow(row);
            }
            msg.add(tbl);
            msg.addBlankLines(2);

            String groupIntro = String.format("These images have also been added to the %1$s group.<br>",
                                              group);
            msg.add(new PageItemString(groupIntro,false));
            String groupUrl = String.format("https://%1$s/%2$s?act=imagehistory&amp;group=%3$s"
                    + "&amp;usrSel=%4$s", host,servlet,group,user);
            PageItemTextLink groupLink = new PageItemTextLink(groupUrl, "Click for image group page.");
            msg.add(groupLink);
            msg.addBlankLines(2);
        }
        else if (!checkOnly)
        {
            msg.add("Sorry but no images were produced.");
            msg.addBlankLines(2);
        }
        if (verbose || imgIds.isEmpty())
        {
            msg.add(new PageItemString("<br>Stdout:<br><br>", false));
            String outText = outStringWriter.toString().replace("\n", "<br>\n");
            msg.add(new PageItemString(outText,false));
            
            msg.add(new PageItemString("<br>Stderr:<br><br>", false));
            outText = errStringWriter.toString().replace("\n", "<br>\n");
            msg.add(new PageItemString(outText,false));
        }
        msg.add(new PageItemString("<br><br>Sincerly,<br>The LigoDV-web group",false));
        
        Float condor = (startTime - qtime) / 1000.f;
        String condorTime = qtime == 0 ? "?" : String.format("%1$.2f", condor);
        Float overhead = (transferStart - startTime) / 1000.f;
        Float xfer=(plotStart - transferStart)/1000.f;
        Float plot=(emailStart - plotStart) /1000.f;
        String timing = String.format("Timing: queue: %1$s, overhead: %2$.2f, data collection: %3$.2f, "
                                      + "plot: %4$.2f seconds", condorTime, overhead, xfer,plot);
        outPrintWriter.println(timing);
        msg.addBlankLines(2);
        msg.add(new PageItemString(timing,false));
        msg.addBlankLines(1);
        
        String msgText = msg.getHtml();
        
        Properties fMailServerConfig;
        fMailServerConfig = new Properties();
        fMailServerConfig.setProperty("mail.host", "ldas-cit.ligo.caltech.edu");
        fMailServerConfig.setProperty("mail.smtp.host", "ldas-cit.ligo.caltech.edu");
        
        Session session = Session.getDefaultInstance(fMailServerConfig, null);
        MimeMessage message = new MimeMessage(session);
        try
        {
            //the "from" address may be set in code, or set in the
            //config file under "mail.from" ; here, the latter style is used
            message.setFrom(new InternetAddress("areeda@ligo.caltech.edu"));

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            
            message.setSubject("Trend plot results are available.");
            message.setText(msgText, "utf-8", "html");
            Transport.send(message);
            outPrintWriter.format("Email sent to %1$s\n", email);
            errPrintWriter.println("Email message:");
            errPrintWriter.println(msgText);
        }
        catch (MessagingException ex)
        {
            throw new WebUtilException("Cannot send email. " + ex);
        }
    }

    private void testEmail()
    {
        Properties fMailServerConfig;
        fMailServerConfig = new Properties();
        fMailServerConfig.setProperty("mail.host", "ldas-cit.ligo.caltech.edu");
        fMailServerConfig.setProperty("mail.smtp.host", "ldas-cit.ligo.caltech.edu");

        Session session = Session.getDefaultInstance(fMailServerConfig, null);
        MimeMessage message = new MimeMessage(session);
        try
        {
            //the "from" address may be set in code, or set in the
            //config file under "mail.from" ; here, the latter style is used
            message.setFrom(new InternetAddress("areeda@ligo.caltech.edu"));
            String email = "joe@areeda.com";

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            String msg = "<h3>this is a test</h3>";
            message.setSubject("Test email");
            message.setText(msg, "utf-8", "html");
            Transport.send(message);
            System.out.format("Email sent to %1$s\n", email);
        }
        catch (MessagingException ex)
        {
            System.err.format("Exception: %1$s: %2$s", ex.getClass().getSimpleName(), ex.getLocalizedMessage());
        }

    }
}
