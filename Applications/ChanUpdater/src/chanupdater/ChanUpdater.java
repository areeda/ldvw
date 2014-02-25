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
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChanListSummary;
import edu.fullerton.ldvjutils.ChanParts;
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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    private ArrayList<ChanListSummary> cLists;
    private NDSProxyClient nds;

    private Database db = null;         // Connection to ligodv data base
    private ChannelTable chnTbl;        // The real Channels table
    private ChanUpdateTable chUpdTbl;   // Records raw chan lists, with md5 hash
    
    private static int verbose = 3;

    // what we're supposed to do
    private boolean doGetFileList = true;
    private boolean doPendingUpds = true;
    private boolean doDeletes = true;
    private boolean doCleanup = true;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            ChanUpdater me = new ChanUpdater();
            me.doit();
        }
        catch (Exception ex)
        {
            Logger.getLogger(ChanUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private int totalAdds;
    private int totalDels;
    private void doit()
    {
        try
        {
            getCounts();
            //@todo add command line argument processor
            if (doGetFileList)
            {
                getChanListFiles();
            }
            
            if (doPendingUpds)
            {
                doUpdates();
            }
            if (doCleanup && (totalAdds + totalDels) == 0)
            {
                cleanUp();
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(ChanUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (verbose > 0)
        {
            long elap = System.currentTimeMillis() - startTime;
            System.out.format("Elapsed time: %1$,.2f\n", elap/1000.);
        }
    }
    //=================================
    private long startTime;
    private long lastTime;

    public ChanUpdater() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        cLists = new ArrayList<ChanListSummary>();
        servers = ChanParts.getServers();
        cTypes = ChanParts.getChanTypes();
        startTime = System.currentTimeMillis();
        lastTime = startTime;
        setup();
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
        db = vc.getDb();
        if (verbose > 1)
        {
            System.out.print("Connected to: ");
            System.out.println(vc.getLog());
        }
        chnTbl = new ChannelTable(db);
        chUpdTbl = new ChanUpdateTable(db);
    }

    private void getCounts()
    {
        int total=0;
        for(String srv : servers)
        {
            try
            {
                nds = new NDSProxyClient(srv);
                nds.connect();
                
                for(String ctyp : cTypes)
                {
                    if (!ctyp.equalsIgnoreCase("unknown"))
                    {
                        int n;
                        n = nds.getChanCount(ctyp);
                        total += n;
                        ChanListSummary cls = new ChanListSummary(srv, ctyp.toString(), n);
                        cLists.add(cls);
                        if (verbose > 1 && n > 0)
                        {
                            System.out.format("Server: %1$s, type: %2$s, count: %3$,d\n",
                                          srv,ctyp.toString(),n);
                        }
                    }
                }
            }
            catch (NDSException ex)
            {
                System.out.format("Server: %1$s.  Failed to connect or get counts: %2$s%n",
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
        int dbCount = chnTbl.getCount("", "");
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
                    nds.connect(300000);
                    ArrayList<ChanInfo> channelList = nds.getChanList(ctypStr);
                    cls.dumpFile(channelList);
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
                boolean needsUpd = chUpdTbl.checkAdd(cls);
                cls.setNeedsUpd(needsUpd);
                cls.printSummary();
               
            }
        }
    }

    private void doUpdates() throws SQLException, IOException, FileNotFoundException, LdvTableException
    {
        ArrayList<ChanListSummary> chanLists;
        chanLists = chUpdTbl.getPendingUpdates();
        HashSet<ChanInfo> del = new HashSet<ChanInfo>();
        totalAdds = 0;
        totalDels = 0;
        
        for(ChanListSummary cls : chanLists)
        {
            
            cls.printSummary();
            String server = cls.getServer();
            String cTyp = cls.getcType();
            
            if (verbose > 1)
            {
                System.out.format("Check %1$s for type:%2$s",server,cTyp);
            }
            
            TreeMap<String, HashSet<ChanInfo>> chanSets = cls.getChanSets();
            for(Entry<String,HashSet<ChanInfo>> ent : chanSets.entrySet())
            {
                del.clear();
                HashSet<ChanInfo> newChans = ent.getValue();
                String ifo = ent.getKey();
                if (verbose > 2)
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
                else if (verbose > 2)
                {
                    System.out.println("    no updates.");
                }
                
            }
            if (verbose > 0 && totalAdds + totalDels > 0)
            {
                System.out.format("Total additions: %1$,d, total removals: %2$,d\n", totalAdds,totalDels);
            }
            else if (verbose > 1 && totalAdds + totalDels == 0)
            {
                System.out.println("No changes to channel table.");
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
/*
            String chstatUrl = "http://localhost/viewer/?act=ChannelStats";
            URL url = new URL(chstatUrl);
            InputStream is = url.openConnection().getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = null;
            while ((line = reader.readLine()) != null)
            {
               
            }
            reader.close();
*/
        }
        catch (SQLException ex)
        {
            String ermsg = "Error optimizing table: " + ex.getClass().getSimpleName();
            ermsg += " - " + ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
    }
}

