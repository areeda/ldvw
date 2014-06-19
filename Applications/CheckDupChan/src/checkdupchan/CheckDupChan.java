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
package checkdupchan;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * Scan all channels in Channels and ChannelIndex tables looking for duplicate entries
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CheckDupChan
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            CheckDupChan me = new CheckDupChan();
            me.doAll(args);
        }
        catch (LdvTableException |WebUtilException | SQLException | 
                ClassNotFoundException | ViewConfigException ex)
        {
            Logger.getLogger(CheckDupChan.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    private ChannelTable chnTbl;
    private Database db;
    private int verbose=2;
    private String configFile="";
    private Set<String> ifoSubsysSet;
    private ChannelIndex cidx;
    private ChanPointerTable cpt;

    private void doAll(String[] args) throws SQLException, ClassNotFoundException, ViewConfigException, WebUtilException, LdvTableException
    {
        setup();
        checkChanDups();
        checkPtrDups();
    }
    
    private void checkChanDups() throws SQLException
    {
        TreeSet<ChanInfo> chans = new TreeSet<>();
        TreeSet<ChanInfo> dups = new TreeSet<>();
        TreeSet<ChanInfo> multRates = new TreeSet<>();
        
        int chnCount=0;
        int dupCount=0;
        
        for (String ifoSubsys : ifoSubsysSet)
        {
            for (String server : ChanParts.getServers())
            {
                chnTbl.streamByServerName(server, ifoSubsys + "%");
                
                chans.clear();
                dups.clear();
                multRates.clear();
                
                ChanInfo ci;
                
                while ((ci = chnTbl.streamNext()) != null)
                {
                    chnCount++;
                    if (!chans.contains(ci))
                    {
                        chans.add(ci);
                    }
                    else
                    {
                        dups.add(ci);
                        dupCount++;
                    }
                }
                chnTbl.streamClose();
                if (!dups.isEmpty())
                {
                    System.out.format("Duplicate channels from server: %1$s, subsystem: %2%s %n", 
                                      server, ifoSubsys);
                    for(ChanInfo cinfo : dups)
                    {
                        System.out.format("    %1$s%n", cinfo.toString());
                    }
                }
                // Make a list of channels that only differ by data type
                ChanInfo oc = null;
                TreeSet<ChanInfo> mdtype = new TreeSet<>();
                TreeSet<ChanInfo> mrate  = new TreeSet<>();
                
                for(ChanInfo ch: chans)
                {
                    if (oc != null && oc.getChanName().equals(ch.getChanName())
                            && oc.getServer().equals(ch.getServer())
                            && oc.getcType().equals(ch.getcType())
                        )
                    {
                        boolean rateDiffers = Math.abs(oc.getRate() - ch.getRate()) > .0001;
                        if (!oc.getdType().equals(ch.getdType()) && ! rateDiffers)
                        {
                            mdtype.add(ch);
                            mdtype.add(oc);
                        }
                        if (rateDiffers)
                        {
                            mrate.add(ch);
                        }
                    }
                    oc = ch;
                }
                if (!mdtype.isEmpty())
                {
                    System.out.println("Channels with multiple data types:");
                    for(ChanInfo ch : mdtype)
                    {
                        System.out.format("    %1$s%n", ch.toString());
                    }
                }
            }
        }
        System.out.format("%1$d channels checked, %2$d duplicates found%n", chnCount, dupCount);
    }
    private void setup() throws SQLException, ClassNotFoundException, ViewConfigException, WebUtilException
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
        chnTbl = new ChannelTable(db);
        cidx = new ChannelIndex(db);
        cpt = new ChanPointerTable(db);

        ifoSubsysSet = chnTbl.buildIfoSubsysSet();

    }

    private void checkPtrDups() throws LdvTableException
    {
        ArrayList<Integer> ptrs = cpt.getAllChanIds();
        Collections.sort(ptrs);
        int oldId = -1;
        for(int p : ptrs)
        {
            if (p == oldId)
            {
                ChanInfo ci = chnTbl.getChanInfo(p);
                System.out.format("Duplicate pointer: %1$d - %2$s%n", p, ci.toString());
            }
            oldId = p;
        }
    }

}
