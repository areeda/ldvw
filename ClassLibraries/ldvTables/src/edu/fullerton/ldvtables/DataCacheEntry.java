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
package edu.fullerton.ldvtables;

import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class DataCacheEntry implements Comparable
{
    private long id;
    private Timestamp firstSeen;
    private Timestamp lastAccess;
    private long ageHr;
    private long dataLength;

    @Override
    public int compareTo(Object t)
    {
        if (t == null)
        {
            throw new NullPointerException("DataCacheEntry compareTo called on null");
        }
        DataCacheEntry rhs = (DataCacheEntry) t;
        int ret = lastAccess.compareTo(rhs.lastAccess);
        if (ret == 0)
        {
            Long theirId = rhs.getId();
            Long myId = id;
            ret = myId.compareTo(theirId);
        }
        return ret;
    }
    
    public void fill(ResultSet rs) throws LdvTableException
    {
        long now = System.currentTimeMillis();
        try
        {
            id = rs.getLong("myId");
            firstSeen = rs.getTimestamp("firstSeen");
            lastAccess = rs.getTimestamp("lastAccess");
            dataLength = rs.getLong("dataLength") * (Float.SIZE/8); // make that bytes
            ageHr = (now - lastAccess.getTime())/1000/3600;
        }
        catch (Exception ex)
        {
            String ermsg = "DataCacheEntry.fill: " + ex.getClass().getSimpleName() + " - " +
                           ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
    }

    public long getId()
    {
        return id;
    }

    public Timestamp getFirstSeen()
    {
        return firstSeen;
    }

    public Timestamp getLastAccess()
    {
        return lastAccess;
    }

    public long getAgeHr()
    {
        return ageHr;
    }

    public long getDataLength()
    {
        return dataLength;
    }
    
}
