/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents data we record when a new session is started
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SessionTable extends Table
{
    // define the columns in our table
    private final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("eduPersonPrincipalName", CType.STRING, 1024, Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE,  Boolean.FALSE),
        new Column("sessStart", CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("remAddr",   CType.STRING,   20,             Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("userAgent", CType.STRING,   256,              Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
    };    

    public SessionTable(Database db)
    {
        super(db,"Sessions");
        setCols(myCols);
    }
    
    public void add(SessionDto sdto) throws LdvTableException
    {
        String ins = "INSERT INTO " + getName() + " SET eduPersonPrincipalName=?,"
                     + "sessStart=now(), remAddr = ?, userAgent=?";
        try
        {
            PreparedStatement ps = db.prepareStatement(ins, Statement.NO_GENERATED_KEYS);
            ps.setString(1, sdto.getEduPersonPrincipalName());
            ps.setString(2, sdto.getRemAddr());
            ps.setString(3, sdto.getUserAgent());
            db.executeUpdate(ps);
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Adding to session table", ex);
        }
    }
}
