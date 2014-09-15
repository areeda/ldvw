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

import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeInterval;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Encapsulates part of the frame_cache_dump file reformatted for fast searching
 * of availability.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class FrameAvailability implements Comparable
{
    private int myId;                   // record Id
    private String site;                // where are the frames located
    private String obsType;             // combination of observatory and frame type
    private TimeInterval availability;  // one time interval
    
    public FrameAvailability()
    {
        
    }
    public FrameAvailability(String site, String obsType, TimeInterval availability)
    {
        this.site = site;
        this.obsType = obsType;
        this.availability = availability;
    }

    @Override
    public int compareTo(Object t)
    {
        int ret = obsType.compareTo(((FrameAvailability)t).obsType);
        if (ret == 0)
        {
            ret = availability.compareTo(((FrameAvailability)t).availability);
        }
        return ret;
    }

    /**
     * Compares only the significant members for equality
     * 
     * @param t
     * @return
     */
    @Override
    public boolean equals(Object t)
    {
        boolean ret = false;
        if (t.getClass().equals(this.getClass()))
        {
            ret = compareTo(t) == 0;
        }
        return ret;
    }
    public boolean overlaps(FrameAvailability fa)
    {
        return availability.overlaps(fa.availability);
    }

    /**
     * Merge the new time interval into ours
     * @param fa
     * @throws LdvTableException 
     */
    public void merge(FrameAvailability fa) throws LdvTableException
    {
        if (!obsType.contentEquals(fa.obsType))
        {
            String ermsg = String.format("Attempt to merge two frame availability objects with "
                    + "differing frame types %1$s, %2$s", obsType, fa.obsType);
            throw new LdvTableException(ermsg);
        }
        availability = availability.mergeIntervals(fa.availability);
    }
    
    public String getSqlFieldValues()
    {
        String ret = String.format("('%1$s', '%2$s', %3$d, %4$d)", site, obsType, 
                                   availability.getStartGps(), availability.getStopGps());
        return ret;
    }
    /**
     * Use a result set to fill
     * @param rs
     * @throws SQLException 
     */
    public void fill(ResultSet rs) throws SQLException
    {
        myId = rs.getInt("myId");
        site = rs.getString("site");
        obsType = rs.getString("obsType");
        availability = new TimeInterval(rs.getLong("startgps"), rs.getLong("stopgps"));
    }

    public int getMyId()
    {
        return myId;
    }

    public void setMyId(int myId)
    {
        this.myId = myId;
    }

    public String getSite()
    {
        return site;
    }

    public void setSite(String site)
    {
        this.site = site;
    }

    public String getObsType()
    {
        return obsType;
    }

    public void setObsType(String obsType)
    {
        this.obsType = obsType;
    }

    public TimeInterval getAvailability()
    {
        return availability;
    }

    public void setAvailability(TimeInterval availability)
    {
        this.availability = availability;
    }
}
