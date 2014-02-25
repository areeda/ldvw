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
public class SpectrumCalcTest
{
    
    public SpectrumCalcTest()
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
     * Test of doCalc method, of class SpectrumCalc.
     */
    @Test
    public void testDoCalc()
    {
        System.out.println("doCalc");
        int n = 1024 * 6;       // match LigoDV test data default
        double[] data = new double[n];
        for(int i=0;i<n;i++)
        {
            data[i] = i== n/2 ? 1024 : 0;
        }
        float fs = 1024.0F;
        SpectrumCalc instance = new SpectrumCalc();
        double[] expResult = null;
        double[] result = instance.doCalc(data, fs);
        for(int i=0;i< 10;i++)
        {
            System.out.format("f: %1$.2f: %2$.4f\n",1/fs*i, result[i]);
        }
    }
}
