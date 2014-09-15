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
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ndsJUtils.NdsChanAvailPair;

/**
 * Table that links the channel description and available intervals
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsChanAvailMap extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null   index          unique         auto inc
        new Column("idx",           CType.INTEGER,  Integer.SIZE/8,     Boolean.TRUE, Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE),
        new Column("chanIdx",       CType.INTEGER,  Integer.SIZE/8,     Boolean.TRUE, Boolean.FALSE, Boolean.FALSE,Boolean.FALSE),
        new Column("availIdx",      CType.INTEGER,  Integer.SIZE/8,     Boolean.TRUE, Boolean.FALSE, Boolean.FALSE,Boolean.FALSE),
    };
    private Integer myId;
    private Integer chanIdx;
    private Integer availIdx;
    
    private PreparedStatement getAvailPs = null;
    private PreparedStatement insAvailPs = null;
    
    public NdsChanAvailMap(Database db) 
    {
        this.db = db;
        setName("ndsChanAvailMap");
        setCols(myCols);
        myId = 0;
        chanIdx = 0;
        availIdx = 0;
    }
    public List<NdsChanAvailPair> getAvail(int chanIdx) throws LdvTableException
    {
        try
        {
            ArrayList<NdsChanAvailPair> ret = new ArrayList<>();
            
            if (getAvailPs == null)
            {
                getAvailPs = db.prepareStatement("SELECT * FROM " + getName() + "  WHERE chanIdx = ?",
                                             Statement.NO_GENERATED_KEYS);
            }
            getAvailPs.setInt(1, chanIdx);
            ResultSet rs = db.executeQuery(getAvailPs);
            while(rs.next())
            {
                NdsChanAvailPair ncap = new NdsChanAvailPair(rs.getInt("idx"), rs.getInt("chanIdx"),
                        rs.getInt("availIdx"));
            }
            return ret;
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("NdsChanAvailMap get availablity list", ex);
        }
    }
    
    public int insert(NdsChanAvailPair ncap) throws SQLException
    {
        int ret = 0;
        if (insAvailPs == null)
        {
            insAvailPs = db.prepareStatement("INSERT INTO " + getName() + " SET chanIdx = ?, "
                                             + "availIdx = ?", Statement.RETURN_GENERATED_KEYS);
        }
        insAvailPs.setInt(1, ncap.getChanIdx());
        insAvailPs.setInt(2, ncap.getAvailIdx());
        db.executeUpdate(insAvailPs);
        ResultSet rs = db.getGeneratedKeys(insAvailPs);
        if (rs.next())
        {
            ret = rs.getInt(1);
        }
        return ret;
    }
    
    // bulk insert
    private int insertCount;
    private StringBuilder insertCommand = null;
    private final int insertNum = 5000;
    
    /**
     * buffer a series of insert statements and run them all at once for efficiency call one last
     * time with a null ChanInfo object to insert any remaining
     *
     * @param ncap Channel to insert
     * @throws SQLException
     */
    public void insertNewBulk(NdsChanAvailPair ncap) throws SQLException
    {
        // if they set up for bulk insert but didn't insert anything both will be null
        if (ncap != null || (insertCommand != null && insertCommand.length() > 0))
        {
            if (insertCommand == null)
            {
                insertCommand = new StringBuilder(4000000);
            }
            if (insertCommand.length() == 0)
            {
                insertCommand.append("INSERT INTO ").append(getName()).append(" ");
                insertCommand.append(" (chanIdx, availIdx) ").append("\n");
                insertCommand.append("VALUES\n");
            }
            if (ncap != null)
            {

                if (insertCount > 0)
                {
                    insertCommand.append(",\n");
                }
                else
                {
                    insertCommand.append("\n");
                }

                insertCommand.append(String.format("(%1$d, %2$d) ", ncap.getChanIdx(), ncap.getAvailIdx()));
                insertCount++;
            }
            if ((ncap == null || insertCount >= insertNum) && insertCommand.length() > 0)
            {
                String excmd = insertCommand.toString();
                db.execute(excmd);
                insertCommand.setLength(0);
                insertCount = 0;
            }
        }
    }
    public NdsChanAvailPair streamNext() throws LdvTableException
    {
        NdsChanAvailPair ret = null;
        try
        {
            if (allStream != null && allStream.next())
            {
                ret = new NdsChanAvailPair(allStream);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("NdsChanAvailPair stream next: ", ex);
        }
        return ret;
    }
}
