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

package edu.fullerton.ldvw;

import edu.fullerton.ldvjutils.TimeInterval;
import java.util.ArrayList;

/**
 * Translate between LIGO epoch names and GPS time ranges
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Epochs
{
    private class epoch
    {
        String name;
        long startGPS;
        long stopGPS;
        
        epoch(String name, long strt,long stop)
        {
            this.name = name;
            startGPS = strt;
            stopGPS = stop;
        }
    }
    final ArrayList<epoch> epochList;
    
    public Epochs()
    {
        epochList = new ArrayList<>();
        epochList.add(new epoch("S5", 815153408, 880920032));
        epochList.add(new epoch("S6", 930960015, 971654415));
        epochList.add(new epoch("S6a", 930960015, 935798415));
        epochList.add(new epoch("S6b", 937785615, 947203215));
        epochList.add(new epoch("S6c", 947635215, 961545615));
        epochList.add(new epoch("S6d", 961545615, 971654415));
        epochList.add(new epoch("ER2", 1025636416, 1028563232));
        epochList.add(new epoch("ER3", 1042934416, 1045353616));
        epochList.add(new epoch("ER4", 1057881616, 1061856016));
        epochList.add(new epoch("ER5", 1073606416, 1078790416));
    }
    
    public TimeInterval getEpoch(String ename)
    {
        TimeInterval ret = null;
        for(epoch e : epochList)
        {
            if (ename.equalsIgnoreCase(e.name))
            {
                ret = new TimeInterval(e.startGPS, e.stopGPS);
                break;
            }
        }
        return ret;
    }
    public ArrayList<String> getEpochNames()
    {
        ArrayList<String> ret = new ArrayList<>();
        for (epoch e : epochList)
        {
            ret.add(e.name);
        }
        return ret;
    }

}
