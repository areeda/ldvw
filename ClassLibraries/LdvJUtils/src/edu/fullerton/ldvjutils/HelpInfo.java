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
import java.sql.Timestamp;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class HelpInfo
{
    private int id;
    private Timestamp lastmod;
    private String name;
    private String location;
    private String title;
    private String helpTxt;
    
    public HelpInfo()
    {
        id=0;
        lastmod=new Timestamp(0);
        name ="";
        location="";
        title="";
        helpTxt="";
    }
    
    public HelpInfo(String name, String location, String title, String helpTxt)
    {
        id=0;
        this.name = name;
        this.location = location;
        this.title = title;
        this.helpTxt = helpTxt;
    }
    
    public HelpInfo(HelpInfo hi)
    {
        this.id = hi.id;
        this.lastmod = hi.lastmod;
        this.name = hi.name;
        this.location = hi.location;
        this.title = hi.title;
        this.helpTxt = hi.helpTxt;
    }

    public int getId()
    {
        return id;
    }

    public Timestamp getLastmod()
    {
        return lastmod;
    }

    public String getName()
    {
        return name;
    }

    public String getLocation()
    {
        return location;
    }

    public String getTitle()
    {
        return title;
    }

    public String getHelpTxt()
    {
        return helpTxt;
    }


    public void fill(ResultSet rs) throws SQLException
    {
        id = rs.getInt("myId");
        lastmod = rs.getTimestamp("lastMod");
        name = rs.getString("name");
        location = rs.getString("location");
        title = rs.getString("title");
        helpTxt = rs.getString("helpTxt");
    }
    
    
}
