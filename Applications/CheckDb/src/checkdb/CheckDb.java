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
package checkdb;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.ChanPointer;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * Perform basic checks on the Channel tables and build the base channel tables
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CheckDb
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            CheckDb me = new CheckDb();
            me.doAll(args);
        }
        catch (WebUtilException | SQLException | ClassNotFoundException | ViewConfigException ex)
        {
            Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private int verbose = 2;
    private String configFile="";
    private final long startTime;
    private long lastTime;
    private ChannelTable chnTbl;
    private Database db;
    private TreeMap<String,TreeMap<String, ChanStat>> chanstats;
    private Set<String> ifoSubsysSet;
    private int count;
    private final boolean showMultiRates = verbose > 2;
    private BufferedWriter out=null;
    private ChannelIndex cidx;
    private ChanPointerTable cpt;
    private String programName = "CheckDb";
    private String version = "0.1.0";
    
    private void doAll(String[] args) throws WebUtilException
    {
        try
        {
            if (processArgs(args))
            {
                setup();
                chnTbl = new ChannelTable(db);
                ifoSubsysSet = chnTbl.buildIfoSubsysSet();
                
                if (verbose > 2)
                {
                    System.out.println("List of ifo:subsystem");
                    for(String ifoSubsys : ifoSubsysSet)
                    {
                        System.out.format("%1$s%n", ifoSubsys);
                    }
                }
                    chanstats = new TreeMap<>();

                cidx = new ChannelIndex(db);
                cidx.recreate();

                cpt = new ChanPointerTable(db);
                cpt.recreate();

                chnTbl = new ChannelTable(db);
                
                long chanCount = chnTbl.getRecordCount();
                System.out.format("There are %1$,d channels in Channels table%n", chanCount);
                
                out = new BufferedWriter(new FileWriter("/tmp/chanNames.txt"));
                int nifoSubsys = ifoSubsysSet.size();
                int cur = 0;
                for(String ifoSubsys : ifoSubsysSet)
                {
                    ifoSubsys = ifoSubsys.replaceAll("_", "\\\\_");
                    cur++;
                    buildChanStats(ifoSubsys);
                    doReport(ifoSubsys);
                    saveNames();
                    try
                    {
                        makeChannelIndexTable();
                        makeChanPointerTable(ifoSubsys);
                        String msg = String.format("Processed: %1$d of %2$d, %3$s", cur, nifoSubsys, ifoSubsys);
                        logTime(msg, 2);
                    }
                    catch (SQLException ex)
                    {
                        Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        catch (ClassNotFoundException | ViewConfigException | SQLException | IOException ex)
        {
            Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    /**
     * Build a map of all channels that are derived from each base name for a single IFO:Subsystem
     * @param ifoSubsys String of the form IFO:Subsystem[-_] 
     */
    private void buildChanStats(String ifoSubsys)
    {
        try
        {
            chanstats.clear();
            
            
            chnTbl.streamByName(ifoSubsys + "%");
            ChanInfo ci;
            count=0;
            while ((ci = chnTbl.streamNext()) != null)
            {
                count++;
                if (verbose > 2 && count > 0 && count % 100000 == 0)
                {
                    System.out.format("\033[2K %,8d\r", count);
                    System.out.flush();
                }
                String name = ci.getChanName();
                String basename=ci.getBaseName();
                String serv = ci.getServer();
                serv = serv.replace(".caltech.edu", "");
                String key = basename;
                TreeMap<String,ChanStat> chanstatLst = chanstats.get(key);
                ChanStat chanstat;
                if (chanstatLst == null)
                {
                    chanstatLst = new TreeMap<>();
                    chanstat = new ChanStat();
                    chanstatLst.put(serv, chanstat);
                    chanstats.put(key, chanstatLst);
                }
                else
                {
                    chanstat = chanstatLst.get(serv);
                    if (chanstat == null)
                    {
                        chanstat = new ChanStat();
                        chanstatLst.put(serv, chanstat);
                    }
                }
                chanstat.add(ci);
                
                chanstats.put(key, chanstatLst);
            }
            
        }
        catch (SQLException ex)
        {
            Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
            try
            {
                chnTbl.streamClose();
            }
            catch (SQLException ex1)
            {
                Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        finally
        {
            try
            {
                chnTbl.streamClose();
            }
            catch (SQLException ex)
            {
                Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    //=================================
    

    public CheckDb() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        startTime = System.currentTimeMillis();
        lastTime = startTime;
        
    }

    /**
     * Print message with timing info if appropriate verbosity level
     *
     * @param opName description of what we just timed
     * @param verbosity priority of message if less than current verbosity we print.
     */
    private void logTime(String opName, int verbosity)
    {
        long curTime = System.currentTimeMillis();
        float elap = (curTime - startTime) / 1000.f;
        float opTime = (curTime - lastTime) / 1000.f;
        float memUsed = Runtime.getRuntime().totalMemory()/1e6f;
        if (verbose >= verbosity)
        {
            System.out.println(String.format("%1$s op: %2$.2fs, elap: %3$.2fs, mem: %4$,.0f MB", 
                                             opName, opTime, elap, memUsed));
        }
        lastTime = curTime;

    }

    /**
     * Open a connection to the database and create the channel table objects we need
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void setup() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        if (db != null)
        {
            db.close();
        }
        ViewerConfig vc;
        vc = new ViewerConfig();
        if (!configFile.isEmpty())
        {
            vc.setConfigFileName(configFile);
        }
        db = vc.getDb();
        if (verbose > 1)
        {
            System.out.print("Connected to: ");
            System.out.println(vc.getLog());
        }
        
    }

    private void doReport(String ifoSubsys)
    {
        int errCount=0;
        int multiRateRawCount = 0;
        int multiRateRdsCount = 0;
        int maxRawRates = 0;
        int maxRdsRates = 0;
        
        int[] srvCnts = new int[8];
        for(int i=0; i < srvCnts.length; i++)
        {
            srvCnts[i] = 0;
        }
        // report channels with multiple rates
        for (Entry<String, TreeMap<String, ChanStat>> csl : chanstats.entrySet())
        {
            for (Entry<String, ChanStat> cs : csl.getValue().entrySet())
            {
                ChanStat chanstat = cs.getValue();
                if (chanstat.hasMultipleRawRates())
                {
                    multiRateRawCount++;
                    maxRawRates = Math.max(maxRawRates, chanstat.getRawRateCount());
                    if(showMultiRates)
                    {
                        System.out.format("%1$s @ %2$s has multiple raw rates: ", csl.getKey(), cs.getKey());
                        System.out.println(chanstat.getRawRateList());
                    }
                }
                if (chanstat.hasMultipleRdsRates())
                {
                    multiRateRdsCount++;
                    maxRdsRates = Math.max(maxRdsRates, chanstat.getRdsRateCount());
                    if (showMultiRates)
                    {
                        System.out.format("%1$s @ %2$s has multiple rds rates: ", csl.getKey(), cs.getKey());
                        System.out.println(chanstat.getRdsRateList());
                    }
                }
            }
            }

        for (Entry<String, TreeMap<String, ChanStat>> csl : chanstats.entrySet())
        {
            String cname = csl.getKey();
            int n = csl.getValue().size();
            if (n >= srvCnts.length)
            {
                srvCnts[srvCnts.length-1]++;
            }
            else
            {
                srvCnts[n]++;
            }
            
            for (Entry<String, ChanStat> ent : csl.getValue().entrySet())
            {
                String serv = ent.getKey();
                ChanStat cstat = ent.getValue();
                String errors = cstat.getError();
                if (!errors.isEmpty())
                {
                    System.out.format("%1$s @ %2$s:%n%3$s%n",cname, serv, errors);
                    errCount++;
                }
            }
        }

        if (verbose > 1)
        {
            System.out.format("%4$s: table rows: %1$,d, Unique channel count: %2$,d, errors: %3$,d%n", 
                              count, chanstats.size(), errCount, ifoSubsys);
        }
        if (verbose > 2)
        {
            System.out.format("Multiple rate count - raw: %1$,d, rds: %2$,d %n", 
                              multiRateRawCount, multiRateRdsCount);
            System.out.format("Maximum number of sample rates - raw: %1$d, rds: %2$d%n",
                              maxRawRates, maxRdsRates);
            System.out.println("Channels with multiple servers:");

            for(int i=0; i < srvCnts.length; i++)
            {
                System.out.format("    %1$d.  %2$,d%n", i, srvCnts[i]);
            }
        }
    }

    private void saveNames()
    {
        try
        {
            
            for(String name : chanstats.keySet())
            {
                out.write(name);
                out.newLine();
            }
            
        }
        catch (IOException ex)
        {
            Logger.getLogger(CheckDb.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void makeChannelIndexTable() throws SQLException
    {
        Pattern ifoSubsysPat = Pattern.compile("(^.+):((.+?)[_-])?");
        
        for (Entry<String, TreeMap<String, ChanStat>> csl : chanstats.entrySet())
        {
            ChanIndexInfo cii = new ChanIndexInfo();

            String cname = csl.getKey();
            cii.setName(cname);

            TreeMap<String, ChanStat> serverList = csl.getValue();
            int n = serverList.size();
            cii.setnServers(n);

            Matcher ifoSubsys = ifoSubsysPat.matcher(cname);
            if (ifoSubsys.find())
            {
                cii.setIfo(ifoSubsys.group(1));
                String subsys = ifoSubsys.group(3);
                subsys = subsys == null ? "" : subsys;
                cii.setSubsys(subsys);
            }
            else
            {
                cii.setIfo("");
                cii.setSubsys("");
            }
            float minRawRate = Float.MAX_VALUE;
            float maxRawRate = Float.MIN_VALUE;
            float minRdsRate = Float.MAX_VALUE;
            float maxRdsRate = Float.MIN_VALUE;
            
            boolean hasRaw=false;
            boolean hasRds=false;
            boolean hasOnline=false;
            boolean hasMtrends=false;
            boolean hasStrends=false;
            boolean hasTstpnt=false;
            boolean hasStatic=false;
            String cisAvail=" ";
            
            for(Entry<String, ChanStat> srv : serverList.entrySet())
            {
                ChanStat cs = srv.getValue();
                minRawRate = Math.min(minRawRate, cs.getMinRawRate());
                maxRawRate = Math.max(maxRawRate, cs.getMaxRawRate());
                maxRdsRate = Math.max(maxRdsRate, cs.getMaxRdsRate());
                minRdsRate = Math.min(minRdsRate, cs.getMinRdsRate());
                hasRaw |= cs.hasRaw();
                hasRds |= cs.hasRds();
                hasMtrends |= cs.hasMtrends();
                hasStrends |= cs.hasStrend();
                hasStatic |= cs.hasStatic();
                hasOnline |= cs.hasOnline();
                String cis= cs.getCisAvail();
                if (cis.equalsIgnoreCase("d"))
                {
                    cisAvail = cis;
                }
                else if (!cisAvail.equalsIgnoreCase("d") && cis.equalsIgnoreCase("a"))
                {
                    cisAvail = cis;
                }
            }
            minRawRate = minRawRate == Float.MAX_VALUE ? 0 : minRawRate;
            cii.setMinRawRate(minRawRate);
            maxRawRate = maxRawRate == Float.MIN_VALUE ? 0 : maxRawRate;
            cii.setMaxRawRate(maxRawRate);
            minRdsRate = minRdsRate == Float.MAX_VALUE ? 0 : minRdsRate;
            cii.setMinRdsRate(minRdsRate);
            maxRdsRate = maxRdsRate == Float.MIN_VALUE ? 0 : maxRdsRate;
            cii.setMaxRdsRate(maxRdsRate);
            
            cii.setCisAvail(cisAvail);
            cii.setHasMtrends(hasMtrends);
            cii.setHasRaw(hasRaw);
            cii.setHasRds(hasRds);
            cii.setHasStrends(hasStrends);
            cii.setHasStatic(hasStatic);
            cii.setHasTestpoint(hasTstpnt);
            cii.setHasOnline(hasOnline);
            
            cidx.insertNewBulk(cii);
        }
        cidx.insertNewBulk(null);   // flush any remaining
    }

    private void makeChanPointerTable(String ifoSubsys) throws SQLException
    {
        cidx.streamByName(ifoSubsys + "%");
        
        ChanIndexInfo cii;
        // one pass through the ChannelIndex table to set the Index ID in our in memory Map
        while ((cii = cidx.streamNext()) != null)
        {
            int indexID = cii.getIndexID();
            
            TreeMap<String, ChanStat> serverList = chanstats.get(cii.getName());
            if (serverList == null)
            {
                System.err.println("Channel in db not found in list: " + cii.getName());
            }
            else
            {
                for (Entry<String, ChanStat> ent : serverList.entrySet())
                {
                    ent.getValue().setIndexID(indexID);
                }
            }
        }
        // another pass through the in memory Map to write out the pointer
        
        for (Entry<String, TreeMap<String, ChanStat>> csl : chanstats.entrySet())
        {

            String cname = csl.getKey();

            TreeMap<String, ChanStat> serverList = csl.getValue();

            for (Entry<String, ChanStat> ent : serverList.entrySet())
            {
                ArrayList<ChanPointer> chanPointerList = ent.getValue().getChanPointerList();
                for(ChanPointer chp : chanPointerList)
                {
                    cpt.insertNewBulk(chp);
                }
            }
        }
        cpt.insertNewBulk(null);
    }

    private boolean processArgs(String[] args)
    {
        boolean ret = true;
        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));

        options.addOption(OptionBuilder.withArgName("config").hasArg().withDescription("ldvw configuration path").create("config"));

        CommandLineParser parser = new GnuParser();

        boolean wantHelp = false;
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
            wantHelp = true;
            line = null;
        }
        if (line != null)
        {
            if (line.hasOption("version"))
            {
                System.out.println(programName + " - version " + version);
            }

            wantHelp = line.hasOption("help");
            if (line.hasOption("config"))
            {
                configFile = line.getOptionValue("config");
            }
        }
        if (wantHelp)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName, options);
            ret = false;
        }
        return ret;
    }
    
}
