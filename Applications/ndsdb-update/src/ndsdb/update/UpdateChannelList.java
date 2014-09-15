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

package ndsdb.update;

import com.areeda.jaDatabaseSupport.Database;
import ndsJUtils.NDSChannelAvailability;
import edu.fullerton.ldvjutils.LdvTableException;
import ndsJUtils.NdsChan;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ndstables.NDSChannelTable;
import edu.fullerton.ndstables.NdsChanAvailMap;
import edu.fullerton.ndstables.NdsChanAvailTable;
import edu.fullerton.ndstables.NdsServerTable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ndsJUtils.NdsChanAvailPair;
import ndsJUtils.NdsServer;
import viewerconfig.DbSupport;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class UpdateChannelList 
{
    private String serverName;
    private NdsServer server;
    private String site;
    private String curFilename;
    private int verbose = 2;
    private Pattern fltPat;
    private int errCnt = 0;
    private Map<String, NdsChan> chanMap;
    private AvailManager availManager;
    private final ViewerConfig viewerConfig;
    private int startChanCount;
    private int startAvailcount;
    private boolean dbLoaded;
    
    public UpdateChannelList() throws LdvTableException
    {
        viewerConfig = new ViewerConfig();
        try
        {
            String[] tableClassNames =
            {
                "edu.fullerton.ldvtables.ServerTable",
                "edu.fullerton.ndstables.NDSChannelTable",
                "edu.fullerton.ndstables.NdsChanAvailTable",
                "edu.fullerton.ndstables.NdsServerTable",
                "edu.fullerton.ndstables.NdsChanAvailMap"
            };
            Database db = viewerConfig.getDb("nds");
            DbSupport dbs = new DbSupport();
            dbs.checkDb(db, tableClassNames);
            
            chanMap = new HashMap<>();
            availManager = new AvailManager(db);
            dbLoaded = false;
        }
        catch (ViewConfigException ex)
        {
            throw new LdvTableException("Updating channel list", ex);
        }
        
    }

    public void setVerbose(int verbose)
    {
        this.verbose = verbose;
        fltPat = Pattern.compile("^(([0-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?$");
    }

    public void updateFromDir(String site, String dir, String serverName) throws ViewConfigException, 
                       LdvTableException
    {
        this.serverName = serverName;
        NdsServerTable st = new NdsServerTable(viewerConfig.getDb("nds"));
        server = st.getByName(serverName);
        
        this.site = site;
        
        if (!dbLoaded)
        {
            loadDb();
        }
        File chanListDir = new File(dir);
        File[] fileList = chanListDir.listFiles();
        
        for (File f : fileList)
        {
            if (!f.isDirectory() && f.canRead() &&
                f.getName().toLowerCase().matches("^.*chan.*list.txt$"))
            {
                curFilename = f.getName();
                if (verbose > 1)
                {
                    try
                    {
                        System.out.println("Scanning: " + f.getCanonicalPath());
                    }
                    catch (IOException ex)
                    {
                        
                    }
                }
                try
                {
                    processDir(f);
                }
                catch (LdvTableException ex)
                {
                    System.err.println(ex.getLocalizedMessage());
                }
            }
        }
        updateDb();
    }

    private int processDir(File f) throws LdvTableException
    {
        BufferedReader br = null;
        int ccount = 0;
        int newccount=0;
        int lcount = 0;
        try
        {
            Matcher m;
            br = new BufferedReader(new FileReader(f));
            String line;
            ArrayList<NDSChannelAvailability> avail = new ArrayList<>();
            while ((line = br.readLine()) != null)
            {
                lcount++;
                if (!line.matches("\\s*#.*$"))
                {
                    String line2 = line.replaceAll("  +", " ");     // remove duplicate spaces
                    String[] flds = line2.split("\\s");
                    if (flds.length < 5)
                    {
                        error(lcount, "Line has too few fields", line);
                    }
                    else
                    {
                        String chname = flds[0];
                        String ctype = flds[1];
                        String access = flds[2];     
                        String dtype = flds[3];
                        String fsStr = flds[4];
                        
                        float fs;
                        m = fltPat.matcher(fsStr);
                        if (m.find())
                        {
                            fs = Float.parseFloat(fsStr);
                        }
                        else
                        {
                            error(lcount, "sample frequency", "Not a number");
                            break;
                        }
                        avail.clear();
                        NDSChannelAvailability ca=null;

                        if (flds.length > 6)
                        {
                            for(int i=5; i<flds.length; i+=2)
                            {
                                if (i+1 >= flds.length)
                                {

                                    error(lcount, "frame type, avail interval", "Not enough fields on line");
                                    break;
                                }
                                String frameType = flds[i];
                                String[] times = flds[i+1].split(":");
                                if (times.length < 2)
                                {
                                    error(lcount, "frame avail interval", "error parsing start:duration");
                                    break;
                                }
                                if (!times[0].matches("^\\d+$") || !times[1].matches("^\\d+$"))
                                {
                                    error(lcount, "frame avail interval", "error parsing start:duration");
                                    break;
                                }
                                long startGps = Long.parseLong(times[0]);
                                long duration = Long.parseLong(times[1]);
                                TimeInterval ti = new TimeInterval(startGps, startGps+duration);
                                ca = new NDSChannelAvailability(server.getMyId(), 
                                        ctype,dtype,fs,frameType, ti);
                            }
                        }
                        else if (flds.length == 6)
                        {
                            String frameType = flds[5];
                            TimeInterval ti = new TimeInterval(0, 1999999999);
                            ca = new NDSChannelAvailability(server.getMyId(),
                                        ctype, dtype, fs, frameType, ti);
                        }
                        // We want a set of unique availability objects and link each channel to 
                        // that object
                        int caIdx = availManager.store(ca);
                        //avail.add(ca);
                        
                        NdsChan ndsChan;
                        ndsChan = new NdsChan();

                        ndsChan.init(server, chname, ctype, access, dtype, fs, caIdx);
                        if (ndsChan.getcTypeStr().equalsIgnoreCase("unknown"))
                        {
                            error(lcount, "Channel type", "unknow type: "+ ctype);
                        }
                        String name = ndsChan.getName();
                        ccount++;
                        if (chanMap.containsKey(name))
                        {
                            NdsChan oldChan = chanMap.get(name);
                            oldChan.merge(ndsChan);
                            chanMap.put(name, oldChan);
                        }
                        else
                        {
                            newccount++;
                            chanMap.put(name, ndsChan);
                        }
                    }
                }
            }
            
            if (verbose > 1)
            {
                System.out.format("File: %1$s, channels in file: %2$d, new channels: %3$d, errors: %4$d.%n",
                f.getName(), ccount, newccount,errCnt);
            }
        }
        catch (IOException ex)
        {
            throw new LdvTableException("Reading channel list file: " + f.getName(), ex);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException ex)
                {
                    // nothing we can do about this
                }
            }
        }
        error(lcount, "", "");
        return ccount;
    }
    private String lastField = "";
    private String lastProblem = "";
    private int cnt=0;
    
    private void error(int lcount, String fieldName, String problem)
    {
        if (lastField.equals(fieldName) && lastProblem.equals(problem))
        {
            cnt++;
        }
        else if (!fieldName.isEmpty() && !problem.isEmpty())
        {
            if (cnt > 0)
            {
                System.err.format("Last error repeated %1$d times.%n", cnt);
            }
            System.err.format("Error: file: %1$s, line: %2$d, field: %3$s, problem: %4$s%n", 
                              curFilename, lcount, fieldName, problem);
            cnt=0;
            lastProblem = problem;
            lastField = fieldName;
        }
        else if (cnt > 0)
        {
            System.err.format("Last error repeated %1$d times.%n", cnt);
        }
        if (fieldName.isEmpty() && problem.isEmpty())
        {
            cnt = 0;
            lastProblem = "";
            lastField = "";
        }
        errCnt++;
    }

    private void updateDb() throws LdvTableException
    {
        if (verbose > 1)
        {
            System.out.println("Update database.");
        }
        try
        {
            NDSChannelTable ndsct = new NDSChannelTable(viewerConfig.getDb("nds"));
            
            for(Entry<String, NdsChan> ent: chanMap.entrySet())
            {
                NdsChan chan = ent.getValue();
                String chnam = ent.getKey();

                if (chan.getServers() == 0)
                {
                    error(cnt, "server", "Map contains chan with no server.");
                }
                if (!chnam.contentEquals(chan.getName()))
                {
                    error(cnt, "name", "Map key does not match chan");
                }
                if (chan.isInited() && chan.isModified())
                {
                    try
                    {
                        ndsct.insertUpdate(chan);
                    }
                    catch(Exception ex)
                    {
                        throw new LdvTableException("Add/update channels", ex);
                    }
                }
            }
            // update list of time intervals for channel availability
            availManager.updateDb();
            // now the table that matches available times with specific channels
            NdsChanAvailMap ncam = new NdsChanAvailMap(viewerConfig.getDb("nds"));
            Map<Integer,Set<Integer>> dbAvails = new HashMap();

            ncam.streamAll();
            NdsChanAvailPair ncap;
            while ((ncap = ncam.streamNext()) != null)
            {
                int chanIdx = ncap.getChanIdx();
                int availIdx = ncap.getAvailIdx();
                if (dbAvails.containsKey(chanIdx))
                {
                    dbAvails.get(chanIdx).add(availIdx);
                }
                else
                {
                    Set<Integer> cav = new HashSet<>();
                    cav.add(availIdx);
                    dbAvails.put(chanIdx, cav);
                }  
            }
            ncam.streamClose();
            
            for (Entry<String, NdsChan> ent : chanMap.entrySet())
            {
                NdsChan chan = ent.getValue();
                if (chan.isModified())
                {
                    Set<Integer> avail = chan.getAvail();
                    int chanIdx = chan.getIndexID();

                    for(Integer availIdx : avail)
                    {
                        if (!(dbAvails.containsKey(chanIdx) && dbAvails.get(chanIdx).contains(availIdx)))
                        {
                            ncap = new NdsChanAvailPair(0, chanIdx, availIdx);
                            ncam.insertNewBulk(ncap);
                        }
                    }
                }
            }
            ncam.insertNewBulk(null);
        }
        catch (ViewConfigException | SQLException ex)
        {
            throw new LdvTableException("Add/update channels", ex);
        }
    }

    /**
     * build tables from what we have in the database
     */
    void loadDb() throws LdvTableException
    {
        try
        {
            startChanCount = 0;
            startAvailcount = 0;
            
            Map<Integer, String>idx = new HashMap<>();
            NDSChannelTable ndsct = new NDSChannelTable(viewerConfig.getDb("nds"));
            NdsChanAvailTable ndscat = new NdsChanAvailTable(viewerConfig.getDb("nds"));
            ndsct.streamAll();
            NdsChan chan;
            while ( (chan =ndsct.streamNext()) != null)
            {
                startChanCount++;
                chanMap.put(chan.getName(), chan);
                idx.put(chan.getIndexID(), chan.getName());
            }
            ndsct.streamClose();
            
            ndscat.streamAll();
            
            availManager.loadDb();
            dbLoaded = true;
        }
        catch (ViewConfigException | SQLException ex)
        {
            throw new LdvTableException("Loading existing databasde", ex);
        }
        if (verbose > 1)
        {
            System.out.format("Loaded existing tables: %1$,d channels, %2$,d intervals%n,",
                              startChanCount, startAvailcount);
        }
    }

    
}
