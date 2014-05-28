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

package edu.fullerton.ldvservlet;

import edu.fullerton.ldvw.ViewManager;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.ViewerConfig;

/**
 * Main LigoDV-web servlet
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class View extends HttpServlet
{
    private long loadTime;

    private final String version;
    private ViewerConfig viewerConfig;      // config file read once on init

    public View()
    {
        this.version = "0.2.10";
    }

    /**
     * Initialization on loading servlet, one time things like
     * 
     * Load our configuration file.
     * Make sure all the tables exist.
     * Read configuration file
     * (see code for a complete list)
     *
     * @throws javax.servlet.ServletException
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
            "edu.fullerton.ldvtables.SessionTable",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        ServletSupport servletSupport = new ServletSupport();
        servletSupport.checkDb(tableNames);
        viewerConfig = servletSupport.getViewerConfig();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        ViewManager vmgr = new ViewManager();
        vmgr.processRequest(request, response, viewerConfig, version);

    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "name: LigoDV-web\n"
               + "version: " + version + "\n"
               + "author: Joseph Areeda, Joshua Smith\n"
               + "copyright: 2012-2014\n"
               + "license:  GNU Public 3.0\n";
    }// </editor-fold>

}
