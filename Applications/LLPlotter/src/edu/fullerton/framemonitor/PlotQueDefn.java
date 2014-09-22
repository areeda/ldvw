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

package edu.fullerton.framemonitor;

/**
 * Base class for watcher or change listener
 * Descendents will pass new frame files to the plot routines
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PlotQueDefn 
{
    private String queName;
    private String frameType;
    private String obs;
    private String dirName;
    
    public PlotQueDefn()
    {
        queName = "";
        frameType = "";
        obs = "";
        dirName = "";
    }
    
    public PlotQueDefn (String queName, String frameType, String dirName)
    {
        this.queName = queName;
        this.frameType = frameType;
        this.dirName = dirName;
    }
    
    
    //=======Getters and Setters

    public String getQueName()
    {
        return queName;
    }

    public void setQueName(String queName)
    {
        this.queName = queName;
    }

    public String getFrameType()
    {
        return frameType;
    }

    public void setFrameType(String frameType)
    {
        this.frameType = frameType;
    }

    public String getObs()
    {
        return obs;
    }

    public void setObs(String obs)
    {
        this.obs = obs;
    }

    public String getDirName()
    {
        return dirName;
    }

    public void setDirName(String dirName)
    {
        this.dirName = dirName;
    }
    
}
