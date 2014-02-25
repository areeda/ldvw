/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Contains times that we know data is available
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanDataAvailability extends Table
{
    // bulk insert
    private int insertCount;
    private StringBuilder insertCommand = null;
    private final int insertNum = 200;
    
    private static final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE/8,   Boolean.TRUE,   Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("name",      CType.CHAR,         64,         Boolean.TRUE,   Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("server",    CType.CHAR,         48,         Boolean.TRUE,   Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("cType",     CType.CHAR,         16,         Boolean.FALSE,  Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("lastMod",   CType.TIMESTAMP, Long.SIZE/8,     Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("availability", CType.STRING,      0,        Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    // Field names for bulk insert statement
    private static final String fldNames = " (name,server,cType,lastMod,availability) ";

    public ChanDataAvailability(Database db)
    {
        super(db,"ChanDataAvailability",myCols);
    }
    public int addOrUpdate(String name,String server,String cType, String availability) throws SQLException
    {
        int myId;
        String find = "SELECT myId FROM " + getName() + " WHERE name=?, server = ?, cType = ?";
        PreparedStatement findps = db.prepareStatement(find, Statement.NO_GENERATED_KEYS);
        ResultSet frs = db.executeQuery(findps);
        if (frs.next())
        {
            String upd = "UPDATE " + getName() + " SET name=?, server = ?, cType = ?, lastMod=now(), availability=? WHERE myId = ?";
            myId = frs.getInt("myId");
            
            PreparedStatement ups = db.prepareStatement(upd, Statement.NO_GENERATED_KEYS);
            ups.setString(1, name);
            ups.setString(2, server);
            ups.setString(3, cType);
            ups.setString(4, availability);
            ups.setInt(5,myId);
            db.executeUpdate(ups);
        }
        else
        {
            myId = insert(name, server, cType, availability);
        }
        return myId;
    }
    public int insert(String name,String server,String cType, String availability) throws SQLException
    {
        String ins = "INSERT INTO " + getName() + " SET name=?, server = ?, cType = ?, lastMod=now(), availability=?";
        PreparedStatement ps = db.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, name);
        ps.setString(2, server);
        ps.setString(3, cType);
        ps.setString(4, availability);
        ps.executeUpdate();
        ResultSet idrs = ps.getGeneratedKeys();
        idrs.next();
        int myId = idrs.getInt(1);
        idrs.close();
        return myId;
    }
    /**
     * Find the availability string by searching
     * @param name channel name
     * @param server server with data
     * @param cType channel type
     * @return the available GPS times
     * @throws SQLException 
     */
    public String getAvailability(String name,String server,String cType) throws SQLException
    {
        String ret = "";
        String find = "SELECT availability FROM " + getName() + " WHERE name=? AND server = ? AND cType = ?";
        PreparedStatement findps = db.prepareStatement(find, Statement.NO_GENERATED_KEYS);
        findps.setString(1, name);
        findps.setString(2, server);
        findps.setString(3, cType);
        ResultSet frs = db.executeQuery(findps);
        if (frs.next())
        {
            ret= frs.getString("availability");
        }
        frs.close();
        if (!ret.matches(".*\\d{8}.*"))
        {
            ret = "";
        }
        
        return ret;
    }
    public String getAvailability(int id) throws SQLException
    {
        String ret = "";
        if (id > 0)
        {
            String find = "SELECT availability FROM " + getName() + " WHERE myId=?";
            PreparedStatement findps = db.prepareStatement(find, Statement.NO_GENERATED_KEYS);
            findps.setInt(1, id);
            ResultSet frs = db.executeQuery(findps);
            if (frs.next())
            {
                ret = frs.getString("availability");
            }
            frs.close();
            if (!ret.matches(".*\\d{8}.*"))
            {
                ret = "";
            }
        }
        return ret;
    }
     public void insertNewBulk(String name,String server,String cType, String availability) throws SQLException
    {
        if (insertCommand==null)
        {
            insertCommand = new StringBuilder(4000000);
        }
        if (insertCommand.length() == 0)
        {
            insertCommand.append("INSERT INTO ").append(getName()).append(" ");
            insertCommand.append(fldNames).append("\n");
            insertCommand.append("VALUES\n");
        }
        if (name != null)
        {
            

            if (insertCount > 0)
            {
                insertCommand.append(",\n");
            }
            else
            {
                insertCommand.append("\n");
            }
            
            String vals = "('" + name + "', '" + server + "', '" + cType + "', now(), '" + availability  + "')";
            insertCommand.append(vals);
            insertCount++;
        }
        if ((name == null || insertCount >= insertNum) && insertCommand.length() > 0)
        {
            db.execute(insertCommand.toString());
            insertCommand.setLength(0);
            insertCount = 0;
        }
        
        
    }
}
