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
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The base channel look up tables
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChannelIndex extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length              can't be null   index         unique        auto inc
        new Column("indexID",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("name",      CType.CHAR,     64,                 Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("nameHash",  CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("ifo",       CType.CHAR,     8,                  Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("subsys",    CType.CHAR,     32,                 Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("minRawRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("maxRawRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("minRdsRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("maxRdsRate",CType.FLOAT,    Float.SIZE / 8,     Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("hasRaw",    CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasRds",    CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasOnline", CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasMtrends",CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasStrends",CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasStatic", CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("hasTstPnt", CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("cisAvail",  CType.CHAR,     1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("nServers",  CType.INTEGER,  Integer.SIZE / 8,   Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE,  Boolean.FALSE),
        new Column("epochs",    CType.INTEGER,  Integer.SIZE / 8,   Boolean.FALSE,  Boolean.TRUE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("isCurrent", CType.BOOLEAN,  1,                  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE,  Boolean.FALSE)

    };
    // bulk insert
    private int insertCount;
    private StringBuilder insertCommand = null;
    private int insertNum = 5000;
    
    // streaming retrieval
    private ResultSet allStream;
    
    //
    private final HashMap<String,String> chanTypeLookup = new HashMap<>();
    /**
     * Set up the object and create the table if necessary
     * @param db
     * @throws SQLException 
     */
    public ChannelIndex(Database db) throws SQLException
    {
        this.db = db;
        setName("ChannelIndex");
        setCols(myCols);
        chanTypeLookup.put("online", "hasOnline");
        chanTypeLookup.put("raw","hasRaw");
        chanTypeLookup.put("rds","hasRds");
        chanTypeLookup.put("static","hasStatic");
        chanTypeLookup.put("second-trend", "hasStrends");
        chanTypeLookup.put("minute-trend", "hasMtrends");
        chanTypeLookup.put("test-point","hasTstPnt");
    }
    
    /**
     * buffer a series of insert statements and run them all at once for efficiency call one last
     * time with a null ChanInfo object to insert any remaining
     *
     * @param ci Channel to insert
     * @throws SQLException
     */
    public void insertNewBulk(ChanIndexInfo ci) throws SQLException
    {
        // if they set up for bulk insert but didn't insert anything both will be null
        if (ci != null || (insertCommand != null && insertCommand.length() > 0))
        {
            if (insertCommand == null)
            {
                insertCommand = new StringBuilder(4000000);
            }
            if (insertCommand.length() == 0)
            {
                insertCommand.append("INSERT INTO ").append(getName()).append(" ");
                insertCommand.append(ci.getSqlFieldNames()).append("\n");
                insertCommand.append("VALUES\n");
            }
            if (ci != null)
            {

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
    /**
     * Open a streaming result set of all Channels.
     *
     * Note that no other operations can be performed on this connection until the stream is closed.
     *
     * @see #streamNext()
     * @see #streamClose()
     * @throws SQLException
     */
    @Override
    public void streamAll() throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        String query = "SELECT * from " + getName();
        allStream = myStmt.executeQuery(query);
    }
    /**
     * Open a streaming result set of all Channels whose name matches the parameter.
     *
     * Note that no other operations can be performed on this connection until the stream is closed.
     *
     * @param namePat  an SQL "like" string wild cards ? and % can be used.  Not case sensitive
     * @see #streamNext()
     * @see #streamClose()
     * @throws SQLException
     */
    public void streamByName(String namePat) throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        String query = "SELECT * from " + getName() + " WHERE name like '" + namePat + "'";
        allStream = myStmt.executeQuery(query);
    }
    /**
     * Get the next channel information object from the open stream
     *
     * @return the ChanInfo object or null is no stream or end of list
     * @throws SQLException
     */
    public ChanIndexInfo streamNext() throws SQLException
    {
        ChanIndexInfo ret = null;
        if (allStream != null && allStream.next())
        {
            ret = new ChanIndexInfo();
            ret.fill(allStream);
        }
        return ret;
    }

    /**
     * close the currently open stream.
     *
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
    
    /**
     * Given the form parameters find matching channel index records
     * @param ifo
     * @param subsys
     * @param fsCmp
     * @param fs
     * @param cType
     * @param cnamePat
     * @param strt
     * @param limit
     * @param currentOnly
     * @return a list of matching records
     * @throws LdvTableException 
     */
    public ArrayList<ChanIndexInfo> search(String ifo, String subsys, String fsCmp, Float fs, 
                                           String cType, String cnamePat, int strt, int limit,
                                           boolean currentOnly) throws LdvTableException
    {
        String where = getWhere(ifo, subsys, fsCmp, fs, cType, cnamePat, currentOnly);
        return getSearchResults(where,strt, limit);
    }
    /**
     * Count how many matches to specification
     * 
     * @param ifo
     * @param subsys
     * @param fsCmp
     * @param fs
     * @param cType
     * @param cnamePat
     * @param currentOnly show only current channels
     * @return
     * @throws LdvTableException 
     * @see #getWhere(java.lang.String, java.lang.String, java.lang.String, java.lang.Float, java.lang.String, java.lang.String) 
     * 
     */
    public int getMatchCount(String ifo, String subsys, String fsCmp, Float fs,
                             String cType, String cnamePat, boolean currentOnly) 
                             throws LdvTableException
    {
        int ret = 0;
        
        String where = getWhere(ifo, subsys, fsCmp, fs, cType, cnamePat, currentOnly);
        String q = "SELECT count(*) AS cnt FROM " + getName() + " ";
        if (!where.isEmpty())
        {
            q += " WHERE " + where;
        }
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
            throw new LdvTableException(ex);
        }
        return ret;
    }
    /**
     * Take the search parameters and generate a where clause for the sql query
     * @param ifo
     * @param subsys
     * @param fsCmp
     * @param fs
     * @param cType
     * @param cnamePat pattern to match against channel name 
     * @param currentOnly only match currently acquired channels
     * @return where clause for mysql (without the keyword WHERE)
     */
    private String getWhere(String ifo, String subsys, String fsCmp, Float fs, 
                            String cType, String cnamePat, boolean currentOnly)
    {
        String where = "";

        if (!ifo.isEmpty())
        {
            where += where.isEmpty() ? "" : " AND ";
            where += " ifo = '" + ifo + "' ";
        }
        if (!subsys.isEmpty())
        {
            where += where.isEmpty() ? "" : " AND ";
            where += " subsys ='" + subsys + "' ";
        }
        if (fs > 0 )
        {
            where += where.isEmpty() ? "" : " AND ";
            if (fsCmp.contains("<"))
            {
                where += String.format(" ( (minRawRate %1$s %2$f AND minRawRate > 0) "
                        + "OR (minRdsRate %1$s %2$f AND minRdsRate > 0)", fsCmp, fs);
            }
            else if (fsCmp.contains(">"))
            {
                where += String.format(" ( (maxRawRate %1$s %2$f AND maxRawRate > 0) "
                + "OR (maxRdsRate %1$s %2$f AND maxRdsRate > 0) )", fsCmp, fs);
            }
            else
            {
                where += String.format(" ( maxRawRate %1$s %2$f OR maxRdsRate %1$s %2$f"
                + "OR minRawRate %1$s %2$f OR minRdsRate %1$s %2$f)", fsCmp, fs);
            }
        }
        if (!cType.isEmpty())
        {
            String fld = chanTypeLookup.get(cType.toLowerCase());
            if (fld != null)
            {
                where += where.isEmpty() ? "" : " AND ";
                where += " " + fld + " = 'T' ";
            }
        }
        
        if (currentOnly)
        {
            where += where.isEmpty() ? "" : " AND ";
            where += "isCurrent>0 ";
        }
        where = getNamePatWhere(ifo, subsys, cnamePat, where);
        return where;
    }
    /**
     * Convert the relevant input parameters into an SQL compare operation and add it to current
     * where clause
     * @param ifo
     * @param subsys
     * @param cnamePat
     * @param oldWhere existing where clause, may be empty or null
     * @return 
     */
    private String getNamePatWhere(String ifo, String subsys, String cnamePat, String oldWhere)
    {
        String where = oldWhere;
        
        if (!cnamePat.isEmpty())
        {
            if (where == null)
            {
                where = "";
            }
            if (needRegex(cnamePat))
            {
                where += where.isEmpty() ? "" : " AND ";
                where += String.format(" name regexp '%1$s' ", makeRegexp(cnamePat));
            }
            else
            {
                where += where.isEmpty() ? "" : " AND ";
                where += String.format(" name = '%1$s' ", cnamePat);
            }
        }
        return where;
    }
    /**
     * Test if the user entered match string can use a simple match or requires a regular expression
     * 
     * @param pat user's match string
     * @return true if we need a regular expression
     */
    public static boolean needRegex(String pat)
    {
        String chars = "* ()?|";     // these have special meanings
        pat = pat == null ? "" : pat.trim();
        boolean ret = false;
        for(int i=0;i<chars.length() && !ret;i++)
        {
            ret |= (pat.indexOf(chars.substring(i, i+1)) > -1 );
        }
        return ret;
    }

    /**
     * Make a mysql regexp from a subset of bash syntax The current implementation handles these
     * constructs: * matches zero or more characters ? matches any one character
     *
     * @param pat bash like pattern
     * @return mysql compatible regexp
     */
    private String makeRegexp(String pat)
    {
        String regexp = pat;
        regexp = regexp.replace(".", "\\.");
        regexp = regexp.replace("*", ".*");
        regexp = regexp.replace(" ", ".*");
        regexp = regexp.replace("?", ".");
        regexp = "(" + regexp + ")";
        return regexp;
    }

    private ArrayList<ChanIndexInfo> getSearchResults(String where, int strt, int count) throws LdvTableException 
    {
        ArrayList<ChanIndexInfo> ret = new ArrayList<>();
        String q = "SELECT * FROM " + getName() + " ";
        if (! where.isEmpty())
        {
            q += " WHERE " + where;
        }
        if (strt != 0 || count != 0)
        {
            q += String.format(" limit %1$d, %2$d ", strt, count);
        }
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ChanIndexInfo cii = new ChanIndexInfo();
                cii.fill(rs);
                ret.add(cii);
            }
        }
        catch (Exception ex)
        {
            throw new LdvTableException(ex);
        }
        
        return ret;
    }
    /**
     * Get the information on a Channel Index record by ID
     * @param id record key
     * @return the database record or null if not found
     * @throws LdvTableException
     * @throws SQLException 
     */
    public ChanIndexInfo getInfo(int id) throws LdvTableException, SQLException
    {
        ChanIndexInfo ret=null;
        String q = String.format("Select * from %1$s where indexID=%2$d", getName(), id);
        ResultSet rs = null;
        try
        {
            rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = new ChanIndexInfo();
                ret.fill(rs);
            }
        }
        
        finally
        {
            if (rs != null)
            {
                try
                {
                    rs.close();
                }
                catch (SQLException ex)
                {
                    
                }
            }
        }
        return ret;
    }
}
