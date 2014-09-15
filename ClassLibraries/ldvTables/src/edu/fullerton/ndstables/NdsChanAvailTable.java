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
import ndsJUtils.NDSChannelAvailability;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the times available 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsChanAvailTable extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length              can't be null   index         unique        auto inc
        new Column("indexID",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("frameType", CType.CHAR,     48,                 Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("server",    CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("chanType",  CType.CHAR,     32,                 Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("binCtype",  CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("dataType",  CType.CHAR,     32,                 Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("fs",        CType.FLOAT,    Float.SIZE /8,      Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("startGps",  CType.LONG,     Long.SIZE / 8,      Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("stopGps",   CType.LONG,     Long.SIZE / 8,      Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    private final PreparedStatement getPs;
    private final PreparedStatement insPs;
    /**
     * Set up the object
     *
     * @param db
     * @throws SQLException
     */
    public NdsChanAvailTable(Database db) throws SQLException
    {
        this.db = db;
        setName("ndsChanAvailability");
        setCols(myCols);
        
        String q = "SELECT * FROM " + getName() + " WHERE frameType = ? AND binCtype = ?";
        getPs = db.prepareStatement(q, Statement.NO_GENERATED_KEYS);
        
        String ins = "INSERT INTO " + getName() + " SET indexID=?, frameType=?, chanType=?,"
                     + "binCtype=?, dataType=?, fs=?, server=?, startGps=?, stopGps=?";
        insPs = db.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
    }

    public void insertUpdate(NDSChannelAvailability navail) throws SQLException
    {
        insPs.setInt(1,navail.getIdx());
        insPs.setString(2, navail.getFrameType());
        insPs.setString(3, navail.getcType());
        insPs.setInt(4, navail.getBCType());
        insPs.setString(5, navail.getdType());
        insPs.setFloat(6, navail.getFs());
        insPs.setInt(7, navail.getServerIdx());
        insPs.setLong(8, navail.getTimeInterval().getStartGps());
        insPs.setLong(9, navail.getTimeInterval().getStopGps());
        db.executeUpdate(insPs);
//        ResultSet rs = db.getGeneratedKeys(insPs);
//        if (rs.next())
//        {
//            int id = rs.getInt("GENERATED_KEY");
//            navail.setIdx(id);
//        }
        
    }
    public List<NDSChannelAvailability> getAvail(String frameType, int bcType) throws LdvTableException
    {
        ArrayList<NDSChannelAvailability> ret = new ArrayList<>();
        try
        {
            getPs.setString(1, frameType);
            getPs.setInt(2, bcType);
            ResultSet rs = db.executeQuery(getPs);
            while (rs.next())
            {
                NDSChannelAvailability avail = new NDSChannelAvailability(rs);
                ret.add(avail);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting channel availability", ex);
        }
        return ret;
    }
    
    public NDSChannelAvailability streamNext() throws LdvTableException
    {
        NDSChannelAvailability ret = null;
        try
        {
            if (allStream != null && allStream.next())
            {
                ret = new NDSChannelAvailability(allStream);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("streaming NdsChan", ex);
        }
        return ret;
    }
}
