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
package edu.fullerton.ldvjutils;

/**
 * represents a requested time interval or an entry available in data cache
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class TimeInterval implements Comparable<TimeInterval>
{
    private long startGps;
    private long stopGps;
    private int cacheId;
    private long dataLength;
    
    public TimeInterval()
    {
        startGps = stopGps = dataLength = 0;
        cacheId = 0;
    }
    /**
     * construct a new time interval representing a request
     * @param start starting gps time in seconds
     * @param stop stopping gps time in seconds
     */
    public TimeInterval(long start, long stop)
    {
        startGps = Math.min(start, stop);
        stopGps = Math.max(start, stop);
        cacheId = 0;
        dataLength = 0;
    }

    /**
     * construct a new time interval representing a cached buffer
     * @param start starting gps time in seconds
     * @param stop stopping gps time in seconds
     * @param cacheId database row id
     * @param dataLength number of samples (not bytes) in buffered row
     */
    public TimeInterval(long start, long stop, int cacheId, long dataLength)
    {
        startGps = Math.min(start, stop);
        stopGps = Math.max(start, stop);
        this.cacheId = cacheId;
        this.dataLength = dataLength;
    }
    /**
     * Copy constructor, creates a duplicate object when a reference just won't do
     * 
     * @param ti the existing TimeInterval to copy
     */
    public TimeInterval(TimeInterval ti)
    {
        startGps = ti.getStartGps();
        stopGps = ti.getStopGps();
        checkTimes();
        cacheId = ti.getCacheId();
        dataLength = ti.getDataLength();
    }
    /**
     * get time of first sample in the interval
     * @return starting gps time in seconds
     */
    public long getStartGps()
    {
        return startGps;
    }

    /**
     * set time of first sample in interval
     * @param startGps gps time in seconds
     */
    public void setStartGps(long startGps)
    {
        this.startGps = startGps;
        checkTimes();
    }

    /**
     * get stop time, next second after last sample
     * @return stop time in gps seconds
     */
    public long getStopGps()
    {
        return stopGps;
    }

    /**
     * get the duration specified by interval in seconds
     * 
     * @return number of seconds in the interval
     */
    public long getDuration()
    {
        return stopGps-startGps;
    }
    /**
     * set stop time, next seconds after last sample
     * @param stopGps stop time in gps seconds
     */
    public void setStopGps(long stopGps)
    {
        this.stopGps = stopGps;
        checkTimes();
    }

    public long getAge()
    {
        long now = TimeAndDate.nowAsGPS();
        return now - stopGps;
    }
    /**
     * get the row id of this record in database
     * @return row id
     */
    public int getCacheId()
    {
        return cacheId;
    }

    /**
     * set row id of this record in database
     * @param cacheId row id
     */
    public void setCacheId(int cacheId)
    {
        this.cacheId = cacheId;
    }

    /**
     * get length of buffer in database in samples (not bytes)
     * @return number of samples in this database record
     */
    public long getDataLength()
    {
        return dataLength;
    }

    /**
     * set length of buffer in database in samples (not bytes)
     * @param dataLength number of samples in this database record
     */
    public void setDataLength(long dataLength)
    {
        this.dataLength = dataLength;
    }
    
    public String getTimeDescription()
    {
        String ret;
        long strt = getStartGps();
        ret = String.format("From %1$s (%2$d) ", TimeAndDate.gpsAsUtcString(strt), strt);

        long stop = getStopGps();
        ret += String.format(" to %1$s (%2$d)", TimeAndDate.gpsAsUtcString(stop), stop);

        ret += String.format(" duration %1$s.", TimeAndDate.hrTime(stop - strt));
        return ret;
    }

    /**
     * Short description for standard utilities
     * @return a short formatted description mainly for the debugger
     */
    @Override
    public String toString()
    {
        String ret;
        long strt = getStartGps();
        ret = String.format("From %1$s ", TimeAndDate.gpsAsUtcString(strt));

        long stop = getStopGps();

        ret += String.format("(%1$s)", TimeAndDate.hrTime(stop - strt));
        return ret;
    }

    @Override
    public int compareTo(TimeInterval t)
    {
        int ret = 0;
        if (getStartGps() < t.getStartGps())
        {
            ret = -1;
        }
        else if (getStartGps() > t.getStartGps())
        {
            ret = 1;
        }
        else
        {
            if (getStopGps() < t.getStopGps())
            {
                ret = -1;
            }
            else if (getStopGps() > t.getStopGps())
            {
                ret = 1;
            }
        }
        return ret;
    }
    public boolean equals(Object t)
    {
        boolean ret = t.getClass().equals(getClass());
        TimeInterval it = (TimeInterval) t;
        if (ret)
        {
            ret = it.startGps == startGps && it.stopGps == stopGps;
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 61 * hash + (int) (this.startGps ^ (this.startGps >>> 32));
        hash = 61 * hash + (int) (this.stopGps ^ (this.stopGps >>> 32));
        return hash;
    }
    
    /**
     * test if 2 time interval overlap or abut
     * @param ti the other time interval
     * @return true if they overlap
     */
    public boolean overlaps(TimeInterval ti)
    {
        boolean ret = false;
        long ostrt = ti.getStartGps();
        long ostop = ti.getStopGps();
        ret |= startGps >= ostrt && startGps <= ostop;
        ret |= stopGps >= ostrt && stopGps <= ostop;
        ret |= startGps <= ostrt && stopGps >= ostop;
        return ret;
    }
    /**
     * Merge two overlapping time intervals
     * @param ti the other time interval
     * @return the combined interval
     * @throws LdvTableException if they do not overlap
     */
    public TimeInterval mergeIntervals(TimeInterval ti) throws LdvTableException
    {
        if (!overlaps(ti))
        {
            String ermsg = String.format("Time intervals cannot be merged because they "
                    + "do not overlap: %1$s and %2$s", toString(), ti.toString());
            throw new LdvTableException(ermsg);
        }
        long start = Math.min(startGps, ti.getStartGps());
        long stop = Math.max(stopGps, ti.getStopGps());
        TimeInterval ret = new TimeInterval(start, stop);
        return ret;
    }

    private void checkTimes()
    {
        if (startGps > stopGps)
        {
            long t=startGps;
            startGps = stopGps;
            stopGps = t;
        }
    }
}
