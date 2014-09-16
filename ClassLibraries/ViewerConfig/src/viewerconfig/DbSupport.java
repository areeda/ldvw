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

package viewerconfig;

import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class DbSupport 
{
    private ViewerConfig viewerConfig;
    /**
     * Read our config file, check database connection and that all necessary tables exist.
     *
     * @param tableNames
     * @throws ServletException
     */

    /**
     * Read our config file, check database connection and that all necessary tables exist.
     * @param tableNames
     * @throws viewerconfig.ViewConfigException
     */
    public void checkDb(String[] tableNames) throws ViewConfigException
    {
        viewerConfig = new ViewerConfig();
        Database mydb;
        try
        {
            mydb = viewerConfig.getDb();
        }
        catch (ViewConfigException ex)
        {
            throw new ViewConfigException(ex.getLocalizedMessage());
        }
        checkDb(mydb, tableNames);
        mydb.close();
    }
    /**
     * Verify that all tables exist, if not try to create them.
     * 
     * @param mydb an open database connection
     * @param tableClassNames 
     */
    public void checkDb(Database mydb, String[] tableClassNames) throws ViewConfigException
    {
        for (String className : tableClassNames)
        {
            try
            {
                Class<?> clazz = Class.forName(className);
                Constructor<?> ctor = clazz.getConstructor(Database.class);
                Table table = (Table) ctor.newInstance(mydb);
                if (!table.exists(true))
                {
                    table.createTable();
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException |
                   SecurityException | InstantiationException |
                   IllegalAccessException | IllegalArgumentException |
                   InvocationTargetException ex)
            {
                String ermsg = "Error verifying necessary tables exist:" + ex.getClass().getSimpleName()
                               + " " + ex.getLocalizedMessage();
                throw new ViewConfigException(ermsg);
            }
        }
    }

    public ViewerConfig getViewerConfig()
    {
        return viewerConfig;
    }

}
