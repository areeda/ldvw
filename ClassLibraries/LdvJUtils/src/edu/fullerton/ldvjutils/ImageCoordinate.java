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
package edu.fullerton.ldvjutils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Represents a record in the ImageCoordinateTable
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageCoordinate
{
    private Integer coordId;     // uid of table row
    private String product;     // product this image contains
    private Integer imgId;      // uid of row in ImageTbl
    
    private Integer imgX0;      // left edge of plot in saved image
    private Integer imgY0;      // top edge of plot in saved image
    private Integer imgWd;      // width of plot in saved image
    private Integer imgHt;      // height of plot in saved image
    
    private Double x0;          // data x value of left edge of plot
    private Double y0;          // data y value of lower edge of plot
    private Double xN;          // data x value of right edge of plot
    private Double yN;          // data y value of top edge of plot;
    
    public ImageCoordinate()
    {
        coordId=0;
        product="";
        imgId=0;
        imgX0 = imgY0 = imgWd = imgHt =0;
        x0 = y0 = xN = yN = 0.;
    }
    
    public ImageCoordinate(ResultSet rs) throws SQLException
    {
        fill(rs);
    }
    
    /**
     * Set up the object from its database record
     * @param rs the row value
     * @throws SQLException must be a bug 
     */
    public final void fill(ResultSet rs) throws SQLException
    {
        coordId = rs.getInt("coordId");
        product = rs.getString("product");
        imgId = rs.getInt("imgId");
        imgX0 = rs.getInt("imgX0");
        imgY0 = rs.getInt("imgY0");
        imgWd = rs.getInt("imgWd");
        imgHt = rs.getInt("imgHt");
        
        x0 = rs.getDouble("x0");
        y0 = rs.getDouble("y0");
        xN = rs.getDouble("xN");
        yN = rs.getDouble("yN");
    }
    /**
     * for a SQL insert or update gets the field names in the order getFieldValues returns the values
     * 
     * @return field names formatted for SQL ie (<name1>, <name2> ...)
     * @see ImageCoordinate#getFieldValues() 
     */
    public String getFieldNames()
    {
        String ret = "(product, imgId, imgX0, imgY0, imgWd, imgHt, x0, y0, xN, yN)";
        return ret;
    }
    /** 
     * for an SQL insert or update gets the field falues that match getFieldNames
     * @return String of formatted and escaped values parentheses not included
     * @see ImageCoordinate#getFieldNames() 
     */
    public String getFieldValues()
    {
        
        String ret = String.format(
                 "'%1$s', %2$d, %3$d, %4$d, %5$d, %6$d, %7$f, %8$f, %9$f, %10$f",
                product, imgId, imgX0, imgY0, imgWd, imgHt, x0, y0, xN, yN);
        return ret;
    }

    public Integer getCoorId()
    {
        return coordId;
    }

    public void setCoorId(Integer coorId)
    {
        this.coordId = coorId;
    }

    public String getProduct()
    {
        return product;
    }

    public void setProduct(String product)
    {
        this.product = product;
    }

    public Integer getImgId()
    {
        return imgId;
    }

    public void setImgId(Integer imgId)
    {
        this.imgId = imgId;
    }

    public Integer getImgX0()
    {
        return imgX0;
    }

    public void setImgX0(Integer imgX0)
    {
        this.imgX0 = imgX0;
    }

    public Integer getImgY0()
    {
        return imgY0;
    }

    public void setImgY0(Integer imgY0)
    {
        this.imgY0 = imgY0;
    }

    public Integer getImgWd()
    {
        return imgWd;
    }

    public void setImgWd(Integer imgWd)
    {
        this.imgWd = imgWd;
    }

    public Integer getImgHt()
    {
        return imgHt;
    }

    public void setImgHt(Integer imgHt)
    {
        this.imgHt = imgHt;
    }

    public Double getX0()
    {
        return x0;
    }

    public void setX0(Double x0)
    {
        this.x0 = x0;
    }

    public Double getY0()
    {
        return y0;
    }

    public void setY0(Double y0)
    {
        this.y0 = y0;
    }

    public Double getxN()
    {
        return xN;
    }

    public void setxN(Double xN)
    {
        this.xN = xN;
    }

    public Double getyN()
    {
        return yN;
    }

    public void setyN(Double yN)
    {
        this.yN = yN;
    }

    /**
     * Since these transforms are used from multiple places that display images javascript is centralized here
     * 
     * @return 
     */
    public String getHeadJS()
    {
        String ret = String.format("initTimeFactor('.%1$s');", product.toLowerCase());
        return ret;
    }


    public String getClassName()
    {
        return product.toLowerCase();
    }

    public String getInitJS(String imageIdname, String timeId)
    {
        // "setTimeFactor(205,28,814,437,1065892855,600,'odc1','#time1')"
        String ret = String.format("setTimeFactor(%1$d,%2$d,%3$d,%4$d,%5$.0f,%6$.0f,'%7$s','#%8$s');",
                                   imgX0,imgY0,imgWd,imgHt,x0,xN,imageIdname, timeId);
        return ret;
    }
    
}
