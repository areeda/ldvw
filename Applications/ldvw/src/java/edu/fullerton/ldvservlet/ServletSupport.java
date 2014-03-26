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
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.UseLog;
import edu.fullerton.ldvtables.ViewUser;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    public ServletSupport()
    {
        startTime=System.currentTimeMillis();
    }


    public void init(HttpServletRequest request, ViewerConfig viewerConfig, boolean embeded) throws ServletException
    {
        this.session = request.getSession();
        helpUrl = viewerConfig.get("mainhelp");
        contextPath = request.getContextPath();
        
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
            long sessionAge = System.currentTimeMillis() - session.getLastAccessedTime();
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

    public boolean isIsPrototype()
    {
        return isPrototype;
    }

    public boolean isIsNewSession()
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
 
    
}
