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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents an entry in the Server table, describes an NDS2 server
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Server
{
    private int myId;
    private String name;
    private String fqdn;
    private Timestamp lastmod;
    private Integer minAvail;
    private Integer maxOnline;
    
    public Server()
    {
        myId = 0;
        name = "";
        fqdn = "";
        lastmod = new Timestamp(0);
        minAvail = 0;
        maxOnline = 0;
    }
    
    public Server(ResultSet rs)
    {
        try
        {
            myId = rs.getInt("myId");
            name = rs.getString("name");
            fqdn = rs.getString("fqdn");
            lastmod = rs.getTimestamp("lastmod");
            minAvail = rs.getInt("minAvail");
            maxOnline = rs.getInt("maxOnline");
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int getMyId()
    {
        return myId;
    }

    public void setMyId(int myId)
    {
        this.myId = myId;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFqdn()
    {
        return fqdn;
    }

    public void setFqdn(String fqdn)
    {
        this.fqdn = fqdn;
    }

    public Timestamp getLastmod()
    {
        return lastmod;
    }

    public void setLastmod(Timestamp lastmod)
    {
        this.lastmod = lastmod;
    }

    public Integer getMinAvail()
    {
        return minAvail;
    }

    public void setMinAvail(int minAvail)
    {
        this.minAvail = minAvail;
    }

    public Integer getMaxOnline()
    {
        return maxOnline;
    }

    public void setMaxOnline(int maxOnline)
    {
        this.maxOnline = maxOnline;
    }
    
    
            
}
