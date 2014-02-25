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

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvjutils.NdsServStat;
import edu.fullerton.ldvtables.NdsHistory;
import edu.fullerton.ldvtables.NdsStatsTable;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsMonitor
{
    private final String[] servers;
    private final ArrayList<ServerStat> stats;
    private final Database db;
    private NdsStatsTable nst;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args == null || args.length == 0)
        {
            NdsMonitor me=null;
            try 
            {
                me = new NdsMonitor();
            }
            catch (ViewConfigException ex) 
            {
                Logger.getLogger(NdsMonitor.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(1);
            }

            me.timeCounts();
            me.printStats();
        }
        else
        {
            String server = args[0];
            ServerStat ss = new ServerStat(server);
            ss.timeCounts();
            int state=ss.getState();
            String msg;
            switch(state)
            {
                case 0:
                    msg = String.format("Connect: %1$.1fs, Get count: %2$.1fs, #Chans: %3$,d",
                                        ss.getConnTimeMs()/1000., ss.getCountTimeMs()/1000.,
                                        ss.getChCount());
                    break;
                case 1:
                    msg = String.format("Channel count unreasonable: %1$,d", ss.getChCount());
                    break;
                case 2:
                    msg = String.format("Error: %1$s, %2$s",ss.getConnErr(), ss.getCntErr());
                    break;
                default:
                    msg = "Unknown state returned by server.";
                    state = 2;
            }
            System.out.println(msg);
            System.exit(state);
        }
    }

    public NdsMonitor() throws ViewConfigException
    {
        this.servers = ChanParts.getServers();
        stats = new ArrayList<ServerStat>();
        ViewerConfig vc = new ViewerConfig();
        db = vc.getDb();
        if (db != null)
        {
            try
            {
                nst = new NdsStatsTable(db);
            }
            catch (SQLException ex)
            {
                nst = null;
            }
        }
    }
    public NdsMonitor(Database db)
    {
        this.servers = ChanParts.getServers();
        stats = new ArrayList<>();
        this.db = db;
        if (db != null)
        {
            try
            {
                nst = new NdsStatsTable(db);
            }
            catch (SQLException ex)
            {
                nst = null;
            }
        }
    }
    public void timeCounts()
    {
        for(String srv : servers)
        {
            ServerStat ss = new ServerStat(srv);
            ss.timeCounts();
            stats.add(ss);
            if (nst != null)
            {
                ss.add2Db(nst);
            }
        }
    }
    
    public void printStats()
    {
        for(ServerStat ss : stats)
        {
            ss.printStats();
        }
    }
    
    public PageTable getStatsAsTable(String baseUrl) throws WebUtilException
    {
        PageTable ret = new PageTable();
        ret.setId("statsTbl");
        boolean odd = true;
        ret.addRow(ServerStat.getHdrRow());
        for (ServerStat ss : stats)
        {
            PageTableRow statRow = ss.getStatsAsRow(baseUrl);
            if(odd)
            {
                statRow.setClassName("odd");
            }
            else
            {
                statRow.setClassName("even");
            }
            odd = !odd;
            ret.addRow(statRow);
        }
        return ret;
    }

    /**
     * Generate the summary report of state, counts and timing for all servers
     * 
     * @return the summary as a Page Item
     * @throws SQLException we had a database problem
     * @throws WebUtilException we had a formatting problem
     */
    public PageItemList getSummary() throws SQLException, WebUtilException
    {
        PageItemList ret = new PageItemList();
        if (nst != null)
        {
            ArrayList<NdsServStat> nssList = nst.getStats();

            Timestamp minDate = null;
            Timestamp maxDate = null;
            PageTable hist = new PageTable();
            hist.setId("statsTbl");
            boolean odd = true;

            PageTableRow h1 = new PageTableRow();
            h1.setRowType(PageTableRow.RowType.HEAD);
            PageTableColumn c = new PageTableColumn("Server");
            c.setRowSpan(2);
            h1.add(c);

            c = new PageTableColumn("state:n");
            c.setRowSpan(2);
            h1.add(c);

            c = new PageTableColumn("Connect");
            c.setSpan(4);
            h1.add(c);

            c = new PageTableColumn("Get Counts");
            c.setSpan(4);
            h1.add(c);

            h1.setAlign(PageItem.Alignment.CENTER);
            hist.addRow(h1);

            PageTableRow h2 = new PageTableRow();
            h2.setRowType(PageTableRow.RowType.HEAD);

            h2.add("min");
            h2.add("max");
            h2.add("mean");
            h2.add("std. dev");

            h2.add("min");
            h2.add("max");
            h2.add("mean");
            h2.add("std. dev");

            h2.setAlign(PageItem.Alignment.CENTER);
            hist.addRow(h2);

            for(NdsServStat nss : nssList)
            {
                if (minDate == null || minDate.after(nss.getMinDate()))
                {
                    minDate = nss.getMinDate();
                }
                if (maxDate == null || maxDate.before(nss.getMaxDate()))
                {
                    maxDate = nss.getMaxDate();
                }

                PageTableRow statRow = new PageTableRow();
                if (odd)
                {
                    statRow.setClassName("odd");
                }
                else
                {
                    statRow.setClassName("even");
                }
                odd = !odd;
                statRow.add(nss.getServer());
                Integer ngood = nss.getnGood();
                Integer nbad = nss.getnBad();
                if (ngood > 0)
                {
                    statRow.add(String.format("good: %1$,5d", ngood));
                }
                else
                {
                    statRow.add(String.format("bad: %1$,5d", nbad));
                }
                statRow.add(String.format("%1$.2fs",nss.getConMin()/1000.));
                statRow.add(String.format("%1$.2fs", nss.getConMax() / 1000.));
                statRow.add(String.format("%1$.2fs",nss.getConAvg()/1000.));
                statRow.add(String.format("%1$.2fs",nss.getConSD()/1000.));

                statRow.add(String.format("%1$.2fs", nss.getCntMin() / 1000.));
                statRow.add(String.format("%1$.2fs", nss.getCntMax() / 1000.));
                statRow.add(String.format("%1$.2fs", nss.getCntAvg() / 1000.));
                statRow.add(String.format("%1$.2fs", nss.getCntSD() / 1000.));

                //statRow.setAlign(PageItem.Alignment.RIGHT);
                hist.addRow(statRow);
            }
            String stitle = String.format("Summary of NDS server history from %1$s to %2$s UTC", 
                                          TimeAndDate.dateAsUtcString(new Date(minDate.getTime())),
                                          TimeAndDate.dateAsUtcString(new Date(maxDate.getTime())));
            PageItemString secTitle = new PageItemString(stitle);
            secTitle.setId("secTitle");
            ret.add(secTitle);
            ret.addBlankLines(2);
            ret.add(hist);
        }
        else
        {
            PageItemString secTitle = new PageItemString("Summary stats are not available.");
            secTitle.setId("secTitle");
            ret.add(secTitle);
        }
        return ret;
    }

    /**
     * Return a formatted report of each state change for one server
     * 
     * @param site FQDN of the server we're to report
     * 
     * @return a PageItem containing formatted report
     */
    public PageItemList getHistory(String site) throws LdvTableException, WebUtilException
    {
        PageItemList ret = new PageItemList();
        ArrayList<NdsHistory> history = nst.getHistory(site);
        int cnt = 0;
        for(NdsHistory h : history)
        {
            cnt += h.getN();
        }
        String sTitle = String.format("History of %1$s as of %2$s UTC with %3$,d entries:", 
                                      site, TimeAndDate.nowAsUtcString(0),cnt);
        PageItemString secTitle = new PageItemString(sTitle);
        secTitle.setId("secTitle");
        ret.add(secTitle);
        ret.addBlankLines(2);
        if (history.size() < 1)
        {
            ret.add("Sorry.  No entries found for " + site);
        }
        else
        {
            PageTable histTbl = new PageTable();
            
            String[] hdr = { "From (UTC)", "To (UTC)", "Count", "n-chan", "State", "Error(s)" };
            PageTableRow hdrRow = new PageTableRow(hdr);
            hdrRow.setRowType(PageTableRow.RowType.HEAD);
            hdrRow.setAlign(PageItem.Alignment.CENTER);
            histTbl.addRow(hdrRow);
            
            boolean odd = true;
            
            for(int idx = history.size()-1; idx >= 0; idx--)
            {
                NdsHistory ent = history.get(idx);
                
                PageTableRow histRow = new PageTableRow();
                if (odd)
                {
                    histRow.setClassName("odd");
                }
                else
                {
                    histRow.setClassName("even");
                }
                odd = !odd;
                // "From", "To", "Count", "n-chan", "State", "Error(s)"
                histRow.add(TimeAndDate.dateAsUtcString(ent.getMinTestTime()));
                histRow.add(TimeAndDate.dateAsUtcString(ent.getMaxTestTime()));
                histRow.add(ent.getN());
                histRow.add(ent.getChanCount());
                String state=ent.getState();
                PageItemString statePI = new PageItemString(state);
                if (state.equalsIgnoreCase("good"))
                {
                    statePI.setId("goodState2");
                }
                else
                {
                    statePI.setId("badState2");
                }
                histRow.add(statePI);
                PageItemString errs = new PageItemString(ent.getErrMsg(), false);
                histRow.add(errs);
                
                histTbl.addRow(histRow);
            }            
            ret.add(histTbl);
        }
        return ret;
    }
}
