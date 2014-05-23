/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of static routines to deal with UTC and GPS data in binary or ascii form and conversions
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TimeAndDate
{

    // gps time at which leap seconds are added.
    private static final long[] leapSeconds =
    {
        46828800, // 1981-Jul-01
        78364801, // 1982-Jul-01
        109900802, // 1983-Jul-01
        173059203, // 1985-Jul-01
        252028804, // 1988-Jan-01
        315187205, // 1990-Jan-01
        346723206, // 1991-Jan-01
        393984007, // 1992-Jul-01
        425520008, // 1993-Jul-01 
        457056009, // 1994-Jul-01 
        504489610, // 1996-Jan-01 
        551750411, // 1997-Jul-01 
        599184012, // 1999-Jan-01 
        820108813, // 2006-Jan-01 
        914803214,   // 2009-Jan-01 
        1025135955   // 2012-JUL-01
    };
    private static final long epoch = 315964800;      // gps epoc in unix time 315964800

    /**
     * Convert GPS seconds to UTC seconds
     * 
     * @param gps GPS time
     * @return  UTC seconds from Unix epoch
     */
    public static long gps2utc(long gps)
    {
        long utc = gps + epoch - countLeapSeconds(gps);
        return utc;
    }
    /**
     * Convert gps seconds to a Java Date object
     * @param gps GPS time
     * @return corresponding Date object
     */
    public static Date gps2date(long gps)
    {
        long utcms = TimeAndDate.gps2utc(gps) * 1000;
        Date ret = new Date(utcms);
        return ret;
    }

    /**
     * Convert UTC time in SECONDS from Unix epoch to GPS time
     * @param utc seconds from Unix epoch eg System.currentTimeMillis()/1000
     * @return gps time in seconds
     */
    public static long utc2gps(long utc)
    {
        long gps = utc - epoch;
        gps += countLeapSeconds(gps);
        return gps;
    }

    private static int countLeapSeconds(long gps)
    {
        int ret = 0;
        for (int i = leapSeconds.length - 1; i >= 0 && ret == 0; i--)
        {
            if (leapSeconds[i] <= gps)
            {
                ret = i + 1;
            }
        }
        return ret;
    }
    public static long nowAsGPS()
    {
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        GregorianCalendar now = new GregorianCalendar(utctz);
        long utcSec = now.getTimeInMillis()/1000;
        return TimeAndDate.utc2gps(utcSec);
    }
    public static String nowAsUtcString(long offsetms)
    {
        
        SimpleTimeZone utctz = new SimpleTimeZone(0,"UTC");
        GregorianCalendar want = new GregorianCalendar(utctz);
        long wantms = want.getTimeInMillis();
        wantms += offsetms;
        want.setTimeInMillis(wantms);
        String ret = dateAsUtcString(want.getTime());
        return ret;
    }
    public static String dateAsUtcString(Date date)
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        GregorianCalendar now = new GregorianCalendar(utctz);

        sdf.setTimeZone(utctz);
        String ret = sdf.format(date);
        return ret;
    }
    public static String gpsAsUtcString(long gps)
    {
        long time = gps2utc(gps) * 1000;
        Date d = new Date(time);
        return dateAsUtcString(d);
    }
    /**
     * A convenience function for string arrays returned from HTML forms.
     * We still only deal with the first value because we only have one return value
     * @param str - one of many time/date formats
     * @return - GPS seconds corresponding to (what we think) input string specifies
     */
    public static long[] getGPS(String[] str)
    {
        long[] ret = null;
        if (str != null && str.length > 0)
        {
            ArrayList<Long> vals = new ArrayList<>();
            for(int i=0;i<str.length;i++)
            {
                String it = str[i].trim();
                if (!it.isEmpty())
                {
                    vals.add(getGPS(it));
                }
            }
            ret = new long[vals.size()];
            for(int i=0;i<vals.size();i++)
            {
                ret[i]=vals.get(i);
            }
        }
        return ret;
    }
    /**
     * Convert a string to GPS seconds
     * empty string => now
     * <positive integer> => gps time
     * <negative integer> => seconds ago, now - n
     * <date> => 00Z on that date
     * <date> <time> => UTC
     * <date> <time> <time zone>
     * <date> :=  YYYY-MM-DD | MM/DD/YYYY | MMM-DD-YYYY | DD-MMM-YYYY  
     * (sorry Europeans DD/MM/YYYY is ambiguous the first 12 days of every month
     * <time> := HH:MM:SS [AM|PM]| HH:MM [AM|PM] |HHMM
     * 
     * @param it - a compatible date time string
     * @return GPS seconds corresponding to (what we think) input string specifies
     */
    public static long getGPS(String it)
    {
        long ret;
        String[] parts;

        String stdDate;                 // date in standard format
        String stdTime;                 // time in standard format
        String timeZone = "UTC";        // default TZ
        String stdStr;                  // input in standard format
        
        if (it == null || it.isEmpty())
        {   // Empty date/time means now
            SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
            GregorianCalendar now = new GregorianCalendar(utctz);
            Date d = now.getTime();
            long utcSeconds = d.getTime() / 1000;
            ret = utc2gps(utcSeconds);
        }
        else
        {
            parts = it.split("\\s+");
            if (it.matches("^\\d{9,11}$"))
            {
                ret = Long.parseLong(it);
            }
            else if (it.matches("^[+\\-]?\\d+$"))
            {
                long t = Long.parseLong(it);
                if (t < 0)
                {
                    SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
                    GregorianCalendar now = new GregorianCalendar(utctz);
                    Date d = now.getTime();
                    long utcSeconds = d.getTime() / 1000;
                    ret = utc2gps(utcSeconds) + t;
                }
                else
                {
                    ret = 0;
                }
            }
            else 
            {
                int idx=0;
                
                stdDate = getDateStr(parts[idx]);
                if (stdDate == null)
                {
                    ret = 0;
                }
                else
                {
                    if (!stdDate.isEmpty() && parts.length > 1)
                    {
                        idx=1;
                    }

                    if (parts.length == 1 && !stdDate.isEmpty())
                    {
                        stdTime = "00:00:00";    // no time means midnight
                    }
                    else
                    {
                        // see if next token(s) contain a time
                        String tstr = parts[idx];
                        idx++;
                        if (idx < parts.length && parts[idx].toUpperCase().matches("AM|PM"))
                        {
                            tstr += " " + parts[idx];
                            idx++;
                        }
                        stdTime = getTimeStr(tstr);
                        if (stdTime == null || stdTime.isEmpty())
                        {
                            stdTime = "00:00:00";
                        }

                        if (idx < parts.length && parts[idx].toUpperCase().matches("[A-Z]{3}"))
                        {
                            timeZone = parts[idx].toUpperCase();
                        }
                    }                
                    stdStr = String.format("%1$s %2$s %3$s", stdDate, stdTime, timeZone);

                    String stdFmt = "yyyy-MM-dd HH:mm:ss z";
                    SimpleDateFormat sdf = new SimpleDateFormat(stdFmt);

                    try
                    {
                        Date d = sdf.parse(stdStr);
                        long utcSeconds = d.getTime()/1000;
                        ret = utc2gps(utcSeconds);
                    }
                    catch (ParseException ex)
                    {
                        ret = 0;
                    }
                }
            }
        }

        return ret;
    }
    /**
     * Check if string matches a data
     * @param d possible date string
     * @return date formatted as YYYY-MM-DD if it matches anything we like or null if it doesn't
     */
    public static String getDateStr(String d)
    {
        String ret;
        Date dt = null;
        SimpleDateFormat outFmt = new SimpleDateFormat("yyyy-MM-dd");
        if (d.matches("\\d{2,4}\\-\\d{1,2}\\-\\d{1,2}"))
        {   // YYYY-MM-DD
            try
            {
                dt = outFmt.parse(d);
            }
            catch (ParseException ex)
            {
            }
        }
        else if (d.matches("\\d{1,2}/\\d{1,2}/\\d{4}"))
        {   // MM/DD/YYYY
            
            try
            {
                SimpleDateFormat inFmt = new SimpleDateFormat("MM/dd/yyyy");
                dt = inFmt.parse(d);
            }
            catch (ParseException ex)
            {
                
            }
            
        }
        else if (d.matches("\\d{1,2}/\\d{1,2}/\\d{2}"))
        {
            // see if they entered a 2 digit year
            try
            {
                SimpleDateFormat inFmt = new SimpleDateFormat("MM/dd/yy");
                dt = inFmt.parse(d);
            }
            catch (ParseException ex2)
            {
                // zero will be returned to signify null
            }
        }
        else 
        {
            dt = null;
        }
        if (dt != null)
        {   // Return the date in standardized format
            ret = outFmt.format(dt);
        }
        else
        {
            ret = null;
        }
        return ret;
    }
    /**
     * Check if the string passed can be interpreted as a time of day
     * Acceptable formats are:  1:14 0114 13:14 1:14:03 1:14:04 PM
     * @param string
     * @return 
     */
    private static String getTimeStr(String t)
    {
        String ret;
        int hr,mn,sc;
        
        t = t.trim().toUpperCase();
        Pattern zuluPat = Pattern.compile("^(\\d{4})Z?$");
        Matcher zuluMat = zuluPat.matcher(t);
        if (zuluMat.find())
        {
            int mtime = Integer.parseInt(zuluMat.group(1));
            hr = mtime/100;
            mn = mtime %100;
            sc = 0;     // we don't accept seconds in this format
        }
        else
        {
            Pattern tPat = Pattern.compile("(\\d{1,2}):(\\d{1,2})(:(\\d{1,2}))?\\s*(AM|PM)?");
            Matcher m = tPat.matcher(t);
            if (m.find())
            {
                hr = Integer.parseInt(m.group(1));
                mn = Integer.parseInt(m.group(2));
                if (m.group(4) == null)
                {
                    sc = 0;
                }
                else
                {
                    sc=Integer.parseInt(m.group(4));
                }
                if (m.group(5) != null)
                {
                    if (m.group(5).equals("PM"))
                    {
                        hr += 12;
                    }
                }
            }
            else 
            {
                hr=mn=sc=0;
            }
        }
        ret = String.format("%1$02d:%2$02d:%3$02d",hr,mn,sc);
        return ret;
    }
    /**
     * Convert a length of time into a human readable format
     * These are Duncan Brown's suggestions:
     * seconds if t<  1000
     * mm:ss if 100 sec<  t<  1 hour  
     * hh:mm:ss if 1 hour<  t<  1 day
     * hh:mm:ss+1d, hh:mm:ss+2d
     * @param sec - duration in seconds
     * @return formatted string
     */
    public static String hrTime(long sec)
    {
        return TimeAndDate.hrTime(sec, false);
    }
    public static String hrTime(long sec, boolean forceMMSS)
    {
        long seconds = sec % 60;
        long minutes = (sec / 60) % 60;
        long hours = (sec / 3600) % 24;
        long days = (sec / 86400);
        String ret;
        if (days > 0)
        {
            ret = String.format("%1$dd %2$02d:%3$02d:%4$02d", days, hours, minutes, seconds);
        }
        else if (hours > 0)
        {
            ret = String.format("%1$d:%2$02d:%3$02d", hours, minutes, seconds);
        }
        else if (sec > 1000 || forceMMSS)
        {
            ret = String.format("%1$d:%2$02d",minutes,seconds);
        }
        else
        {
            ret = String.format("%1$,ds", sec);
        }
        
        return ret;
        
    }

    /**
     * Duration can be specified as Days HH:MM:SS or as a long int any of the fields may be null. So
     * 3600 = 1:0:0 = 60:0 ...
     *
     * @param in user entry to be converted to seconds
     * @return time specified in seconds
     */
    public static long getDuration(String in)
    {
        long ret;
        boolean isNegative;
        Pattern inpDays = Pattern.compile("^(\\d+)\\s+(\\d+)\\s*:\\s*(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpHrs = Pattern.compile("^(\\d+)\\s*:\\s*(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpMin = Pattern.compile("^(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpSec = Pattern.compile("^(\\d+)$");
        int days = 0;
        int hrs = 0;
        int min = 0;
        int sec;
        
        if (in == null || in.isEmpty())
        {
            ret = 0;
            isNegative = false;
        }
        else
        {
            in = in.trim();
            isNegative = false;
            if (in.startsWith("-"))
            {
                isNegative = true;
                in = in.substring(1);
            }
            Matcher m = inpDays.matcher(in);
            if (m.find())
            {
                days = Integer.parseInt(m.group(1));
                hrs = Integer.parseInt(m.group(2));
                min = Integer.parseInt(m.group(3));
                sec = Integer.parseInt(m.group(4));
            }
            else
            {
                m = inpHrs.matcher(in);
                if (m.find())
                {
                    hrs = Integer.parseInt(m.group(1));
                    min = Integer.parseInt(m.group(2));
                    sec = Integer.parseInt(m.group(3));
                }
                else
                {
                    m = inpMin.matcher(in);
                    if (m.find())
                    {
                        min = Integer.parseInt(m.group(1));
                        sec = Integer.parseInt(m.group(2));
                    }
                    else
                    {
                        m = inpSec.matcher(in);
                        if (m.find())
                        {
                            sec = Integer.parseInt(m.group(1));
                        }
                        else
                        {
                            days=hrs=min=sec=0;
                        }
                    }
                }
            }
            ret = days * 3600 * 24 + hrs * 3600 + min * 60 + sec;
        }
        if (isNegative)
        {
            ret = -ret;
        }
        return ret;
    }
  
}