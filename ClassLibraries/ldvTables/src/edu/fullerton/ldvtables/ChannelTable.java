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

import edu.fullerton.ldvjutils.ChanParts;
import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemImageLink;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;

import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * The channel table is an interface between the NDS client and the LigoDV database.
 * it is used to create and query the table.
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class ChannelTable extends Table
{
    private final Statement stmt;
    private final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE/8, Boolean.TRUE,   Boolean.TRUE,    Boolean.TRUE,  Boolean.TRUE),
        new Column("name",      CType.CHAR,     64,             Boolean.TRUE,   Boolean.TRUE,    Boolean.FALSE, Boolean.FALSE),
        new Column("server",    CType.CHAR,     48,             Boolean.TRUE,   Boolean.TRUE,    Boolean.FALSE, Boolean.FALSE),
        new Column("available", CType.BOOLEAN,  1,              Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("updating",  CType.BOOLEAN,  1,              Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("sampleRate",CType.FLOAT,    Float.SIZE/8,   Boolean.FALSE,  Boolean.TRUE,    Boolean.FALSE, Boolean.FALSE),
        new Column("tstPnt",    CType.INTEGER,  1,              Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("cType",     CType.CHAR,     16,             Boolean.FALSE,  Boolean.TRUE,    Boolean.FALSE, Boolean.FALSE),
        new Column("bytesPerSam",CType.INTEGER, 1,              Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("dType",     CType.CHAR,     16,             Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("gain",      CType.FLOAT,    Float.SIZE/8,   Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("offset",    CType.FLOAT,    Float.SIZE/8,   Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("slope",     CType.FLOAT,    Float.SIZE/8,   Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("units",     CType.CHAR,     64,             Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("firstSeen", CType.TIMESTAMP,Long.SIZE/8,    Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("lastMod",   CType.TIMESTAMP,Long.SIZE/8,    Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("cisAvail",  CType.CHAR,     1,              Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
        new Column("availId",   CType.INTEGER,  Integer.SIZE/8, Boolean.FALSE,  Boolean.FALSE,   Boolean.FALSE, Boolean.FALSE),
    };
    private final String colNames[] =
    {
        "myId", "name", "server", "sampleRate", "cType", "dtype"
    };
    private final String[] hdr =
    {
        "", "Name", "Sample<br/>Rate", "Type", "Server", "Data<br/>Type", "CIS"
    };
    private final String baseCISurl = "https://cis.ligo.org/channel/byname/";
    private final String infoIconDescUrl = "/ldvw/view/infoicon3.png";
    private final String infoIconNoDescUrl = "/ldvw/view/infoicon4.png";
    private PageItemImage infoDescIcon = null;
    private PageItemImage infoNoDescIcon = null;
    
    // bulk insert
    private int insertCount;
    private StringBuilder insertCommand=null;
    private final int insertNum=5000;
    // streaming retrieval
    private ResultSet allStream;
    private int minChanId, maxChanId;

    public ChannelTable(Database db) throws SQLException
    {
        this.db = db;
        setName("Channels");
        setCols(myCols);
        stmt = db.createStatement();
        minChanId = maxChanId = 0;
    }

    public ChannelTable(Database db, String altName) throws SQLException
    {
        this.db = db;
        setName(altName);
        setCols(myCols);
        stmt = db.createStatement();
    }
    public void setUpdateBit(String server) throws SQLException
    {
        String sub = "UPDATE " + this.getName() + " SET updating=TRUE WHERE server = '" + server + "'";
        db.execute(sub);
    }
    public void setAvailable(int id, boolean val) throws SQLException
    {
        String sval = val ? "TRUE" : "FALSE";
        
        String sub = "UPDATE " + this.getName() + " SET available=" + sval + ", lastMod=now()  WHERE myId = " + id ;
        db.execute(sub);
    }

    public void addIfNecessary(String server, ChanInfo ci) throws SQLException
    {
        ChanInfo cold = getRow(server,ci);
        if (cold == null)
        {
            insertNew(ci);
        }
        else if (cold.isSame(ci))
        {
            clearUpdate(cold);
        }
        else
        {
            update(cold,ci);
        }
    }

    public ChanInfo getRow(String server, ChanInfo ci) throws SQLException
    {
        ChanInfo ret = null;
        String q = "SELECT * FROM " + getName() + " WHERE ";
        q += "server ='" + server +"' AND name='" + ci.getChanName() + "' AND ";
        q += "cType='" + ci.getcType() + "'";
        
        ResultSet rs = db.executeQuery(q);
        if (rs.next())
        {
            ret = new ChanInfo();
            ret.fill(rs);
        }
        rs.close();
        return ret;
    }

    public void insertNew(ChanInfo ci) throws SQLException
    {
        String q = "INSERT INTO " + getName() + " SET ";
        ci.setUpdating(false);
        ci.setAvailable(true);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        ci.setFirstSeen(now);
        ci.setLastMod(now);
        q += ci.addSQLFields(ci);
        stmt.executeUpdate(q);
    }
    /**
     * buffer a series of insert statements and run them all at once for efficiency
     * call one last time with a null ChanInfo object to insert any remaining
     * @param ci Channel to insert
     * @throws SQLException 
     */
    public void insertNewBulk(ChanInfo ci) throws SQLException
    {
        // if they set up for bulk insert but didn't insert anything both will be null
        if (ci != null || (insertCommand != null && insertCommand.length() > 0))
        {
            if (insertCommand==null)
            {
                insertCommand = new StringBuilder(4000000);
            }
            if (insertCommand.length() == 0)
            {
                insertCommand.append("INSERT INTO ").append(getName()).append(" ");
                insertCommand.append(ChanInfo.getSqlFieldNames()).append("\n");
                insertCommand.append("VALUES\n");
            }
            if (ci != null)
            {
                ci.setUpdating(false);
                ci.setAvailable(true);
                Timestamp now = new Timestamp(System.currentTimeMillis());
                ci.setFirstSeen(now);
                ci.setLastMod(now);

                if (insertCount > 0)
                {
                    insertCommand.append(",\n");
                }
                else
                {
                    insertCommand.append("\n");
                }

                insertCommand.append(ci.getSqlFieldValues());
                insertCount++;
            }
            if ((ci == null || insertCount >= insertNum) && insertCommand.length() > 0)
            {
                String excmd = insertCommand.toString();
                db.execute(excmd);
                insertCommand.setLength(0);
                insertCount = 0;
            }
        }
    }

    public void update(ChanInfo cold, ChanInfo ci) throws SQLException
    {
        String q = "UPDATE " + getName() + " SET ";
        ci.setUpdating(false);
        ci.setAvailable(true);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        ci.setFirstSeen(cold.getFirstSeen());
        ci.setLastMod(now);
        q += ci.addSQLFields(ci);
        q += String.format(" WHERE myId = %1$d", cold.getId());
        db.execute(q);
        
    }

    public void clearUpdate(ChanInfo cold) throws SQLException
    {
        String q = String.format("UPDATE %1$s SET updating=false where myId=%2$s",getName(),cold.getId());
        db.execute(q);
    }

    public HashMap<ChanInfo,ChanInfo> getAsMap(String server) throws SQLException
    {
        HashMap<ChanInfo,ChanInfo> map = new HashMap<ChanInfo,ChanInfo>();
        Statement st = db.createStatement(1000);
        String q = "SELECT * from " + getName() + " WHERE server='" + server + "'";
        ResultSet rs = st.executeQuery(q);
        while (rs.next())
        {
            ChanInfo ci = new ChanInfo();
            ci.fill(rs);
            map.put(ci, ci);
        }
        rs.close();
        return map;
    }

    public TreeSet<ChanInfo> getAsSet(String server,String nameMatchString, int size) throws SQLException
    {
        return getAsSet(server,nameMatchString,"",size);
    }
    public TreeSet<ChanInfo> getAsSet(String server,String nameMatchString, String cType, int size) throws SQLException
    {
        TreeSet<ChanInfo> ret = new TreeSet<>();
        Statement st = db.createStatement(1000);
        String q = "SELECT * from " + getName();
        String w = " available = 1 ";
        if (server != null && !server.isEmpty())
        {
            w +=  " AND server='" + server + "'";
        }
        if (nameMatchString != null && !nameMatchString.isEmpty())
        {
            w += " AND name like '" + nameMatchString + "'";
        }
        if (cType != null && !cType.isEmpty())
        {
            w += " AND  cType = '" + cType + "'";
        }
        q = q + " WHERE " + w;

        try (ResultSet rs = st.executeQuery(q))
        {
            while (rs.next())
            {
                ChanInfo ci = new ChanInfo();
                ci.fill(rs);
                if (ci.getChanName() == null)
                {
                    System.err.println("null chanel name for id=" + rs.getInt("myId"));
                }
                else
                {
                    ret.add(ci);
                }
            }
        }
        return ret;
    }

    public HashSet<ChanInfo> getAsSet(Set<Integer> selections) throws SQLException
    {
        HashSet<ChanInfo> ret = new HashSet<ChanInfo>();
        
        String q = "SELECT * from " + getName() ;
        String where = "";
        for(Integer id: selections)
        {
            if (!where.isEmpty())
            {
                where += " OR ";
            }

            where += String.format(" myId=%1$d ",id);
        }
        if (!where.isEmpty())
        {
            q += " WHERE " + where;
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ChanInfo ci = new ChanInfo();
                ci.fill(rs);
                ret.add(ci);
            }
        }
        return ret;
    }
    
    public int getMatchCount(String filter)
    {
        int ret = 0;
        String f = filter == null ? "" : filter;

        String q = "SELECT count(*) as nMatch FROM " + getName();
        if (f.length() > 0)
        {
            q += " WHERE " + f + " ";
        }
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = rs.getInt("nMatch");
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChannelTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    /**
     * Get a table to select channels from a database query
     * @param nCols
     * @param strt
     * @param count
     * @param sel
     * @param filter
     * @param selections
     * @return a table object ready for display with selection
     * @throws LdvTableException
     * @throws WebUtilException 
     */
    public PageTable getPageTable(int nCols, int strt, int count, boolean sel, 
                                  String filter, HashSet<Integer> selections) throws LdvTableException, WebUtilException
    {
        PageTable t = new PageTable();
        
        
        String q = "SELECT * FROM " + getName();
        if (filter.length() > 0)
        {
            q += " WHERE (" + filter + ") ";
        }
        
        q += " ORDER BY name,server,cType ";
        
        if ( strt != 0 || count != 0)
        {
            q += String.format(" limit %1$d, %2$d ", strt, count);
        }
        try
        {
            ResultSet rs = db.executeQuery(q);
            boolean doHdr = true;
            boolean odd = true;
            infoDescIcon = new PageItemImage(infoIconDescUrl, "chan info", "CIS");
            infoDescIcon.setDim(18, 18);
            ChanDataAvailability cda = new ChanDataAvailability(db);
            
            while(rs.next())
            {
                if (doHdr)
                {
                    t.addRow(getHdrRow());
                    doHdr = false;
                }
                
                PageTableRow d = new PageTableRow();        // d contains data for all channels
                PageTableRow d1 = new PageTableRow();       // d1 contains availability if we have it
                // change color on alternating rows of table
                d.setClassName(odd ? "odd" : "even");
                d1.setClassName(odd ? "odd" : "even");
                odd = !odd;
                
                ChanInfo ci = new ChanInfo();
                ci.fill(rs);
                
                Integer myId = rs.getInt("myId");
                String selName = String.format("selchan_%1$d", myId);
                PageFormCheckbox selcb = new PageFormCheckbox(selName, "");
                selcb.setClassName("selBox");
                if (selections.contains(myId))
                {
                    selcb.setChecked(true);
                    selections.remove(myId);
                }
                PageItemString cname = new PageItemString(ci.getChanName());
                PageTableColumn selcbCol = new PageTableColumn(selcb);
                PageTableColumn cnameCol = new PageTableColumn(cname);
                
                String availability = cda.getAvailability(ci.getAvailId());
                boolean gotAvailability = availability.length()>6;
                if (gotAvailability)
                {
                    selcbCol.setRowSpan(2);
                    cnameCol.setRowSpan(2);
                    PageTableColumn availCol = new PageTableColumn(availability);
                    availCol.setSpan(hdr.length-2);
                    d1.add(availCol);
                }
                
                d.add(selcbCol);
                d.add(cnameCol);
                
                d.add(getNumericCol(ci.getRate()));
                
                String ctyp = ci.getcType();
                PageTableColumn ctc = new PageTableColumn(ctyp);
                ctc.setAlign(PageItem.Alignment.CENTER);
                d.add(ctc);
                
                d.add(ci.getServer());
                d.add(ci.getdType());
                
                // Add link to CIS if we think they have anything for us
                if (ci.getCisAvail().equalsIgnoreCase("a") || ci.getCisAvail().equalsIgnoreCase("d"))
                {
                    PageItemImageLink infoLink = getCisLink(ci);
                    d.add(infoLink);
                }
                else
                {
                    d.add();    // else make it blank
                }
                t.addRow(d);
                if (gotAvailability)
                {
                    t.addRow(d1);
                }
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException(ex);
        }
        
        return t;
    }

    public ChanInfo getChanInfo(int id)
    {
        ChanInfo ret = null;
        String q = String.format("SELECT * FROM %1$s where myId=%2$d",getName(),id);
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = new ChanInfo();
                ret.fill(rs);
            }
        }
        catch (SQLException ex)
        {
            
        }
        return ret;
    }
    
    /**
     * Summarize all the channels in the db organized into a table by server and channel type
     * @return a PageItemList containing the display info
     * @throws SQLException
     * @throws WebUtilException 
     */
    public PageItemList getStatsByCType() throws SQLException, WebUtilException
    {
        PageItemList ret = new PageItemList();
        String servq = "select distinct server from Channels";
        ArrayList<String> servers = new ArrayList<String>();
        ResultSet rs = db.executeQuery(servq);
        while(rs.next())
        {
            servers.add(rs.getString("server"));
        }
        rs.close();
        TreeSet<String> cTypeset = new TreeSet<String>();
        TreeMap<String,HashMap<String,Integer>> srvrCnts = new TreeMap<String,HashMap<String,Integer>>();
        
        for(String s : servers)
        {
            HashMap<String,Integer> aserver = new HashMap<String, Integer>();
            
            String sCntr = "select cType,count(cType) as cnt from Channels where server='" + s +"' group by cType";
            rs = db.executeQuery(sCntr);
            
            while(rs.next())
            {
                String ctyp = rs.getString("cType");
                aserver.put(ctyp, rs.getInt("cnt"));
                cTypeset.add(ctyp);
            }
            srvrCnts.put(s,aserver);
        }
        
        PageTable stbl = new PageTable();
        PageTableRow servNames = new PageTableRow();
        servNames.add("");  // that's where ctype will go
        servNames.add("Total");
        for (String s : servers)
        {
            servNames.add(s);
        }
        servNames.setRowType(PageTableRow.RowType.HEAD);
        stbl.addRow(servNames);
        Integer[] totals = new Integer[servers.size() + 1];
        for(int i=0;i<=servers.size();i++)
        {
            totals[i] = 0;
        }
            
        boolean odd = true;
        for(String ctyp : cTypeset)
        {
            PageTableRow typeCounts = new PageTableRow();
            typeCounts.setClassName(odd ? "odd" : "even");
            odd = !odd;
            
            String rlbl = ctyp;
            
                
            PageItemString rlblPI = new PageItemString(rlbl,false);
            rlblPI.setAlign(PageItem.Alignment.CENTER);
            typeCounts.add(rlblPI);
            
            Integer[] counts = new Integer[servers.size()+1];
            Integer c=1;
            
            for (String s : servers)
            {
                HashMap<String,Integer> aserver = srvrCnts.get(s);
                Integer cnt = aserver.get(ctyp);
                cnt = cnt==null ? 0 : cnt;
                if (rlbl.toLowerCase().contains("trend"))
                {
                    cnt = (cnt+4)/5;
                }
                counts[c++] = cnt;
            }
            counts[0] = 0;
            for(c=1;c<counts.length;c++)
            {
                counts[0] += counts[c];
                totals[0] += counts[c];
                totals[c] += counts[c];
            }
            
            for(Integer c1 : counts)
            {
                if (c1 > 0)
                {
                    typeCounts.add(c1);
                }
                else
                {
                    typeCounts.add("");
                }
                
            }
            stbl.addRow(typeCounts);
        }
        PageTableRow totalCounts = new PageTableRow();
        totalCounts.setClassName(odd ? "odd" : "even");
        odd = !odd;
        
        PageItemString totPI = new PageItemString("Totals", false);
        totPI.setAlign(PageItem.Alignment.CENTER);
        totalCounts.add(totPI);
        totalCounts.add(totals);
        stbl.addRow(totalCounts);
        
        ret.add(stbl);
        return ret;
    }
    public PageItemList getStatsByIfoSubsys() throws SQLException, WebUtilException
    {
        PageItemList ret = new PageItemList();
        
        String[] servers = ChanParts.getServers();
        String[] ifos = ChanParts.getIFOList();
        String[] subsystems2 = ChanParts.getSubSystems();
        String[] ctypes = getCTypeList();
        
        String[] subsystems = new String[subsystems2.length+1];
        System.arraycopy(subsystems2, 0, subsystems, 0, subsystems2.length);
        subsystems[subsystems2.length] = "";
        
        // create the header rows, they will be the same for all tables
        PageTableRow pthdr = new PageTableRow();
        pthdr.setRowType(PageTableRow.RowType.HEAD);
        PageTableRow pthdr2 = new PageTableRow();
        pthdr2.setRowType(PageTableRow.RowType.HEAD);
        
        // and a copy of the header to stick into this possibly long table
        PageTableRow phdr = new PageTableRow();
        PageTableRow phdr2 = new PageTableRow();
        
        PageTableColumn lbl = new PageTableColumn("IFO:subsys");
        lbl.setRowSpan(2);
        pthdr.add(lbl);
        phdr.add(lbl);
        
      
        
        for(String srv : servers)
        {
            PageTableColumn srvhdr = new PageTableColumn(srv);
            srvhdr.setSpan(ctypes.length);
            pthdr.add(srvhdr);
            phdr.add(srvhdr);
            for(String ctyp : ctypes)
            {
                pthdr2.add(ctyp);
                phdr2.add(ctyp);
            }
        }
        
        PageTableColumn tot = new PageTableColumn("Total");
        tot.setRowSpan(2);
        pthdr.add(tot);
        phdr.add(tot);       
        
        String qcnt="select count(*) as cnt, server, cType from Channels where name like "
                + "? and server=? and cType=?";
        PreparedStatement cntps = db.prepareStatement(qcnt, Statement.NO_GENERATED_KEYS);
        
        String qtcnt = "select count(*) as cnt from Channels where name like ?";
        PreparedStatement tcntps = db.prepareStatement(qtcnt, Statement.NO_GENERATED_KEYS);
        
        
        
        for(String ifo : ifos)
        {
            int rowCnt = 0;
            boolean odd = true;
            PageTable pt = new PageTable();
            pt.addRow(pthdr);
            pt.addRow(pthdr2);
            
            for(String subsys : subsystems)
            {
                String chSpec = ifo + ":" + subsys;
                tcntps.setString(1, chSpec + "%");
                
                ResultSet trs = db.executeQuery(tcntps);
                int tcnt=0;
                if (trs != null && trs.next())
                {
                    tcnt = trs.getInt("cnt");
                }
                if (tcnt > 0)
                {   // only if there is something for this IFO:Subsys
                    rowCnt++;
                    if (rowCnt % 25 == 0)
                    {   // it's a long table so add headers along the way
                        pt.addRow(phdr);
                        pt.addRow(phdr2);
                    }
                    int rowTotal=0;
                    PageTableRow stat = new PageTableRow();
                    stat.setClassName(odd ? "odd" : "even");
                    odd = !odd;
                    
                    String chSpecifier = subsys.isEmpty() ? ifo + " - total" : chSpec;
                    stat.add(chSpecifier);
                    
                    for (String srv : servers)
                    {
                        for (String ctyp : ctypes)
                        {
                            cntps.setString(1, chSpec + "%");
                            cntps.setString(2, srv);
                            cntps.setString(3, ctyp);
                            int cnt = 0;
                            ResultSet rs = db.executeQuery(cntps);
                            if (rs != null && rs.next())
                            {
                                cnt = rs.getInt("cnt");
                            }
                            if (ctyp.toLowerCase().contains("trend"))
                            {
                                cnt = (cnt+4)/5;
                            }
                            rowTotal += cnt;
                            if (cnt > 0)
                            {
                                stat.add(cnt);
                            }
                            else
                            {
                                stat.add("");
                            }
                        }
                    }
                    stat.add(rowTotal);
                    pt.addRow(stat);
                }
            }
            ret.add(pt);
            ret.addBlankLines(2);
        }
        
        
        return ret;
    }
    //===========================Private Methods============================
    public PageTableRow getHdrRow() throws WebUtilException
    {
        PageTableRow r = new PageTableRow();
        for (String h : hdr)
        {
            if(h.isEmpty())
            {
                h = "&nbsp;";
            }
            r.add(new PageItemString(h, false));
        }
        r.setRowType(PageTableRow.RowType.HEAD);

        return r;
    }
    private PageTableColumn getNumericCol(Float fs) throws WebUtilException
    {
        String str;
        if (fs > 0.9999)
        {
            str = String.format("%1$.0f", fs);
        }
        else
        {
            str = String.format("%1$.3f", fs);
        }
        PageTableColumn fsc = new PageTableColumn(str);
        fsc.setAlign(PageItem.Alignment.RIGHT);
        return fsc;
    }

    /**
     * Link into the Channel Information system
     * 
     * @param ci our channel info object
     * @return an image link
     */
    public PageItemImageLink getCisLink(ChanInfo ci)
    {
        if (infoDescIcon == null)
        {
            infoDescIcon = new PageItemImage(infoIconDescUrl, "chan info", "CIS");
            infoDescIcon.setDim(24, 24);
        }
        if (infoNoDescIcon == null)
        {
            infoNoDescIcon = new PageItemImage(infoIconNoDescUrl, "chan info", "CIS");
            infoNoDescIcon.setDim(24, 24);
        }
        
        String cname = ci.getChanName();
        if (cname.contains("."))
        {   // trend data has .mean or .max
            cname = cname.substring(0, cname.lastIndexOf("."));
        }
        String cisUrl = baseCISurl + cname;
        PageItemImageLink infoLink;
        if (ci.getCisAvail().equalsIgnoreCase("d"))
        {
            infoLink = new PageItemImageLink(cisUrl, infoDescIcon, "_blank");
        }
        else
        {
            infoLink = new PageItemImageLink(cisUrl, infoNoDescIcon, "_blank");
        }
        
        return infoLink;
    }
    /**
     * get the list of channel types
     * @return distinct channel types in db
     */
    public String[] getCTypeList()
    {
        String q = "select distinct cType from " + getName();
        ArrayList<String> ctypes = new ArrayList<String>();
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ctypes.add(rs.getString("cType"));
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChannelTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        String[] t = new String[0];
        return ctypes.toArray(t);
    }
    /**
     * get a partial list of ChanInfo objects in order by name
     * @param server match server
     * @param chanType match channel type
     * @param start start in the list
     * @param cnt number of channels to return
     * @return a list of channel info objects specified
     * @throws SQLException 
     */
    public ArrayList<ChanInfo> getList(String server, String chanType, int start, int cnt) throws SQLException
    {
        ArrayList<ChanInfo> ret = new ArrayList<>();
        String q = "SELECT * FROM " + getName() + " WHERE " 
        + " server = '" + server + "' and cType like '" + chanType + "' order by name ";
        q += String.format(" limit %1$d, %2$d", start, cnt);
        ResultSet rs=null;
        try 
        {
            rs = db.executeQuery(q);
            while(rs.next())
            {
                ChanInfo ci = new ChanInfo();
                ci.fill(rs);
                ret.add(ci);
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }

        return ret;
    }
    /**
     * get a partial list of Channel Id's in order by name
     *
     * @param server empty string for all, null not allowed
     * @param chanType Channel Type, empty string for all, null not allowed
     * @param start start in the list
     * @param cnt number of channels to return
     * @return a list of channel info objects specified
     * @throws SQLException
     */
    public ArrayList<Integer> getIdList(String server, String chanType) throws SQLException
    {
        ArrayList<Integer> ret = new ArrayList<>();
        String q = "SELECT * FROM " + getName();
        String where = "";
        if (! server.isEmpty())
        {
            where += " server = '" + server + "' ";
        }
        if ( ! chanType.isEmpty())
        {
            if (! where.isEmpty())
            {
                where += " and ";
            }
            where += "cType = '" + chanType + "' ";
        }
        if (chanType.toLowerCase().contains("trend"))
        {
            if (! where.isEmpty())
            {
                where += " and ";
            }
            where += " name like '%mean'";
        }
        if (! where.isEmpty())
        {
            q += " WHERE " + where;
        }
        ResultSet rs = null;
        try
        {
            rs = db.executeQuery(q);
            while (rs.next())
            {
                int id = rs.getInt("myId");
                ret.add(id);
            }
        }
        finally
        {
            if (rs != null)
            {
                rs.close();
            }
        }

        return ret;
    }

    public ResultSet findChannelByServerType(String server, String chanType, int offset, int count) throws SQLException
    {
        String q =  "SELECT * FROM " + getName() + " WHERE " 
                    + " server = '" + server + "' and cType like '" + chanType + "' order by name "
                    + " limit " + Integer.toString(offset) + ", " + Integer.toString(count);
        
        ResultSet ret = db.executeQuery(q);
        
        return ret;
    }
    /**
     * count the number of active channels in the table
     * @param server match server name
     * @param chanType match channel type
     * @return the number of channels of specified type on that server
     */
    public int getCount(String server, String chanType)
    {
        String q = "SELECT count(*) as cnt FROM " + getName();
        String where = "";
        if (server != null && !server.isEmpty())
        {
            where += " server = '" + server;
        }
        if (chanType != null && !chanType.isEmpty())
        {
            if (! where.isEmpty())
            {
                where += " AND ";
            }
        
            where += "' AND cType like '" + chanType + "' ";
        }
        if (!where.isEmpty())
        {
            where += " AND ";
        }
        where += " available > 0";
        if (!where.isEmpty())
        {
            q += " WHERE " + where;
        }
        

        int ret = 0;
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = rs.getInt("cnt");
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ChannelTable.class.getName()).log(Level.SEVERE, null, ex);
        }

        return ret;
    }
    /**
     * Find a channel from name for plotting current data
     * @param chanName it can be a pattern but probably works better with a full name
     * @return a single channel id or 0 if nothing was found
     * @throws SQLException problem with search
     */
    public int getBestMatch(String chanName) throws SQLException
    {
        int ret = 0;
        Set<ChanInfo> matches=getAsSet("", chanName, 20);
        int nmatches = matches.size();
        ChanInfo bestGuess = null;
        for(ChanInfo ci: matches)
        {
            if (bestGuess == null)
            {   // anything is better than nothing
                bestGuess = ci;
            }
            else if (ci.getcType().equalsIgnoreCase("raw") && !bestGuess.getcType().equalsIgnoreCase("raw"))
            {   // raw is best
                bestGuess = ci;
            }
            else if (ci.getcType().equalsIgnoreCase("raw") && bestGuess.getcType().equalsIgnoreCase("raw"))
            {   // data at the observatories is generally fresher
                String bsrv = bestGuess.getServer();
                String csrv = ci.getServer();
                if (bsrv.equals("nds.ligo.caltech.edu") && csrv.matches(".*\\.ligo-.*\\.caltech.edu")  )
                {
                    bestGuess = ci;
                }
            }
            else if (ci.getcType().equalsIgnoreCase("rds") && !bestGuess.getcType().contains("trend"))
            {
                bestGuess = ci;
            }
            else if (ci.getcType().equalsIgnoreCase("rds") && bestGuess.getcType().equalsIgnoreCase("rds"))
            {   // data at the observatories is generally fresher
                String bsrv = bestGuess.getServer();
                String csrv = ci.getServer();
                if (bsrv.equals("nds.ligo.caltech.edu") && csrv.matches(".*\\.ligo-.*\\.caltech.edu"))
                {
                    bestGuess = ci;
                }
            }
        }
        if (bestGuess != null)
        {
            ret = bestGuess.getId();
        }
        return ret;
    }
    // ========== CIS availability functions =============
    /**
     * Set all channels to signify no CIS info available, usually used to start import of CIS tables
     * @throws SQLException - probably a bug in the update command
     */
    public void clearCisAvail() throws SQLException
    {
        String sub = "UPDATE " + this.getName() + " SET cisAvail=' ' ";
        db.execute(sub);
    }
    /**
     * Update the CIS availabilty status for a list of channels
     * @param clist - list of Integer id (record keys)
     * @param val 1 character value
     * @throws SQLException probably a bug in my statement or value is more than 1 char
     */
    public void setCisAvail(Collection<Integer> clist, String val) throws SQLException
    {
        
        String upd = String.format("UPDATE %1$s SET cisAvail='%2$s' WHERE ", getName(), val);
        String cmd = upd;
        int count=0;
        for(Integer cnum : clist)
        {
            if (count > 0 && count % 1000 == 0)
            {
                db.execute(cmd);
                count =0;
                cmd = upd;
            }
            if (count > 0)
            {
                cmd += " OR ";
            }
            cmd += String.format(" myId=%1$d ", cnum);
            count++;
        }
        if (count > 0)
        {
            db.execute(cmd);
        }
    }
    /**
     * Get a streaming result set of name and id for use in updating CIS
     * @return the resultset which much be closed before any other operation on this connection can be done
     */
    public ResultSet getAllNameId() throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        String query="SELECT myId, name from " + getName();
        ResultSet ret = myStmt.executeQuery(query);
        return ret;
    }
    /**
     * Open a streaming result set of all Channels.
     * 
     * Note that no other operations can be performed on this connection until the stream is closed.
     * 
     * @see #streamNext() 
     * @see #streamClose() 
     * @throws SQLException 
     */
    public void streamAll() throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        String query="SELECT * from " + getName();
        allStream = myStmt.executeQuery(query);
    }
    /**
     * Open a streaming result set of all Channels whose name matches the parameter.
     * 
     *
     * Note that no other operations can be performed on this connection until the stream is closed.
     *
     * @param chanNameMatchStr an SQL "like" string wild cards ? and % can be used.  Not case sensitive
     * @see #streamNext()
     * @see #streamClose()
     * @throws SQLException
     */
    public void streamByName(String chanNameMatchStr) throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        String query = "SELECT * from " + getName() + " WHERE name like '" + chanNameMatchStr + "'";
        allStream = myStmt.executeQuery(query);
    }
    /**
     * Get the next channel information object from the open stream
     * @return  the ChanInfo object or null is no stream or end of list
     * @throws SQLException 
     */
    public ChanInfo streamNext() throws SQLException
    {
        ChanInfo ret = null;
        if (allStream != null && allStream.next())
        {
            ret = new ChanInfo();
            ret.fill(allStream);
        }
        return ret;
    }
    
    /**
     * close the currently open stream.
     * @see #streamAll() 
     * @see #streamClose() 
     * @throws SQLException 
     */
    public void streamClose() throws SQLException
    {
        if (allStream != null)
        {
            allStream.close();
            allStream = null;
        }
    }

    public int getMinChanId() throws SQLException
    {
        if (maxChanId == 0)
        {
            getMinMaxId();
        }
        return minChanId;
    }

    public int getMaxChanId() throws SQLException
    {
        if (maxChanId == 0)
        {
            getMinMaxId();
        }
        return maxChanId;
    }

    private void getMinMaxId() throws SQLException
    {
        String q = "select min(myId) as min, max(myId) as max from Channels";
        ResultSet rs = stmt.executeQuery(q);
        if (rs!= null && rs.next())
        {
            minChanId = rs.getInt("min");
            maxChanId = rs.getInt("max");
        }
    }
}
