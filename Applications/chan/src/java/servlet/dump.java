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
package servlet;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.DbSupport;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
@WebServlet(name = "dump", urlPatterns =
    {
        "/dump"
})
public class dump extends HttpServlet
{
    private ViewerConfig viewerConfig;
    private long loadTime;
    /**
     * Initialization on loading servlet, one time things like
     *
     * Load our configuration file. Make sure all the tables exist. Read configuration file (see
     * code for a complete list)
     *
     * @throws javax.servlet.ServletException
     */
    @Override
    public void init() throws ServletException
    {
        loadTime = System.currentTimeMillis();
        String[] tableNames =
        {
            "edu.fullerton.ldvtables.ChanPointerTable",
            "edu.fullerton.ldvtables.ChanUpdateTable",
            "edu.fullerton.ldvtables.ChannelIndex",
            "edu.fullerton.ldvtables.ChannelTable",
            "edu.fullerton.ldvtables.ErrorLog",
            "edu.fullerton.ldvtables.HelpTextTable",
            "edu.fullerton.ldvtables.NdsStatsTable",
            "edu.fullerton.ldvtables.PageItemCache",
            "edu.fullerton.ldvtables.ServerTable",
            "edu.fullerton.ldvtables.SessionTable",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        DbSupport dbSupport = new DbSupport();
        try
        {
            dbSupport.checkDb(tableNames);
        }
        catch (ViewConfigException ex)
        {
            throw new ServletException("Verifying database tables: ", ex);
        }
        viewerConfig = dbSupport.getViewerConfig();
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
        Database db=null;
        try
        {
            Map<String, String[]> parameterMap = request.getParameterMap();
            String server = getParameter(parameterMap,"server");
            if (parameterMap.containsKey("cache"))
            {
                if (server.isEmpty())
                {
                    response.setContentType("text/plain;charset=us-ascii");
                    try (PrintWriter out = response.getWriter())
                    {
                        out.println("Error: Must set the server parameter for a cache retrieval");
                    }
                }
                else
                {
                    String cachePath="/usr1/cache/";
                    File data = new File(cachePath + server + ".csv");
                    if (!data.canRead())
                    {
                        response.setContentType("text/plain;charset=us-ascii");
                        try (PrintWriter out = response.getWriter())
                        {
                            out.println("Error: data for " + server + " is not available.");
                        }
                    }
                    else
                    {
                        long size=data.length();
                        response.setContentType("text/csv;charset=us-ascii");
                        response.setHeader("Content-Disposition", "attachment; filename=\"" + "chanList.csv\"");
//                        response.setContentLength((int)size);
                        try (PrintWriter out = response.getWriter(); FileReader rdr = new FileReader(data))
                        {
                            char[] buf = new char[5000];
                            int bsize;
                            while( (bsize=rdr.read(buf)) >0)
                            {
                                if (bsize == buf.length)
                                {
                                    out.print(buf);
                                }
                                else
                                {
                                    char[] b2 = new char[bsize];
                                    System.arraycopy(buf, 0, b2, 0, bsize);
                                    out.print(b2);
                                }
                            }
                        }
                    }
                }
            }
            else if (parameterMap.containsKey("base"))
            {
                
                db = viewerConfig.getDb();
                ChannelIndex cidx = new ChannelIndex(db);
                cidx.streamAll();
                ChanIndexInfo cii;
                response.setContentType("text/csv;charset=us-ascii");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + "chanList.csv\"");
                StringBuilder buf = new StringBuilder(700000);
                try (PrintWriter out = response.getWriter())
                {
                    int lc=0;
                    while ((cii = cidx.streamNext()) != null)
                    {
                        if (lc >= 10000)
                        {
                            out.append(buf);
                            lc=0;
                            buf.setLength(0);
                        }
                        buf.append(cii.getCSV()).append("\n");
                        lc++;
                    }
                    cidx.streamClose();
                    if (buf.length() > 0)
                    {
                        out.append(buf);
                    }
                    out.close();
                }
            }
            else if (server.isEmpty())
            {
                response.setContentType("text/plain;charset=us-ascii");
                try (PrintWriter out = response.getWriter())
                {
                    out.println("Error: Must set either base or server" );
                }
            }
            else
            {
                db = viewerConfig.getDb();
                ChannelTable ct = new ChannelTable(db);
                ct.streamByServerName(server, "");
                ChanInfo ci;
                response.setContentType("text/csv;charset=us-ascii");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + "chanList.csv\"");
                try (PrintWriter out = response.getWriter())
                {
                    while ((ci = ct.streamNext()) != null)
                    {
                        out.println(ci.getCSV());
                    }
                    ct.streamClose();
                }

            }
        }
        catch (ViewConfigException | SQLException ex)
        {
            response.setContentType("text/plain;charset=us-ascii");
            try (PrintWriter out = response.getWriter())
            {
                out.println("Error: " + ex.getLocalizedMessage());
            }
        }
        finally
        {
            if (db != null)
            {
                db.close();
            }
        }
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
        return "Short description";
    }// </editor-fold>

    private String getParameter(Map<String, String[]> parameterMap, String param)
    {
        String[] p = parameterMap.get(param);
        String ret = "";
        if (p != null)
        {
            ret = p[0];
        }
        return ret;
    }

}
