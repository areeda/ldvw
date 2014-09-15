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

package edu.fullerton.ldvjutils;

import ndsJUtils.NDSChannelAvailability;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents an entry in the Server table, describes an NDS2 server
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Server
{
    private int myId;
    private String name;
    private String site;
    private String fqdn;
    private Timestamp lastmod;
    private Set<NDSChannelAvailability> avail;    // used only in some apps
    private boolean availModified;
    
    public Server()
    {
        myId = 0;
        name = "";
        site = "";
        fqdn = "";
        lastmod = new Timestamp(0);
        avail = null;
        availModified = false;
    }
    public Server(String name, String site, String fqdn)
    {
        this.name = name;
        this.site = site;
        this.fqdn = fqdn;
        lastmod = new Timestamp(0);
        avail = null;
        availModified = false;
    }
    public Server(ResultSet rs) throws SQLException
    {
        myId = rs.getInt("myId");
        name = rs.getString("name");
        fqdn = rs.getString("fqdn");
        site = rs.getString("site");
        lastmod = rs.getTimestamp("lastmod");
        avail = null;
    }

    Server(Server server)
    {
        myId = server.myId;
        name = server.name;
        fqdn = server.fqdn;
        site = server.site;
        if (server.avail != null)
        {
            avail = new HashSet<>();
            avail.addAll(server.avail);
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

    public String getSite()
    {
        return site;
    }

    public void setSite(String site)
    {
        this.site = site;
    }

    public void addAvail(Collection<NDSChannelAvailability> av)
    {
        if (avail == null)
        {
            avail = new HashSet<>();
        }
        if (!avail.containsAll(av))
        {
            avail.addAll(av);
            availModified = true;
        }
    }
    
    public Set getAvail()
    {
        if (avail == null)
        {
            avail = new HashSet<>();
        }
        return avail;
    }
}
