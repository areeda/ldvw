/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.fullerton.ldvjutils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 *
 * @author Joseph Areeda <joseph.areeda@ligo.org>
 */
public class UseLogInfo
{
    private Integer myId;
    private String user;
    private Timestamp whenSent;
    private String remoteIP;
    private Integer dbQueryCount;
    private Integer dbTimeMs;
    private Integer xferCount;
    private Integer xferBytes;
    private Integer xferTimeMs;
    private Integer plotCount;
    private Integer plotTimeMs;
    private Integer pageSize;
    private Integer pageTimeMs;
    private String description;
    

    public UseLogInfo(ResultSet rs) throws SQLException
    {
        myId = rs.getInt("myId");
        user = rs.getString("user");
        whenSent = rs.getTimestamp("whenSent");
        remoteIP = rs.getString("remoteIP");
        dbQueryCount =  rs.getInt("dbQueryCount");
        dbTimeMs =  rs.getInt("dbTimeMs");
        xferCount =  rs.getInt("xferCount");
        xferBytes =  rs.getInt("xferBytes");
        xferTimeMs =  rs.getInt("xferTimeMs");
        plotCount =  rs.getInt("plotCount");
        plotTimeMs =  rs.getInt("plotTimeMs");
        pageSize =  rs.getInt("pageSize");
        pageTimeMs =  rs.getInt("pageTimeMs");
        description = rs.getString("description");
    }

    public Integer getMyId()
    {
        return myId;
    }

    public void setMyId(Integer myId)
    {
        this.myId = myId;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public Timestamp getWhenSent()
    {
        return whenSent;
    }

    public void setWhenSent(Timestamp whenSent)
    {
        this.whenSent = whenSent;
    }

    public String getRemoteIP()
    {
        return remoteIP;
    }

    public void setRemoteIP(String remoteIP)
    {
        this.remoteIP = remoteIP;
    }

    public Integer getDbQueryCount()
    {
        return dbQueryCount;
    }

    public void setDbQueryCount(Integer dbQueryCount)
    {
        this.dbQueryCount = dbQueryCount;
    }

    public Integer getDbTimeMs()
    {
        return dbTimeMs;
    }

    public void setDbTimeMs(Integer dbTimeMs)
    {
        this.dbTimeMs = dbTimeMs;
    }

    public Integer getXferCount()
    {
        return xferCount;
    }

    public void setXferCount(Integer xferCount)
    {
        this.xferCount = xferCount;
    }

    public Integer getXferBytes()
    {
        return xferBytes;
    }

    public void setXferBytes(Integer xferBytes)
    {
        this.xferBytes = xferBytes;
    }

    public Integer getXferTimeMs()
    {
        return xferTimeMs;
    }

    public void setXferTimeMs(Integer xferTimeMs)
    {
        this.xferTimeMs = xferTimeMs;
    }

    public Integer getPlotCount()
    {
        return plotCount;
    }

    public void setPlotCount(Integer plotCount)
    {
        this.plotCount = plotCount;
    }

    public Integer getPlotTimeMs()
    {
        return plotTimeMs;
    }

    public void setPlotTimeMs(Integer plotTimeMs)
    {
        this.plotTimeMs = plotTimeMs;
    }

    public Integer getPageSize()
    {
        return pageSize;
    }

    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }

    public Integer getPageTimeMs()
    {
        return pageTimeMs;
    }

    public void setPageTimeMs(Integer pageTimeMs)
    {
        this.pageTimeMs = pageTimeMs;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
    
}
