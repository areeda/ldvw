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

import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemHeader;
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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * Manage permissions and sessions
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ServletSupport
{
    private HttpSession session;
    private String helpUrl;
    private Page vpage;
    private String contextPath;
    private Database db;
    private boolean isPrototype;
    private boolean isNewSession;
    private ViewUser vuser;
    private UseLog uLog;
    private ViewerConfig viewerConfig;
    private final long startTime;
    private String servletPath;
    private static final String maintFilename = "/usr/local/ldvw/maint.txt";


    public ServletSupport()
    {
        startTime=System.currentTimeMillis();
    }


    public void init(HttpServletRequest request, ViewerConfig viewerConfig, boolean embeded) throws ServletException
    {
        this.session = request.getSession();
        helpUrl = viewerConfig.get("mainhelp");
        contextPath = request.getContextPath();
        servletPath = contextPath + request.getServletPath();
        
        vpage = new Page();
        if (!embeded)
        {
            addJS_CSS();        // put some java script on the page
        }
        try
        {
            // use the configuration file to set options
            db = viewerConfig.getDb();
        }
        catch (ViewConfigException ex)
        {
            throw new ServletException("Configuration error. Can't connect to our database: "
                                       + ex.getLocalizedMessage());
        }

        String protoWarn = viewerConfig.get("protoWarn");
        isPrototype = protoWarn != null && protoWarn.equalsIgnoreCase("yes");
        isNewSession = session.isNew();
        if (! isNewSession)
        {
            long sessionAge = System.currentTimeMillis() - session.getCreationTime();
            isNewSession = sessionAge > 30 * 60 * 1000;
        }
        vuser = new ViewUser(request, db);
        try
        {
            uLog = new UseLog(db);
        }
        catch (SQLException ex)
        {
            String ermsg = "Can't initialize our User Log tables: " + ex.getClass().getSimpleName()
                           + " - " + ex.getLocalizedMessage();
            throw new ServletException(ermsg);
        }
        vuser.setUlog(uLog);
        if (isNewSession)
        {
            try
            {
                vuser.sessionStart();
            }
            catch (SQLException | LdvTableException ex)
            {
                String ermsg = "Unable to start an ldvw session: " + ex.getClass().getSimpleName()
                               + ex.getLocalizedMessage();
                throw new ServletException(ermsg);
            }
        }
        session.setAttribute("isValidUser", vuser.isValid());
        session.setAttribute("isAdmin", vuser.isAdmin());
        session.setAttribute("commonName", vuser.getCn());

    }
    /**
     * Read our config file, check database connection and that all necessary tables exist.
     * 
     * @param tableNames
     * @throws ServletException 
     */
    
    public void checkDb(String[] tableNames) throws ServletException
    {
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
        for (String tableName : tableNames)
        {
            try
            {
                Class<?> clazz = Class.forName(tableName);
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
                String ermsg = "Error verifying necessary tables exist:" + ex.getClass().getSimpleName() +
                               " " + ex.getLocalizedMessage();
                throw new ServletException(ermsg);
            }
        }
        mydb.close();

    }
    public void showPage(HttpServletResponse response) throws ServletException
    {
        try
        {
            PrintWriter out;
            out = response.getWriter();
            
            response.setContentType("text/html");
            String pageHtml = vpage.getHTML();
            out.print(pageHtml);
            out.flush();
            String descrip = vpage.getTitle() + vuser.getEduPersonPrincipalName();
            Long timeMs = System.currentTimeMillis() - startTime;
            vuser.logPage(descrip, pageHtml.length(), timeMs.intValue());
        }
        catch (IOException | WebUtilException | SQLException ex)
        {
            String ermsg = "Error sending page to client: " + ex.getClass() + " " 
                           + ex.getLocalizedMessage();
            throw new ServletException(ermsg);
        }
    }
    //================================ private methods==================
    /**
     * Add necessary javascript and css files for a normal page.
     */
    private void addJS_CSS()
    {
        String cntxt = contextPath + "/jquery";
        vpage.setJsRoot(cntxt);
        vpage.includeJS("jquery-1.9.1.min.js");
        vpage.includeJS("jquery-ui.min.js");
        
        // @todo use base style as a user selectable theme
        vpage.setCssRoot(contextPath);
        String baseCss = "defaultStyleR08.css";
        vpage.setLastCSS(baseCss);

        String jqueryCss = "jquery/jquery-ui_1.css";
        vpage.includeCSS(jqueryCss);
    }
    public static boolean inMaintMode()
    {
        File maint = new File(maintFilename);
        return maint.canRead();
    }

    public static String getMaintFilename()
    {
        return maintFilename;
    }
    
    public static String getMaintMsg()
    {
        String ret = "";
        File maint = new File(maintFilename);
        if (maint.canRead())
        {
            try
            {
                ret = new String(Files.readAllBytes(Paths.get(maintFilename)));
            }
            catch (IOException ex)
            {
                ret = "Error reading the message.";
            }
        }
        return ret;
    }
    
    public static void setMaintMsg(String msg) throws WebUtilException
    {
        clearMaintMsg();
        try (PrintWriter out = new PrintWriter(maintFilename))
        {
            out.println(msg);
        }
        catch (FileNotFoundException ex)
        {
            throw new WebUtilException("Setting maintenance mode message", ex);
        }
    }
    
    public static void clearMaintMsg()
    {
        File maint = new File(maintFilename);
        if (maint.exists())
        {
            maint.delete();
        }        
    }
    
    public void addStandardHeader(String version) throws WebUtilException
    {
        PageTable hdrTbl = new PageTable();

        hdrTbl.setClassName("hdrTable");
        String maintMsg = getMaintMsg();

        if(inMaintMode())
        {
            vpage.setTitle("Maintenance Mode");
            vpage.add(new PageItemHeader("Maintenance Mode", 2));
            vpage.add(new PageItemHeader(maintMsg, 3,false));
        }

        
        PageTableRow hdrRow = new PageTableRow();
        PageItemImage h1 = new PageItemImage(contextPath + "/LIGO_logo50a.png", "LigoDV-Web", "LigoDV-Web");

        h1.setDim(70, 50);
        PageTableColumn h1Col = new PageTableColumn(h1);
        h1Col.setId("hdrImg");
        hdrRow.add(h1Col);

        PageItemList l2 = new PageItemList();
        String progId;
        if (version.isEmpty())
        {
            progId = "LigoDV-Web <br>";
        }
        else
        {
            progId = String.format("LigoDV-Web &mdash; <i>v%1$s</i><br>",version);
        }
        PageItemString h2 = new PageItemString(progId, false);
        h2.setClassName("title");
        l2.add(h2);

        String name = vuser.getCn();
        if (vuser.isAdmin())
        {
            name += " (admin)";
        }
        else if (vuser.isTester())
        {
            name += " (Experimental commands enabled)";
        }
        PageItemString u = new PageItemString("Welcome " + name, false);
        l2.add(u);

        PageTableColumn h2Col = new PageTableColumn(l2);
        h2Col.setId("hdrTxt");
        hdrRow.add(h2Col);

        hdrTbl.addRow(hdrRow);
        vpage.add(hdrTbl);

        if (isPrototype && isNewSession)
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
            PageItemTextLink realServer = new PageItemTextLink("https://ldvw.ligo.caltech.edu/ldvw/view", "ldvw.ligo.caltech.edu");
            protoWarnMsg.add(realServer);

            vpage.add(protoWarnMsg);
        }

    }
    /**
     * Add a table of links to our main pages.
     *
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public void addNavBar() throws WebUtilException
    {
        String mainHelpUrl = getHelpUrl();
        NavBar navBar = new NavBar();
        navBar.setLink("Help", mainHelpUrl);
        
        PageItem nav = navBar.getNavBar(contextPath, vuser.isTester(), vuser.isAdmin());

        vpage.add(nav);
        vpage.addBlankLines(2);
    }

    //=============================GETTERS and SETTERS================
    public String getHelpUrl()
    {
        return helpUrl;
    }

    public ViewerConfig getViewerConfig()
    {
        return viewerConfig;
    }

    
    public HttpSession getSession()
    {
        return session;
    }

    public Page getVpage()
    {
        return vpage;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public Database getDb()
    {
        return db;
    }

    public boolean getIsPrototype()
    {
        return isPrototype;
    }

    public boolean getIsNewSession()
    {
        return isNewSession;
    }

    public ViewUser getVuser()
    {
        return vuser;
    }

    public UseLog getuLog()
    {
        return uLog;
    }

    public String getServletPath()
    {
        return servletPath;
    }

    public long getStartTime()
    {
        return startTime;
    }

    
    void close()
    {
        if (db!= null)
        {
            db.close();
        }
        db = null;
    }
 
    
}
