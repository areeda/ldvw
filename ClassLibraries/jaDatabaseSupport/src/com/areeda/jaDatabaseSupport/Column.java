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

import com.areeda.jaDatabaseSupport.Database.DbType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * represents a database column, definitions are used to create the appropriate sql clause to create
 * the table. 
 * @author joe areeda
 */
public class Column
{

    private String name;
    private CType type;
    private Integer length;
    private boolean notNull;
    private boolean indexKey;
    private boolean identity;
    private String defaultVal = "";
    private boolean unique;
    private boolean autoIncrement;
    private String comment;
    private Database.DbType dbt;



    /**
     * Create Column from
     * @param n - column name
     * @param t - CType of column
     * @param l - length of column (may be require, used, or ignored.  depends on type)
     * @param nl - can be null
     * @param idx - is an index column
     */
    public Column(String n, CType t, Integer l, Boolean nl, Boolean idx)
    {
        init(n, t, l, nl, idx, false, false);
    }

    /**
     * Create Column from
     * @param n - column name
     * @param t - CType of column
     * @param l - length of column
     * @param nl - can not be null
     * @param idx - is an index column
     * @param u - is unique
     * @param ai - is auto increment
     */
    public Column(String n, CType t, Integer l, Boolean nl, Boolean idx, Boolean u, Boolean ai)
    {
        init(n, t, l, nl, idx, u, ai);
    }

    private void init(String n, CType t, Integer l, Boolean nl, Boolean idx, Boolean u, Boolean ai)
    {
        name = n;
        type = t;
        length = l;
        notNull = nl;
        indexKey = idx;
        unique = u;
        autoIncrement = ai;
        identity = false;
    }

    /**
     * Create the column from a "show columns from" result set
     *
     * @param r - the result set
     */
    public Column(ResultSet r)
    {
        try
        {
            //  Field    | Type         | Null | Key | Default
            String n = r.getString("Field");
            String tn = r.getString("Type");
            String nl = r.getString("Null");
            String k = r.getString("Key");
            String def = r.getString("Default");
            this.name = n;


        }
        catch (SQLException ex)
        {
            Logger.getLogger(Column.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public void setIdentity(Boolean v)
    {
        identity = v;
    }

    public String getColDef()
    {
        String myType = type.dbType(length);
        String ret = name + " " + myType;
        if (notNull)
        {
            ret += " NOT NULL";
        }
        else if (type.equals(CType.TIMESTAMP))
        {
            ret += " DEFAULT 0";     // otherwise it becomes auto init auto update
        }
        if (unique && !indexKey)
        {
            ret += " UNIQUE";
        }
        if (autoIncrement)
        {
            switch(dbt)
            {
                case MYSQL:
                    ret += " AUTO_INCREMENT";
                    break;
                case SQLITE:
                    ret += " PRIMARY KEY AUTOINCREMENT";
                    break;
            }
        }
        else if (defaultVal.length() > 0)
        {
            ret += " DEFAULT " + defaultVal;
        }
        else if (identity  && dbt == Database.DbType.MYSQL)
        {
            ret += " GENERATED ALWAYS AS IDENTITY";
        }
        if (indexKey)
        {
            ret += ", INDEX " + " (" + name;
            if (myType.toLowerCase().contains("text") || myType.toLowerCase().contains("blob"))
            {
                ret += "(1024)";    // add a maximum key length
            }
            ret += ") ";
        }
        return ret;
    }
    public String getInsert(Object o)
    {
        String ret = name + "=" + type.dbVal(o);
        return ret;
    }

    public Boolean getAutoIncrement()
    {
        return autoIncrement;
    }

    public String getName()
    {
        return name;
    }

    public CType getType()
    {
        return type;
    }

    public void setDbt(DbType dbt)
    {
        this.dbt = dbt;
    }

    public boolean isIndexKey()
    {
        return indexKey;
    }

}
