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

package edu.fullerton.ldvtables;

import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 * Represents a row in the SessionTable
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SessionDto
{
    private int myId;
    private String eduPersonPrincipalName;
    private Timestamp sessStart;
    private String remAddr;
    private String userAgent;
    
    public SessionDto()
    {
        myId = 0;
        eduPersonPrincipalName = "unknown";
        sessStart = new Timestamp(System.currentTimeMillis());
        remAddr = "unknown";
        userAgent = "unknown";
    }
    public SessionDto(HttpServletRequest request)
    {
        myId = 0;
        eduPersonPrincipalName = (String) request.getAttribute("eduPersonPrincipalName");
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = request.getHeader("eduPersonPrincipalName");
        }
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = "local.admin";
        }
        sessStart = new Timestamp(System.currentTimeMillis());
        remAddr = request.getRemoteAddr();
        userAgent = request.getHeader("user-agent");
    }
    public SessionDto(ResultSet rs) throws LdvTableException
    {
        try
        {
            myId = rs.getInt("myId");
            eduPersonPrincipalName = rs.getString("eduPersonPrincipalName");
            sessStart = rs.getTimestamp("sessStart");
            remAddr = rs.getString("remAddr");
            userAgent = rs.getString("userAgent");
                    
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Initializing a SeessionDao",ex);
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

    public String getEduPersonPrincipalName()
    {
        return eduPersonPrincipalName;
    }

    public void setEduPersonPrincipalName(String eduPersonPrincipalName)
    {
        this.eduPersonPrincipalName = eduPersonPrincipalName;
    }

    public Timestamp getSessStart()
    {
        return sessStart;
    }

    public void setSessStart(Timestamp sessStart)
    {
        this.sessStart = sessStart;
    }

    public String getRemAddr()
    {
        return remAddr;
    }

    public void setRemAddr(String remAddr)
    {
        this.remAddr = remAddr;
    }

    public String getUserAgent()
    {
        return userAgent;
    }

    public void setUserAgent(String userAgent)
    {
        this.userAgent = userAgent;
    }
    
}
