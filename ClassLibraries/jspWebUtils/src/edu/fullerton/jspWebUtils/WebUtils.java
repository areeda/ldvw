/*
 * Copyright (C) 2012 joe
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
package edu.fullerton.jspWebUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @author joe
 */
public class WebUtils
{
    public static String commaFormat(Integer value)
    {
        return commaFormat(value.toString());
    }

    public static String commaFormat(Long value)
    {
        return commaFormat(value.toString());
    }

    public static String commaFormat(Float value)
    {
        return commaFormat(value.toString());
    }

    public static String commaFormat(Double value)
    {
        return commaFormat(value.toString());
    }

    private static String commaFormat(String value)
    {
        String txt = "";

        String t = value;
        while (t.length() > 0)
        {
            if (txt.length() > 0)
            {
                txt = "," + txt;
            }
            if (t.length() > 3)
            {
                txt = t.substring(t.length() - 3) + txt;
                t = t.substring(0, t.length() - 3);
            }
            else
            {
                txt = t + txt;
                t = "";
            }
        }
        return txt;
    }
    public static String hrInteger(int it)
    {
        return hrInteger((long)it);
    }
    public static String hrInteger(long it)
    {
        Long[] steps =
        {
            0L, 1000L, 1000000L, 1000000000L, 1000000000000L, 1000000000000000L
        };
        String[] fmts =
        {
            "%1$.0f", "%1$.1f K", "%1$.1f M", "%1$.1f G", "%1$.1f T", "%1$.1f P"
        };
        String ret = "";
        for(int i=1;i<steps.length && ret.isEmpty(); i++)
        {
            if (it < steps[i] || i==steps.length-1)
            {
                double denom = steps[i-1];
                denom = denom > 0 ? denom : 1;
                double d = it / denom;
                ret = String.format(fmts[i-1], d);
            }
        }
        return ret;
    }
    /**
     * Format a date string in http-date format (Thu, 01 Dec 1994 16:00:00 GMT)
     * @param days offset in days from now
     * @return String appropriate for Expires and other http fields
     */
    public static String getHttpDate(int days)
    {
        
        Calendar calendar = Calendar.getInstance();
        long time = calendar.getTimeInMillis();
        time += days * 24*60*60*1000L;
        calendar.setTimeInMillis(time);
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        String ret = dateFormat.format(calendar.getTime());
        return ret;
        }

        

}
