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
package datacachemanager;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.DataCacheEntry;
import edu.fullerton.ldvtables.DataTable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * A program that runs in the background (probably cron managed) to clear old cache entries based on
 * some semi intelligent criteria
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class DataCacheManager
{
    // 
    long maxAgeHr = 24*7;
    long giga = 1024L * 1024L * 1024L;
    long maxSizeBytes = 2 * giga;
    long maxN = 1000;
    boolean verbose = true;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        DataCacheManager dcm = new DataCacheManager();
        dcm.cleanDb();
    }
    private Database db;
    private DataTable dcTbl;

    private void cleanDb()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        GregorianCalendar date;
        
        try
        {
            if (verbose)
            {
                System.out.println("===========================");
                date = new GregorianCalendar();
                String dateTime = sdf.format(date.getTime());
                System.out.format("%1$s %2$s%n", "Starting dataCacheManager", dateTime);
            }
            setup();
            TreeSet<DataCacheEntry> entries = dcTbl.getCacheEntries();
            long initSize = 0;
            for (DataCacheEntry dce : entries)
            {
                initSize += dce.getDataLength();
            }
            if (verbose)
            {
                date = new GregorianCalendar();
                String dateTime = sdf.format(date.getTime());
                String hrsize = String.format("%1$.1fMB", initSize/1000000.);
                System.out.format("%1$s There are %2$d entries using %3$s%n", dateTime, entries.size(),hrsize);
            }
            TreeSet<DataCacheEntry> deletions = new TreeSet<DataCacheEntry>();
            
            int nTooOld =0;
            int nTooMany = 0;
            int nTooMuch = 0;
            
            // first select the ones that are too old
            for(DataCacheEntry dce : entries)
            {
                if (dce.getAgeHr() > maxAgeHr)
                {
                    deletions.add(dce);
                    nTooOld ++;
                }
            }
            entries.removeAll(deletions);
            
            // if we have too many entries get rid of the oldest ones
            int n = entries.size();
            for(long i=maxN;i<n; i++)
            {
                DataCacheEntry dce = entries.last();
                deletions.add(dce);
                
                nTooMany++;
            }
            entries.removeAll(deletions);
            // How much space are we using
            long totalSize = 0;
            for(DataCacheEntry dce : entries)
            {
                totalSize += dce.getDataLength();
            }
            long curSize = totalSize;
            while(curSize > maxSizeBytes)
            {
                DataCacheEntry dce = entries.last();
                deletions.add(dce);
                curSize -= dce.getDataLength();
                nTooMuch++;
            }
            entries.removeAll(deletions);
            if (verbose)
            {
                date = new GregorianCalendar();
                String dateTime = sdf.format(date.getTime());
                System.out.format("%1$s ", dateTime);
                String summary;
                summary = String.format("DataCacheManage: %1$d deletions, age: %2$d, num: %3$d, size: %4$d",
                                        deletions.size(), nTooOld, nTooMany, nTooMuch);
                System.out.println(summary);
            }
            
            dcTbl.delete(deletions);
        }   
        catch (Exception ex)
        {
            Logger.getLogger(DataCacheManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        date = new GregorianCalendar();
        String dateTime = sdf.format(date.getTime());
        System.out.format("%1$s %2$s%n", "Finished", dateTime);
    }

    private void setup() throws LdvTableException, SQLException, ViewConfigException
    {
        if (db == null)
        {
            ViewerConfig vc = new ViewerConfig();
            db = vc.getDb();
            if (db == null)
            {
                throw new LdvTableException("Can't connect to LigoDV-web database");
            }
        }
        if (dcTbl == null)
        {
            dcTbl = new DataTable(db);
        }

    }

    
}
