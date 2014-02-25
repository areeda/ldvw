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
package edu.fullerton.ldvw;

import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

// tables used by the application, we make sure they exist
/**
 * the main program that controls building and sending the output to the client browser.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */

public class Ldvw extends HttpServlet 
{
    private long loadTime;

    private final String version;
    private ViewerConfig viewerConfig;      // config file read once on init
    
    public Ldvw()
    {
        this.version = "0.1.55  ";
    }
    
    /**
     * If the servlet is called with GET style form data this is the entry point
     * 
     * @param request
     * @param response
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        try
        {
            ViewManager vmgr = new ViewManager();
            vmgr.processRequest(request, response, viewerConfig, version);
        }
        catch (ViewConfigException ex)
        {
            throw new ServletException(ex.getLocalizedMessage());
        }
    }
    /**
     * When the servlet is called with PUT style form data this is the entry point
     * 
     * @param request
     * @param response
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException
    {
        try
        {
            ViewManager vmgr = new ViewManager();
            vmgr.processRequest(request, response, viewerConfig, version);
        }
        catch (ViewConfigException ex)
        {
            throw new ServletException(ex.getLocalizedMessage());
        }
    }

    /**
     * Initialization on loading servlet, one time things like making sure all the tables exist.
     * 
     */
    @Override
    public void init() throws ServletException
    {
        loadTime = System.currentTimeMillis();
        String[] tableNames = 
        {
            "edu.fullerton.ldvtables.ChanDataAvailability",
            "edu.fullerton.ldvtables.ChanPointerTable",
            "edu.fullerton.ldvtables.ChanUpdateTable",
            "edu.fullerton.ldvtables.ChannelIndex",
            "edu.fullerton.ldvtables.ChannelTable",
            "edu.fullerton.ldvtables.DataTable",
            "edu.fullerton.ldvtables.ErrorLog",
            "edu.fullerton.ldvtables.HelpTextTable",
            "edu.fullerton.ldvtables.ImageCoordinateTbl",
            "edu.fullerton.ldvtables.ImageGroupTable",
            "edu.fullerton.ldvtables.ImageTable",
            "edu.fullerton.ldvtables.NdsStatsTable",
            "edu.fullerton.ldvtables.PageItemCache",
            "edu.fullerton.ldvtables.ServerTable",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        viewerConfig = new ViewerConfig();
        Database mydb;
        try
        {
            mydb = viewerConfig.getDb();
        }
        catch (ViewConfigException ex)
        {
            throw new ServletException(ex.getLocalizedMessage());
        }
        for(String tableName: tableNames)
        {
            try
            {
                Class<?> clazz = Class.forName(tableName);
                Constructor<?> ctor = clazz.getConstructor(Database.class);
                Table table = (Table) ctor.newInstance(mydb);
                if (! table.exists(true))
                {
                    table.createTable();
                }
            }
            catch (ClassNotFoundException | NoSuchMethodException | 
                    SecurityException | InstantiationException | 
                    IllegalAccessException | IllegalArgumentException | 
                    InvocationTargetException ex)
            {
                Logger.getLogger(Ldvw.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        mydb.close();
    }
    /**
     *
     * @return
     */
    @Override
    public String getServletInfo()
    {
        return "name: LigoDV-web\n"
                + "version: " + version + "\n"
                + "author: Joseph Areeda, Joshua Smith\n"
                + "copyright: 2012-2013\n"
                + "license:  GNU Public 3.0\n";
    }
}
