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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanDataBufferTest
{
    Database db;
    ViewUser vuser = null;
    Page vpage = null;
    ChanInfo ci;
    TimeInterval ti;
    private ChannelTable chanTbl;
    HashSet<Integer> selections;
    ArrayList<TimeInterval> times;

    
    public ChanDataBufferTest() throws LdvTableException, SQLException, ViewConfigException
    {
        getDbTables();
        int n = chanTbl.getBestMatch("L1:PSL-ISS_PDB_OUT_DQ");
        ci = chanTbl.getChanInfo(n);
        selections = new HashSet<Integer>();
        selections.add(ci.getId());
        
        ti = new TimeInterval(1054771216, 1054774816);
        times = new ArrayList<TimeInterval>();
        times.add(ti);
    }
    /**
     * Connect to the database and create table objects we need
     */
    private void getDbTables() throws LdvTableException, SQLException, ViewConfigException
    {
        if (db == null)
        {
            ViewerConfig vc = new ViewerConfig();
            db = vc.getDb();
            if (db == null)
            {
                throw new LdvTableException("Can't connect to LigoDV-web database");
            }
        }
        if (chanTbl == null)
        {
            chanTbl = new ChannelTable(db);
        }
    }

    @BeforeClass
    public static void setUpClass()
    {
    }
    
    @AfterClass
    public static void tearDownClass()
    {
    }

    /**
     * Test of setVuser method, of class ChanDataBuffer.
     */
    @Test
    public void testSetVuser() throws SQLException, WebUtilException, LdvTableException
    {
        System.out.println("setVuser");
        
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

            instance.setVuser(vuser);
        }    
    }

    /**
     * Test of getDataLength method, of class ChanDataBuffer.
     */
    @Test
    public void testGetDataLength() throws SQLException, WebUtilException, LdvTableException
    {
        System.out.println("getDataLength");
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);
            int expResult = Math.round((ti.getStopGps()-ti.getStartGps()) * ci.getRate());
            int result = instance.getDataLength();
            assertEquals(expResult, result);
        }
        else
        {
            fail("dataBufFactory did not create any instances");
        }
    }

    /**
     * Test of getChanInfo method, of class ChanDataBuffer.
     */
    @Test
    public void testGetChanInfo() throws LdvTableException
    {
        System.out.println("getChanInfo");
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

            ChanInfo expResult = ci;
            expResult.setChanName("Single sine wave");
            ChanInfo result = instance.getChanInfo();
            Assert.assertTrue("Returned channel info not correct.", result.isSame(expResult));
        }
    }

    /**
     * Test of setChanInfo method, of class ChanDataBuffer.
     */
    @Test
    public void testSetChanInfo() throws LdvTableException
    {
        System.out.println("setChanInfo");
        ChanInfo chanInfo = ci;
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

            instance.setChanInfo(chanInfo);
        }
    }

    /**
     * Test of getData method, of class ChanDataBuffer.
     */
    @Test
    public void testGetData() throws LdvTableException
    {
        System.out.println("getData");
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                             times, vpage, vuser);
        if (!instances.isEmpty())
        {
            
            ChanDataBuffer instance = instances.get(0);
            float[] result = instance.getData();
            int explen = Math.round((ti.getStopGps()-ti.getStartGps()) * ci.getRate());
            assertTrue("getData did not return proper length buf", explen==result.length);
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
    }

    /**
     * Test of setData method, of class ChanDataBuffer.
     */
    @Test
    public void testSetData() throws LdvTableException
    {
        System.out.println("setData");
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                             times, vpage, vuser);
        if (!instances.isEmpty())
        {

            ChanDataBuffer instance = instances.get(0);
            
            int explen = Math.round((ti.getStopGps() - ti.getStartGps()) * ci.getRate());
            float[] expected = new float[explen];
            instance.setData(expected);
            float[] result = instance.getData();
            assertArrayEquals(expected, result, 1e-4f);
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
    }

    /**
     * Test of getTimeInterval method, of class ChanDataBuffer.
     */
    @Test
    public void testGetTimeInterval() throws LdvTableException
    {
        System.out.println("getTimeInterval");
        
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                             times, vpage, vuser);
        if (!instances.isEmpty())
        {

            ChanDataBuffer instance = instances.get(0);
            TimeInterval result = instance.getTimeInterval();
            boolean isGood = result.getStartGps() == ti.getStartGps() && result.getStopGps() ==ti.getStopGps();
            assertTrue("returned interval doesn't match",isGood);
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
        
    }

    /**
     * Test of setTimeInterval method, of class ChanDataBuffer.
     */
    @Test
    public void testSetTimeInterval() throws LdvTableException
    {
        System.out.println("setTimeInterval");
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                             times, vpage, vuser);
        if (!instances.isEmpty())
        {

            ChanDataBuffer instance = instances.get(0);
            instance.setTimeInterval(ti);
            TimeInterval result = instance.getTimeInterval();
            boolean isGood = result.getStartGps() == ti.getStartGps() && result.getStopGps() == ti.getStopGps();
            assertTrue("returned interval doesn't match", isGood);
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
    }

    /**
     * Test of getLastError method, of class ChanDataBuffer.
     */
    @Test
    public void testGetLastError() throws LdvTableException
    {
        System.out.println("getLastError");
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

            String expResult = "";
            String result = instance.getLastError();
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getNDSQuery method, of class ChanDataBuffer.
     */
    @Test
    public void testGetNDSQuery_0args() throws LdvTableException
    {
        System.out.println("getNDSQuery");
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                   times, vpage, vuser);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

            String expResult = String.format("nds_query -n %1$s -s %2$d -e %3$d %4$s,%5$s", 
                                             ci.getServer(), ti.getStartGps(), ti.getStopGps(), 
                                             "Single sine wave", ci.getcType());
            String result = instance.getNDSQuery();
            assertEquals(expResult, result);
        }
    }

    /**
     * Test of getNDSQuery method, of class ChanDataBuffer.
     */
    @Test
    public void testGetNDSQuery_ChanInfo_TimeInterval()
    {
        System.out.println("getNDSQuery");
        String expResult = String.format("nds_query -n %1$s -s %2$d -e %3$d %4$s,%5$s",
                                         ci.getServer(), ti.getStartGps(), ti.getStopGps(),
                                         ci.getChanName(), ci.getcType());
        String result = ChanDataBuffer.getNDSQuery(ci, ti);
        assertEquals(expResult, result);
    }

    /**
     * Test of dataBufFactory method, of class ChanDataBuffer.
     */
    @Test
    public void testDataBufFactory_5args() throws Exception
    {
        System.out.println("dataBufFactory");
        ArrayList<ChanDataBuffer> result = ChanDataBuffer.dataBufFactory(db, selections, times, vpage, vuser);
        assertFalse("dataBuffer factory did not return a result",result.isEmpty());
    }

    /**
     * Test of dataBufFactory method, of class ChanDataBuffer.
     */
    @Test
    public void testDataBufFactory_6args() throws Exception
    {
        System.out.println("dataBufFactory");
        boolean noxfer = true;
        ArrayList expResult = null;
        ArrayList result = ChanDataBuffer.dataBufFactory(db, selections, times, vpage, vuser, noxfer);
        assertFalse("dataBuffer factory did not return a result", result.isEmpty());
    }

    /**
     * Test of testDataFactory method, of class ChanDataBuffer.
     */
    @Test
    public void testTestDataFactory() throws Exception
    {
        System.out.println("testDataFactory");
        
        ArrayList<ChanDataBuffer> result = ChanDataBuffer.testDataFactory("sine1", db, selections, 
                                                                          times, vpage, vuser);
        // TODO we could verify what we get back but for now be happy we didn't get an error
        assertTrue("testDataFactory didn't return a buf", !result.isEmpty());
    }

    /**
     * Test of detrend method, of class ChanDataBuffer.
     */
    @Test
    public void testDetrend() throws LdvTableException
    {
        System.out.println("detrend");
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                 times, vpage, vuser);

        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);
            instance.detrend();
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
    }

    /**
     * Test of filter method, of class ChanDataBuffer.
     */
    @Test
    public void testFilter() throws LdvTableException
    {
        System.out.println("filter");
        
        ArrayList<ChanDataBuffer> instances = ChanDataBuffer.testDataFactory("sine1", db, selections,
                                                                             times, vpage, vuser);
        if (!instances.isEmpty())
        {
            double[] fkernel = { 1,1,1};
            ChanDataBuffer instance = instances.get(0);
            instance.filter(fkernel);
        }
        else
        {
            fail("testDataFactory did not return a data buffer");
        }
    }

    /**
     * Test of toString method, of class ChanDataBuffer.
     */
    @Test
    public void testToString() throws LdvTableException
    {
        System.out.println("toString");
        ArrayList<ChanDataBuffer> instances;
        instances = ChanDataBuffer.dataBufFactory(db, selections, times, vpage, vuser, true);
        if (!instances.isEmpty())
        {
            ChanDataBuffer instance = instances.get(0);

           
            String result = instance.toString();
            assertTrue("toString does not contain the correct channel name", result.contains(ci.getChanName()));
            assertTrue("toString does not contain the correct channel type", result.contains(ci.getcType()));
        }
    }
}