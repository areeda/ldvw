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
package viewerconfig;

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
public class ViewerConfigTest
{
    
    public ViewerConfigTest()
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
     * Test of readConfig method, of class ViewerConfig.
     */
    @Test
    public void testReadConfig() throws Exception
    {
        System.out.println("readConfig");
        ViewerConfig instance = new ViewerConfig();
        instance.readConfig();
        
    }

    /**
     * Test of readConfig method, of class ViewerConfig.
     */
    @Test
    public void testReadConfig_String() throws Exception
    {
        System.out.println("readConfig");
        String fname = "/usr/local/ldvw/ldvw.conf";
        ViewerConfig instance = new ViewerConfig();
        instance.readConfig(fname);
        
    }
}
