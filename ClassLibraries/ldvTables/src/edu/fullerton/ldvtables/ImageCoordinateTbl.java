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
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Some plots like ODC and Spectrograms have subimages that we would like to click on and extract information
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageCoordinateTbl extends Table
{
    private final Column[] myCols =
    {
        //         name,        type            length              can't be null   index         unique        auto inc
        new Column("coordId",   CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,   Boolean.TRUE,    Boolean.TRUE),
        new Column("product",   CType.CHAR,     16,                 Boolean.TRUE,   Boolean.TRUE,   Boolean.FALSE,   Boolean.FALSE),
        new Column("imgId",     CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.TRUE,   Boolean.TRUE,    Boolean.FALSE),

        new Column("imgX0",     CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("imgY0",     CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("imgWd",     CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("imgHt",     CType.INTEGER,  Integer.SIZE / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("x0",        CType.FLOAT,    Double.SIZE  / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("xN",        CType.FLOAT,    Double.SIZE  / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("y0",        CType.FLOAT,    Double.SIZE  / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("yN",        CType.FLOAT,    Double.SIZE  / 8,   Boolean.TRUE,   Boolean.FALSE,  Boolean.FALSE,  Boolean.FALSE),
    };
    
    public ImageCoordinateTbl(Database db)
    {
        this.db = db;
        setName("ImageCoordinates");
        setCols(myCols);
    }

    public void add(ImageCoordinate imgCoord) throws LdvTableException
    {
        String ins = "INSERT INTO " + getName() + " ";
        ins += imgCoord.getFieldNames();
        ins += " VALUES (" + imgCoord.getFieldValues() + ")";
        try
        {
            db.execute(ins);
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Adding image coordinates: ", ex);
        }
    }

    /**
     * Given an image table row ID number return coordinate transform if we have it
     * @param imgId - row primary key in Images table
     * @return the image coordinates or null if we don't have them
     */
    public ImageCoordinate getCoordinate(Integer imgId) throws LdvTableException
    {
        ImageCoordinate ret = null;
        
        String q = "SELECT * FROM " + getName() + " WHERE imgId = ";
        q += String.format("%1$d", imgId);
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = new ImageCoordinate(rs);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Getting image coordinates: ", ex);
        }
        return ret;
    }
}
