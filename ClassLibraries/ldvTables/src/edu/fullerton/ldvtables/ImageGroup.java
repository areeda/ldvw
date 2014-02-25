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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * represents a row in the ImageGroupTable
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageGroup
{
    private Integer myId;
    private String user;
    private String groupName;
    private Integer imageId;
    
    public void fill(ResultSet rs) throws SQLException
    {
        myId = rs.getInt("myId");
        user = rs.getString("user");
        groupName = rs.getString("groupName");
        imageId = rs.getInt("imageID");
    }

    public Integer getMyId()
    {
        return myId;
    }

    public void setMyId(Integer myId)
    {
        this.myId = myId;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getGroupName()
    {
        return groupName;
    }

    public void setGroupName(String groupName)
    {
        this.groupName = groupName;
    }

    public Integer getImageId()
    {
        return imageId;
    }

    public void setImageId(Integer imageId)
    {
        this.imageId = imageId;
    }
    
}
