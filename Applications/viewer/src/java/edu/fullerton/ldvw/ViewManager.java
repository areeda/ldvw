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

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemHrLong;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageItemTextLink;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.UseLog;
import edu.fullerton.ldvtables.ViewUser;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * This class is used for one request.  An object is created by the servlet
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ViewManager
{
    private PrintWriter out;
    private HttpSession session;
    private HttpServletRequest request;
    private HttpServletResponse response;
    
    private long startTime;

    private Page vpage;
    private Database db;
    private ViewUser vuser;
    private UseLog uLog;

    // these are used to decide if they need to be warned about programmers on the loose
    private boolean isPrototype = false;
    private boolean isNewSession = false;
    
    private String servletPath;
    private String contextPath;
    private boolean embeded = false;        // when embedded we send much less stuff

    private final String servletName = "ldvw";
    private String version;
    private String helpUrl;
    
    public void oldRequest(HttpServletRequest request, HttpServletResponse response) throws ViewConfigException
    {
        
            ViewerConfig viewerConfig = new ViewerConfig();
            Database mydb = viewerConfig.getDb();
            processRequest(request, response, viewerConfig, "0.1.53-compatible");
        
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response, 
                                                    ViewerConfig viewerConfig, String version) throws ViewConfigException
    {
        this.out = null;
        this.session = request.getSession();
        this.request = request;
        this.response = response;
        this.version = version;
        startTime = System.currentTimeMillis();
        
        try
        {
            contextPath = request.getContextPath();
            servletPath = request.getServletPath();
            
            helpUrl = viewerConfig.get("mainhelp");
            embeded = request.getParameter("embed") != null;

            vpage = new Page();
            addJS();        // put some java script on the page

            // use the configuration file to set options
            
            db = viewerConfig.getDb();
            if (db == null)
            {
                vpage.setTitle("Configuration error.");
                vpage.add("Can't connect to our database.  Sorry.");
                if (out == null)
                {
                    out = response.getWriter();
                }
                String pageHtml = vpage.getHTML();
                out.print(pageHtml);
                out.flush();
            }
            else
            {
                String protoWarn = viewerConfig.get("protoWarn");
                isPrototype = protoWarn != null && protoWarn.equalsIgnoreCase("yes");
                isNewSession = session.isNew();
                mainLoop();
            }
        }
        catch (IOException | WebUtilException | LdvTableException ex)
        {
            try
            {
                String myermsg = "An error occured:" + ex.getClass() + " - " + ex.getMessage();
                vpage.add(myermsg);
                vpage.addBlankLines(2);
                WebUtilException wex = ex instanceof WebUtilException ? (WebUtilException) ex : new WebUtilException(ex);
                logError(wex);

                if (out == null)
                {
                    out = response.getWriter();
                }
                String pageHtml;
                try
                {
                    pageHtml = vpage.getHTML();
                }
                catch (WebUtilException ex2)
                {
                    Page errPage = new Page();
                    errPage.setTitle("Error generating html occurred");
                    errPage.add(myermsg);
                    pageHtml = errPage.getHTML();
                }
                out.print(pageHtml);
                out.flush();
                out.close();
            }
            catch (Exception nex)
            {
                // if we get an exception reporting the uncaught exception we're pretty screwed up
                // so we'll just choke.
            }
        }
        if (db != null)
        {
            db.close();
            db = null;
        }
    }
    /**
     * All pages go through here with the act parameters specifying what we're to do
     *
     * @throws WebUtilException - probably a bug in generating html
     * @throws LdvTableException - some database problem
     */
    private void mainLoop() throws WebUtilException, LdvTableException
    {
        try
        {
            // @todo use base style as a user selectable theme
            vpage.setCssRoot(contextPath);
            String baseCss = "defaultStyleR08.css";
            vpage.setLastCSS(baseCss);

            if (!embeded)
            {
                String jqueryCss = "jquery/jquery-ui_1.css";
                vpage.includeCSS(jqueryCss);
            }

            vuser = new ViewUser(request, db);
            uLog = new UseLog(db);
            vuser.setUlog(uLog);

            if (!vuser.isValid())
            {
                String erMsg = "We're sorry but you do not have privileges to access this service.<br>"
                               + "You must be included in the appropriate community.<br>"
                               + "Please send an email to our mailing list ligodv-web@gravity.phys.uwm.edu "
                               + "We will try to help but we don't control that community.<br>"
                               + "User: " + vuser.getCn() + "<br>";
                vpage.add(new PageItemString(erMsg, false));
                vpage.setTitle("Not Authorized");
                response.setContentType("text/html;charset=UTF-8");
                if (out == null)
                {
                    out = response.getWriter();
                }
                String pageHtml = vpage.getHTML();
                out.print(pageHtml);
                out.flush();
                String descrip = vpage.getTitle() + vuser.getEduPersonPrincipalName();
                Long timeMs = System.currentTimeMillis() - startTime;
                vuser.logPage(descrip, pageHtml.length(), timeMs.intValue());

                return;
            }

            if (isNewSession)
            {
                vuser.sessionStart();
            }
            String noHdr = request.getParameter("noHeader");    // want a full page of good stuff 
            boolean showHdr = (noHdr == null || noHdr.isEmpty()) && !embeded;
            if (showHdr)
            {
                PageTable hdrTbl = new PageTable();
                hdrTbl.setClassName("hdrTable");
                PageTableRow hdrRow = new PageTableRow();
                PageItemImage h1 = new PageItemImage(contextPath + "/LIGO_logo50a.png", "LigoDV-Web", "LigoDV-Web");

                h1.setDim(70, 50);
                PageTableColumn h1Col = new PageTableColumn(h1);
                h1Col.setId("hdrImg");
                hdrRow.add(h1Col);

                PageItemList l2 = new PageItemList();
                PageItemString h2 = new PageItemString(String.format("LigoDV-Web &mdash; <i>v%1$s</i><br>",
                                                                     version), false);
                h2.setClassName("title");
                l2.add(h2);

                String name = vuser.getCn();
                if (vuser.isAdmin())
                {
                    name += " (admin)";
                }
                PageItemString u = new PageItemString("Welcome " + name, false);
                l2.add(u);

                PageTableColumn h2Col = new PageTableColumn(l2);
                h2Col.setId("hdrTxt");
                hdrRow.add(h2Col);

                hdrTbl.addRow(hdrRow);
                vpage.add(hdrTbl);

                if (isPrototype && isNewSession && showHdr)
                {
                    PageItemList protoWarnMsg = new PageItemList();
                    protoWarnMsg.setId("warnDiv");
                    protoWarnMsg.addEvent("onclick", "jQuery('#warnDiv').hide();");

                    PageItemString warn = new PageItemString("WARNING");
                    warn.setId("warnTitle");
                    protoWarnMsg.add(warn);
                    protoWarnMsg.addLine("Click in the pannel to hide this warning message.<br>");

                    String warnMsg = "This LigoDV-Web server is the prototyping system used to test new and "
                                     + "experimental software.  You are more than welcome to use the site but be forewarned "
                                     + "that the level of bugs and other strange behavior is likely to be higher here "
                                     + "than on the production server.  Also this server is subject to unannounced "
                                     + "brief outages as we load the next version.  Here's a link to the production server ";
                    protoWarnMsg.add(warnMsg);
                    PageItemTextLink realServer = new PageItemTextLink("https://ldvw.ligo.caltech.edu/viewer", "ldvw.ligo.caltech.edu");
                    protoWarnMsg.add(realServer);

                    vpage.add(protoWarnMsg);
                }
            }
            LdvDispatcher dispatcher = new LdvDispatcher(request, response, db, vpage, vuser);
            dispatcher.setContextPath(contextPath);
            dispatcher.setServletPath(servletPath);
            
            helpUrl = helpUrl == null ? "" : helpUrl;
            dispatcher.setMainHelp(helpUrl);
            boolean sendHtml = dispatcher.dispatchCommand();

            if (sendHtml)
            {
                response.setContentType("text/html");
                if (out == null)
                {
                    out = response.getWriter();
                }
                PageItemList ftr = new PageItemList();

                // add database stats
                Float dbsec = db.getTotalMs() / 1000.f;
                ftr.add(String.format("%1$d db queries in %2$.2f sec.  ", db.getnQueries(), dbsec));

                // network data stats
                long ndsms = vuser.getTotalMs();
                Float ndssec = ndsms / 1000.f;
                long bytes = vuser.getBytesXfered();

                if (bytes > 0 && ndsms > 0)
                {
                    Float xferRate = bytes / ndssec;
                    ftr.add("NDS: bytes xfered ");
                    ftr.add(new PageItemHrLong(bytes, 3, "B"));
                    ftr.add(String.format(", %1$.2f sec ", ndssec));
                    ftr.add(new PageItemHrLong(xferRate.longValue(), 3, "Bytes/sec "));
                }

                Float plotsec = vuser.getPlotMs() / 1000.f;
                long nPlots = vuser.getNPlots();
                if (nPlots > 0)
                {
                    ftr.add(String.format("%1$d plots generated in %2$.2f sec.  ", nPlots, plotsec));
                }

                ftr.setClassName("footer");
                if (showHdr)
                {
                    vpage.addFoot(ftr);
                }
                String pageHtml = vpage.getHTML();
                out.print(pageHtml);
                out.flush();
                out.close();
                Long timeMs = System.currentTimeMillis() - startTime;
                vuser.logPage(vpage.getTitle(), pageHtml.length(), timeMs.intValue());
            }
        }

        catch (IOException | SQLException ex)
        {
            WebUtilException wex = new WebUtilException(ex);
            throw wex;
        }

        catch (WebUtilException ex)
        {
            throw ex;
        }

    }

    private void logError(WebUtilException wex)
    {
        Long timeMs = System.currentTimeMillis() - startTime;
        String ermsg = "Error on page " + vpage.getTitle() + ": " + wex.getLocalizedMessage();
        try
        {
            vuser.logPage(ermsg, 0, timeMs.intValue());
        }
        catch (SQLException ex)
        {
            // well we tried to log it.
        }
    }

    /**
     * Add necessary javascript files.
     */
    private void addJS()
    {
        if (!embeded)
        {
            String cntxt = contextPath + "/jquery";
            vpage.setJsRoot(cntxt);
            vpage.includeJS("jquery-1.9.1.min.js");
            vpage.includeJS("jquery-ui.min.js");
        }
    }

}
