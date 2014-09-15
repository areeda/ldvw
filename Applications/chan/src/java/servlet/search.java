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

import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.ChannelTable.ChanFiltType;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
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
public class search extends HttpServlet
{
    private long loadTime;

    private final String version;
    private ViewerConfig viewerConfig;      // config file read once on init

    public search()
    {
        this.version = "0.0.1";        
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
        try
        {
            Map<String, String[]> parameterMap = request.getParameterMap();

            if (parameterMap.containsKey("bsearch") && parameterMap.containsKey("base") )
            {
                ArrayList<ChanIndexInfo> ciiList = doBsearch(parameterMap);
                if (ciiList.isEmpty())
                {
                    response.setContentType("text/plain;charset=us-ascii");
                    try (PrintWriter out = response.getWriter())
                    {
                        out.println("Error: no channels matched spec.");
                    }
                }
                else
                {
                    response.setContentType("text/csv;charset=us-ascii");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + "chanList.csv\"");
                    try (PrintWriter out = response.getWriter())
                    {
                        out.println(ChanIndexInfo.getCSVHeader());
                        for (ChanIndexInfo cii : ciiList)
                        {
                            out.println(cii.getCSV());
                        }
                    }
 
                }
            }
            else
            {
                ArrayList<ChanInfo> cList;
                cList=doSearch(parameterMap);

                if (cList != null && !cList.isEmpty())
                {
                    response.setContentType("text/csv;charset=us-ascii");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + "chanList.csv\"");
                    try (PrintWriter out = response.getWriter())
                    {
                        for(ChanInfo ci : cList)
                        {
                            out.println(ci.getCSV());
                        }
                    }
                }
                else
                {
                    response.setContentType("text/plain;charset=us-ascii");
                    try (PrintWriter out = response.getWriter())
                    {
                        out.println("Error: no channels matched spec.");
                    }
                }
            }
        }
        catch (WebUtilException ex)
        {
            response.setContentType("text/plain;charset=us-ascii");
            try (PrintWriter out = response.getWriter())
            {
                out.println("Error: " + ex.getLocalizedMessage());
            }
        }
        
    }
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
     * Perform the channel search using the LigoDV-web classic name matching pattern
     * @param parameterMap
     * @return 
     */
    private ArrayList<ChanInfo> doSearch(Map<String, String[]> parameterMap) throws WebUtilException
    {
        ArrayList<ChanInfo> ret = null;
        
        try
        {
            // extract fields to filter from the request, NB all are optional
            String server = getParameter(parameterMap,"server");
            String ifo = getParameter(parameterMap,"ifo");
            String subsys = getParameter(parameterMap,"subsys");
            String fsCmp = getParameter(parameterMap,"fsCmp");
            String fs = getParameter(parameterMap,"fs");
            String ctype = getParameter(parameterMap,"ctype");
            String dtype = getParameter(parameterMap,"dtype");
            String chnamefilt = getParameter(parameterMap,"chnamefilt");
            String chname = getParameter(parameterMap,"chname");
            String chregex = getParameter(parameterMap,"chregex");
            String bsearch = getParameter(parameterMap,"bsearch");
            String startStr = getParameter(parameterMap,"start");
            String countStr = getParameter(parameterMap,"count");

            int start = 0;
            int count = 10000;
            if (startStr.matches("^\\d+$"))
            {
                start=Integer.parseInt(startStr);
            }

            if (countStr.matches("^\\d+$"))
            {
                count=Integer.parseInt(countStr);
            }

            int nchSpec=0;
            ChanFiltType cft = ChanFiltType.NONE;
            String chPattern="";
            if (!chnamefilt.isEmpty())
            {
                cft = ChanFiltType.LDVW;
                chPattern = chnamefilt;
                nchSpec++;
            }
            if (!chname.isEmpty())
            {
                cft = ChanFiltType.GLOB;
                chPattern=chname;
                nchSpec++;
            }
            if (!chregex.isEmpty())
            {
                cft = ChanFiltType.REGEX;
                chPattern=chregex;
                nchSpec++;
            }
            if (!bsearch.isEmpty())
            {
                nchSpec++;
            }
            if (nchSpec > 1)
            {
                throw new WebUtilException("More than one channel name search pattern specified.");
            }
            if (bsearch.isEmpty())
            {
                ChannelTable ct = new ChannelTable(viewerConfig.getDb());
                ret = ct.getList(server, ifo, subsys, fsCmp, fs, ctype, dtype, cft, chPattern, 
                                 start, count);
            }
            else
            {
                ArrayList<Integer> chanIndxList = new ArrayList<>();
                ArrayList<ChanIndexInfo> ciiList = doBsearch(parameterMap);
                if (!ciiList.isEmpty())
                {
                    ChanPointerTable cpt = new ChanPointerTable(viewerConfig.getDb());
                    if (ctype.isEmpty())
                    {
                        for (ChanIndexInfo cii : ciiList)
                        {
                            List<Integer> chanList = cpt.getChanList(cii.getIndexID());
                            chanIndxList.addAll(chanList);
                        }
                    }
                    else
                    {
                        for (ChanIndexInfo cii : ciiList)
                        {
                            List<Integer> chanList = cpt.getChanList(cii.getIndexID(), ctype);
                            chanIndxList.addAll(chanList);
                        }
                    }
                }
                if (!chanIndxList.isEmpty())
                {
                    if (!parameterMap.containsKey("base"))
                    {
                        ChannelTable ct = new ChannelTable(viewerConfig.getDb());
                        HashSet<ChanInfo> chanSet = ct.getAsSet(chanIndxList);
                        ret = new ArrayList<>(chanSet);
                    }
                }

            }
        }
        catch (LdvTableException | SQLException | ViewConfigException ex)
        {
            throw new WebUtilException("Searching channel list: ", ex);
        }

        if (ret == null)
        {
            ret = new ArrayList<>();
        }
        return ret;
    }

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

    private ArrayList<ChanIndexInfo> doBsearch(Map<String, String[]> parameterMap) throws WebUtilException
    {
        ArrayList<ChanIndexInfo> ciiList=null;

        try
        {
            // extract fields to filter from the request, NB all are optional
            String server = getParameter(parameterMap, "server");
            String ifo = getParameter(parameterMap, "ifo");
            String subsys = getParameter(parameterMap, "subsys");
            String fsCmp = getParameter(parameterMap, "fsCmp");
            String fs = getParameter(parameterMap, "fs");
            String ctype = getParameter(parameterMap, "ctype");
            String dtype = getParameter(parameterMap, "dtype");
            String chnamefilt = getParameter(parameterMap, "chnamefilt");
            String chname = getParameter(parameterMap, "chname");
            String chregex = getParameter(parameterMap, "chregex");
            String bsearch = getParameter(parameterMap, "bsearch");
            String startStr = getParameter(parameterMap, "start");
            String countStr = getParameter(parameterMap, "count");

            int start = 0;
            int count = 10000;
            if (startStr.matches("^\\d+$"))
            {
                start = Integer.parseInt(startStr);
            }

            if (countStr.matches("^\\d+$"))
            {
                count = Integer.parseInt(countStr);
            }

            ChannelIndex cidx = new ChannelIndex(viewerConfig.getDb());
            float fsVal = 0;
            if (fs.matches("(([1-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?"))
            {
                fsVal = Float.parseFloat(fs);
            }
            ciiList = cidx.search(ifo, subsys, fsCmp,
                                               fsVal, ctype, bsearch, start, count);
        }
        catch (LdvTableException | ViewConfigException | SQLException ex)
        {
            throw new WebUtilException("Searching channel list: ", ex);
        }
        if (ciiList == null)
        {
            ciiList = new ArrayList<>();
        }
        return ciiList;
    }

}
