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

package edu.fullerton.ldvw;

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
public class env extends HttpServlet
{
    private PrintWriter out;
    private HttpSession session;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Page vpage;
    private long startTime;

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
        response.setContentType("text/html;charset=UTF-8");
        try
        {
            this.request = request;
            this.response = response;
            startTime = System.currentTimeMillis();
            session = request.getSession();

            this.out = response.getWriter();
            vpage = new Page();
            // @todo use base style as a user selectable theme theme
            String baseCss = "/viewer/defaultStyle.css";

            vpage.includeCSS("https://ajax.googleapis.com/ajax/libs/dojo/1.7.1/dijit/themes/tundra/tundra.css");
            vpage.includeCSS(baseCss);
            vpage.setBodyClass("tundra");

            vpage.setTitle("JSP environment");
            add1Attribute("AJP_eduPersonPrincipalName");
            add1Attribute("eduPersonPrincipalName");
            add1Hdr("eduPersonPrincipalName");
            add1Hdr("isMemberOf");
            add1Attribute("isMemberOf");
            add1Attribute("Shib-Identity-Provider");

            
            addEnv();
            addCookies();
            addParts();
            addHeaders();
            addParameters();
            addRequestAttributes();
            addOtherRequestVals();
            addSessionAttributes();
            
            
            String pageHtml = vpage.getHTML();
            out.print(pageHtml);
            out.flush();
            
        }
        catch (WebUtilException | IOException | IllegalStateException | ServletException ex)
        {
            try
            {
                vpage.add("Error: " + ex.getClass().getSimpleName() + " - " + ex.getLocalizedMessage());
                String pageHtml = vpage.getHTML();
                out.print(pageHtml);
                out.flush();
            }
            catch (WebUtilException ex1)
            {
                // ignore errors reporting errors
            }
        }

    }

    private void add1Attribute(String atName)
    {
        String attr = (String) request.getAttribute(atName);

        if (attr == null)
        {
            attr = "<null>";
        }
        vpage.add("Attribute: " + atName + " = " + attr);
        vpage.addBlankLines(1);
    }

    private void addEnv() throws WebUtilException
    {
        vpage.addBlankLines(2);
        tblTitle("Environment variables:");
        Map<String, String> env = System.getenv();
        addTable(env);
    }

    private void tblTitle(String t) throws WebUtilException
    {
        PageItemString title = new PageItemString(t);
        title.addStyle("font-weight", "bold");
        vpage.add(title);
        vpage.addBlankLines(2);
    }

    private void addTable(Map<String, String> env) throws WebUtilException
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

    private void addCookies() throws WebUtilException
    {
        tblTitle("Cookies:");
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

    private void addParts() throws WebUtilException, IOException, IllegalStateException, ServletException
    {
        tblTitle("Parts (if multi-part:");

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

    private void addHeaders() throws WebUtilException
    {
        tblTitle("Headers:");
        TreeMap<String, String> hd = new TreeMap<>();

        Enumeration hdrs = request.getHeaderNames();

        while (hdrs.hasMoreElements())
        {
            String headerName = (String) hdrs.nextElement();
            String headerVal = request.getHeader(headerName);
            hd.put(headerName, headerVal);
        }
        addTable(hd);

    }

    private void addParameters() throws WebUtilException
    {
        tblTitle("Parameters:");
        TreeMap<String, String> par = new TreeMap<>();

        Enumeration parms = request.getParameterNames();
        while (parms.hasMoreElements())
        {
            String pname = (String) parms.nextElement();
            String pval = request.getParameter(pname);
            par.put(pname, pval);
        }
        addTable(par);
    }

    private void addRequestAttributes() throws WebUtilException
    {
        tblTitle("Request attributes:");
        TreeMap<String, String> ats = new TreeMap<>();

        Enumeration rq = request.getAttributeNames();

        while (rq.hasMoreElements())
        {
            String at = (String) rq.nextElement();
            String atv = request.getAttribute(at).toString();
            ats.put(at, atv);
        }
        addTable(ats);
    }

    private void addOtherRequestVals() throws WebUtilException
    {
        tblTitle("Other request values:");
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

        addTable(ats);
    }

    private void addSessionAttributes() throws WebUtilException
    {
        tblTitle("Session attributes:");
        TreeMap<String, String> ats = new TreeMap<>();

        Enumeration sess = session.getAttributeNames();
        while (sess.hasMoreElements())
        {
            String it = (String) sess.nextElement();
            Object ob = session.getAttribute(it);
            ats.put(it, ob.toString());
        }
        addTable(ats);
    }

    private void add1Hdr(String atName)
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
        vpage.add("Header: " + atName + " = " + attr);
        vpage.addBlankLines(1);
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

}
