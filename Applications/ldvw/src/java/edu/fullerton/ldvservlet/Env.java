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

import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Env extends HttpServlet
{
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
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
        Page vpage;
        long startTime;

        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter())
        {
            startTime = System.currentTimeMillis();

            vpage = new Page();
            // @todo use base style as a user selectable theme theme
            String baseCss = request.getContextPath() + "/defaultStyleR08.css";

            
            vpage.includeCSS(baseCss);
            vpage.setBodyClass("tundra");

            vpage.setTitle("Servlet environment");
            
            addShib(request, vpage);
            addEnv(vpage);
            addCookies(request, vpage);
            addParts(request, vpage);
            addHeaders(request, vpage);
            addParameters(request, vpage);
            addRequestAttributes(request, vpage);
            addOtherRequestVals(request, vpage);
            addSessionAttributes(request, vpage);
            
            
            String pageHtml = vpage.getHTML();
            out.print(pageHtml);
            out.flush();
            
        }
        catch (WebUtilException | IOException | IllegalStateException | ServletException ex)
        {
            String ermsg = "collecting environment info: " + ex.getClass().getSimpleName()
                           + ex.getMessage();
            throw new WebUtilException(ermsg);
        }
    }

    private void addShib(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        TreeMap<String, String> shibData = new TreeMap<>();
        
        add1Attribute("AJP_eduPersonPrincipalName", request, shibData);
        add1Attribute("eduPersonPrincipalName", request, shibData);
        add1Hdr("eduPersonPrincipalName", request, shibData);
        add1Hdr("isMemberOf", request, shibData);
        add1Attribute("isMemberOf", request, shibData);
        add1Hdr("AJP_eduPersonPrincipalName", request, shibData);
        add1Hdr("AJP_isMemberOf", request, shibData);
        add1Attribute("AJP_isMemberOf", request, shibData);
        add1Attribute("Shib-Identity-Provider", request, shibData);

        tblTitle("Environment variables:", vpage);

        addTable(shibData, vpage);    
    }
    
    private void add1Attribute(String atName, HttpServletRequest request, Map<String, String> data)
    {
        String attr = (String) request.getAttribute(atName);

        if (attr == null)
        {
            attr = "<null>";
        }
        data.put(atName, attr);
    }

    private void addEnv(Page vpage) throws WebUtilException
    {
        vpage.addBlankLines(2);
        tblTitle("Environment variables:", vpage);
        Map<String, String> env = System.getenv();
        addTable(env, vpage);
    }

    private void tblTitle(String t, Page vpage) throws WebUtilException
    {
        PageItemString title = new PageItemString(t);
        title.addStyle("font-weight", "bold");
        vpage.add(title);
        vpage.addBlankLines(2);
    }

    private void addTable(Map<String, String> env, Page vpage) throws WebUtilException
    {

        PageTable pt = new PageTable();
        String[] hdr =
        {
            "Name", "Value"
        };
        PageTableRow h = new PageTableRow();
        h.add(hdr);
        h.setHeader();
        pt.addRow(h);

        TreeSet<String> sortedKeys;

        sortedKeys = new TreeSet<>(env.keySet());
        for (String k : sortedKeys)
        {
            PageTableRow row = new PageTableRow();

            String v = env.get(k);
            v = v == null ? "" : v;
            row.add(k);
            row.add(v);
            pt.addRow(row);
        }
        vpage.add(pt);
        vpage.addBlankLines(2);
    }

    private void addCookies(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Cookies:", vpage);
        Cookie[] cookies = request.getCookies();
        PageTable pt = new PageTable();

        if (cookies != null)
        {
            String[] hdr =
            {
                "Name", "Path", "Value", "Comment"
            };
            PageTableRow h = new PageTableRow();
            h.add(hdr);
            h.setHeader();
            pt.addRow(h);
            for (Cookie c : cookies)
            {
                PageTableRow row = new PageTableRow();
                row.add(c.getName());
                row.add(c.getPath());
                row.add(c.getValue());
                row.add(c.getComment());
                pt.addRow(row);
            }
        }
        vpage.add(pt);
        vpage.addBlankLines(2);
    }

    private void addParts(HttpServletRequest request, Page vpage) throws WebUtilException, IOException, IllegalStateException, ServletException
    {
        tblTitle("Parts (if multi-part:",vpage);

        try
        {
            Collection<Part> parts;
            parts = request.getParts();
            if (!parts.isEmpty())
            {
                PageTable pt = new PageTable();
                String[] hdr =
                {
                    "Name", "Mime", "Size"
                };
                PageTableRow h = new PageTableRow();
                h.add(hdr);
                h.setHeader();
                pt.addRow(h);
                for (Part p : parts)
                {
                    PageTableRow r = new PageTableRow();
                    r.add(p.getName());
                    r.add(p.getContentType());
                    r.add(p.getSize());
                    pt.addRow(r);
                }
                vpage.add(pt);
            }
            vpage.addBlankLines(2);
        }
        catch (ServletException ex)
        {
            // this seems to mean there are no parts
        }
    }

    private void addHeaders(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Headers:",vpage);
        TreeMap<String, String> hd = new TreeMap<>();

        Enumeration hdrs = request.getHeaderNames();

        while (hdrs.hasMoreElements())
        {
            String headerName = (String) hdrs.nextElement();
            String headerVal = request.getHeader(headerName);
            hd.put(headerName, headerVal);
        }
        addTable(hd, vpage);

    }

    private void addParameters(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Parameters:",vpage);
        TreeMap<String, String> par = new TreeMap<>();

        Enumeration parms = request.getParameterNames();
        while (parms.hasMoreElements())
        {
            String pname = (String) parms.nextElement();
            String pval = request.getParameter(pname);
            par.put(pname, pval);
        }
        addTable(par,vpage);
    }

    private void addRequestAttributes(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Request attributes:",vpage);
        TreeMap<String, String> ats = new TreeMap<>();

        Enumeration rq = request.getAttributeNames();

        while (rq.hasMoreElements())
        {
            String at = (String) rq.nextElement();
            String atv = request.getAttribute(at).toString();
            ats.put(at, atv);
        }
        addTable(ats, vpage);
    }

    private void addOtherRequestVals(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Other request values:",vpage);
        TreeMap<String, String> ats = new TreeMap<>();

        ats.put("Remote User", request.getRemoteUser());
        ats.put("Auth type", request.getAuthType());
        ats.put("Character Endoding", request.getCharacterEncoding());
        ats.put("Content Type", request.getContentType());
        ats.put("Context Path", request.getContextPath());
        ats.put("Local Address", request.getLocalAddr());
        ats.put("Local Name", request.getLocalName());
        ats.put("Method", request.getMethod());
        ats.put("Path Info", request.getPathInfo());
        ats.put("Path Translated", request.getPathTranslated());
        ats.put("Protocol", request.getProtocol());
        ats.put("Query String", request.getQueryString());
        ats.put("Remote Address", request.getRemoteAddr());
        ats.put("Remote Host", request.getRemoteHost());
        ats.put("Remote URI", request.getRequestURI());
        ats.put("Requested Session ID", request.getRequestedSessionId());
        ats.put("Scheme", request.getScheme());
        ats.put("Server Name", request.getServerName());
        ats.put("Servlet Path", request.getServletPath());
        String userPrincipal;
        Principal userPrincipal1 = request.getUserPrincipal();
        userPrincipal = userPrincipal1 == null ? null : userPrincipal1.getName();
        ats.put("User Principal", userPrincipal);

        addTable(ats, vpage);
    }

    private void addSessionAttributes(HttpServletRequest request, Page vpage) throws WebUtilException
    {
        tblTitle("Session attributes:", vpage);
        TreeMap<String, String> ats = new TreeMap<>();

        HttpSession session = request.getSession();
        Enumeration sess = session.getAttributeNames();
        while (sess.hasMoreElements())
        {
            String it = (String) sess.nextElement();
            Object ob = session.getAttribute(it);
            ats.put(it, ob.toString());
        }
        addTable(ats, vpage);
    }

    private void add1Hdr(String atName, HttpServletRequest request, Map<String, String> data)
    {
        String attr = (String) request.getHeader(atName);

        if (attr == null || attr.isEmpty())
        {
            attr = (String) request.getHeader("AJP_" + atName);
            if (attr == null)
            {
                attr = "<null>";
            }
        }
        data.put("hdr: " + atName, attr);
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
            throw new ServletException(ex.getLocalizedMessage());
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
            throw new ServletException(ex.getLocalizedMessage());
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

}
