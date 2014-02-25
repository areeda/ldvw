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
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TreeMap;

/**
 * Represents the database table keeping logs of who did what and how login it took
 * 
 * @author joe
 */
public class UseLog extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId", CType.INTEGER, Integer.SIZE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("user", CType.CHAR, 64, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("whenSent", CType.TIMESTAMP, Long.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("remoteIP", CType.STRING, 50, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("dbQueryCount", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("dbTimeMs", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("xferCount", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("xferBytes", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("xferTimeMs", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("plotCount", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("plotTimeMs", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("pageSize", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("pageTimeMs", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("description", CType.STRING, 16384, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE)
    };
   
    public UseLog(Database db) throws SQLException
    {
        this.db = db;
        setName("UseLog");
        setCols(myCols);
    }
 
    public void add(String user, String remoteIP, int qCnt, int qTime, int xCnt, int xByt, int xTim, 
                    int pCnt, int pTim, int pagSiz, int pagTim, String descrip) throws SQLException
    {
        String addit = "INSERT INTO " + getName() + " SET ";
        addit += "user=?, whenSent=now(), remoteIP=?, ";            //#1,2
        addit += "dbQueryCount=?, dbTimeMs=?, ";                    //#3,4
        addit += "xferCount=?, xferBytes=?, xferTimeMs=?, ";        //#5,6,7
        addit += "plotCount=?, plotTimeMs =?,";                     //#8,9
        addit += "pageSize = ?, pageTimeMs =?, description=?";      // #10, 11, 12
        
        PreparedStatement addStmt = db.prepareStatement(addit, Statement.RETURN_GENERATED_KEYS);
        
        addStmt.setString(1, user);
        addStmt.setString(2, remoteIP);
        
        addStmt.setInt(3, qCnt);
        addStmt.setInt(4,qTime);
        
        addStmt.setInt(5, xCnt);
        addStmt.setInt(6, xByt);
        addStmt.setInt(7, xTim);
        
        addStmt.setInt(8, pCnt);
        addStmt.setInt(9, pTim);
        
        addStmt.setInt(10, pagSiz);
        addStmt.setInt(11, pagTim);
        addStmt.setString(12, descrip);
        db.executeUpdate(addStmt);
    }
    
    public PageItemList getStats() throws WebUtilException, SQLException
    {
        PageItemList pil = new PageItemList();
        PageTable tbl = new PageTable();
        String[] hdr =
        {
            "Count", "Avg KB", "Avg sec", 
            "DB ops", "DB tot time", 
            "NDS xfers", "NDS Bytes", "xfer rate", 
            "Plots", "Avg sec",
        };
        String[] pageIds = 
        {
            "select channels", "specify times", "results", "Image History", "Sent image", "Stats", 
            "Channel Table Stats"
        };
        String q = "SELECT "
                + "count(*) as cnt, avg(pageSize) as avgbyt, avg(pageTimeMs) as avgmsec,"
                + "sum(dbQueryCount) as dbops, sum(dbTimeMs) as totdbms,"
                + "sum(xferCount) as xfers, sum(xferBytes) as bytes, sum(xferTimeMs) as xtime,"
                + "sum(plotCount) as pcnt, avg(plotTimeMs) as apTim "
                + " FROM " + getName() + " WHERE description like '%"
                ;
        
        PageTableRow h0 = new PageTableRow();
        h0.setRowType(PageTableRow.RowType.HEAD);
        PageTableColumn c1 = new PageTableColumn("Page");
        c1.setRowSpan(2);
        c1.setAlign(PageItem.Alignment.CENTER);
        h0.add(c1);
        
        c1 = new PageTableColumn("HTML");
        c1.setSpan(3);
        c1.setAlign(PageItem.Alignment.CENTER);
        h0.add(c1);
        
        c1 = new PageTableColumn("Database");
        c1.setSpan(2);
        c1.setAlign(PageItem.Alignment.CENTER);
        h0.add(c1);

        c1 = new PageTableColumn("Network");
        c1.setSpan(3);
        c1.setAlign(PageItem.Alignment.CENTER);
        h0.add(c1);

        c1 = new PageTableColumn("Results");
        c1.setSpan(2);
        c1.setAlign(PageItem.Alignment.CENTER);
        h0.add(c1);
        
        tbl.addRow(h0);

        PageTableRow h = new PageTableRow(hdr);
        h.setRowType(PageTableRow.RowType.HEAD);
        tbl.addRow(h);
        boolean odd = true;
        
        for (String id : pageIds)
        {
            PageTableRow stat = new PageTableRow();
            stat.add(id);
            
            String qry = q + id + "%'";
            ResultSet rs = db.executeQuery(qry);
            if (rs.next())
            {
                Integer it;     // cast int to Integer so they get comma-fied
                Double dt;
                // pages
                it = rs.getInt("cnt");
                stat.add(it);
                dt = rs.getDouble("avgbyt");
                stat.add(dt.intValue());
                
                dt = rs.getDouble("avgmsec");
                stat.add(dt/1000);
                
                // database
                it = rs.getInt("dbops");
                stat.add(it);
                        
                dt = rs.getDouble("totdbms");
                stat.add(dt/1000);

                // nds transfers
                it = rs.getInt("xfers");
                if (it > 0)
                {
                    stat.add(it);

                    Long bytes = rs.getLong("bytes");
                    String s = WebUtils.hrInteger(bytes) + "B";
                    PageTableColumn col= new PageTableColumn(s);
                    col.setAlign(PageItem.Alignment.RIGHT);
                    stat.add(col);

                    Integer xferTimeMs = rs.getInt("xtime");
                    s = "n/a";
                    if (xferTimeMs > 0)
                    {
                        Long bps = bytes * 1000L / xferTimeMs;
                        s = WebUtils.hrInteger(bps) + "Bps";
                    }
                    col = new PageTableColumn(s);
                    col.setAlign(PageItem.Alignment.RIGHT);
                    stat.add(col);
                }
                else
                {
                    for(int i=0;i<3;i++)
                        stat.add(null);
                }
                // plots
                it = rs.getInt("pcnt");
                if (it > 0)
                {
                    stat.add(it);
                    dt = rs.getDouble("apTim");
                    stat.add(dt / 1000);
                }
                else
                {
                    for (int i = 0; i < 2; i++)
                    {
                        stat.add(null);
                    }
                }  
            }
            else
            {
                PageTableColumn oops = new PageTableColumn(" no data");
                oops.setSpan(hdr.length-1);
                stat.add(oops);
            }
            // change color on alternating rows of table
            
            stat.setClassName(odd ? "odd" : "even");
            odd = !odd;
            tbl.addRow(stat);
        }
        
        pil.add(tbl);
        return pil;
    }
    /**
     * Scan the log and count # of images viewed
     *
     * @return a map of user names to number of images
     */
    public TreeMap<String, Integer> getViewsByUser() throws LdvTableException
    {
        String q = "SELECT user,count(*) AS cnt FROM " + getName()
                   + " WHERE description LIKE \"Sent image%\" GROUP BY user";
        
        TreeMap<String, Integer> ret = new TreeMap<String, Integer>();
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                String usr = rs.getString("user");
                Integer cnt = rs.getInt("cnt");
                ret.put(usr, cnt);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting user's displayed image count: " + ex.getLocalizedMessage());
        }
        return ret;
    }

}
