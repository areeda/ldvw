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
import edu.fullerton.ldvjutils.DiskCacheEntry;
import ndsJUtils.FrameAvailability;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ndstables.FrameAvailabilityTable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import viewerconfig.DbSupport;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class UpdateFrameAvailability 
{
    private final Pattern diskCachePat;
    private final Pattern longPat;
    private int errCnt;
    private final ArrayList<DiskCacheEntry> cache;
    private final HashMap<String, TreeSet<FrameAvailability>> avail;
    private final int verbose;
    private final ViewerConfig viewerConfig;
    
    public UpdateFrameAvailability(int verbose)
    {
        this.verbose=1;
        diskCachePat = Pattern.compile("(.*),(.+),(.+),(\\d+),(\\d+) (\\d+) (\\d+) \\{(.+)\\}");
        longPat = Pattern.compile("\\d+");
        cache = new ArrayList<>();
        avail = new HashMap<>();
        viewerConfig = new ViewerConfig();
    }
    public void updateFromCacheDump(String site, String dir)
    {
        try
        {
            File diskCache = new File(dir + "/frame_cache_dump");
            if (!diskCache.canRead())
            {
                System.err.println("Frame cache: " + diskCache.getCanonicalPath() + " cannot be read.");
            }
            else
            {
                importDiskCache(site, diskCache);
                buildAvailability(site);
                updateAvailabilityDb(site);
            }
        }
        catch (LdvTableException | IOException ex)
        {
            System.err.println("Error on disk cache import: " + ex.getLocalizedMessage());
        }
    }
    private void importDiskCache(String site, File diskCache) throws FileNotFoundException, IOException
    {
        long strt = System.currentTimeMillis();
        BufferedReader br = new BufferedReader(new FileReader(diskCache));
        String line;
        int lcount = 0;
        errCnt = 0;
        Matcher m;
        String dir;    // directory with frame files
        String obs;    // observatory
        String typ;    // frame type
        String unk;    // we think that may be frames per file, always 1
        String spf;    // seconds/file as string
        String lup;    // last update (scan) of directory
        String nfl;    // number of frame files in this directory
        String tms;    // list of times for frames

        while ((line = br.readLine()) != null)
        {
            line = line.trim();
            if (!line.isEmpty())
            {
                lcount++;
                m = diskCachePat.matcher(line);
                if (m.find())
                {
                    dir = m.group(1);    // directory with frame files
                    obs = m.group(2);    // observatory
                    typ = m.group(3);    // frame type
                    unk = m.group(4);    // we think that may be frames per file, always 1
                    spf = m.group(5);    // seconds/file as string
                    lup = m.group(6);    // last update (scan) of directory
                    nfl = m.group(7);    // number of frame files in this directory
                    tms = m.group(8);    // list of times for frames

                    String obsType = obs + "-" + typ; // this will be our main key for searching
                    int secPframe = getInt(lcount, "Seconds per frame", spf);
                    long lastScan = getLong(lcount, "last dir scan", lup);
                    int nFiles = getInt(lcount, "Num. files in dir", nfl);
                    List<TimeInterval> times = getTimes(lcount, "Time interval(s)", tms);

                    DiskCacheEntry dce = new DiskCacheEntry(site, dir, obsType, obs, typ, nFiles,
                                                            lastScan, nFiles, times);
                    cache.add(dce);
                }
                else
                {
                    errCnt++;
                    System.err.println("Error parsing line: " + line);
                }
            }
        }
        double elap = (System.currentTimeMillis() - strt) / 1000.;

        if (verbose > 0)
        {
            System.out.format("Frame cache. Dir: %1$s, entries: %2$,d, errors: %3$d, time: %4$.1f sec.%n",
                              diskCache.getParent(), lcount, errCnt, elap);
        }
    }

    private int getInt(int lcount, String fieldName, String inStr)
    {
        long t = getLong(lcount, fieldName, inStr);
        if (t < Integer.MIN_VALUE || t > Integer.MAX_VALUE)
        {
            error(lcount, fieldName, "exceeds integer range");
        }
        return 0;
    }

    private long getLong(int lcount, String fieldName, String inStr)
    {
        long ret = 0;
        if (inStr.matches("^\\d+"))
        {
            ret = Long.parseLong(inStr);
        }
        else
        {
            error(lcount, fieldName, "not an integer string");
        }
        return ret;
    }

    private List<TimeInterval> getTimes(int lcount, String fieldName, String tms)
    {
        Matcher m = longPat.matcher(tms);
        ArrayList<TimeInterval> ret = new ArrayList<>();
        while (m.find())
        {
            String s1 = m.group(0);
            if (m.find())
            {
                String s2 = m.group(0);
                long t1 = getLong(lcount, fieldName, s1);
                long t2 = getLong(lcount, fieldName, s2);
                if (t1 > 0 && t2 > 0)
                {
                    TimeInterval ti = new TimeInterval(t1, t2);
                    ret.add(ti);
                }
            }
            else
            {
                error(lcount, fieldName, "odd number of values detected");
            }
        }
        if (ret.isEmpty())
        {
            error(lcount, fieldName, "no intervals found");
        }
        return ret;
    }

    private void error(int lcount, String fieldName, String problem)
    {
        System.err.format("Error: line: %1$d, field: %2$s, problem: %3$s%n", lcount, fieldName, problem);
        errCnt++;
    }

    /**
     * Regardless of directory merge all time intervals available for each obsType
     */
    private void buildAvailability(String site) throws LdvTableException
    {

        String obsType;
        int tiCount = 0;    // number of time intervals we start with

        // this pass builds a list of possible overlapping (but no duplicate) time interval
        // for each obsType at this site
        for (DiskCacheEntry dce : cache)
        {
            obsType = dce.getObsType();
            TreeSet<FrameAvailability> faList;
            if (avail.containsKey(obsType))
            {
                faList = avail.get(obsType);
            }
            else
            {
                faList = new TreeSet<>();
                avail.put(obsType, faList);
            }
            for (TimeInterval ti : dce.getTimes())
            {
                FrameAvailability fa = new FrameAvailability(site, obsType, ti);
                faList.add(fa);
                tiCount++;
            }
            avail.put(obsType, faList);
        }
        if (verbose > 0)
        {
            System.out.format("Site: %1$s, ObsType count: %2$,d, Time interval count: %3$,d%n",
                              site, avail.size(), tiCount);
        }
        int newIntervalCount = 0;
        for (Map.Entry<String, TreeSet<FrameAvailability>> ent : avail.entrySet())
        {
            TreeSet<FrameAvailability> fa = ent.getValue();
            TreeSet<FrameAvailability> newFa = mergeFrameAvailability(fa);
            if (newFa.size() < fa.size())
            {
                avail.put(ent.getKey(), newFa);
                newIntervalCount += newFa.size();
            }
            else
            {
                newIntervalCount += fa.size();
            }
        }
        if (verbose > 0 && newIntervalCount != tiCount)
        {
            System.out.format("Merge complete. Old: %1$,d, new: %2$,d%n", tiCount, newIntervalCount);
        }
    }

    /**
     * merge all time intervals that abut or overlap
     *
     * @param fa
     */
    private TreeSet<FrameAvailability> mergeFrameAvailability(TreeSet<FrameAvailability> faSet) throws LdvTableException
    {
        TreeSet<FrameAvailability> ret = new TreeSet<>();
        FrameAvailability oldFa = null;
        for (FrameAvailability fa : faSet)
        {
            if (oldFa == null)
            {
                oldFa = fa;
            }
            else
            {
                if (oldFa.overlaps(fa))
                {
                    oldFa.merge(fa);
                }
                else
                {
                    ret.add(oldFa);
                    oldFa = fa;
                }
            }
        }
        if (oldFa != null)
        {
            ret.add(oldFa);
        }
        return ret;
    }

    private void updateAvailabilityDb(String site) throws LdvTableException
    {
        try
        {
            String[] tableClassNames =
            {
                "edu.fullerton.ndstables.FrameAvailabilityTable"
            };
            Database db = viewerConfig.getDb("nds");
            DbSupport dbs = new DbSupport();
            dbs.checkDb(db, tableClassNames);
            FrameAvailabilityTable fat = new FrameAvailabilityTable(db);

            for (Map.Entry<String, TreeSet<FrameAvailability>> ent : avail.entrySet())
            {
                Set<FrameAvailability> newFa = ent.getValue();
                String obsType = ent.getKey();

                // find the common entries in old and new
                Set<FrameAvailability> oldFa = fat.getAsSet(site, obsType);
                Set<FrameAvailability> commonFa = new TreeSet<>(oldFa);
                commonFa.retainAll(newFa);

                // At this point we have three sets.  
                // oldFa is the one in the database
                // newFa is the one we just read in from the frame_cache_dump
                // commonFa are the ones that are exactly the same (these are the entries we shouldn't touch
                // oldFa - commonFa are the ones to delete
                // newFa - commonFa are the ones to add
                Set<FrameAvailability> delFa = new TreeSet<>(oldFa);
                delFa.removeAll(commonFa);
                for (FrameAvailability fa : delFa)
                {
                    fat.deleteById(fa.getMyId());
                }
                newFa.removeAll(commonFa);
                for (FrameAvailability fa : newFa)
                {
                    fat.insertNewBulk(fa);
                }
                fat.insertNewBulk(null);
            }
        }
        catch (SQLException | ViewConfigException ex)
        {
            throw new LdvTableException("Opening nds database: ", ex);
        }
    }

}
