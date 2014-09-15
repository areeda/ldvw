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

package ndsJUtils;

import edu.fullerton.ldvjutils.TimeInterval;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Represents a single frame type, time interval from NDS2 channel list
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NDSChannelAvailability 
{
    private int idx;
    private boolean fromDb;
    private TimeInterval timeInterval;
    private String frameType;
    private String cType;
    private String dType;
    private Float fs;
    private int serverIdx;

    public NDSChannelAvailability()
    {
        idx = serverIdx = 0;
        frameType = cType = dType="";
        fs = 0f;
        timeInterval=new TimeInterval();
        fromDb = false;
    }
    
    public NDSChannelAvailability(int serverIdx, String cType, String dType, Float fs, 
                                  String frameType, TimeInterval ti)
    {
        idx = 0;
        this.cType = cType;
        this.dType = dType;
        this.fs = fs;
        this.frameType = frameType;
        this.serverIdx = serverIdx;
        this.timeInterval = new TimeInterval(ti);
    }
    
    public NDSChannelAvailability(ResultSet rs) throws SQLException
    {
        idx = rs.getInt("indexID");
         frameType = rs.getString("frameType");
        cType = rs.getString("chanType");
        dType = rs.getString("dataType");
        fs = rs.getFloat("fs");
        serverIdx = rs.getInt("server");
        long strtGps = rs.getLong("startGps");
        long stopGps = rs.getLong("stopGps");
        timeInterval = new TimeInterval(strtGps, stopGps);
    }
    @Override
    public boolean equals(Object t)
    {
        boolean ret = t.getClass().equals(getClass());
        NDSChannelAvailability it = (NDSChannelAvailability)t;
        if (ret)
        {
            ret = cType.contentEquals(it.cType);
        }
        if (ret)
        {
            ret = serverIdx == it.serverIdx;
        }
        if (ret)
        {
            ret = frameType.contentEquals(it.frameType);
        }
        if (ret)
        {
            ret = dType.contentEquals(it.dType);
        }
        if (ret)
        {
            ret = Math.abs(fs - it.fs) < 1e-4;
        }
        if (ret)
        {
            ret = timeInterval.equals(it.timeInterval);
        }
        return ret;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.timeInterval);
        hash = 29 * hash + Objects.hashCode(this.frameType);
        hash = 29 * hash + Objects.hashCode(this.cType);
        hash = 31 * hash + Objects.hashCode(fs);
        hash = 23 * hash + Objects.hashCode(this.dType);
        hash = 27 * hash + Objects.hashCode(this.serverIdx);
        return hash;
    }

    public int getIdx()
    {
        return idx;
    }

    public void setIdx(int idx)
    {
        this.idx = idx;
    }

    public TimeInterval getTimeInterval()
    {
        return timeInterval;
    }

    public void setTimeInterval(TimeInterval timeInterval)
    {
        this.timeInterval = timeInterval;
    }

    public String getFrameType()
    {
        return frameType;
    }

    public void setFrameType(String frameType)
    {
        this.frameType = frameType;
    }

    public String getcType()
    {
        return cType;
    }

    public void setcType(String cType)
    {
        this.cType = cType;
    }

    public int getBCType()
    {
        return NDSChanType.name2int(cType);
    }

    public int getServerIdx()
    {
        return serverIdx;
    }

    public void setServerIdx(int serverIdx)
    {
        this.serverIdx = serverIdx;
    }
    

    public String getdType()
    {
        return dType;
    }

    public void setdType(String dType)
    {
        this.dType = dType;
    }

    public Float getFs()
    {
        return fs;
    }

    public void setFs(Float fs)
    {
        this.fs = fs;
    }

    public boolean isFromDb()
    {
        return fromDb;
    }

    public void setFromDb(boolean fromDb)
    {
        this.fromDb = fromDb;
    }
    
}
