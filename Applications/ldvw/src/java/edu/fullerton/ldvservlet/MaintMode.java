/*
 * Copyright (C) 2016 Joseph Areeda <joseph.areeda@ligo.org>
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
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormButton;
import edu.fullerton.jspWebUtils.PageFormText;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import static edu.fullerton.ldvservlet.ServletSupport.clearMaintMsg;
import java.io.IOException;
import java.util.Map;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import viewerconfig.ViewerConfig;

/**
 * A servlet to control entering and exiting maintenance mode, with an 
 * explanatory message.
 * @author Joseph Areeda <joseph.areeda@ligo.org>
 */
public class MaintMode extends HttpServlet
{
    private ViewerConfig viewerConfig;
    private long loadTime;
    private String contextPath;
    private Page vpage;
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
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {

        try
        {
            response.setContentType("text/html;charset=UTF-8");

            ServletSupport servletSupport;

            servletSupport = new ServletSupport();
            servletSupport.init(request, viewerConfig, false);

            vpage = servletSupport.getVpage();
            vpage.includeJS("showByClass.js");
            vpage.setTitle("Manage maintenance mode");
            contextPath = request.getContextPath();

            Map<String, String[]> parameterMap = request.getParameterMap();
            String act = "new";
            if (parameterMap.containsKey("act"))
            {
                act = parameterMap.get(act)[0];
            }
            else if (parameterMap.containsKey("Maint Mode"))
            {
                act = "maint";
            }
            else if (parameterMap.containsKey("Normal"))
            {
                act = "norm";
            }
            else  if (parameterMap.containsKey("Cancel"))
            {
                act = "cancel";
            }
            if (act.contentEquals("cancel"))
            {
                RequestDispatcher dispatcher = request.getRequestDispatcher("view");
                dispatcher.forward(request, response);
            }
            else
            {
                
                switch (act)
                {
                    case "norm":
                        ServletSupport.clearMaintMsg();
                        break;
                        
                    case "maint":
                        String mmesg = parameterMap.get("mesg")[0];
                        ServletSupport.setMaintMsg(mmesg);
                        break;
                }
                servletSupport.addStandardHeader("");
                servletSupport.addNavBar();

                
                PageForm form = new PageForm();
                form.setAction(servletSupport.getServletPath());
                form.setMethod("GET");
                form.setNoSubmit(true);
                form.add(getCurSetup());
                PageFormButton cancel = new PageFormButton("Cancel", "Cancel", "Cancel");
                PageFormButton mmod = new PageFormButton("Maint Mode", "Maint Mode", "Maint Mode");
                PageFormButton norm = new PageFormButton("Normal", "Normal", "Normal");
                PageTable btns = new PageTable();
                btns.setClassName("noborder");
                PageTableRow btrw = new PageTableRow();
                btrw.add(cancel);
                btrw.add(mmod);
                btrw.add(norm);
                btrw.setClassAll("noborder");
                btns.addRow(btrw);

                form.add(btns);
                vpage.add(form);

                servletSupport.showPage(response);
            }
        }
        catch (WebUtilException ex)
        {
            String ermsg = String.format("Error displaying source data %1$s - %2$s",
                    ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new ServletException(ermsg);
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

    private PageItem getCurSetup() throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        String state = ServletSupport.inMaintMode() ? "Maintence mode" : "Normal mode";
        PageItemHeader cur = new PageItemHeader("Currently in " + state, 3);
        ret.add(cur);
        
        String msg = ServletSupport.getMaintMsg();
        PageFormText msgArea = new PageFormText("mesg", msg, 40);
        msgArea.setnLines(6);
        msgArea.setUseEditor(true);
        if (!msg.isEmpty())
        {
            String editorContent = msg.replaceAll("'", "&#39;").replaceAll("\\n", "");
            editorContent = editorContent.replaceAll("\\r", "");
            String script = String.format("tinyMCE.get('mesg').setContent('%1$s');", editorContent);
            vpage.addLoadJS(script);
        }

        ret.add(msgArea);
        
        return ret;
    }

}
