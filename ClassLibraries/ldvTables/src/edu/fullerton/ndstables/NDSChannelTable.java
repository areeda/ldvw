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

package edu.fullerton.ndstables;

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.LdvTableException;
import ndsJUtils.NdsChan;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NDSChannelTable extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length              can't be null   index         unique        auto inc
        new Column("indexID",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("name",      CType.CHAR,     64,                 Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("nameHash",  CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("servers",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("ifo",       CType.CHAR,     8,                  Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("subsys",    CType.CHAR,     32,                 Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("minRawRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("maxRawRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("minRdsRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("maxRdsRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("cTypeMask", CType.INTEGER,  Integer.SIZE / 8,   Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("cisAvail",  CType.CHAR,     1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
// bulk insert
    private int insertCount;
    private StringBuilder insertCommand = null;
    private int insertNum = 5000;

    
    /**
     * Set up the object
     *
     * @param db
     * @throws SQLException
     */
    public NDSChannelTable(Database db) throws SQLException
    {
        this.db = db;
        setName("ndsChannels");
        setCols(myCols);
    }
    public void insertUpdate(NdsChan chan) throws LdvTableException
    {
        try
        {
            if (!chan.isInited())
            {
                throw new LdvTableException("NDSChannelTable: Attempt to insert/update with an ininitialized channel object");
            }
            
            StringBuilder sql = new StringBuilder();
            String where="";
            
            if (chan.getIndexID() == 0)
            {
                sql.append("INSERT INTO ").append(getName()).append(" ");
            }
            else
            {
                sql.append("UPDATE ").append(getName()).append(" ");
                where = String.format(" WHERE indexID = %1$d ", chan.getIndexID());
            }
            sql.append(" SET ").append("\n");
            sql.append("name = '").append(chan.getName()).append("', ");
            sql.append("namehash = ").append(Integer.toString(chan.getNameHash())).append(", ");
            sql.append("servers = ").append(Integer.toString(chan.getServers())).append(", ");
            sql.append("ifo = '").append(chan.getIfo()).append("', ");
            sql.append("subsys = '").append(chan.getSubsys()).append("', ");
            sql.append("minRawRate = ").append(chan.getMinRawRateStr()).append(", ");
            sql.append("maxRawRate = ").append(chan.getMaxRawRateStr()).append(", ");
            sql.append("minRdsRate = ").append(chan.getMinRdsRateStr()).append(", ");
            sql.append("maxRdsRate = ").append(chan.getMaxRdsRateStr()).append(", ");
            sql.append("cTypeMask = ").append(chan.getcTypeStr()).append(", ");
            sql.append("cisAvail = ' ' ");
            
            if (!where.isEmpty())
            {
                // update existing entry
                sql.append(where);
                db.execute(sql.toString());
            }
            else
            {
                // add new entry
                ResultSet rs = db.executeUpdateGetKeys(sql.toString());
                if (rs != null && rs.next())
                {
                    int newId = rs.getInt("GENERATED_KEY");
                    chan.setIndexID(newId);
                }
            }
            
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("NDSChannelTable:insertUpdate: ", ex);
        }
    }
    public NdsChan streamNext() throws LdvTableException
    {
        NdsChan ret = null;
        try
        {
            if (allStream != null && allStream.next())
            {
                ret = new NdsChan();
                ret.fill(allStream);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("streaming NdsChan", ex);
        }
        return ret;
    }
}
