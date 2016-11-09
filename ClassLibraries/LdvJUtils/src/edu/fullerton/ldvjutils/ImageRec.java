/*
 * Copyright (C) 2016 Joseph Areeda <joseph.areeda@ligo.org>
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

/**
 * Class representing a record in the image table
 * @author Joseph Areeda <joseph.areeda@ligo.org>
 */
public class ImageRec
{
    private int myId;
    private String userName;
    private byte[] image;
    private String mime;
    private String group;
    private String description;
    private  int resNum;

    public int getMyId()
    {
        return myId;
    }

    public void setMyId(int myId)
    {
        this.myId = myId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public byte[] getImage()
    {
        return image;
    }

    public void setImage(byte[] image)
    {
        this.image = image;
    }

    public String getMime()
    {
        return mime;
    }

    public void setMime(String mime)
    {
        this.mime = mime;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup(String group)
    {
        this.group = group;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public int getResNum()
    {
        return resNum;
    }

    public void setResNum(int resNum)
    {
        this.resNum = resNum;
    }
    
    
}
