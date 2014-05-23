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

package edu.fullerton.ndsproxyclient;

import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;


import edu.fullerton.ldvjutils.TimeAndDate;

import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanSourceData
{
    private final TreeMap<String, ArrayList<TimeInterval>> frmTypes;
    private ChanInfo chanInfo;
    private final StringBuilder errors;
    private TreeSet<TimeInterval> mergedIntervals;
    private final int maxBins;    // for plotting availability
    private int[] bins;
    private TimeInterval timeRange;
    private int binSize;
    private long serverMin, serverMax;
    private int nRawIntervals;
    
    public ChanSourceData()

    {
        this.maxBins = 500;
        frmTypes = new TreeMap<>();
        errors = new StringBuilder();
        mergedIntervals = null;
        timeRange=null;
    }
    
    public void pullData(ChanInfo ci)
    {
        this.chanInfo = ci;
        NDSProxyClient nds = null;
        serverMax = serverMin = -1;
        nRawIntervals = 0;
        
        try
        {
            Pattern intervalPat = Pattern.compile("(.*):(\\d+)-(\\d+|all|inf)");
            Matcher intervalMat;

            nds = new NDSProxyClient(ci.getServer());
            nds.connect();
            String[] channelNames =
            {
                ci.getChanName() + "," + ci.getcType()
            };
            String channelSourceInfo = nds.getChannelSourceInfo(channelNames);
            int strt = channelSourceInfo.indexOf("{");
            long earliestCframeGps=TimeAndDate.utc2gps(System.currentTimeMillis() / 1000) - 28*24*3600;
            
            if (strt > 0 && strt < channelSourceInfo.length() - 2)
            {
                String[] intervals = channelSourceInfo.substring(strt + 1).split(" ");

                for (String interval : intervals)
                {
                    intervalMat = intervalPat.matcher(interval);
                    if (intervalMat.find())
                    {
                        String frameType = intervalMat.group(1);
                        String strtStr = intervalMat.group(2);
                        String stopStr = intervalMat.group(3);
                        long strtGps = Long.parseLong(strtStr);
                        long stopGps;
                        if (stopStr.trim().matches("^\\d+$"))
                        {
                            stopGps = Long.parseLong(stopStr);
                        }
                        else if (stopStr.equalsIgnoreCase("inf"))
                        {
                            stopGps = TimeAndDate.utc2gps(System.currentTimeMillis() / 1000) - 600;
                        }
                        else
                        {
                            errors.append("I didn't undertstand this interval: ").append(interval);
                            errors.append("\n");
                            continue;
                        }
                        if (nRawIntervals == 0)
                        {
                            serverMax = stopGps;
                            serverMin = strtGps;
                        }
                        else
                        {
                            serverMax = Math.max(stopGps, serverMax);
                            serverMin = Math.min(strtGps, serverMin);
                        }
                        nRawIntervals++;
                        
                        if (stopGps < strtGps)
                        {
                            errors.append(String.format("Interval stop < start %1$s", interval));
                        }
                        else
                        {
                            strtGps = Math.max(strtGps, 815153408); // ignore data before start of S5
                            if (frameType.contentEquals("H-H1_C") || frameType.contentEquals("L-L1_C"))
                            {   // commissioning frames go away in about a month but the server doesn't know that
                                strtGps = Math.max(strtGps, earliestCframeGps);
                            }
                            addInterval(frameType, strtGps, stopGps);
                        }

                    }
                    else
                    {
                        errors.append("I didn't undertstand this interval: ").append(interval);
                        errors.append("\n");
                    }
                }

            }
            else
            {
                errors.append("I didn't understand this server response: ").append(channelSourceInfo);
                errors.append("\n");
            }
        }
        catch (NDSException ex)
        {
            String ermsg = String.format("Error getting source list for %1$s,%2$s at %3$s. %4$s: %5$s",
                                         ci.getChanName(), ci.getcType(), ci.getServer(),
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            errors.append(ermsg);
            errors.append("\n");
        }
        finally
        {
            if (nds != null)
            {
                try
                {
                    nds.bye();
                }
                catch (NDSException ex)
                {

                }
            }
        }

    }
    private void addInterval(String frameType, long strtGps, long stopGps)
    {
        ArrayList<TimeInterval> times;
        if (frmTypes.containsKey(frameType))
        {
            times = frmTypes.get(frameType);
        }
        else
        {
            times = new ArrayList<>();
        }
        times.add(new TimeInterval(strtGps, stopGps));
        frmTypes.put(frameType, times);
    }
    public void mergeIntervals() throws LdvTableException
    {
        TimeInterval fullRange = new TimeInterval(0, 1800000000);
        mergeIntervals(fullRange);
    }
    public void mergeIntervals(TimeInterval searchRange) throws LdvTableException
    {
        if (mergedIntervals == null)
        {
            mergedIntervals = new TreeSet<>();
            TreeSet<TimeInterval> allIntervals = new TreeSet<>();
            for ( Map.Entry<String, ArrayList<TimeInterval>> ent : frmTypes.entrySet())
            {
                allIntervals.addAll(ent.getValue());
            }
            TimeInterval lastTi = null;
            long srchStrt = searchRange.getStartGps();
            long srchStop = searchRange.getStopGps();
            long minGps = Long.MAX_VALUE;
            long maxGps = Long.MIN_VALUE;
            for(TimeInterval tti : allIntervals)
            {
                TimeInterval ti = null;
                long tstrt = tti.getStartGps();
                long tstop = tti.getStopGps();
                if (tstrt <= srchStop && tstop >= srchStrt)
                {
                    // interval overlaps range
                    ti = new TimeInterval(Math.max(srchStrt, tstrt), Math.min(srchStop,tstop));
                }
                if (ti != null)
                {
                    minGps = Math.min(minGps, ti.getStartGps());
                    maxGps = Math.max( maxGps, ti.getStopGps());
                }
                if (lastTi != null && ti != null)
                {
                    if (!ti.overlaps(lastTi))
                    {
                        mergedIntervals.add(lastTi);
                        lastTi = ti;
                    }
                    else
                    {
                        lastTi = lastTi.mergeIntervals(ti);
                    }
                }
                else
                {
                    lastTi = ti;
                }
            }
            if (lastTi != null)
            {
                mergedIntervals.add(lastTi);
            }
            if (minGps <= searchRange.getStartGps() && maxGps >= searchRange.getStopGps())
            {
                timeRange = new TimeInterval(minGps, maxGps);
            }
            else
            {
                timeRange = null;
            }
        }
    }
    public String getErrors()
    {
        return errors.toString();
    }

    public TreeSet<TimeInterval> getMergedIntervals()
    {
        if (mergedIntervals == null)
        {
            try
            {
                mergeIntervals();
            }
            catch (LdvTableException ex)
            {
                mergedIntervals = null;
            }
        }
        return mergedIntervals;
    }

    public ChanInfo getChanInfo()
    {
        return chanInfo;
    }
    
    public String[] getFrameTypes()
    {
        Object[] keys = frmTypes.keySet().toArray();
        String[] ret = new String[keys.length];
        for(int i=0; i<keys.length; i++)
        {
            ret[i] = (String)keys[i];
        }
        return ret;
    }
    
    public void calcGraphData() throws LdvTableException
    {
        mergeIntervals();
        if (!mergedIntervals.isEmpty())
        {
            TimeInterval first = mergedIntervals.first();
            TimeInterval last = mergedIntervals.last();
            long startGps = first.getStartGps();
            long stopGps = last.getStopGps();
            timeRange = new TimeInterval(startGps, stopGps);
            long len = stopGps-startGps;
            int nbins = (int) (len < maxBins ? len : maxBins);
            binSize = (int) Math.ceil((float) len / nbins);
            bins = new int[nbins];
            
            
            for(TimeInterval ti : mergedIntervals)
            {
                long iStrt = ti.getStartGps()- startGps;
                long iStop = ti.getStopGps()- startGps;
                if (iStrt < 0 || iStop > len)
                {
                    throw new LdvTableException("logic error in calcGraphData, start stop values are wrong");
                }
                
                int binNum = (int) (iStrt/binSize);
                binNum = binNum < nbins ? binNum : nbins - 1;
                long binStrt;
                long binStop;
                binStrt = binNum * binSize;
                binStop = binStrt + binSize - 1;
                
                while (iStrt < binStop && iStop >= binStrt)
                {
                    if (iStrt <= binStrt && iStop >= binStop)
                    {   // interval covers whole bin
                        bins[binNum] += binSize;
                        iStrt = binStop;
                    }
                    else if (iStrt <= binStrt && iStop < binStop)
                    {   // interval covers start of bin but not stop
                        bins[binNum] += iStop - binStrt;
                        iStrt = binStop;
                    }
                    else if (iStrt > binStrt && iStop >= binStop)
                    {   // interval covers stop but not start
                        bins[binNum] += binStop - iStrt;
                        iStrt = binStop;
                    }
                    else
                    {   // otherwise the entire interval is in this bin
                        bins[binNum] += iStop - iStrt;
                        iStrt = binStop;
                    }
                    binNum++;   // see if next bin is also affected by this interval
                    binNum = binNum < nbins ? binNum : nbins - 1;
                    binStrt = binNum * binSize;
                    binStop = binStrt + binSize - 1;
                }
            }
        }
    }
    public double[][] getGraphData()
    {
        double[][] ret=null;
        if (bins != null)
        {

            ret = new double[bins.length][2];
            for(int i=0; i< bins.length;i++)
            {
                ret[i][0] = binSize * i +  timeRange.getStartGps();
                ret[i][1] = bins[i] *100. / binSize;
            }
        }
        return ret;
    }
    public String getJson()
    {
        return "";
        
    }

    public TimeInterval getTimeRange()
    {
        return timeRange;
    }

    public long getServerMin()
    {
        return serverMin;
    }

    public long getServerMax()
    {
        return serverMax;
    }

    public int getnRawIntervals()
    {
        return nRawIntervals;
    }
    
}
