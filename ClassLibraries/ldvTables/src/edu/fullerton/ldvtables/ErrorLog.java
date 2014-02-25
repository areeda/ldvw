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
package edu.fullerton.ldvtables;

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * Represents the internal error log table
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ErrorLog extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null  index        unique        auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("whenSent",      CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("module",        CType.STRING,    40,            Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("severity",      CType.INTEGER,    2,            Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("description",   CType.STRING,   16384,          Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE)
    };
    private static final String myName = "InternalErrorLog";

    /**
     * constructor used currently only to create the table
     * most usage will be through the static methods
     * @param db database object to use
     * @throws SQLException problem creating new table
     */
    public ErrorLog(Database db) throws SQLException
    {
        this.db = db;
        setName(myName);
        setCols(myCols);
    }
    /**
     * log an internal error, used when user information is unavailable
     * @param db database to use
     * @param module module that detected the error
     * @param severity 0-5 where 0 is information and 5 is fatal
     * @param description text to describe the problem
     */
    public static void logError(Database db, String module, int severity, String description)
    {
        try
        {
            if (!db.tableExists(myName))
            {
                ErrorLog me = new ErrorLog(db);     // that will create the table if we're the first
            }
            String ins = "INSERT INTO " + myName + " SET whenSent=now(), module=?, severity=?, description=?";
            PreparedStatement insps = db.prepareStatement(ins, Statement.NO_GENERATED_KEYS);
            insps.setString(1, module);
            insps.setInt(2, severity);
            insps.setString(3, description);
            db.executeQuery(insps);
        }
        catch(Exception ex)
        {
            System.err.println("Error logging an error (ouch): " + module + ", " + description);
        }
    }

}
