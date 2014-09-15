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

package edu.fullerton.ndstables;

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import ndsJUtils.FrameAvailability;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulates the availability of frames obtained from the DiskCacheAPI at each site
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class FrameAvailabilityTable  extends Table
{
    // bulk insert

    private int insertCount;
    private StringBuilder insertCommand = null;
    private final int insertNum = 200;

    private static final Column[] myCols =
    {
        //         name,        type            length          can't be null  index        unique        auto inc
        new Column("myId",      CType.INTEGER,  Integer.SIZE / 8, Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE,  Boolean.TRUE),
        new Column("site",      CType.CHAR,     16,               Boolean.TRUE, Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("obsType",   CType.CHAR,     48,               Boolean.TRUE, Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("startgps",  CType.INTEGER,  Long.SIZE / 8,    Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE),
        new Column("stopgps",   CType.INTEGER,  Long.SIZE / 8,    Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, Boolean.FALSE)
    };
    // Field names for bulk insert statement
    private static final String fldNames = " (site, obsType, startgps, stopgps) ";

    public FrameAvailabilityTable(Database db)
    {
        super(db, "FrameAvailability", myCols);
    }

    /**
     * buffer a series of insert statements and run them all at once for efficiency call one last
     * time with a null ChanInfo object to insert any remaining
     *
     * @param ci Channel to insert
     * @throws SQLException
     */
    public void insertNewBulk(FrameAvailability fa) throws SQLException
    {
        // if they set up for bulk insert but didn't insert anything both will be null
        if (fa != null || (insertCommand != null && insertCommand.length() > 0))
        {
            if (insertCommand == null)
            {
                insertCommand = new StringBuilder(4000000);
            }
            if (insertCommand.length() == 0)
            {
                insertCommand.append("INSERT INTO ").append(getName()).append(" ");
                insertCommand.append(fldNames).append("\n");
                insertCommand.append("VALUES\n");
            }
            if (fa != null)
            {

                if (insertCount > 0)
                {
                    insertCommand.append(",\n");
                }
                else
                {
                    insertCommand.append("\n");
                }

                insertCommand.append(fa.getSqlFieldValues());
                insertCount++;
            }
            if ((fa == null || insertCount >= insertNum) && insertCommand.length() > 0)
            {
                String excmd = insertCommand.toString();
                db.execute(excmd);
                insertCommand.setLength(0);
                insertCount = 0;
            }
        }
    }
    public Set<FrameAvailability> getAsSet(String site, String obsType) throws LdvTableException
    {
        TreeSet<FrameAvailability> ret = new TreeSet<>();
        try
        {
            String query = "SELECT * from " + getName() + " WHERE site = '" + site + "' and obsType = '"
                           + obsType + "'";
            streamByQuery(query);
            FrameAvailability fa;
            while ((fa = streamNext()) != null)
            {
                ret.add(fa);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Get Frame Availability as set. ", ex);
        }
        
        return ret;
    }
    public FrameAvailability streamNext() throws SQLException
    {
        FrameAvailability ret = null;
        if (allStream != null && allStream.next())
        {
            ret = new FrameAvailability();
            ret.fill(allStream);
        }
        return ret;
    }
}
