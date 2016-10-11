/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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
package chanavailability;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChannelIndex;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * Update the database with channel availability at a particular time
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanAvailability
{
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        ChanAvailability me = new ChanAvailability();
        me.processArgs(args);
        me.getLists();
        if (me.curChans.size() > 0)
        {
            me.updateChannelIndex();
        }
    }

    private Long requestedTime;
    private String chanList;

    public ArrayList<String> curChans;
    private final int verbose=6;
    
    /**
     * Stub for command line argument processing
     * @param args as passed to main
     */
    private void processArgs(String[] args)
    {
        requestedTime = TimeAndDate.nowAsGPS();
        chanList = "/tmp/all.clist";
    }

    /**
     * We used nds_query to simultaneously get full channel lists from all sites.
     * Here we read in the results
     */
    private void getLists()
    {
        BufferedReader in = null;
        curChans = new ArrayList<>(200000);
        try
        {
            in = new BufferedReader(new FileReader(chanList));            
            String line;
            while ((line = in.readLine()) != null)
            {
               curChans.add(line);
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(ChanAvailability.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            try
            {
                if (in != null)
                {
                    in.close();
                }
            }
            catch (IOException ex)
            {
                Logger.getLogger(ChanAvailability.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (verbose > 1)
        {
            System.out.format("%1$,d current channels", curChans.size());
        }
    }

    private void updateChannelIndex() 
    {
        try
        {
            ViewerConfig vc;
            vc = new ViewerConfig();
            
            Database db = vc.getDb();
            if (verbose > 1)
            {
                System.out.print("Connected to: ");
                System.out.println(vc.getLog());
            }
            updateByName(db);
        }
        catch (ViewConfigException ex)
        {
            Logger.getLogger(ChanAvailability.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    void updateById(Database db)
    {
        try
        {
            ArrayList<Integer> activeList = new ArrayList<>(200000);
            
            ChannelIndex index = new ChannelIndex(db);
            index.streamAll();
            
            ChanIndexInfo cInfo = index.streamNext();
            Integer activePtr = 0;
            String curName = curChans.get(activePtr);
            
            while(cInfo != null && activePtr < curChans.size() && curName != null)
            {
                String cName =  cInfo.getName();
                curName = curChans.get(activePtr);
                int cmp = cName.compareTo(curName);
                if (cmp == 0)
                {
                    activeList.add(cInfo.getIndexID());
                    activePtr++;
                    cInfo = index.streamNext();
                }
                else if (cmp < 0)
                {
                    cInfo = index.streamNext();
                }
                else
                {
                    activePtr++;
                }
            }
            index.streamClose();
            if (verbose > 1)
            {
                System.out.format("%1$d to be updated%n", activeList.size());
            }
            
            // set them all not current
            String clrCmd = String.format("update %s set isCurrent=0", index.getName());
            db.execute(clrCmd);
            
            StringBuilder updtList = new StringBuilder();
            int curUpdt = 0;
            int maxUpdt = 10000;
            
            String updtPrefix = String.format("update %s set isCurrent=1 where ", index.getName());
            StringBuilder updtCmd = new StringBuilder();
            for(Integer id : activeList)
            {
                if (updtList.length() > 0)
                {
                    updtList.append(" or ");
                }
                updtList.append(String.format("indexID=%1$d", id));
                curUpdt++;
                if (curUpdt >= maxUpdt)
                {
                    updtCmd.setLength(0);
                    updtCmd.append(updtPrefix).append(updtList);
                    db.execute(updtCmd.toString());
                    curUpdt = 0;
                    updtList.setLength(0);
                }
            }
            // if any are left in buffer clear them
            if (updtList.length() > 0)
            {
                updtCmd.setLength(0);
                updtCmd.append(updtPrefix).append(updtList);
                db.execute(updtCmd.toString());                
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChanAvailability.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void updateByName(Database db)
    {
        
        try
        {
            ChannelIndex index = new ChannelIndex(db);
            
            // set them all not current
            String clrCmd = String.format("update %s set isCurrent=0", index.getName());
            db.execute(clrCmd);

            StringBuilder updtList = new StringBuilder();
            int curUpdt = 0;
            int maxUpdt = 10000;

            String updtPrefix = String.format("update %s set isCurrent=1 where ", index.getName());
            StringBuilder updtCmd = new StringBuilder();
            for (String name :curChans)
            {
                if (updtList.length() > 0)
                {
                    updtList.append(" or ");
                }
                updtList.append("name='").append(name).append("' ");
                curUpdt++;
                if (curUpdt >= maxUpdt)
                {
                    updtCmd.setLength(0);
                    updtCmd.append(updtPrefix).append(updtList);
                    db.execute(updtCmd.toString());
                    curUpdt = 0;
                    updtList.setLength(0);
                }
            }
            // if any are left in buffer clear them
            if (updtList.length() > 0)
            {
                updtCmd.setLength(0);
                updtCmd.append(updtPrefix).append(updtList);
                db.execute(updtCmd.toString());
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChanAvailability.class.getName()).log(Level.SEVERE, null, ex);
        }


    }
    
}
