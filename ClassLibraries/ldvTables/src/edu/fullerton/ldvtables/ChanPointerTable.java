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

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.ChanPointer;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanPointerTable extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length              can't be null   index         unique        auto inc
        new Column("indexID",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("myId",      CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("cType",     CType.CHAR,     16,                 Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("trendType", CType.CHAR,     6,                  Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("sampleRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
    };    

    // bulk insert
    private int insertCount;
    private StringBuilder insertCommand = null;
    private final int insertNum = 5000;
    
    public ChanPointerTable(Database db)
    {
        this.db = db;
        setName("ChannelPointers");
        setCols(myCols);
    }
    
    /**
     * buffer a series of insert statements and run them all at once for efficiency call one last
     * time with a null ChanInfo object to insert any remaining
     *
     * @param ci Channel to insert
     * @throws SQLException
     */
    public void insertNewBulk(ChanPointer cptr) throws SQLException
    {
        // if they set up for bulk insert but didn't insert anything both will be null
        if (cptr != null || (insertCommand != null && insertCommand.length() > 0))
        {
            if (insertCommand == null)
            {
                insertCommand = new StringBuilder(4000000);
                insertCount = 0;
            }
            if (insertCommand.length() == 0)
            {
                insertCommand.append("INSERT INTO ").append(getName()).append(" ");
                insertCommand.append(cptr.getSqlFieldNames()).append("\n");
                insertCommand.append("VALUES\n");
            }
            if (cptr != null)
            {

                if (insertCount > 0)
                {
                    insertCommand.append(",\n");
                }
                else
                {
                    insertCommand.append("\n");
                }

                insertCommand.append(cptr.getSqlFieldValues());
                insertCount++;
            }
            if ((cptr == null || insertCount >= insertNum) && insertCommand.length() > 0)
            {
                String excmd = insertCommand.toString();
                db.execute(excmd);
                insertCommand.setLength(0);
                insertCount = 0;
            }
        }
    }
    /**
     * get a list of pointers to all entries in the Channel table for this base channel
     *
     * @param indexID id of the base channel
     * @return list of ids in Channels table
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public List<Integer> getChanList(Integer indexID) throws LdvTableException
    {
        String q = String.format("SELECT * FROM %1$s WHERE indexID = %2$s",
                                 getName(), indexID);
        ArrayList<Integer> ret = new ArrayList<>();

        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ret.add(rs.getInt("myId"));
            }
        }
        catch (Exception ex)
        {
            throw new LdvTableException("Getting channel pointers.", ex);
        }
        return ret;
    }

    /**
     * get a list of all entries in the Channel table for this base channel and type
     * @param indexID id of the base channel
     * @param cType channel type (raw, rds, minute-trend ...
     * @return list of ids in Channels table
     * @throws edu.fullerton.ldvjutils.LdvTableException 
     */
    public List<Integer> getChanList(Integer indexID, String cType) throws LdvTableException
    {
        String q = String.format("SELECT * FROM %1$s WHERE indexID = %2$s and cType='%3$s'", 
                                 getName(), indexID, cType);
        ArrayList<Integer> ret = new ArrayList<>();
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            while(rs.next())
            {
                ret.add(rs.getInt("myId"));
            }
        }
        catch (Exception ex)
        {
            throw new LdvTableException("Getting channel pointers.", ex);
        }
        return ret;
    }

    /**
     * Get list of entries that belong to this base channel and the appropriate trend
     * @param indexID id of the base channel
     * @param trType (minute-trend or second-trend)
     * @param trendList )min, max, mean...)
     * @return list of ids in the Channels table
     * @throws LdvTableException 
     */
    public List<Integer> getChanList(Integer indexID, String trType, String[] trendList) throws LdvTableException
    {
        String q = String.format("SELECT * FROM %1$s WHERE indexID = %2$s and cType='%3$s'",
                                 getName(), indexID, trType);
        ArrayList<Integer> ret = new ArrayList<>();

        String tList = "";
        for(String trend : trendList)
        {
            if (!tList.isEmpty())
            {
                tList += " OR ";
            }
            tList += " trendType = '" + trend + "' ";
        }
        q += "AND (" + tList + ") ";
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ret.add(rs.getInt("myId"));
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting channel pointers.", ex);
        }
        return ret;
    }
    public ArrayList<Integer> getAllChanIds() throws LdvTableException
    {
        ArrayList<Integer> ret = new ArrayList<>();
        try
        {
            streamAll();
            ResultSet rs;
            while(allStream.next())
            {
                ret.add(allStream.getInt("myId"));
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting list of all channel poitners", ex);
        }
        return ret;
    }
    /**
     * Given the id of an entry in the channel table return its base channel id
     * @param chanId channel table key
     * @return base channel table (ChannelIndex) key
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public int getBaseId(int chanId) throws LdvTableException
    {
        int ret =0;
        try
        {
            String psTemplate="Select indexID from ChannelPointers where myId=?";
            PreparedStatement ps = db.prepareStatement(psTemplate, Statement.NO_GENERATED_KEYS);
            ps.setInt(1, chanId);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
            {
                ret = rs.getInt("indexID");
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting base channel Id", ex);
        }
        return ret;
    }
}

