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
public class NdsServStat
{
    private String server;
    private int nGood;
    private int nBad;
    private Timestamp minDate, maxDate;
    private int conMin, conMax;
    private float conAvg, conSD;
    private int cntMin, cntMax;
    private float cntAvg, cntSD;
    
    
    public NdsServStat()
    {
    }

    public void fill(ResultSet rs) throws SQLException
    {
        server = rs.getString("server");
        int n = rs.getInt("n");
        String state = rs.getString("state");
        if (state.equalsIgnoreCase("good"))
        {
            nGood = n;
            nBad = 0;
        }
        else
        {
            nBad=n;
            nGood = 0;
        }
        minDate = rs.getTimestamp("minDate");
        maxDate = rs.getTimestamp("maxDate");
        conMin = rs.getInt("conMin");
        conMax = rs.getInt("conMax");
        conAvg = rs.getFloat("conAvg");
        conSD = rs.getFloat("conSD");
        cntMin = rs.getInt("cntMin");
        cntMax = rs.getInt("cntMax");
        cntAvg = rs.getFloat("cntAvg");
        cntSD = rs.getFloat("cntSD");
    }

    public String getServer()
    {
        return server;
    }

    public int getnGood()
    {
        return nGood;
    }

    public int getnBad()
    {
        return nBad;
    }

    public Timestamp getMinDate()
    {
        return minDate;
    }

    public Timestamp getMaxDate()
    {
        return maxDate;
    }

    public int getConMin()
    {
        return conMin;
    }

    public int getConMax()
    {
        return conMax;
    }

    public float getConAvg()
    {
        return conAvg;
    }

    public float getConSD()
    {
        return conSD;
    }

    public int getCntMin()
    {
        return cntMin;
    }

    public int getCntMax()
    {
        return cntMax;
    }

    public float getCntAvg()
    {
        return cntAvg;
    }

    public float getCntSD()
    {
        return cntSD;
    }
    
}
