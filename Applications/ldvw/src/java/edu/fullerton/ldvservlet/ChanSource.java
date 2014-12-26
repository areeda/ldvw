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

import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ndsproxyclient.ChanSourceData;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanSource extends HttpServlet
{
    private ViewerConfig viewerConfig;
    private long loadTime;

    /**
     * Initialization on loading servlet, one time things like
     *
     * Load our configuration file. Make sure all the tables exist.
     *
     * @throws javax.servlet.ServletException
     */

    @Override
    public void init() throws ServletException
    {
        loadTime = System.currentTimeMillis();
        String[] tableNames =
        {
            "edu.fullerton.ldvtables.ErrorLog",
            "edu.fullerton.ldvtables.HelpTextTable",
            "edu.fullerton.ldvtables.ChannelTable",
            "edu.fullerton.ldvtables.ChannelIndex",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        ServletSupport servSupport = new ServletSupport();
        servSupport.checkDb(tableNames);
        viewerConfig = servSupport.getViewerConfig();
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
        String name = request.getParameter("name");
        if (name == null)
        {   //  required parameters not available, show them where help is
            response.setContentType("text/html;charset=UTF-8");
            try (PrintWriter out = response.getWriter())
            {
                out.println("<!DOCTYPE html>");
                out.println("<html>");
                out.println("<head>");
                out.println("<title>Servlet ChanSource</title>");            
                out.println("</head>");
                out.println("<body>");
                out.println("<h1>Servlet ChanSource at " + request.getContextPath() + "</h1>");
                out.println("<a href='https://ldvw.ligo.caltech.edu/dokuwiki/doku.php?id=ldvwchansourcedoc'>");
                out.println("Servlet documentation</a> is in the ldvw wiki");
                out.println("</body>");
                out.println("</html>");
            }
        }
        else
        {
            String server = request.getParameter("server");
            String type = request.getParameter("type");
            TimeInterval searchRange = new TimeInterval(0, 1800000000);
            String strtStr = request.getParameter("start");
            String endStr = request.getParameter("end");
            
            if (strtStr != null && strtStr.matches("^\\d+$"))
            {
                searchRange.setStartGps(Long.parseLong(strtStr));
            }
            if (endStr != null && endStr.matches("^\\d+$"))
            {
                searchRange.setStopGps(Long.parseLong(endStr));
            }
            try
            {
                ServletSupport servletSupport;

                servletSupport = new ServletSupport();
                servletSupport.init(request, viewerConfig, false);
                ChannelTable ctbl = new ChannelTable(servletSupport.getDb());
                ChannelIndex cindx = new ChannelIndex(servletSupport.getDb());
                String ctype = type == null ? "" : type;
                ArrayList<ChanIndexInfo> ciiList = cindx.search("", "", ">=", 0.f, ctype, name, 0, 20);
                ChanPointerTable cptrs = new ChanPointerTable(servletSupport.getDb());
                
                response.setContentType("text/plain;charset=UTF-8");
                try (PrintWriter out = response.getWriter())
                {
                    int nDone=0;
                    for(ChanIndexInfo cii: ciiList)
                    {
                        List<ChanInfo> chanList = getChanList(cii, cptrs, ctbl, type, server);
                        for(ChanInfo ci : chanList)
                        {
                            ChanSourceData csd = new ChanSourceData();
                            csd.pullData(ci);
                            StringBuilder strb = csd.getAsStringBuilder(searchRange, 1);
                            if (strb.length() > 0)
                            {
                                out.println(String.format("%1$s, %2$s\n", 
                                                          ci.getChanName(),ci.getServer()));
                                out.println(strb);
                                nDone++;
                            }
                        }
                    }
                    if (nDone == 0)
                    {
                        out.println("Error: no sources found.");
                    }
                    out.flush();
                }                

            }
            catch (LdvTableException |SQLException ex)
            {
                Logger.getLogger(ChanSource.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private List<ChanInfo> getChanList(ChanIndexInfo cii, ChanPointerTable cptrs, ChannelTable ctbl,
                                       String type, String server) throws LdvTableException
    {
        List<Integer> chanList = new ArrayList<>();
        int bid = cii.getIndexID();
        String[] trtype =
        {
            "mean"
        };
        if (cii.hasRaw() && wantType(type,"raw"))
        {
            chanList.addAll(cptrs.getChanList(bid, "raw"));
        }
        if (cii.hasRds() && wantType(type,"rds"))
        {
            chanList.addAll(cptrs.getChanList(bid, "rds"));
        }
        if (cii.hasMtrends() && wantType(type,"minute-trend"))
        {
            chanList.addAll(cptrs.getChanList(bid, "minute-trend", trtype));
        }
        if (cii.hasStrends() && wantType(type,"second-trend"))
        {
            chanList.addAll(cptrs.getChanList(bid, "second-trend", trtype));
        }
        if (cii.hasStatic() && wantType(type,"static"))
        {
            chanList.addAll(cptrs.getChanList(bid, "static"));
        }
        ArrayList<ChanInfo> chanInfoList = new ArrayList<>();
        for(int idx : chanList)
        {
            ChanInfo ci = ctbl.getChanInfo(idx);
            if (server == null || server.isEmpty() || server.equalsIgnoreCase(ci.getServer()))
            {
                chanInfoList.add(ci);
            }
        }
        return chanInfoList;
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

    /**
     * Interpret the type parameter vs a specific type
     * @param wtype the type they want
     * @param gtype the type we got
     * @return 
     */
    private boolean wantType(String wtype, String gtype)
    {
        boolean ret = true;
        if (wtype != null && !wtype.isEmpty())
        {
            if (wtype.equalsIgnoreCase(gtype))
            {
                ret = true;
            }
            else if (wtype.equalsIgnoreCase(gtype))
            {
                ret = true;
            }
        }
        return ret;
    }

}
