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

import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.ChanInfo;
import com.areeda.jaDatabaseSupport.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;
import javax.sql.rowset.serial.SerialBlob;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class DataTable extends Table
{

    private final Column[] myCols =
    {
        //         name,            type                length          can't be null  index          unique         auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE,       Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("name",          CType.CHAR,     255,                Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("server",        CType.CHAR,     64,                 Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("cType",         CType.CHAR,     16,                 Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("dType",         CType.CHAR,     16,                 Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("sampleRate",    CType.FLOAT,    Float.SIZE,         Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("firstSeen",     CType.TIMESTAMP, Long.SIZE,         Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("lastAccess",    CType.TIMESTAMP, Long.SIZE,         Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("startGps",      CType.LONG,     Long.SIZE,          Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("stopGps",       CType.LONG,     Long.SIZE,          Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("dataLength",    CType.LONG,     Long.SIZE,          Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("dataMd5",       CType.CHAR,     34,                 Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("data",          CType.BLOB,     1000000,            Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    private final PreparedStatement insps;
    private final PreparedStatement fndps;
    private final int maxBlobSize = 10000000;
    private final int maxCacheSize = 1000000000;

    public DataTable(Database db) throws SQLException
    {
        this.db = db;
        setName("dataCache");
        setCols(myCols);
        // Prepare statements to find existing entries and insert new entries
        String q = "SELECT myId,startGps, stopGps, dataLength, length(data) as size from " + getName();
        q += "WHERE name=? AND cType=? AND startGps <= ? AND ";
        q += "stopGps >= ?";

        fndps = db.prepareStatement(q, Statement.NO_GENERATED_KEYS);

        
        String ins = "INSERT INTO " + getName() + " SET name=?, server=?, cType =?, dType=?, sampleRate=?, firstSeen=now(), lastAccess=now(), startGps=?,"
                     + " stopGps=?, dataLength=?, dataMd5=?, data=?";
        insps = db.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
        
    }

    public int findRec(String name, String cType, long strt, long stop) throws SQLException
    {
        int ret = 0;
        fndps.setString(1, name);
        fndps.setString(2, cType);
        fndps.setLong(3, strt);
        fndps.setLong(4, stop);
        ResultSet rs = db.executeQuery(fndps);
        if (rs.next())
        {
            ret = rs.getInt("myId");
        }
        return ret;
    }

    /**
     * Save this data in our table.  Make sure only new data is saved.
     *
     * @param chanInfo
     * @param timeInterval
     * @param data
     */
    public void cache(ChanInfo chanInfo, TimeInterval timeInterval, float[] data) throws LdvTableException
{
        ArrayList<TimeInterval> cachedData = findBuffer(chanInfo, timeInterval);

        // search through cache and fill in any gaps we now have in memory
        long outStart = timeInterval.getStartGps();
        long outStop = timeInterval.getStopGps();
        float rate = chanInfo.getRate();
        boolean done = false;
        // check if this is fresh, ie doesn't overlap anything saved
        if (cachedData.isEmpty())
        {
            addData(chanInfo,timeInterval,data);
        }
        else
        {
            for (TimeInterval ti : cachedData)
            {
                long cstrt = ti.getStartGps();
                long cstop = ti.getStopGps();
                if (cstrt <= outStart && cstop >= outStop)
                {
                    done = true;
                    break;
                }
                else if (cstrt <= outStart)
                {
                    // part of our buffer is already in the table
                    outStart = cstop;
                }
                else
                {
                    // we have some data to write out
                    int wstrt = Math.round(rate * (outStart-timeInterval.getStartGps()));
                    int wcnt = Math.round(rate * (cstrt-outStart));
                    float[] buf = new float[wcnt];
                    System.arraycopy(data, wstrt, buf, 0, wcnt);
                    TimeInterval tti = new TimeInterval(outStart, cstrt);
                    addData(chanInfo,tti,buf);
                    outStart=cstop;
                }
            }
            if (outStart < outStop && !done)
            {
                int wstrt = Math.round(rate * (outStart-timeInterval.getStartGps()));
                int wcnt = Math.round(rate * (outStop - outStart));
                float[] buf = new float[wcnt];
                System.arraycopy(data, wstrt, buf, 0, wcnt);
                TimeInterval tti = new TimeInterval(outStart, outStop);
                addData(chanInfo, tti, buf);

            }
        }
    }
    /**
     * Break the buffer into chunks that mysql likes if necessary and add to data base
     * @param chanInfo
     * @param timeInterval
     * @param data
     * @throws LdvTableException 
     */
    private void addData(ChanInfo chanInfo, TimeInterval timeInterval, float[] data) throws LdvTableException
    {
        int len = data.length;
        long nBytes = (long)data.length * (Float.SIZE/8);
        if (nBytes <= maxBlobSize)
        {
            // if it fits we don't have to allocate and copy it
            addBuffer(chanInfo, timeInterval,data);
        }
        else
        {
            // too long, break it into pieces 
            int written = 0;
            long startGps = timeInterval.getStartGps();
            long stopGps = timeInterval.getStopGps();
            long bstart = startGps;     // start of next buffer
            float sampleRate = chanInfo.getRate();
            int maxBufCount = (int)(( (int)(maxBlobSize * 8/Float.SIZE / sampleRate)) * sampleRate); // integer seconds
            while(written < len)
            {
                
                int blen = (int) Math.min(maxBufCount, len - written);  
                long bstop = bstart + Math.round(blen / sampleRate);
                float[] buf = new float[blen];
                System.arraycopy(data, written, buf, 0, blen);
                TimeInterval ti=new TimeInterval(bstart,bstop);
                addBuffer(chanInfo,ti,buf);
                written += blen;
                bstart = bstop;
            }
        }
    }
    /**
     * Save this buffer to the database for quick access
     * @param chanInfo - defines what channel we have
     * @param timeInterval = defines the time
     * @param data - the data buffer
     * @throws LdvTableException problem with conversion or database access
     */
    private void addBuffer(ChanInfo chanInfo, TimeInterval timeInterval, float[] data) throws LdvTableException
    {
        if (data.length > maxBlobSize)
        {
            throw new LdvTableException("DataTable::addBuffer - buffer is too big: " + Integer.toString(data.length));
        }
        try
        {
            String md5 = Utils.md5sum(data);
            insps.setString(1, chanInfo.getChanName());
            insps.setString(2, chanInfo.getServer());
            insps.setString(3, chanInfo.getcType());
            insps.setString(4, chanInfo.getdType());
            insps.setFloat(5, chanInfo.getRate());
            insps.setLong(6, timeInterval.getStartGps());
            insps.setLong(7, timeInterval.getStopGps());
            insps.setInt(8, data.length);
            insps.setString(9, md5);
            byte[] blobd = Utils.float2ByteArray(data);
            SerialBlob blob = new SerialBlob(blobd);
            insps.setBlob(10, blob);
            insps.executeUpdate();
        }
        catch (Exception ex)
        {
            throw new LdvTableException("Attempting to add buffer to data chache:" +  ex.getLocalizedMessage());
        }
    }

    public ArrayList<TimeInterval> findBuffer(ChanInfo chanInfo, TimeInterval timeInterval) throws LdvTableException
    {
        ArrayList<TimeInterval> ret = new ArrayList<TimeInterval>();
        try
        {
            // first see if there is any overlap with anything already saved
            String q = "SELECT myId,startGps, stopGps, dataLength, length(data) as size,"
                       + "dataMd5, md5(data) as calcmd5 from " + getName();
            q += " WHERE name=? AND cType=? AND sampleRate = ? AND ";
            q += "((startGps >= ? AND stopGps >= ?) AND (startGps <= ? and stopGps<= ?))";
            q += " ORDER BY startGps";

            PreparedStatement ps = db.prepareStatement(q, Statement.NO_GENERATED_KEYS);
            ps.setString(1, chanInfo.getChanName());
            ps.setString(2, chanInfo.getcType());
            ps.setFloat(3, chanInfo.getRate());
            ps.setLong(4, timeInterval.getStartGps());
            ps.setLong(5, timeInterval.getStartGps());
            ps.setLong(6, timeInterval.getStopGps());
            ps.setLong(7, timeInterval.getStopGps());
            ResultSet rs = db.executeQuery(ps);

            while (rs.next())
            {
                int id = rs.getInt("myId");
                long cstrt = rs.getLong("startGps");
                long cstop = rs.getLong("stopGps");
                long dataLength = rs.getLong("dataLength");
                long size = rs.getLong("size");
                String dataMd5 = rs.getString("dataMd5");
                String calcMd5 = rs.getString("calcMd5");
                String err = "";

                if (dataLength * Float.SIZE / 8 != size)
                {
                    err += "Size does not match number of samples.  ";
                }
                if (!dataMd5.equalsIgnoreCase(calcMd5))
                {
                    err += "MD5 value does not match data.  ";
                }
                if (err.length() > 0)
                {
                    err += "Chan: " + chanInfo.getChanName() + ", cType: " + chanInfo.getcType()
                           + ", Start: " + Long.toString(timeInterval.getStartGps());
                    ErrorLog.logError(db, "DataTable.Buffer", 3, err);
                    this.deleteById(id);
                }
                else
                {
                    TimeInterval ti = new TimeInterval(cstrt, cstop, id, dataLength);
                    ret.add(ti);
                }
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException(ex);
        }

        return ret;
    }
    public float[] getData(TimeInterval ti) throws LdvTableException
    {
        return getBuf(ti.getCacheId());
    }
    /**
     * Get the data from the cached record
     * @param recn id column for the record
     * @return buffer with the data points
     * @throws LdvTableException 
     */
    private float[] getBuf(int recn) throws LdvTableException
    {
        float[] ret=null;
        String getit = "SELECT data FROM " + getName() + " WHERE myId=" + Integer.toString(recn);
        
        try
        {
            ResultSet rs = db.executeQuery(getit);
            if (rs != null && rs.next())
            {
                Blob blob = rs.getBlob("data");
                ret = Utils.blob2FloatArray(blob);
            }
            else
            {
                throw new LdvTableException(String.format("Error, requested cache buffer not available"));
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Trying to get existing cached buffer", ex);
        }
        return ret;
    }

    public void updateLastAccess(int cacheId) throws LdvTableException
    {
        try
        {
            String upd = String.format("UPDATE %s SET lastAccess=now() WHERE myId=%d", getName(),cacheId);
            db.execute(upd);
        }
        catch (Exception ex)
        {
            String ermsg = "Data cache, update last access: " + ex.getClass().getSimpleName() +
                           " - " + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
        
    }
    
    public TreeSet<DataCacheEntry> getCacheEntries() throws LdvTableException
    {
        TreeSet<DataCacheEntry> entries = new TreeSet<DataCacheEntry>();
        String q = "SELECT myId, firstSeen, lastAccess, dataLength FROM " + getName();
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())            
            {
                DataCacheEntry dce = new DataCacheEntry();
                dce.fill(rs);
                entries.add(dce);
            }
        }
        catch (Exception ex)
        {
            String ermsg = "DataTable.getCacheEntries: " + ex.getClass().getSimpleName() + " - " +
                           ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
        return entries;
    }
    public void delete(Collection<DataCacheEntry> deletions) throws LdvTableException
    {
        try
        {
            String del = "DELETE FROM " + getName() + " WHERE myId=?";
            PreparedStatement delps = db.prepareStatement(del, Statement.NO_GENERATED_KEYS);
            for (DataCacheEntry ent : deletions)
            {
                delps.setInt(1, (int) ent.getId());
                delps.execute();
            }
        }
        catch (SQLException ex)
        {
            String ermsg;
            ermsg = "Deleting cache entries: " + ex.getClass().getSimpleName() + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
    }
}
