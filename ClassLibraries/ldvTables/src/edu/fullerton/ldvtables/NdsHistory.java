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

import java.sql.Timestamp;

/**
 * Data structure to hold condensed history records
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsHistory
{
    private Timestamp minTestTime, maxTestTime;
    private String state,errMsg;
    private int chanCount;
    private int n;

    public NdsHistory(Timestamp testTime, String state, String errMsg, int chanCount)
    {
        minTestTime = maxTestTime = testTime;
        this.state = state;
        this.errMsg = errMsg;
        this.chanCount = chanCount;
        this.n = 1;
    }
    
    public boolean combineIfPossible(Timestamp testTime, String state, String errMsg, int chanCount)
    {
        boolean ret;
        
        if (state.equals(this.state) && chanCount == this.chanCount)
        {
            n++;
            if (!errMsg.isEmpty() && ! this.errMsg.contains(errMsg))
            {
                if (!this.errMsg.isEmpty())
                {
                    this.errMsg += "<br>";
                }
                this.errMsg += errMsg;
            }
            if (testTime.before(minTestTime))
            {
                minTestTime = testTime;
            }
            if (testTime.after(maxTestTime))
            {
                maxTestTime = testTime;
            }

            ret = true;
        }
        else
        {
            ret = false;
        }
        return ret;
    }

    public Timestamp getMinTestTime()
    {
        return minTestTime;
    }

    public Timestamp getMaxTestTime()
    {
        return maxTestTime;
    }

    public String getState()
    {
        return state;
    }

    public String getErrMsg()
    {
        return errMsg;
    }

    public int getChanCount()
    {
        return chanCount;
    }

    public int getN()
    {
        return n;
    }
    
    
}
