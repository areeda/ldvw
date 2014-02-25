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

import edu.fullerton.ldvjutils.NdsServStat;
import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import com.areeda.jaDatabaseSupport.Utils;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsStatsTable extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("testTime",  CType.TIMESTAMP,Long.SIZE,      Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("server",    CType.STRING,   255,            Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("state",     CType.STRING,   255,            Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("conTimeMs", CType.INTEGER,  Integer.SIZE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("cntTimeMs", CType.INTEGER,  Integer.SIZE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("chanCount", CType.INTEGER,  Integer.SIZE,   Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("errMsg",    CType.STRING,   16383,          Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
    };

    public NdsStatsTable(Database db) throws SQLException
    {
        this.db = db;
        setName("NdsStats");
        setCols(myCols);
    }

    public void add(String server, String state, int conTime, int cntTime, int chanCnt, String ermsg) 
            throws SQLException
    {
        String ins = "INSERT INTO " + getName() + " SET " +
                     "testTime = now(), server='" + server + "', " +
                     "state = '" + state + "', \n" +
                     "conTimeMs = " + Integer.toString(conTime) + ", " +
                     "cntTimeMs = " + Integer.toString(cntTime) + ", " +
                     "chanCount = " + Integer.toString(chanCnt) + ", \n" +
                     "errMsg = " + Utils.sqlQuote(ermsg);
        
        db.execute(ins);
    }

    /**
     * Generate summary statistic for all servers
     * @return list of server stats
     * @throws SQLException problem with the database
     */
    public ArrayList<NdsServStat> getStats() throws SQLException
    {
        String q = "select server,count(*) as n,state,min(testTime) as minDate,max(testTime) as maxDate,"
                + "min(conTimeMs) as conMin,max(conTimeMs)as conMax,avg(conTimeMs) as conAvg,"
                + "stddev(conTimeMs) as conSD,min(cntTimeMs) as CntMin,max(cntTimeMs) as cntMax,"
                + "avg(cntTimeMs) as cntAvg,stddev(cntTimeMs) as cntSD from "
                + getName() + " group by server,state;";
        

        ArrayList<NdsServStat> ret = new ArrayList<NdsServStat>();
       
        
        ResultSet rs = db.executeQuery(q);
        while(rs.next())
        {
            NdsServStat nss = new NdsServStat();
            nss.fill(rs);
            
            ret.add(nss);
        }
        
        return ret;
    }

    /**
     * Generate a condensed list of entries for this server
     * @param site FQDN of server
     * @return list of history records
     * @throws LdvTableException - probably a database error
     */
    public ArrayList<NdsHistory> getHistory(String site) throws LdvTableException
    {
        ArrayList<NdsHistory> ret = new ArrayList<NdsHistory>();
        try
        {
            String q = "SELECT testTime,state,chanCount,errMsg FROM " + getName()
                       + " WHERE server='" + site + "' ORDER BY testTime";
            
            ResultSet rs = db.executeQuery(q);
            
            NdsHistory cur=null;
            
            while(rs.next())
            {
                Timestamp testTime = rs.getTimestamp("testTime");
                String state = rs.getString("state");
                int chanCount = rs.getInt("chanCount");
                String errMsg = rs.getString("errMsg");
                
                if (cur == null)
                {
                    cur = new NdsHistory(testTime, state, errMsg, chanCount);
                }
                else if (! cur.combineIfPossible(testTime, state, errMsg, chanCount))
                {
                    ret.add(cur);
                    cur = new NdsHistory(testTime, state, errMsg, chanCount);
                }
            }
            if (cur != null)
            {
                ret.add(cur);
            }
        }
        catch (SQLException ex)
        {
            String msg = "Error getting nds history: " + ex.getClass().getSimpleName() + " - " + ex.getLocalizedMessage();
            throw new LdvTableException(msg);
        }
        return ret;
    }
   
}
