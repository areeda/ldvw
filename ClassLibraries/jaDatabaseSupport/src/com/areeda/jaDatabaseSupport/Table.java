/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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

package com.areeda.jaDatabaseSupport;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 * represents an individual table in the database.
 * Provides common functions such as does it exist, how many records but the real purpose is
 * the creation and examining of table metadata.
 * 
 * @author joe areeda
 */
public class Table
{

    protected Column[] cols;
    protected String tableName;
    protected String lastError = "";
    protected Database db;
    protected HashMap<String, CType> fieldNames = null;
    protected TreeMap<String, Integer> unknownColumns = new TreeMap<String, Integer>();
    protected ResultSet allStream;

    protected Table()
    {
        // used in the package to set it up manually
    }

    public Table(Database db, String tblName)
    {
        this.db = db;
        this.tableName = tblName;
    }
    
    public Table(Database db, String tblName,Column[] myCols)
    {
        setDb(db);
        setName(tblName);
        setCols(myCols);
    }

    public final void setDb(Database db)
    {
        this.db = db;
    }

    protected final void setCols(Column[] myCols)
    {
        cols = myCols;
        fieldNames = new HashMap<String, CType>(myCols.length);
        for (Column c : cols)
        {
            c.setDbt(db.getDbt());
            fieldNames.put(c.getName(), c.getType());
        }
    }

    /**
     * table name
     * @return table name suitable for use in sql statements
     */
    public String getName()
    {
        return tableName;
    }

    /**
     * Set Table name
     * @param name the new name matching what's in the database
     */
    public final void setName(String name)
    {
        tableName = name;
    }

    /**
     *
     * @param chkCol - verify all our columns exist and are proper type (maybe others)
     * @return true if table exists and needed columns are there if that check enabled[
     */
    public final Boolean exists(boolean chkCol)
    {
        boolean ret = false;
        if (db.tableExists(tableName))
        {
            ret = true;
            if (chkCol)
            {
                // @TODO check the column definitions are same as we expect
            }
        }


        return ret;
    }

    public final Boolean createTable()
    {
        Boolean ret = true;
        String cmd = "CREATE TABLE " + tableName + "(\n";
        String colD = "";
        for (Column c : cols)
        {
            if (colD.length() > 0)
            {
                colD += ",\n";
            }
            colD += c.getColDef();
        }
        cmd += colD + ")";
        try
        {
            db.execute(cmd);
            if (!db.getConn().getAutoCommit())
            {
                db.getConn().commit();
            }
        }
        catch (SQLException ex)
        {
            ret = false;
            lastError = ex.getLocalizedMessage();
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, "Error executing [" + cmd + "]", ex);
        }
        return ret;
    }

    /**
     * Delete all records from this table, usually so you can rebuild it
     * @throws SQLException 
     */
    public void emptyTable() throws SQLException
    {
        String cmd = "DELETE FROM " + getName();
        db.execute(cmd);
    }
    /**
     * Another way to end up with a fresh empty table.  This one ensures the columns match the 
     * current definition.
     * @see #emptyTable() 
     */
    public void recreate()
    {
        db.dropTable(getName());
        createTable();
    }
    
    public void makeColumnsFromDB()
    {
        try
        {
            ArrayList<Column> ca = new ArrayList<Column>();
            
            ResultSet rs = db.executeQuery("SHOW COLUMNS FROM " + this.tableName);

            while (rs.next())
            {
                Column c = new Column(rs);
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void fatalError(String str)
    {
        JOptionPane.showMessageDialog(null, str, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public CType getColType(String cName)
    {
        CType ret = CType.IGNORE;
        if (fieldNames.containsKey(cName))
        {
            ret = fieldNames.get(cName);
        }
        return ret;
    }

    public int insertMap(Map<String, String> map)
    {
        int ret;
        String ins = "INSERT INTO " + tableName + "\n";
        String vals = "", fields="";

        for (Map.Entry<String, String> entry : map.entrySet())
        {
            String fldName = entry.getKey();
            String newVal = entry.getValue();
            if (fieldNames.containsKey(fldName))
            {
                if (vals.length() > 0)
                {
                    vals += ", ";
                    fields += ", ";
                }
                else
                {
                    fields = "( ";
                    vals = "( ";
                }
                fields += fldName + "\n";
                vals += Utils.sqlQuote(newVal) + "\n";
            }
            else
            {
                System.out.println("Unknown column: " + fldName + " = " + map.get(fldName));
                if (!unknownColumns.containsKey(fldName) || map.get(fldName).length() > unknownColumns.get(fldName))
                {
                    unknownColumns.put(fldName, map.get(fldName).length());
                }
            }
        }
        ret = doInsert(ins + fields + ") VALUES " + vals + ")");
        return ret;
    }

    public int insert(Object... fields)
    {
        int ret;
        String ins = "INSERT INTO " + tableName + " SET \n";
        int fn = 0, nf = fields.length;
        for (Column c : cols)
        {
            if (!c.getAutoIncrement())
            {
                if (fn >= nf)
                {
                    fatalError("Insert does not have enough arguments");
                }
                if (fn > 0)
                {
                    ins += ",\n";
                }
                ins += c.getInsert(fields[fn]);
                fn++;
            }
        }
        ret = doInsert(ins);
        return ret;
    }

    protected int doInsert(String ins)
    {
        int ret = -1;
        try
        {
            Statement s = db.createStatement2();

            switch(db.getDbt())
            {
                case MYSQL:
                    s.executeUpdate(ins, Statement.RETURN_GENERATED_KEYS);
                    ResultSet r = s.getGeneratedKeys();
                    if (r != null && r.next())
                    {
                        ret = r.getInt(1);
                    }
                    break;

                case SQLITE:
                    s.executeUpdate(ins);
                    ResultSet r1 = s.getGeneratedKeys();
                    if (r1 != null && r1.next())
                    {
                        ret = r1.getInt(1);
                    }
                    break;
            }

            s.close();
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, "SQL error on [" + ins + "]", ex);
        }
        return ret;
    }

    public TreeMap<String, Integer> getUnknownColumns()
    {
        return unknownColumns;
    }
    /**
     * Insert a new record into the table
     * @param flds TreeMap keys are column names, values are external form strings.
     * Note strings are parsed then formatted
     * @see writeRecObj if fields are in internal format.
     *
     */
    public void writeRec(TreeMap<String, String> flds)
    {
        String ins = "INSERT INTO " + getName() + " SET\n   ";
        String fld = "";

        for (Map.Entry<String, String> entry : flds.entrySet())
        {
            String key = entry.getKey();
            if (fld.length() > 0)
            {
                fld += ",\n   ";
            }
            CType fldType = getColType(key);
            if (fldType != CType.IGNORE)
            {
                String vStr = entry.getValue();
                String val;
                if (fldType != CType.STRING)
                {
                    Object v = fldType.parse(vStr);
                    val = fldType.dbVal(v);
                }
                else
                {

                    val = Utils.sqlQuote(vStr);
                }
                fld += key + "=" + val;
            }
        }
        if (fld.length() > 0)
        {
            doInsert(ins + fld);
        }
    }
    /**
     * Insert a new record into the table
     * @param flds TreeMap keys are column names, values are Objects corresponding to column types
     *
     */
    public void writeRecObj(TreeMap<String, Object> flds)
    {
        String ins = "INSERT INTO " + getName() + " SET\n   ";
        String fld = "";

        for (Map.Entry<String, Object> ent : flds.entrySet())
        {
            String key = ent.getKey();
            if (fld.length() > 0)
            {
                fld += ",\n   ";
            }
            CType fldType = getColType(key);
            if (fldType != CType.IGNORE)
            {
                Object v = ent.getValue();
                String val = fldType.dbVal(v);
                fld += key + "=" + val;
            }
        }
        if (fld.length() > 0)
        {
            doInsert(ins + fld);
        }
    }
    public long getRecordCount()
    {
        long ret = 0;
        try
        {
            String q = "select count(*) as nrec from " + tableName;
            ResultSet rs = db.executeQuery(q);

            if (rs.next())
            {
                ret = rs.getLong("nrec");
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(Table.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }
    public void lockForBulkUpdate() throws SQLException
    {
        
        db.execute("LOCK TABLES " + getName() + " WRITE");
        db.execute("ALTER TABLE " + getName() + " DISABLE KEYS");
    }
    
    public void unlockFromBulkUpdate() throws SQLException
    {
        
        db.execute("ALTER TABLE " + getName() + " ENABLE KEYS");
        db.execute("UNLOCK TABLES ");
    }

    /**
     * If the first column is an Integer, and an index key, delete the row matching id
     * @param id specifies which row to delete
     */
    public void deleteById(Integer id) throws SQLException
    {
        if (cols[0].getType() == CType.INTEGER && cols[0].isIndexKey())
        {
            String del = String.format("DELETE from %1$s WHERE %2$s=%3$d", getName(), cols[0].getName(),id);
            db.execute(del);
        }
    }

    public void optimize() throws SQLException
    {
        String cmd = "OPTIMIZE TABLE " + getName();
        db.execute(cmd);
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
        String query = "SELECT * from " + getName();
        allStream = myStmt.executeQuery(query);
    }
    
    public void streamByQuery(String query) throws SQLException
    {
        Statement myStmt = db.createStatement(1);
        allStream = myStmt.executeQuery(query);
    }
    
    public ResultSet streamNextRs() throws SQLException
    {
        ResultSet  ret = null;
        if (allStream != null && allStream.next())
        {
            ret = allStream;
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

}
