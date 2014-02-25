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

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.LdvTableException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanUpdateTable extends Table
{
    private Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId", CType.INTEGER, Integer.SIZE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("server", CType.CHAR, 64, Boolean.TRUE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("cType", CType.CHAR, 16, Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("chCount", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("chlsMd5", CType.CHAR, 32, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("filename", CType.STRING, 1024, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("needsUpd", CType.BOOLEAN, 1, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("whenAdded", CType.TIMESTAMP, Long.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("added", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("deleted", CType.INTEGER, Integer.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };
    
    public ChanUpdateTable(Database db) throws SQLException
    {
        this.db = db;
        setName("ChanUpdate");
        setCols(myCols);
    }
    public boolean checkAdd(ChanListSummary cls) throws LdvTableException, IOException
    {
        return checkAdd(cls.getServer(), cls.getcType(), cls.getCount(), cls.getMd5(), cls.getcListFilename());
    }
    /**
     * See if we need to update this set of channels
     * 
     * @param server
     * @param cType
     * @param count
     * @param md5
     * @return true if a new record was added indicating we need to process that file
     */
    public boolean checkAdd(String server, String cType, int count, String md5, String fName) 
            throws LdvTableException
    {
        boolean ret = false;
        String q = "SELECT * FROM " + getName() + " WHERE server='" + server + "' and cType ='"
                    + cType + "' ORDER BY whenAdded desc limit 1";
        
        try
        {
            boolean doAdd;
            ResultSet rs = db.executeQuery(q);
            if (rs.next())            
            {
                int rcnt = rs.getInt("chCount");
                String rmd5 = rs.getString("chlsMd5");
                doAdd = !(count == rcnt && md5.equalsIgnoreCase(rmd5));
                String clrOld = "UPDATE " + getName() + " SET needsUpd=false WHERE "
                                + "server='" + server + "' AND "
                                + "cType='" + cType +"'";
                try
                {
                    db.execute(clrOld);
                }
                catch (Exception ex2)
                {
                }
            }
            else
            {
                doAdd = true;
            }
            if (doAdd)
            {
                
                String ins = "INSERT INTO " + getName() + " SET "
                             + "server='" + server + "', "
                             + "cType='" + cType + "', "
                             + "chCount=" + Integer.toString(count) + ", "
                             + "chlsMd5='" + md5 + "', "
                             + "filename='" + fName + "', "
                             + "needsUpd=true,  whenAdded=now(), added=0, deleted=0";
                db.execute(ins);
                ret = true;
            }
        }
        catch (SQLException ex)
        {
            String ermsg = "Checking if a channel table update is needed: " + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
        return ret;
    }

    /**
     * Find all the files that have updates pending
     * @return the list of server/type that needs an update
     */
    public ArrayList<ChanListSummary> getPendingUpdates() throws SQLException
    {
        ArrayList<ChanListSummary> ret = new ArrayList<ChanListSummary>();
        String q = "SELECT * FROM " + getName() + " WHERE needsUpd=true";
        ResultSet rs = db.executeQuery(q);
        while (rs.next())
        {
            String srv=rs.getString("server");
            String ctyp = rs.getString("cType");
            int count = rs.getInt("chCount");
            String fName = rs.getString("filename");
            ChanListSummary cls = new ChanListSummary(srv, ctyp, count);
            cls.setcListFile(fName);
            ret.add(cls);
        }
        return ret;
    }

}
