/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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
package chanavailabilityupdater;

import edu.fullerton.ldvjutils.ChanParts;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.*;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * Program queries all NDS servers to update the table of data availabile times
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class ChanAvailabilityUpdater
{

    private ChanDataAvailability cda;
    private String[] servers;
    private String[] cTypes;
    private ArrayList<ChanListSummary> cLists;
    private NDSProxyClient nds;
    private String libPath = "";
    private Database db = null;         // Connection to ligodv data base
    private ChannelTable chnTbl;        // The real Channels table
    private static int verbose = 3;
    private HashMap<ChanInfo, String> chanMap;
    private int grpCnt=10;
    private String curServer;
    
    public ChanAvailabilityUpdater() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        cLists = new ArrayList<ChanListSummary>();
        servers = ChanParts.getServers();
        cTypes = ChanParts.getChanTypes();
        startTime = System.currentTimeMillis();
        lastTime = startTime;
        setup();
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
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws LdvTableException
    {
        try
        {
            ChanAvailabilityUpdater me = new ChanAvailabilityUpdater();
            me.logTime("build map of channels");
            me.buildMap();
            me.logTime("query nds server for all channels");
            
            me.doTransfer(true);
        }
        catch (SQLException | ViewConfigException | ClassNotFoundException ex)
        {
            Logger.getLogger(ChanAvailabilityUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void doTransfer(boolean clear) throws LdvTableException
    {
        startTimes();

        try
        {
            cda = new ChanDataAvailability(db);
            if(clear) 
            {
                cda.emptyTable();
            }
            
            ArrayList<String> cnameList = new ArrayList<String>();
            
            String[] ifos = ChanParts.getIFOList();
            String[] types = ChanParts.getChanTypes();
            String curType = "";
            for (String server : servers)
            {
                curServer = server;
                System.out.println(String.format("Processing %1$s", server));
                try
                {
                    System.out.println("Server: " + server);
                    nds = new NDSProxyClient(server);
                    if (nds.connect())
                    {
                        logTime("Connect");
                        ChannelTable ct = new ChannelTable(db);
                        int count=0;
                        
                        for(String ifo : ifos)
                        {   // the channel list is getting long so break it into smaller pieces
                            for(String ctype : types)
                            {
                                curType = ctype;    // in case we need an error message
                                if (!ctype.equalsIgnoreCase("online"))
                                {
                                    TreeSet<ChanInfo> allChan = ct.getAsSet(server, ifo+ "%", ctype, count);
                                    System.out.println(String.format("%s - %s has %d channels of type %s", 
                                                                    server, ifo, allChan.size(),ctype));
                                    if (ctype.toLowerCase().contains("trend"))
                                    {   // only look up one of the trend channels
                                        TreeSet<ChanInfo> allChan2 = new TreeSet<ChanInfo>();
                                        for (ChanInfo ci : allChan)
                                        {
                                            String name = ci.getChanName();
                                            if (name.endsWith(".n"))
                                                allChan2.add(ci);
                                        }
                                       // allChan = allChan2;
                                    }
                                    // there are problems with a few trend channels (labled as raw)
                                    TreeSet<ChanInfo> allChan2 = new TreeSet<ChanInfo>();
                                    for (ChanInfo ci : allChan)
                                    {
                                        String name = ci.getChanName();
                                        if (name.length() <= 63 && name.matches("^[\\x20-\\x7f]+$"))
                                        {
                                            allChan2.add(ci);
                                        }
                                        else
                                        {
                                            System.err.println("Invalid channel name: " + name);
                                        }
                                    }
                                    allChan = allChan2;
                                    String name;
                                    cnameList.clear();
                                    String[] cliststr;
                                    for (ChanInfo ci : allChan)
                                    {
                                        name = ci.getChanName();
                                        if (name == null || name.equalsIgnoreCase("null"))
                                        {
                                            System.out.println("null channel name found " + server + ":" + ifo);
                                            ci.print();
                                        }
                                        else
                                        {
                                            cnameList.add(name);
                                        }
                                        if (cnameList.size() >= grpCnt)
                                        {
                                            cliststr = new String[0];
                                            cliststr = cnameList.toArray(cliststr);
                                            addToDb(nds,server,cliststr,ctype);
                                            cnameList.clear();
                                        }

                                    }
                                    if (cnameList.size() > 0)
                                    {
                                        cliststr = new String[0];
                                        cliststr = cnameList.toArray(cliststr);
                                        addToDb(nds,server,cliststr,ctype);
                                        cnameList.clear();
                                    }
                                    logTime(server + ":" + ifo + " - " + ctype + " processed. ");
                                }
                            }
                        }
                        nds.disconnect();
                    }
                    else
                    {
                        System.out.println("Failed to connect to: " + server);
                    }
                    nds.bye();
                }
                catch (NDSException ex)
                {
                    String msg = String.format("Serv: %1$s. type: %2$s",server, curType);
                    Logger.getLogger(ChanAvailabilityUpdater.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChanAvailabilityUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private long startTime;
    private long lastTime;

    private void startTimes()
    {
        startTime = System.currentTimeMillis();
        lastTime = startTime;
    }

    private void logTime(String opName)
    {
        long curTime = System.currentTimeMillis();
        float elap = (curTime - startTime) / 1000.f;
        float opTime = (curTime - lastTime) / 1000.f;
        System.out.println(String.format("%1$s op: %2$.2fs, elap: %3$.2fs", opName, opTime, elap));

        lastTime = curTime;

    }
    /**
     * Take a list of channels and availability data and insert into our db
     * An input line looks like:
     * L1:ISI-HAM4_BLND_GS13Z1_SW1R {L-R:1011084640-1012246848 L-R:1015316384-1017012096}
     * <channel name> {<frame type>:<time spec> ...}
     * A time spec can be all = which means unknown
     * <start gps>-<stop gps>
     * <start gps>-inf where inf means unknown
     *
     * @param server what server was queried
     * @param srcInfo results from an
     * @see NDSClient#getChannelSourceInfo
     */
    private static final String[] trends = {"max", "mean", "min", "n", "rms"};
    
    public void addList(String server, String srcInfo, String ctype) throws IOException, SQLException
    {
        BufferedReader br = new BufferedReader(new StringReader(srcInfo));
        String line;
        Pattern p1 = Pattern.compile("\\s*(.*)\\s+\\{(.*)\\}");
        
        
        while ((line = br.readLine()) != null)
        {
            Matcher m = p1.matcher(line);
            if (m.find())
            {
                String chName = m.group(1);
                String availability = m.group(2);
                availability = availability.replaceAll("all", "?");
                availability = availability.replaceAll("inf", "?");
                if (! availability.matches("^.*:\\?$") && ctype.toLowerCase().contains("trend"))
                {
                    String baseName = chName.substring(0, chName.indexOf(".")+1);   // include the dot
                    for(String suffix : trends)
                    {
                        String name = baseName + suffix;
                        cda.insertNewBulk(name, server, ctype, availability);
                    }
                }
                else if (! availability.matches("^.*?:\\?$"))
                {
                    cda.insertNewBulk(chName, server, "", availability);
                }            
            }
        }
    }

    private void addToDb(NDSProxyClient nds, String server, String[] cliststr, String ctype)
    {
        String srcInfo;
        try
        {
            srcInfo = nds.getChannelSourceInfo(cliststr);
        }
        catch (NDSException ex)
        {
            srcInfo = "";
            System.err.println("Error getting Channel Source Info: " + ex.getLocalizedMessage());
            
            
            try
            {
                nds.bye();
                nds = new NDSProxyClient(server);
                nds.connect(60000);
            }
            catch (NDSException ex1)
            {
                Logger.getLogger(ChanAvailabilityUpdater.class.getName()).log(Level.SEVERE, null, ex1);
            }
        }
        try
        {
            if (!srcInfo.isEmpty())
            {
                addList(server, srcInfo, ctype);
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error adding list to db: " + ex.getLocalizedMessage());
        }
    }

    private void buildMap() throws SQLException
    {
        chanMap = new HashMap<ChanInfo, String>();
        chnTbl.streamAll();
        ChanInfo chan;
        int count = 0;
        
        while ( (chan = chnTbl.streamNext()) != null )
        {
            String cType = chan.getcType().toLowerCase();
            if ( ! cType.equals("online") )
            {
                
                if ( ! cType.toLowerCase().contains("trend") || chan.getChanName().contains(".mean"))
                {
                    count++;
                   // chanMap.put(chan, "");
                }
            }
        }
        chnTbl.streamClose();
        if (verbose > 1)
        {
            System.out.format("%d channels in map%n", count);
        }
    }
}
