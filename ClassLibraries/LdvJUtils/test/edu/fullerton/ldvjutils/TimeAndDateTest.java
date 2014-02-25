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

import java.util.Date;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 * Collection of conversions to and from GPS seconds
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TimeAndDateTest
{
    
    public TimeAndDateTest()
    {
    }
    
    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }
    
    @Before
    public void setUp()
    {
    }
    
    @After
    public void tearDown()
    {
    }

    /**
     * Test of gps2utc method, of class TimeAndDate.
     */
    @Ignore @Test
    public void testGps2utc()
    {
        System.out.println("gps2utc");
        long gps = 0L;
        long expResult = 0L;
        long result = TimeAndDate.gps2utc(gps);
        assertEquals(expResult, result);
        
        
    }

    /**
     * Test of utc2gps method, of class TimeAndDate.
     */
    @Test
    public void testUtc2gps()
    {
        System.out.println("utc2gps");
        long utc = 0L;
        long expResult = 0L;
        long result = TimeAndDate.utc2gps(utc);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of nowAsUtcString method, of class TimeAndDate.
     * I'm not sure how to get an independent gps time for now
     */
    @Ignore @Test
    public void testNowAsUtcString()
    {
        System.out.println("nowAsUtcString");
        long offsetms = 0L;
        String expResult = "";
        String result = TimeAndDate.nowAsUtcString(offsetms);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of dateAsUtcString method, of class TimeAndDate.
     */
    @Test
    public void testDateAsUtcString()
    {
        System.out.println("dateAsUtcString");
        Date date = null;
        String expResult = "";
        String result = TimeAndDate.dateAsUtcString(date);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of gpsAsUtcString method, of class TimeAndDate.
     */
    @Test
    public void testGpsAsUtcString()
    {
        System.out.println("gpsAsUtcString");
        long gps = 0L;
        String expResult = "";
        String result = TimeAndDate.gpsAsUtcString(gps);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getGPS method, of class TimeAndDate.
     */
    @Test
    public void testGetGPS_StringArr()
    {
        System.out.println("getGPS");
        String[] str = null;
        long expResult = 0L;
        long[] results = TimeAndDate.getGPS(str);
        long result = 0;
        if (results.length > 0)
        {
            result = results[0];
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getGPS method, of class TimeAndDate.
     */
    @Test
    public void testGetGPS_String()
    {
        System.out.println("getGPS");
        String[] it = 
        {   // all of these should define the same time
            "1/1/2014", "1/1/2014 00:00", "1/1/2014 00:00:00", "1/1/2014 00:00:00 GMT", "1/1/2014 0000Z",
            "2014-1-1", "2014-1-1 00:00", "2014-1-1 00:00:00", "2014-1-1 00:00:00 GMT", "2014-1-1 0000Z",
            "2013-12-31 16:00 PST", "2013-12-31 17:00 MST", "2013-12-31 19:00 EST", "2014-1-1 1:00 CET"
        };
        
        long expResult = 1072569616L;
        for (int i=0;i<it.length;i++)
        {
            System.out.println(it[i]);
            long result = TimeAndDate.getGPS(it[i]);
            assertEquals(expResult, result);
        }        
    }

    /**
     * Test of hrTime method, of class TimeAndDate.
     */
    @Test
    public void testHrTime()
    {
        System.out.println("hrTime");
        long[] secList = { 0L, 30L, 100L, 500L, 999L, 1000L, 1001L, 1800L, 7200L, 90000L };
        for(long sec : secList)
        {
            String expResult = "";
            String result = TimeAndDate.hrTime(sec);
            System.out.format("%1$,d -> %2$s\n", sec, result);
        }
    }
}
