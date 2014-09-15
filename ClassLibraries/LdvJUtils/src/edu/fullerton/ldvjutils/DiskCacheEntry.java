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

package edu.fullerton.ldvjutils;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates entries in the DiskCacheAPI frame_cache_dump file
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class DiskCacheEntry 
{
    private String site;            // which site provided the frame_cache_dump
    private String directory;       // directory examined
    private String obsType;         // derived key
    private String observatory;
    private String type;
    private int secPerFile;         // length of the frame files
    private long lastScan;          // unix time stamp of last scan
    private int nfiles;             // number of files in directory
    private List<TimeInterval> times; // times covered by frames in this directory
    
    public DiskCacheEntry()
    {
        
    }
    public DiskCacheEntry (String site, String dir, String obsType, String obs, String type, int spf,
                           long lastScan, int nfiles, List<TimeInterval> times)
    {
        this.site = site;
        directory = dir;
        this.obsType = obsType;
        observatory = obs;
        this.type = type;
        secPerFile = spf;
        this.lastScan = lastScan;
        this.nfiles = nfiles;
        this.times = new ArrayList<>(times);
    }

    public String getSite()
    {
        return site;
    }

    public void setSite(String site)
    {
        this.site = site;
    }

    public String getDirectory()
    {
        return directory;
    }

    public void setDirectory(String directory)
    {
        this.directory = directory;
    }

    public String getObsType()
    {
        return obsType;
    }

    public void setObsType(String obsType)
    {
        this.obsType = obsType;
    }

    public String getObservatory()
    {
        return observatory;
    }

    public void setObservatory(String observatory)
    {
        this.observatory = observatory;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public int getSecPerFile()
    {
        return secPerFile;
    }

    public void setSecPerFile(int secPerFile)
    {
        this.secPerFile = secPerFile;
    }

    public long getLastScan()
    {
        return lastScan;
    }

    public void setLastScan(long lastScan)
    {
        this.lastScan = lastScan;
    }

    public int getNfiles()
    {
        return nfiles;
    }

    public void setNfiles(int nfiles)
    {
        this.nfiles = nfiles;
    }

    public List<TimeInterval> getTimes()
    {
        return times;
    }

    public void setTimes(List<TimeInterval> times)
    {
        this.times = times;
    }

}
