/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.fullerton.ldvservlet;

import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.SessionTable;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.ViewerConfig;

/**
 *
 * @author areeda
 */
public class SessionHistory extends HttpServlet
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
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, WebUtilException
    {
        response.setContentType("text/html;charset=UTF-8");
        
        ServletSupport servletSupport;

        servletSupport = new ServletSupport();
        servletSupport.init(request, viewerConfig, false);

        Page vpage = servletSupport.getVpage();
        vpage.includeJS("showByClass.js");
        vpage.setTitle("Session history");
        servletSupport.addStandardHeader("");
        servletSupport.addNavBar();
        String contextPath = request.getContextPath();
 
        try
        {            
            SessionTable sessions = new SessionTable(servletSupport.getDb());
            String q1 = "select min(sessStart) as mn, max(sessStart) as mx from "
                    + sessions.getName();
            ResultSet res = servletSupport.getDb().executeQuery(q1);
            Timestamp tmin, tmax;
            if (res.next())
            {
                tmin = res.getTimestamp("mn");
                tmax = res.getTimestamp("mx");
            }
            else
            {
                throw new WebUtilException("Problem accessing session table");
            }
            
            TimeUnit tu = TimeUnit.DAYS;
            int ndays = (int) tu.convert(tmax.getTime()  - tmin.getTime(), TimeUnit.MILLISECONDS) + 1;
            int nweeks = (int) (ndays + 6) / 7;
            
            int[] dow = new int[8];
            for (int i=0;i<8;i++) dow[i] = 0;
            int[] days = new int[ndays];
            for (int i=0;i<ndays;i++) days[i] = 0;
            int[] weeks = new int[nweeks];
            for (int i=0;i<nweeks;i++) weeks[i] = 0;
            
            sessions.streamByQuery("SELECT sessStart FROM " + sessions.getName());
            ResultSet rs;
            while ((rs = sessions.streamNextRs()) != null)
            {
                Timestamp time = rs.getTimestamp("sessStart");
                Calendar c = Calendar.getInstance();
                c.setTime(time);
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                dow[dayOfWeek]++;
                int day = (int) tu.convert(time.getTime()  - tmin.getTime(), TimeUnit.MILLISECONDS);
                days[day]++;
                int week = day / 7;
                weeks[week]++;
            }

            addTableDow(vpage, dow, 8, "Day of week");
            addTableWeek(vpage, weeks, nweeks, "Weeks",tmin);
            addTableDay(vpage, days, ndays, "Days",tmin);
            
            servletSupport.showPage(response);
            
        }
        catch ( SQLException | WebUtilException | ServletException ex)
        {
            String ermsg = String.format("Error showing channel source data. %1$s - %2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            vpage.add(ermsg);
            servletSupport.showPage(response);
        }
        finally
        {
            servletSupport.close();
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
        try
        {
            processRequest(request, response);
        }
        catch (WebUtilException ex)
        {
            String ermsg = String.format("Error displaying source data %1$s - %2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new ServletException(ermsg);
        }
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
        try
        {
            processRequest(request, response);
        }
        catch (WebUtilException ex)
        {
            String ermsg = String.format("Error displaying source data %1$s - %2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new ServletException(ermsg);
        }
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

    private void addTableDay(Page vpage, int[] cnts, int n, String title, Timestamp tmin) throws WebUtilException
    {
        PageItemHeader titl = new PageItemHeader(title, 3);
        PageTable tbl = new PageTable();
        tbl.setId("byday");
        
        tbl.setSortable(true);
        PageTableRow hdr = new PageTableRow("Date");
        hdr.add("Sessions");
        hdr.setHeader();
        tbl.addRow(hdr);
        for (int i=0; i<n; i++)
        {
            long ms = i * 24L * 3600L * 1000L + tmin.getTime();
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(ms);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String dt = sdf.format(c.getTime());

            PageTableRow r = new PageTableRow(dt);
            r.add(cnts[i]);
            tbl.addRow(r);

        }
        vpage.add(titl);
        vpage.add(tbl);
    }

    private void addTableDow(Page vpage, int[] cnts, int n, String title) throws WebUtilException
    {
        String[] dayNames = { "","Sunday", "Monday", "Tuesday", "Wednesday",
            "Thursday", "Friday","Saturday"};
        
        PageItemHeader titl = new PageItemHeader(title, 3);
        PageTable tbl = new PageTable();
        
        for (int i = 1; i < n; i++)
        {
            PageTableRow r =new PageTableRow(dayNames[i]);
            r.add(cnts[i]);
            tbl.addRow(r);
        }
        vpage.add(titl);
        vpage.add(tbl);
    }

    private void addTableWeek(Page vpage, int[] weeks, int nweeks, String title, Timestamp tmin) throws WebUtilException
    {
        PageItemHeader titl = new PageItemHeader(title, 3);
        PageTable tbl = new PageTable();
        tbl.setId("byweek");
        tbl.setSortable(true);
        
        PageTableRow hdr = new PageTableRow("Date");
        hdr.add("Sessions");
        hdr.setHeader();
        tbl.addRow(hdr);
        for (int i = 0; i < nweeks; i++)
        {
            long ms = i * 7L * 24L * 3600L * 1000L + tmin.getTime();
            Calendar c = new GregorianCalendar();
            c.setTimeInMillis(ms);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            String dt =sdf.format(c.getTime());
            
            PageTableRow r = new PageTableRow(dt);
            r.add(weeks[i]);
            tbl.addRow(r);
        }
        vpage.add(titl);
        vpage.add(tbl);
    }

}
