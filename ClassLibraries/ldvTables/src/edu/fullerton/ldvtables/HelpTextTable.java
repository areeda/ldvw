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
import edu.fullerton.ldvjutils.HelpInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class HelpTextTable extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null   index           unique         auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE),
        new Column("lastMod",       CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE),
        new Column("name",          CType.STRING,   40,             Boolean.FALSE,  Boolean.TRUE,   Boolean.FALSE, Boolean.FALSE),
        new Column("location",      CType.STRING,   256,            Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE),
        new Column("title",         CType.STRING,   256,            Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE),
        new Column("helpTxt",       CType.STRING,   16384,          Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE)
            
    };
    private static String myName = "HelpText";

    /**
     * constructor used currently only to create the table most usage will be through the static
     * methods
     *
     * @param db database object to use
     * @throws SQLException problem creating new table
     */
    public HelpTextTable(Database db) throws SQLException
    {
        this.db = db;
        setName(myName);
        setCols(myCols);
    }

    public int save(HelpInfo hi) throws LdvTableException
    {
        HelpInfo oldHi = new HelpInfo(hi);
        int id = find(oldHi);   // note find fills in the details to what was saved if found.
        String q, w;
        if (id > 0)
        {
            q = "UPDATE ";
            w = " WHERE myId=?";
        }
        else
        {
            q = "INSERT INTO ";
            w = "";
        }
        String s = q + getName() + " SET name=?, location=?, title=?, helpTxt=?, lastmod=now() " + w;
        try
        {
            PreparedStatement ps;
            ps = db.prepareStatement(s,
                    id == 0  ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS );
            ps.setString(1, hi.getName());
            ps.setString(2, hi.getLocation());
            ps.setString(3, hi.getTitle());
            ps.setString(4, hi.getHelpTxt());

            if (id > 0)
            {
                ps.setInt(5, id);
                db.executeUpdate(ps);
            }
            else
            {
                int r = db.executeUpdate(ps);

                if (r == 1)
                {
                    ResultSet rs = ps.getGeneratedKeys();
                    if (rs.next())
                    {
                        id = rs.getInt(1);
                    }
                }

            }

        }
        catch (SQLException ex)
        {
            String ermsg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
        return id;
    }

    public int find(HelpInfo hi) throws LdvTableException
    {
        String q = String.format("SELECT * FROM %1$s WHERE name = '%2$s'", getName(), hi.getName());
        int ret = 0;
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())            
            {
                hi.fill(rs);
                ret = hi.getId();
            }
        }
        catch (SQLException ex)
        {
            String ermsg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
        return ret;
    }
    
    public HelpInfo getHelpInfo(String name) throws LdvTableException
    {
        HelpInfo ret = new HelpInfo(name, "", "", "");
        find(ret);
        return ret;
    }
    public ArrayList<String> getAllNames() throws LdvTableException
    {
        ArrayList<String> ret = new ArrayList<String>();
        String q = "SELECT name FROM " + getName() + " ORDER BY name";
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ret.add(rs.getString("name"));
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting list of help entries", ex);
        }
        return ret;
    }
}
