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

package ndsJUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import ndsJUtils.NDSChannelAvailability;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsServer 
{

    private int myId;
    private int bitMask;
    private String name;
    private String site;
    private String fqdn;
    private Timestamp lastmod;
    
    public NdsServer()
    {
        myId = 0;
        bitMask = 0;
        name = "";
        site = "";
        fqdn = "";
        lastmod = new Timestamp(0);
    }

    public NdsServer(String name, String site, String fqdn)
    {
        this.name = name;
        this.site = site;
        this.fqdn = fqdn;
        lastmod = new Timestamp(0);
    }

    public NdsServer(ResultSet rs) throws SQLException
    {
        myId = rs.getInt("myId");
        bitMask = rs.getInt("bitMask");
        name = rs.getString("name");
        fqdn = rs.getString("fqdn");
        site = rs.getString("site");
        lastmod = rs.getTimestamp("lastmod");
    }

    NdsServer(NdsServer server)
    {
        myId = server.myId;
        bitMask = server.myId;
        name = server.name;
        fqdn = server.fqdn;
        site = server.site;
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

    public int getBitMask()
    {
        return bitMask;
    }

    public void setBitMask(int bitMask)
    {
        this.bitMask = bitMask;
    }

}
