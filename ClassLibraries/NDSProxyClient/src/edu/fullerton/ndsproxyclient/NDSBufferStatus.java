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

package edu.fullerton.ndsproxyclient;

/**
 * Represents information on data actually received from server.
 * Things like sample frequency and data type may differ from what we requested.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NDSBufferStatus 
{
    private String name;
    private long start;
    private float fs;
    private int size;
    private String type;
    
    public NDSBufferStatus()
    {
        name = type = "";
        start = size = 0;
        fs = 0;
    }
    public void init (String[] recv) throws NDSException
    {
        if (recv.length < 5)
        {
            throw new NDSException("NDS buffer init with invalid data");
        }
         init (recv[0], recv[1], recv[2],recv[3],recv[4]);
    }
    public void init(String name, String start, String fs, String size, String type) throws NDSException
    {
        try
        {
            this.name = name;
            this.start = Long.parseLong(start);
            this.fs = Float.parseFloat(fs);
            this.size = Integer.parseInt(size);
            this.type = type;
        }
        catch (NumberFormatException ex)
        {
            throw new NDSException("Error parsing data fro server", ex);
        }
    }
    public String toString()
    {
        String ret;
        ret = String.format("%1$s, %2$d, %3$.2f, %4$d, %5$s", name, start, fs, size, type);
        return ret;
    }
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getStart()
    {
        return start;
    }

    public void setStart(long start)
    {
        this.start = start;
    }

    public float getFs()
    {
        return fs;
    }

    public void setFs(float fs)
    {
        this.fs = fs;
    }

    public int getSize()
    {
        return size;
    }

    public void setSize(int size)
    {
        this.size = size;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
}
