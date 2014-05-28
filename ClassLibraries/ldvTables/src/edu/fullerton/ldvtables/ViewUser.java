/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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
import com.areeda.jaDatabaseSupport.Utils;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageItemTextLink;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.*;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class ViewUser extends Table
{

    private String eduPersonPrincipalName;
    private String cn;
    private String mail;
    private String givenName;
    private String isMemberOf;
    private String remoteIP;
    
    // keep track of some stats
    private long dataRequests = 0;      // how many times did we hit the nds server
    private long bytesXfered = 0;       // how much did we get back
    private long totalMs = 0;           // how long did it take
    
    private long plotRequests=0;        // number of images requested
    private long prTotalMs=0;           // total time to generate those plots
    
    private UseLog ulog = null;         // db table for usage log

    // define the columns in our table
    private final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("eduPersonPrincipalName", CType.STRING, 1024, Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("lastSeen",  CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("firstSeen", CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("cn",        CType.STRING,   0,              Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("givenName", CType.STRING,   0,              Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("mail",      CType.STRING,   0,              Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("isMemberOf",CType.STRING,   0,              Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("nVisits",   CType.INTEGER,  Integer.SIZE,   Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    private final boolean localhostIsAdmin = false;    // for debugging login from local host can be a regular user or administrator
    private String mailFwdAddr;
    private String userAgent;
    
    public ViewUser(Database db)
    {
        super(db,"ViewUser");
        setCols(myCols);
    }
    public ViewUser(HttpServletRequest request, Database db)
    {
        super(db,"ViewUser");
        setCols(myCols);
        getRequestInfo(request);
    }
    public void logTransfer(long bytes, long ms)
    {
        dataRequests++;
        bytesXfered += bytes;
        totalMs += ms;
    }
    public void logPlots(int nPlots, long ms)
    {
        plotRequests+=nPlots;
        prTotalMs+= ms;
    }

    public long getNPlots()
    {
        return plotRequests;
    }
    public long getPlotMs()
    {
        return prTotalMs;
    }
    public long getBytesXfered()
    {
        return bytesXfered;
    }

    public long getDataRequests()
    {
        return dataRequests;
    }

    public long getTotalMs()
    {
        return totalMs;
    }
    

    public String getCn()
    {
        String ret = cn == null ? "local.admin" : cn;
        return ret;
    }

    public Database getDb()
    {
        return db;
    }

    public String getGivenName()
    {
        String ret = givenName == null ? "<name not available>" : givenName;
        return ret;
    }

    public String getIsMemberOf()
    {
        String ret = isMemberOf == null ? "" : isMemberOf;
        return ret;
    }

    public String getMailFwdAddr()
    {
        String ret = mailFwdAddr == null ? "" : mailFwdAddr;
        return ret;
    }
    
    public String getMail()
    {
        String ret = mail == null ? "joe@areeda.com" : mail;
        return ret;
    }

    public String getRemoteIP()
    {
        return remoteIP;
    }

    public String getEduPersonPrincipalName()
    {
        String ret = eduPersonPrincipalName == null ? "" : eduPersonPrincipalName;
        return ret;
    }
    

    private void getRequestInfo(HttpServletRequest request)
    {
        eduPersonPrincipalName = (String) request.getAttribute("eduPersonPrincipalName");
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = request.getHeader("eduPersonPrincipalName");
        }
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = "local.admin";
        }
        cn = (String) request.getAttribute("cn");
        if (cn == null || cn.isEmpty())
        {
            cn = request.getHeader("cn");
        }
        if (cn == null || cn.isEmpty())
        {
            cn = "Local Admin";
        }
        mail = (String) request.getAttribute("mail");
        if (mail == null || mail.isEmpty())
        {
            mail = request.getHeader("mail");
        }
        mailFwdAddr = (String) request.getAttribute("mailForwardingAddress");
        if (mailFwdAddr == null || mailFwdAddr.isEmpty())
        {
            mailFwdAddr = request.getHeader("mailForwardingAddress");
        }
        givenName = (String) request.getAttribute("givenName");
        if (givenName == null || givenName.isEmpty())
        {
            givenName = request.getHeader("AJP_givenName");
        }
        isMemberOf = (String) request.getAttribute("isMemberOf"); 
        if (isMemberOf == null || isMemberOf.isEmpty())
        {
            isMemberOf = request.getHeader("AJP_isMemberOf");
        }
        remoteIP = request.getRemoteAddr();
        userAgent = request.getHeader("user-agent");
    }

    /**
     * Tests the lowest privilege level.  If they aren't "valid" we won't do anything
     * @return true if they meet these requirements
     * 
     */
    public boolean isValid()
    {
        boolean ret = isLocalHost();
        ret |= getIsMemberOf().contains("LSCVirgoLIGOGroupMembers");
        
        return ret;
    }
    public boolean isLocalHost()
    {
        boolean ret = remoteIP.equalsIgnoreCase("127.0.0.1");       // to allow testing
        ret |= remoteIP.equalsIgnoreCase("0:0:0:0:0:0:0:1%0");
        ret |= remoteIP.equalsIgnoreCase("0:0:0:0:0:0:0:1");
        return ret;
    }
    /**
     * Tests the highest privilege level. If they are "admin" we let them do anything
     *
     * @return true if they meet these requirements
     *
     */
    public boolean isAdmin()
    {
        boolean ret = isLocalHost() && localhostIsAdmin;
        boolean admin = isValid() && getIsMemberOf().contains("ldvwAdmin");
        
        
        ret |= admin;
        return ret;
    }
    /**
     * The next highest privilege level allows access to experimental pages
     * 
     * @return true if they are members of an appropriate group
     */
    public boolean isTester()
    {
        boolean ret = isValid();
        ret &= isAdmin() || getIsMemberOf().contains("ldvwTester");
        return ret;
    }
    /**
     * A fresh user session has just been started, log it, load preferences
     */
    public void sessionStart() throws LdvTableException, SQLException
    {
        String q = String.format("SELECT * FROM %1$s where eduPersonPrincipalName = '%2$s'", getName(),eduPersonPrincipalName);
        ResultSet rs = db.executeQuery(q);
        
        if (rs.next())
        {
            String upd = "UPDATE " + getName() + " SET lastSeen=now(), cn=?, givenName=?, mail = ?, "
                         + "isMemberOf=?, nVisits=? ";
            upd += " WHERE eduPersonPrincipalName=?";
            PreparedStatement ps = db.prepareStatement(upd, Statement.NO_GENERATED_KEYS);
            ps.setString(1, cn);
            ps.setString(2, givenName);
            ps.setString(3, mail);
            ps.setString(4, isMemberOf);
            
            int nVisits = rs.getInt("nVisits");
            ps.setInt(5, nVisits+1);
            
            ps.setString(6, eduPersonPrincipalName);
            db.executeUpdate(ps);
        }
        else
        {
            String ins = "INSERT INTO " + getName() + " SET lastSeen=now(), firstSeen=now(), cn=?, givenName=?, mail = ?, "
                         + "isMemberOf=?, nVisits=1, eduPersonPrincipalName=? ";
           
            PreparedStatement ps = db.prepareStatement(ins, Statement.NO_GENERATED_KEYS);
            ps.setString(1, cn);
            ps.setString(2, givenName);
            ps.setString(3, mail);
            ps.setString(4, isMemberOf);
            ps.setString(5, eduPersonPrincipalName);
            db.executeUpdate(ps);
        }
        SessionDto sdto;
        sdto = new SessionDto();
        sdto.setEduPersonPrincipalName(eduPersonPrincipalName);
        sdto.setRemAddr(remoteIP);
        sdto.setUserAgent(userAgent);
        SessionTable st = new SessionTable(db);
        st.add(sdto);
    }
    /**
     * return a summary of user activity
     * @param linkBase if not empty base for image history link
     * @return object to be added to the page as needed
     * @throws WebUtilException anything goes wrong
     */
    public PageItemList getStats(String linkBase) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        try
        {
            if (ulog == null)
            {
                ulog = new UseLog(db);
            }

            ResultSet rs;
            ImageTable it = new ImageTable(db);
            TreeMap<String, Integer> imgsByUser = it.getCountByUser();
            TreeMap<String, Integer> viewsByUser = ulog.getViewsByUser();
            int users=0, last30=0, last7=0, last24h=0, visits = 0;

            rs = db.executeQuery("SELECT count(*) as cnt FROM " + getName());
            if (rs.next())
            {
                users = rs.getInt("cnt");
            }
            rs.close();

            rs = db.executeQuery("SELECT count(*) as cnt FROM " + getName() +
                    " where lastSeen >= adddate(now(), -30)");
            if (rs.next())
            {
                last30 = rs.getInt("cnt");
            }
            rs.close();

            rs = db.executeQuery("SELECT count(*) as cnt FROM " + getName()
                    + " where lastSeen >= adddate(now(), -7)");
            if (rs.next())
            {
                last7 = rs.getInt("cnt");
            }

            rs = db.executeQuery("SELECT count(*) as cnt FROM " + getName()
                    + " where lastSeen >= addtime(now(), \"-1 0:0:0\")");
            if (rs.next())
            {
                last24h = rs.getInt("cnt");
            }
            rs.close();

            rs = db.executeQuery("SELECT sum(nVisits) as visits FROM " + getName());
            if (rs.next())
            {
                visits = rs.getInt("visits");
            }

            ret.add(String.format("Total: %1$d sessions by %2$d people.  "
                    + "Last 30 days: %3$d, last 7 days: %4$d, last 24 hr: %5$d users ", 
                    visits, users, last30, last7, last24h));
            ret.addBlankLines(2);

            String[] hdr = {"Name", "Sessions&nbsp;&nbsp;&nbsp;", "Plots&nbsp;&nbsp;&nbsp;", 
                            "Views&nbsp;&nbsp;&nbsp;", "First Visit", "Last Visit"};
            PageTable tbl = new PageTable();
            tbl.setId("statsTbl");
            tbl.setSortable(true);
            tbl.setInitialSortColumn(5,1);
            PageTableRow r = new PageTableRow(hdr,false);
            r.setAlign(PageItem.Alignment.CENTER);
            r.setRowType(PageTableRow.RowType.HEAD);
            tbl.addRow(r);

            rs = db.executeQuery("SELECT eduPersonPrincipalName,cn,nVisits, firstSeen, lastSeen FROM " 
                                 + getName() + " ORDER BY lastSeen desc");
            boolean odd = true;

            while (rs.next())
            {
                PageTableRow vr = new PageTableRow();
                // change color on alternating rows of table
                //vr.setClassName(odd ? "odd" : "even");
                vr.setClassName("even");
                odd = !odd;
                String user = rs.getString("eduPersonPrincipalName");
                user = user == null ? "" : user;
                // common name
                cn = rs.getString("cn");
                
                // get the number of images they have
                Integer pc = imgsByUser.get(cn);
                pc = pc == null ? 0 : pc;
                if (pc > 0)
                {
                    PageItemTextLink imgHistLink= new PageItemTextLink(linkBase+cn, cn, "_blank");
                    vr.add(imgHistLink);
                }
                else
                {
                    vr.add(cn);
                }

                // visit count
                Integer nvis = rs.getInt("nVisits");
                PageItemString nv = new PageItemString(String.format("%1$d", nvis));
                nv.setAlign(PageItem.Alignment.RIGHT);
                vr.add(nv);

                // plot count
                
                PageItemString pci = new PageItemString(String.format("%1$d", pc));
                pci.setAlign(PageItem.Alignment.RIGHT);
                vr.add(pci);

                // images viewed count
                Integer vc  = viewsByUser.get(user);
                vc = vc == null? 0 : vc;
                PageItemString vci = new PageItemString(String.format("%1$d", vc));
                vci.setAlign(PageItem.Alignment.RIGHT);
                vr.add(vci);

                Timestamp ts = rs.getTimestamp("firstSeen");
                vr.add(Utils.timestamp2DateTime(ts.getTime()));

                ts = rs.getTimestamp("lastSeen");
                vr.add(Utils.timestamp2DateTime(ts.getTime()));

                tbl.addRow(vr);
            }
            rs.close();
            ret.add(tbl);
        }
        catch(Exception ex)
        {
            String ermsg = "Generate user stats: " + ex.getClass().getSimpleName() + " - " +
                           ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
        return ret;
    }

    public void setUlog(UseLog ulog)
    {
        this.ulog = ulog;
    }
    
    /**
     * Make a log entry for each page sent as html
     * This log will be used for security audits, performance evaluation
     * @param title
     * @param nBytes 
     * @param pageTime 
     * @throws java.sql.SQLException 
     */
    public void logPage(String title, int nBytes, int pageTime) throws SQLException
    {
        if (ulog != null)
        {
            String userId = eduPersonPrincipalName == null ? getCn() : eduPersonPrincipalName;        // null eduPersonPrincipalName is supposed to mean local administrator
            ulog.add(eduPersonPrincipalName, remoteIP, 
                        (int)db.getnQueries(), (int)db.getTotalMs(), 
                        (int)dataRequests, (int)bytesXfered, (int)totalMs, 
                        (int)plotRequests, (int)prTotalMs,
                        nBytes, pageTime, title);
        }
    }

}
