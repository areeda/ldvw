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
package edu.fullerton.ldvtables;

import edu.fullerton.ldvjutils.TimeAndDate;

/**
 * represents a requested time interval or an entry available in data cache
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class TimeInterval
{
    private long startGps;
    private long stopGps;
    private int cacheId;
    private long dataLength;
    
    /**
     * construct a new time interval representing a request
     * @param start starting gps time in seconds
     * @param stop stopping gps time in seconds
     */
    public TimeInterval(long start, long stop)
    {
        startGps = start;
        stopGps = stop;
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
        startGps = start;
        stopGps = stop;
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
     * set stop time, next seconds after last sample
     * @param stopGps stop time in gps seconds
     */
    public void setStopGps(long stopGps)
    {
        this.stopGps = stopGps;
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
}
