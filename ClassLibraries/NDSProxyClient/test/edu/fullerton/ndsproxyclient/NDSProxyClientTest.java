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
package edu.fullerton.ndsproxyclient;

import edu.fullerton.ldvjutils.ChanInfo;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NDSProxyClientTest
{
    // these hostnames/IPs have a running nds2 server (usually)

    static String[] goodServerList =
    {
        "nds.ligo.caltech.edu", "nds.ligo-la.caltech.edu", "nds.ligo-wa.caltech.edu",
        "131.215.115.249", "208.69.128.69", "198.129.208.252"
    };
    // these hostnames/IPs do not have a nds server, do not exist, or are nonsensical
    static String[] badServerList =
    {
        "nds.areeda.com", "ldas-pcdev2.ligo.caltech.edu", "192.168.4.4", "192.168.2"
    };
    static String[] shortServerList =
    {
        "nds.ligo.caltech.edu", "nds.ligo-la.caltech.edu", "nds.ligo-wa.caltech.edu"
    };
    
    
    static long[] someGpsTimes =
    {
        977918415, // 2011-1-1 12:00
        980596815, // 2011-2-1 12:00
        983016015, // 2011-3-1 12:00
        985694415, // 2011-4-1 12:00
        988286415, // 2011-5-1 12:00
        990964815, // 2011-6-1 12:00
        993556815, // 2011-7-1 12:00
        996235215, // 2011-8-1 12:00
        998913615, // 2011-9-1 12:00
        1001505615, // 2011-10-1 12:00
        1004184015, // 2011-11-1 12:00
        1006776015, // 2011-12-1 12:00
        1009454415, // 2012-1-1 12:00
        1012132815, // 2012-2-1 12:00
        1014638415, // 2012-3-1 12:00
        1017316815, // 2012-4-1 12:00
        1019908815, // 2012-5-1 12:00
        1022587215, // 2012-6-1 12:00
        1025179216, // 2012-7-1 12:00
        1027857616, // 2012-8-1 12:00
        1030536016, // 2012-9-1 12:00
        1033128016, // 2012-10-1 12:00
        1035806416, // 2012-11-1 12:00
        1038398416  // 2012-12-1 12:00
    };

    //=========================================================
    public NDSProxyClientTest()
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

    //=============================================================

    /**
     * Test of getNextDouble method, of class NDSProxyClient.
     */
    @Test
    public void testGetNextDouble() throws Exception
    {
        System.out.println("getNextDouble");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNextLong method, of class NDSProxyClient.
     */
    @Test
    public void testGetNextLong() throws Exception
    {
        System.out.println("getNextLong");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getProxy method, of class NDSProxyClient.
     */
    @Test
    public void testGetProxy()
    {
        System.out.println("getProxy");
        NDSProxyClient instance = new NDSProxyClient("nds.ligo.caltech.edu");
        String expResult = "";
        String result = instance.getProxy();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setProxy method, of class NDSProxyClient.
     */
    @Test
    public void testSetProxy()
    {
        System.out.println("setProxy");
        String proxy = "";
        NDSProxyClient instance = new NDSProxyClient("nds.ligo.caltech.edu");
        instance.setProxy(proxy);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of connect method, of class NDSProxyClient.
     */
    @Test
    public void testConnect_0args() throws Exception
    {
        System.out.println("connect w/o timeout");
        
        for (String server : goodServerList)
        {
            NDSProxyClient instance = new NDSProxyClient(server);
            System.out.format("connect: %1$s\n", server);
            instance.connect();
            instance.disconnect();
            instance.bye();
        }
        
        for (String server : badServerList)
        {
            NDSProxyClient instance=null;
            try
            {
                instance = new NDSProxyClient(server);
                System.out.format("(bad) connect: %1$s\n", server);
                instance.connect();
                instance.disconnect();
                instance.bye();
                String er = String.format("Server from bad server list did not throw. (%s)", server);
                fail(er);
            }
            catch (Throwable ex)
            {
                String exClass = ex.getClass().getSimpleName();
                String m = ex.getMessage();
                if (instance != null)
                {
                    instance.bye();
                }
                if (!exClass.equalsIgnoreCase("NDSException"))
                {
                    fail("Expected an NDSException exception. Got: " + exClass + " Message: " + m);
                }
            }
        }

    }

    /**
     * Test of connect method, of class NDSProxyClient.
     */
    @Test
    public void testConnect_int() throws Exception
    {
        System.out.println("connect w/timeout");
        int timeout = 10000;
        for (String server : goodServerList)
        {
            NDSProxyClient instance = new NDSProxyClient(server);
            System.out.format("connect: %1$s\n", server);
            instance.connect(timeout);
            instance.disconnect();
            instance.bye();
        }
        NDSProxyClient instance = null;
        for (String server : badServerList)
        {
            try
            {
                instance = new NDSProxyClient(server);
                System.out.format("(bad) connect: %1$s\n", server);
                instance.connect(timeout);
                instance.disconnect();
                instance.bye();
                String er = String.format("Server from bad server list did not throw. (%s)", server);
                fail(er);
            }
            catch (Throwable ex)
            {
                if (instance != null)
                {
                    instance.bye();
                }
                String exClass = ex.getClass().getSimpleName();
                String m = ex.getMessage();
                if (!exClass.equalsIgnoreCase("NDSException"))
                {
                    fail("Expected an NDSException exception. Got: " + exClass + " Message: " + m);
                }
                
            }
        }
    }

    /**
     * Test of disconnect method, of class NDSProxyClient.
     */
    @Test
    public void testDisconnect() throws Exception
    {
        System.out.println("disconnect");
        
        fail("The test case is a prototype.");
    }

    /**
     * Test of bye method, of class NDSProxyClient.
     */
    @Test
    public void testBye() throws Exception
    {
        System.out.println("bye");
        fail("The test case is a prototype.");
    }

    /**
     * Test of requestData method, of class NDSProxyClient.
     */
    @Test
    public void testRequestData()
    {
        System.out.println("requestData");
        String chan = "";
        String cType = "";
        long start = 0L;
        long stop = 0L;
        int stride = 0;
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChanCount method, of class NDSProxyClient.
     */
    @Test
    public void testGetChanCount_0args() throws Exception
    {
        System.out.println("getChanCount");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChanCount method, of class NDSProxyClient.
     */
    @Test
    public void testGetChanCount_String() throws Exception
    {
        System.out.println("getChanCount");
        String cType = "";
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChanList method, of class NDSProxyClient.
     */
    @Test
    public void testGetChanList() throws Exception
    {
        System.out.println("getChanList");
        String cType = "";
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getBufferedChanList method, of class NDSProxyClient.
     */
    @Test
    public void testGetBufferedChanList() throws Exception
    {
        System.out.println("getBufferedChanList");
        String cType = "";
       
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getNextChannel method, of class NDSProxyClient.
     */
    @Test
    public void testGetNextChannel() throws Exception
    {
        System.out.println("getNextChannel");
        NDSProxyClient instance = null;
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getChannelSourceInfo method, of class NDSProxyClient.
     */
    @Test
    public void testGetChannelSourceInfo() throws Exception
    {
        System.out.println("getChannelSourceInfo");
        String[] channelNames = null;
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLastError method, of class NDSProxyClient.
     */
    @Test
    public void testGetLastError()
    {
        System.out.println("getLastError");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartGPS method, of class NDSProxyClient.
     */
    @Test
    public void testGetStartGPS()
    {
        System.out.println("getStartGPS");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isVerbose method, of class NDSProxyClient.
     */
    @Test
    public void testIsVerbose()
    {
        System.out.println("isVerbose");
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setVerbose method, of class NDSProxyClient.
     */
    @Test
    public void testSetVerbose()
    {
        System.out.println("setVerbose");
        boolean verbose = false;
        
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}