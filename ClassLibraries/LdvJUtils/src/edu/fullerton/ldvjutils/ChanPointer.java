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
package edu.fullerton.ldvjutils;

/**
 * encapsulates a row in the Channel pointer table
 * Enough information about sub-channels for quick selection
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanPointer
{
    private Integer indexID;    // primary key in the ChannelIndex table
    private Integer myId;       // primate key in Channels table
    private String cType;       // channel type (raw, rds, mintue-trend...
    private String trendType;   // min, max ...
    private Float sampleRate;
    private final static String fldNames = "( indexID, myId, cType, trendType, sampleRate )";
    public ChanPointer()
    {
        indexID = 0;
        myId = 0;
        cType = "";
        trendType = "";
        sampleRate = 0.f;
    }
    public String getSqlFieldNames()
    {
        return fldNames;
    }

    public String getSqlFieldValues()
    {
        String ret = String.format("( %1$d, %2$d, '%3$s', '%4$s', %5$.5f )",
                                   indexID, myId, cType, trendType, sampleRate);
        return ret;
    }

    public Integer getIndexID()
    {
        return indexID;
    }

    public void setIndexID(Integer indexID)
    {
        this.indexID = indexID;
    }

    public Integer getMyID()
    {
        return myId;
    }

    public void setMyID(Integer myID)
    {
        this.myId = myID;
    }

    public String getcType()
    {
        return cType;
    }

    public void setcType(String cType)
    {
        this.cType = cType;
    }

    public String getTrendType()
    {
        return trendType;
    }

    public void setTrendType(String trendType)
    {
        this.trendType = trendType;
    }

    public Float getSampleRate()
    {
        return sampleRate;
    }

    public void setSampleRate(Float sampleRate)
    {
        this.sampleRate = sampleRate;
    }

    
}
