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

import edu.fullerton.ldvjutils.Server;
import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * A database table to 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ServerTable extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null   index          unique         auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE/8,     Boolean.TRUE, Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE),
        new Column("name",          CType.CHAR,     64,                 Boolean.TRUE, Boolean.TRUE,  Boolean.TRUE, Boolean.FALSE),
        new Column("fqdn",          CType.CHAR,     64,                 Boolean.TRUE, Boolean.TRUE,  Boolean.TRUE, Boolean.FALSE),
        new Column("site",          CType.CHAR,     48,                 Boolean.TRUE, Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("lastmod",       CType.TIMESTAMP, 8,                 Boolean.TRUE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    
    public ServerTable(Database db) 
    {
        this.db = db;
        setName("Servers");
        setCols(myCols);
    }
    
    public ArrayList<Server> getAll() throws LdvTableException
    {
        ArrayList<Server> ret = new ArrayList<>();
        String q = "SELECT * FROM " + getName();
        
        try (ResultSet rs = db.executeQuery(q))
        {
            while (rs.next())
            {
                Server srv = new Server(rs);
                ret.add(srv);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting list of servers: " + ex.getLocalizedMessage());
        }
        
        return ret;
    }
    public void save(Server srv) throws LdvTableException
    {
        try
        {
            String q = "SELECT * FROM " + getName() + " WHERE name = ?";
            PreparedStatement ps = db.prepareStatement(q, Statement.NO_GENERATED_KEYS);
            ps.setString(1, srv.getName());
            ResultSet rs = db.executeQuery(ps);
            if (rs.next())
            {
                Server oldSrv = new Server(rs);
                
                String upd = "UPDATE " + getName() + "SET name=?, fqdn=?, lastMod=now(),"
                             + "site=?, WHERE myId = ?";
                PreparedStatement updPs = db.prepareStatement(upd, Statement.NO_GENERATED_KEYS);
                updPs.setString(1, srv.getName());
                updPs.setString(2, srv.getFqdn());
                updPs.setString(3, srv.getSite());
                updPs.setInt(5, oldSrv.getMyId());
                db.execute(updPs);
                updPs.close();
            }
            else
            {
                String ins = "INSERT INTO " + getName() + " SET name=?, fqdn=?, lastMod=now(),"
                             + "site=?";
                PreparedStatement insPs = db.prepareStatement(ins, Statement.RETURN_GENERATED_KEYS);
                insPs.setString(1, srv.getName());
                insPs.setString(2, srv.getFqdn());
                insPs.setString(3, srv.getSite());
                db.execute(insPs);
                insPs.close();
            }
            rs.close();
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Saving server: " + ex.getLocalizedMessage());
        }
        
    }

    public Server getByName(String selName) throws LdvTableException
    {
        Server ret = null;
        try
        {
            String q = "SELECT * FROM " + getName() + " WHERE name = ? OR fqdn = ?";
            PreparedStatement ps = db.prepareStatement(q, Statement.NO_GENERATED_KEYS);
            ps.setString(1, selName);
            ps.setString(2, selName);
            ResultSet rs = db.executeQuery(ps);
            if (rs.next())
            {
                ret = new Server(rs);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting existing server record: " + ex.getLocalizedMessage());
        }
        return ret;
    }
}
