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
package edu.fullerton.ldvtables;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.SQLException;
import java.util.TreeSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageGroupTableTest
{
    Database db;
    String user = "testUser";
    String group = "testGroup";
    
    public ImageGroupTableTest() throws LdvTableException, SQLException, ViewConfigException
    {
        getDbTables();
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        
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
    }
    /**
     * Test of addToGroup method, of class ImageGroupTable.
     */
    @Test
    public void testAddToGroup() throws Exception
    {
        System.out.println("addToGroup");
        Integer imageID = 123;
        ImageGroupTable instance = new ImageGroupTable(db);
        int result = instance.addToGroup(user, group, imageID);
        // if it fails an exception will be thrown
    }

    /**
     * Test of getGroupMembers method, of class ImageGroupTable.
     */
    @Test
    public void testGetGroupMembers() throws Exception
    {
        System.out.println("getGroupMembers");
        ImageGroupTable instance = new ImageGroupTable(db);
        
        TreeSet result = instance.getGroupMembers(user, group);
        assertTrue( result.size() == instance.getCount(user, group));
        
    }

    /**
     * Test of getCount method, of class ImageGroupTable.
     */
    @Test
    public void testGetCount() throws Exception
    {
        System.out.println("getCount");
        ImageGroupTable instance = new ImageGroupTable(db);
        int result = instance.getCount(user, group);
        assertTrue(result == instance.getGroupMembers(user, group).size());
    }

    /**
     * Test of deleteGroup method, of class ImageGroupTable.
     */
    @Test
    public void testDeleteGroup() throws Exception
    {
        System.out.println("deleteGroup");
        ImageGroupTable instance = new ImageGroupTable(db);
        instance.deleteGroup(user, group);
        assertTrue(instance.getCount(user, group) == 0);
    }

    /**
     * Test of getGroups method, of class ImageGroupTable.
     */
    @Test
    public void testGetGroups() throws Exception
    {
        System.out.println("getGroups");
        
        ImageGroupTable instance = new ImageGroupTable(db);
        instance.deleteGroup(user, group);
        instance.addToGroup(user, group, 123);
        instance.addToGroup(user, group, 234);
        TreeSet result = instance.getGroups(user);
        assertTrue(result.size()==2);
    }
}