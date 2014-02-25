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
package ndsmonitor;

import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageItemTextLink;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.NdsStatsTable;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class ServerStat
{
    private String server;
    private String connErr="";
    private long connTimeMs=-1;
    
    private String cntErr="";
    private long countTimeMs=-1;
    private int chCount = 0;
    
    public ServerStat(String srv)
    {
        server = srv;
    }
    
    public void timeCounts()
    {
        long strt = System.currentTimeMillis();
        NDSProxyClient client = new NDSProxyClient(server);
        try
        {
            client.connect();
            connTimeMs = System.currentTimeMillis() - strt;
        }
        catch (NDSException ex)
        {
            connErr= ex.getClass().getSimpleName() + ":" + ex.getLocalizedMessage();
            connTimeMs = System.currentTimeMillis() - strt;
        }
        if (connErr.isEmpty())
        {
            try
            {
                strt = System.currentTimeMillis();
                chCount = client.getChanCount();
                countTimeMs = System.currentTimeMillis() - strt;
            }
            catch (NDSException ex)
            {
                cntErr = ex.getClass().getSimpleName() + ":" + ex.getLocalizedMessage();
                countTimeMs = System.currentTimeMillis() - strt;
            }
        }
        try
        {
            
            client.bye();
        }
        catch (NDSException ex)
        {
            // we tried to be nice, we don't care.
            System.err.println("Error on disconnect: " + ex.getLocalizedMessage());
        }
    }
    
    public void printStats()
    {
        String stat = server + ": ";
        if (connErr.isEmpty())
        {
            stat+=String.format("connect: %1$.2f sec ", connTimeMs/1000.);
            if (cntErr.isEmpty())
            {
                stat += String.format("get counts: %1$.2f sec.  %2$,d channels ",countTimeMs/1000., chCount);
            }
            else
            {
                stat += String.format("error on get count: %1$s ",cntErr);
            }
        }
        else
        {
            stat += String.format("connect error: %1$s. ",connErr);
        }
        System.out.println(stat);
    }
    public static PageTableRow getHdrRow() throws WebUtilException
    {
        String[] columnNames = { "State", "Server (click for history)","Connect", "Get Count", "Channels", "Error msg" };
        PageTableRow ret = new PageTableRow(columnNames);
        ret.setRowType(PageTableRow.RowType.HEAD);
        ret.setAlign(PageItem.Alignment.CENTER);
        return ret;
    }

    public PageTableRow getStatsAsRow(String baseUrl) throws WebUtilException
    {
        PageTableRow ret = new PageTableRow();
        PageItemString state;
        if (connErr.isEmpty() && cntErr.isEmpty())
        {
            state = new PageItemString(" GOOD ");
            state.setId("goodState");
        }
        else
        {
            state = new PageItemString(" ERROR ");
            state.setId("badState");
        }
        ret.add(state);
        String url = baseUrl + "&site=" + server ;
        PageItemTextLink srv = new PageItemTextLink(url,"  " +  server + "  ", "_blank");
        ret.add(srv);
        PageTableColumn col = new PageTableColumn(String.format("%1$.2fs  ", connTimeMs/1000.));
        col.setAlign(PageItem.Alignment.RIGHT);
        ret.add(col);
        
        // time to get the counts if we have it
        if (countTimeMs < 0)
        {
            ret.add();
        }
        else
        {
            col = new PageTableColumn(String.format("%1$.2fs  ", countTimeMs/1000.));
            col.setAlign(PageItem.Alignment.RIGHT);
            ret.add(col);
        }
        // channel count if we have it
        if (chCount < 1)
        {
            ret.add();
        }
        else
        {
            col = new PageTableColumn(String.format("%1$,d  ", chCount));
            col.setAlign(PageItem.Alignment.RIGHT);
            ret.add(col);
        } 
        // any errors?
        if (connErr.isEmpty() && cntErr.isEmpty())
        {
            ret.add();
        }
        else
        {
            ret.add(connErr + cntErr);
        }
        
        return ret;
    }

    /**
     * save this measurement to the database
     * @param nst table to use
     */
    void add2Db(NdsStatsTable nst)
    {
        String state = "Error";
        if (connErr.isEmpty() && cntErr.isEmpty())
        {
            state = "Good";
        }
        try
        {
            nst.add(server, state, (int)connTimeMs, (int)countTimeMs, (int)chCount, connErr + cntErr);
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ServerStat.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    int getState()
    {
        int ret = 0;
        if (!connErr.isEmpty() || !cntErr.isEmpty())
        {
            ret = 2;
        }
        else if (chCount > 50000000)
        {
            ret = 1;
        }
        return ret;
    }

    public String getServer()
    {
        return server;
    }

    public String getConnErr()
    {
        return connErr;
    }

    public long getConnTimeMs()
    {
        return connTimeMs;
    }

    public String getCntErr()
    {
        return cntErr;
    }

    public long getCountTimeMs()
    {
        return countTimeMs;
    }

    public int getChCount()
    {
        return chCount;
    }
    
}
