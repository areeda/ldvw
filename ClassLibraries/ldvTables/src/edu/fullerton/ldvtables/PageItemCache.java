/*
 * Copyright (C) 2012 joe
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
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.WebUtilException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Store partial page contents for mostly static pages, or full page 
 * Beware of full page if things like username or date change
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageItemCache extends Table
{
    private final Statement stmt;
    private final Column[] myCols =
    {
        //         name,        type            length    can't be null         index            unique        auto inc
        new Column("myId",     CType.INTEGER,   Integer.SIZE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("name",     CType.CHAR,      255,            Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("type",     CType.CHAR,      64,             Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("theMd5",  CType.CHAR,      64,             Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("contents", CType.STRING,    100000,         Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("lastMod", CType.TIMESTAMP,  Long.SIZE,      Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    public PageItemCache(Database db) throws SQLException
    {
        this.db = db;
        setName("PageItemCache");
        setCols(myCols);
        stmt = db.createStatement();
    } 
    public String getContents(String name) throws SQLException
    {
        String ret = "";
        String check = "SELECT myId,contents from " + getName() + " WHERE name=?";
        PreparedStatement chk = db.prepareStatement(check, Statement.NO_GENERATED_KEYS);
        chk.setString(1, name);
        ResultSet rs = db.executeQuery(chk);
        if (rs != null && rs.next())
        {
            ret = rs.getString("contents");
            rs.close();
        }
        return ret;
    }
    public void remove (String name) throws SQLException
    {
        String rem = "DELETE from " + getName() + " WHERE name=?";
        PreparedStatement remps = db.prepareStatement(rem, Statement.NO_GENERATED_KEYS);
        remps.setString(1, name);
        db.execute(remps);
    }
    public boolean save(String name, PageItem it) throws WebUtilException, SQLException
    {
        String sit = it.getHtml();
        return save( name,sit,"part");
    }
    public boolean save (String name, Page it) throws WebUtilException, SQLException
    {
        String sit = it.getHTML();
        return save( name, sit, "page");
    }
    public boolean save(String name, String it, String type) throws SQLException
    {
        boolean ret = false;
        
        String check = "SELECT myId from " + getName() + " WHERE name=?";
        String insert = "INSERT INTO " + getName() + " SET name=?,type=?,theMd5=0,contents=?,lastMod=now()";
        String update = "UPDATE " + getName() + " SET name=?,type=?,contents=?,lastMod=now() WHERE myId=?";
        String setmd5 = "UPDATE " + getName() + " SET theMd5=md5(contents) WHERE myId=?";
        
        PreparedStatement chk = db.prepareStatement(check, Statement.NO_GENERATED_KEYS);
        chk.setString(1, it);
        int myId = 0;
        
        ResultSet rs = db.executeQuery(chk);
        if (rs != null && rs.next())
        {
            myId = rs.getInt("myId");
            rs.close();
        }
        if (myId > 0)
        {
            PreparedStatement ps = db.prepareStatement(update, Statement.NO_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2,type);
            ps.setString(3, it);
            ps.setInt(4, myId);
            
            db.executeUpdate(ps);
            
        }
        else
        {
            PreparedStatement ps = db.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setString(3, it);
            
            db.executeUpdate(ps);
            
            rs = db.getGeneratedKeys(ps);
            if (rs != null && rs.next())
            {
                myId = rs.getInt(1);
            }
        }
        if (myId > 0)
        {
            PreparedStatement ps = db.prepareStatement(setmd5, Statement.NO_GENERATED_KEYS);
            ps.setInt(1, myId);
            db.executeUpdate(ps);
            ret=true;
        }
        return ret;
    }
    
}
