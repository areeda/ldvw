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
public class ProgressFrameTest
{
    ProgressFrame instance;
    public ProgressFrameTest()
    {
        instance = new ProgressFrame();
        instance.setVisible(true);
        instance.setPosition();     
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
        instance.dispose();
    }

    /**
     * Test of setChanName method, of class Progress.
     */
    @Test
    public void testSetChanName()
    {
        System.out.println("setChanName");
        String val = "";
        
        instance.setChanName(val);
        // good enough
        //fail("The test case is a prototype.");
    }

    /**
     * Test of setWorkingOn method, of class Progress.
     */
    @Test
    public void testSetWorkingOn()
    {
        System.out.println("setWorkingOn");
        String val = "";
        
        instance.setWorkingOn(val);
        // good enough
        //fail("The test case is a prototype.");
    }

    /**
     * Test of setEstTimeLeft method, of class Progress.
     */
    @Test
    public void testSetEstTimeLeft()
    {
        System.out.println("setEstTimeLeft");
        String val = "";
       
        instance.setEstTimeLeft(val);
        // good enoug
        //fail("The test case is a prototype.");
    }

    /**
     * Test of setTitleLbl method, of class Progress.
     */
    @Test
    public void testSetTitleLbl()
    {
        System.out.println("setTitleLbl");
        String val = "";
        
        instance.setTitleLbl(val);
        // 
        //fail("The test case is a prototype.");
    }

    /**
     * Test of setProgress method, of class Progress.
     */
    @Test
    public void testSetProgress() throws InterruptedException
    {
        System.out.println("setProgress");
        int val = 0;
        instance.setPosition();
        instance.setWorkingOn("test indeterminate mode");
        instance.setProgress(-1); // set it indeterminate here
        Thread.sleep(2000);
        instance.setWorkingOn("test determinate mode");
        int imax=10000;
        for(int i=0;i<imax;i++)
        {
            instance.setEstTimeLeft(String.format("%1$d of %2$d",i,imax));
            float pct = 100.f * i / imax;
            instance.setProgress((int)(pct+0.5));
            Thread.sleep(3);
            if (instance.wantsCancel())
            {
                break;
            }
        }
    }

    /**
     * Test of setPosition method, of class Progress.
     */
    @Test
    public void testSetPosition()
    {
        System.out.println("setPosition");
        ProgressFrame instance = new ProgressFrame();
        instance.setPosition();
        
    }

    /**
     * Test of wantsCancel method, of class Progress.
     */
    @Test
    public void testWantsCancel()
    {
        System.out.println("wantsCancel");
        ProgressFrame instance = new ProgressFrame();
        boolean expResult = false;
        boolean result = instance.wantsCancel();
        assertEquals(expResult, result);
        
    }
}
