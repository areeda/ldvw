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
package chanupdater;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChanListSummary;
import edu.fullerton.ldvtables.ChanUpdateTable;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.PageItemCache;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * New and improved way to maintain the channel tables
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanUpdater
{
    private String[] servers;
    private String[] cTypes;
    
    private final ArrayList<ChanListSummary> cLists;
    private NDSProxyClient nds;

    private Database db = null;         // Connection to ligodv data base
    private ChannelTable chnTbl;        // The real Channels table
    private ChanUpdateTable chUpdTbl;   // Records raw chan lists, with md5 hash
    
    private int verbose = 1;

    // what we're supposed to do
    private boolean doGetFileList = true;
    
    private boolean doPendingUpds = true;
    private final boolean doDeletes = true;
    private final boolean doCleanup = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            ChanUpdater me = new ChanUpdater();
            me.doit(args);
        }
        catch (Exception ex)
        {
            Logger.getLogger(ChanUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private int totalAdds=0;
    private int totalDels=0;
    private int countErrors=0;
    private int dbCount=0;
    private int total=0;
    private final String programName="ChanUpdater";
    private String configFile = "";
    private final String version="0.1.0";
    private boolean rebuild;
    
    private void doit(String[] args)
    {
        try
        {
            if (processArgs(args))
            {
                setup();
                getCounts();
                
                if (doGetFileList)
                {
                    getChanListFiles();
                }

                if (doPendingUpds)
                {
                    doUpdates();
                }
                if (doCleanup && (totalAdds + totalDels) != 0)
                {
                    cleanUp();
                }
            }
            if (verbose > 0)
            {
                long elap = System.currentTimeMillis() - startTime;
                System.out.format("Errors getting counts from servers: %1$d%n", countErrors);
                System.out.format("Total channels reported by nds servers: %1$,d, in our DB %2$,d%n", total, dbCount);
                if (totalAdds + totalDels > 0)
                {
                    System.out.format("Total additions: %1$,d, total removals: %2$,d\n", totalAdds, totalDels);
                }
                else
                {
                    System.out.println("No changes to channel tables.");
                }

                System.out.format("Elapsed time: %1$,.1fs\n", elap / 1000.);
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(ChanUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    //=================================
    private final long startTime;
    private long lastTime;

    public ChanUpdater() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        cLists = new ArrayList<>();
        // @todo get server list from database
        servers = ChanParts.getServers();
        cTypes = ChanParts.getChanTypes();
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
        if (verbose >= verbosity)
        {
            System.out.println(String.format("%1$s op: %2$.2fs, elap: %3$.2fs", opName, opTime, elap));
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
        ViewerConfig vc = new ViewerConfig();
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
        chnTbl = new ChannelTable(db);
        if (rebuild)
        {
            chnTbl.recreate();      // drop table and create a new empty table
        }
        else if (! chnTbl.exists(true))
        {
            chnTbl.createTable();
        }
        
        chUpdTbl = new ChanUpdateTable(db);
        if (rebuild)
        {
            chUpdTbl.recreate();
        }
        else if (! chUpdTbl.exists(true))
        {
            chUpdTbl.createTable();
        }
    }

    /**
     * Request counts and channel list hash from each server, channel type to determine which will
     * need to transfer the full channel list.
     */
    private void getCounts()
    {
        total=0;
        for(String srv : servers)
        {
            try
            {
                nds = new NDSProxyClient(srv);
                nds.connect(120000);
                
                for(String ctyp : cTypes)
                {
                    if (!ctyp.equalsIgnoreCase("unknown"))
                    {
                        int n;
                        n = nds.getChanCount(ctyp);
                        String crc="";
                        boolean needsUpdate = false;
                        if (n > 0)
                        {
                            crc = nds.getChanHash(ctyp);
                            total += n;
                            ChanListSummary cls = new ChanListSummary(srv, ctyp, n);
                            cls.setCrc(crc);
                            needsUpdate = chUpdTbl.checkAdd(cls);
                            int dbcnt = chnTbl.getCount(srv, ctyp);
                            needsUpdate |= n == dbcnt;
                            cls.setNeedsUpd(needsUpdate);
                            if (needsUpdate)
                            {
                                cLists.add(cls);
                            }
                            if (verbose > 0)
                            {
                                System.out.format("Server: %1$-24s, type: %2$-12s, count: %3$,9d,"
                                                  + " crc: %4$-9s, needs Update: %5$b\n",
                                                  srv, ctyp.toString(), n, crc, needsUpdate);
                            }
                        }
                        chUpdTbl.setUpdateFlag(srv, ctyp, needsUpdate);
                    }
                }
            }
            catch (WebUtilException | LdvTableException | NDSException | IOException ex)
            {
                countErrors++;
                System.err.format("Server: %1$s.  Failed to connect or get counts: %2$s%n",
                                  srv,ex.getLocalizedMessage());
            }

            try
            {
                if (nds != null)
                {
                    nds.bye();
                }
            }
            catch (NDSException ex)
            {
                System.out.format("Server: %1$s, error on disconnect: %2$s%n", 
                                  srv,ex.getLocalizedMessage());
            }
        }
        dbCount = chnTbl.getCount("", "");
        System.out.format("Total channels reported by nds servers: %1$,d, in our DB %2$,d%n",total, dbCount);
    }

    private void getChanListFiles() throws IOException, NoSuchAlgorithmException, LdvTableException
    {
        for(ChanListSummary cls : cLists)
        {
            int cnt = cls.getCount();
            if (cnt > 0)
            {
                String srv="";
                String ctypStr="";
                try
                {
                    srv = cls.getServer();
                    ctypStr = cls.getcType();
                    if (verbose > 1)
                    {
                        System.out.format("Get channel list from %1$s for type: %2$s%n", srv,ctypStr);
                    }
                    nds = new NDSProxyClient(srv);
                    nds.connect(600000);
                    ArrayList<ChanInfo> channelList = nds.getChanList(ctypStr);
                    if (channelList != null && ! channelList.isEmpty())
                    {
                        Collections.sort(channelList);
                        cls.dumpFile(channelList);
                    }
                }
                catch (NDSException ex)
                {
                    System.err.format("Error with %1$s type at %2$s: %3$s - %4$s",
                                      ctypStr, srv, ex.getClass().getSimpleName(), ex.getLocalizedMessage());
                }
                try
                {
                    if (nds != null)
                    {
                        nds.bye();
                    }
                }
                catch (NDSException ex)
                {
                    System.out.format("Server: %1$s, error on disconnect: %2$s%n",
                                      srv, ex.getLocalizedMessage());
                }
                cls.printSummary();
            }
        }
    }

    private void doUpdates() throws SQLException, IOException, FileNotFoundException, LdvTableException
    {
        if (verbose > 1)
        {
            System.out.println("Starting update process.");
        }
        ArrayList<ChanListSummary> chanLists;
        
        HashSet<ChanInfo> del = new HashSet<>();
        totalAdds = 0;
        totalDels = 0;
        
        for(ChanListSummary cls : cLists)
        {
            
            cls.printSummary();
            String server = cls.getServer();
            String cTyp = cls.getcType();
            
            if (verbose > 2)
            {
                System.out.format("Check %1$s for type:%2$s ",server,cTyp);
            }
            
            TreeMap<String, HashSet<ChanInfo>> chanSets = cls.getChanSets();
            for(Entry<String,HashSet<ChanInfo>> ent : chanSets.entrySet())
            {
                del.clear();
                HashSet<ChanInfo> newChans = ent.getValue();
                String ifo = ent.getKey();
                if (verbose > 1)
                {
                    System.out.format("Server: %1$s, cType: %2$s, IFO: %3$s, count: %4$,d\n", 
                                  cls.getServer(),cls.getcType(),ifo,newChans.size());
                }
                String namePat = ifo + ":%";
                TreeSet<ChanInfo> oldSet = chnTbl.getAsSet(server, namePat, cTyp,newChans.size());
                
                for(ChanInfo old: oldSet)
                {
                    boolean gotit = newChans.contains(old);
                    if (gotit)
                    {
                        // it's in both
                        newChans.remove(old);
                    }
                    else
                    {
                        if (old.isAvailable())
                        {
                            // only in old add it to be deleted set
                            del.add(old);
                        }
                    }
                }
                totalAdds += newChans.size();
                totalDels += del.size();
                
                if ((newChans.size() > 0 || del.size() > 0))
                {
                    if (verbose > 1)
                    {
                        System.out.format("    add: %1$d, del %2$d\n",newChans.size(),del.size());
                    }
                    for(ChanInfo ci : newChans)
                    {
                        if (verbose > 2)
                        {
                            System.out.print("Add: ");
                            ci.print();
                        }
                        chnTbl.insertNewBulk(ci);
                    }
                    if (newChans.size() > 0)
                    {
                        chnTbl.insertNewBulk(null);     // complete the bulk insert
                    }
                    if (doDeletes)
                    {
                        for(ChanInfo ci : del)
                        {
                            if (verbose > 2)
                            {
                                System.out.print("Del: ");
                                ci.print();
                            }
                            chnTbl.setAvailable(ci.getId(), false);
                        }
                    }
                }
                else if (verbose > 1)
                {
                    System.out.println("    no updates.");
                }
                
            }
            if (verbose > 0 && totalAdds + totalDels > 0)
            {
                System.out.format("Total additions: %1$,d, total removals: %2$,d, "
                        + "Server: %3$s, type: %4$s%n", totalAdds,totalDels,cls.getServer(), cls.getcType());
            }
            else if (verbose > 1 && totalAdds + totalDels == 0)
            {
                System.out.println("No changes to channel table. %n");
            }
        }
    }

    private void cleanUp() throws WebUtilException, MalformedURLException, IOException
    {
        try
        {
            chnTbl.optimize();
            PageItemCache pic = new PageItemCache(db);
            String cmd = "DELETE FROM " + pic.getName() + 
                         " WHERE name='ChannelStats'";
            db.execute(cmd);
            
//            // get a kerberos ticket
//            String home = System.getenv("HOME");
//            String keytab = home + "/secure/joseph.areeda.keytab";
//            String user = "joseph.areeda@LIGO.ORG";
//            ExternalProgramManager.getTGT(keytab, user);
//
//            String chstatUrl = "http://localhost/viewer/?act=ChannelStats";
//            URL url = new URL(chstatUrl);
//            InputStream is = url.openConnection().getInputStream();
//
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is)))
//            {
//                String line = null;               
//                while ((line = reader.readLine()) != null)
//                {
//                    
//                }
//            }
        }
        catch (SQLException ex)
        {
            String ermsg = "Error optimizing table: " + ex.getClass().getSimpleName();
            ermsg += " - " + ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
    }
    private boolean processArgs(String[] args)
    {
        boolean ret = true;
        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));
        options.addOption(new Option("rebuild", "delete and rebuild tables, default is update if needed"));
        options.addOption(new Option("skip_download", "don't download channel lists, use existing files"));
        options.addOption(new Option("noupdates", "don't update the database"));

        options.addOption(OptionBuilder.withArgName("config").hasArg().withDescription("ldvw configuration path").create("config"));
        options.addOption(OptionBuilder.withArgName("verbose").hasArg().withDescription("verbosity level 0-5").create("verbose"));
        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("comma separated list of servers, default=all").create("server"));
        options.addOption(OptionBuilder.withArgName("chantype").hasArg().withDescription("comma separated list of channel types, default=all").create("chantype"));

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
                ret = false;
            }
            rebuild = line.hasOption("rebuild");
            doGetFileList = ! line.hasOption("skip_download");
            doPendingUpds = ! line.hasOption("noupdates");
            if (line.hasOption("verbose"))
            {
                String verboseStr = line.getOptionValue("verbose");
                if (verboseStr.matches("^\\d+"))
                {
                    verbose = Integer.parseInt(verboseStr);
                }
            }
            if (line.hasOption("server"))
            {
                String srvStr = line.getOptionValue("server");
                String[] srv = srvStr.split(",");
                if (srv.length > 0)
                {
                    servers = new String[srv.length];
                    for(int i=0;i<srv.length;i++)
                    {
                        servers[i] = srv[i].trim();
                    }
                }
            }
            
            if (line.hasOption("chantype"))
            {
                String optStr = line.getOptionValue("chantype");
                String[] opts = optStr.split(",");
                if (opts.length > 0)
                {
                    cTypes = new String[opts.length];
                    for (int i = 0; i < opts.length; i++)
                    {
                        cTypes[i] = opts[i].trim();
                    }
                }
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

