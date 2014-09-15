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

package ndsJUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Provide mappings between external Strings and internal bit maps
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NDSChanType 
{
    private static final Map<String, Integer> name2bitmask;
    static
    {
        name2bitmask = new HashMap<>();
        name2bitmask.put("raw", 1);
        name2bitmask.put("rds", 2);
        name2bitmask.put("reduced", 2);
        name2bitmask.put("online", 4);
        name2bitmask.put("s-trend", 8);
        name2bitmask.put("m-trend", 16);
        name2bitmask.put("static", 32);
    }
    private static final Map<Integer, String> bitmask2name;
    static
    {
        bitmask2name = new HashMap<>();
        bitmask2name.put(1, "raw");
        bitmask2name.put(2, "rds");
        bitmask2name.put(4, "online");
        bitmask2name.put(8, "s-trend");
        bitmask2name.put(16, "m-trend");
        bitmask2name.put(32, "static");
    }
    private static final int unknown=63;
    
    public static int name2int(String name)
    {
        int ret = unknown;
        if (name2bitmask.containsKey(name.toLowerCase()))
        {
            ret = name2bitmask.get(name.toLowerCase());
        }
        return ret;
    }
    
    public static String int2name(int it)
    {
        StringBuilder ret = new StringBuilder();
        if (it == unknown || it == 0)
        {
            ret.append("unknown");
        }
        else
        {
            for(int m=1; m < unknown; m = m << 1)
            {
                if ((it & m) != 0)
                {
                    if (ret.length() > 0)
                    {
                        ret.append(", ");
                    }
                    ret.append(bitmask2name.get(m));
                }
            }
        }
        return ret.toString();
    }
    static boolean isValidName(String it)
    {
        return name2bitmask.containsKey(it);
    }
}
