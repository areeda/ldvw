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
import java.sql.SQLException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class StringTable  extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null  index        unique        auto inc
        new Column("myId", CType.INTEGER, Integer.SIZE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("lastModified", CType.TIMESTAMP, Long.SIZE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("module", CType.STRING, 40, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("content", CType.STRING, 16384, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE)
    };
    private static final String myName = "Strings";
    
    /**
     * 
     *
     * @param db database object to use
     * @throws SQLException problem creating new table
     */
    public StringTable(Database db) throws SQLException
    {
        this.db = db;
        setName(myName);
        setCols(myCols);
    }

    
}
