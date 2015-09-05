/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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

import edu.fullerton.ldvjutils.TimeAndDate;
import java.io.IOException;
import java.io.PrintWriter;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.script.ScriptEngine;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Nagios extends HttpServlet
{
    private String eduPersonPrincipalName;
    private String cn;
    private String mail;
    private String mailFwdAddr;
    private String givenName;
    private String isMemberOf;
    private String remoteIP;
    private String userAgent;

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
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Pragma", "no-cache");
        
        try (PrintWriter out = response.getWriter())
        {
            try
            {
                getRequestInfo(request);
                JsonObjectBuilder builder = Json.createObjectBuilder();
                builder.add("nagios_shib_scraper_ver", "0.1");
                long now = TimeAndDate.nowAsGPS();
                builder.add("created_gps", now);
                builder.add("author", (Json.createObjectBuilder()
                        .add("name", "Joseph Areeda")
                        .add("email", "joseph.areeda@ligo.org")
                        .add("created_by", "https://ldvw.ligo.caltech.edu/ldvw/Nagios")
                        ));
                JsonArrayBuilder status = Json.createArrayBuilder();

                if (isValid())
                {
                    status.add(Json.createObjectBuilder()
                            .add("num_status", 0)
                            .add("txt_status", "OK, Apache, tomcat, shibboleth tested OK")
                            .add("start_sec", 0)
                    );
                }
                else
                {
                    status.add(Json.createObjectBuilder()
                            .add("num_status", 1)
                            .add("txt_status", "Warning, Apache, tomcat, OK, but shibboleth"
                                    + " did not return valid group")
                            .add("start_sec", 0)
                    );
                }
                
                // add something incase it's cached for too long
                
                status.add(Json.createObjectBuilder()
                        .add("num_status", 3)
                        .add("txt_status", "UNKNOWN, cached result is older than 10 minutes.")
                        .add("start_sec", 600)
                );
                
                builder.add("status_intervals", status);
                JsonObject result = builder.build();
                JsonWriter jwriter = Json.createWriter(out);
                jwriter.writeObject(result);
                out.flush();
            }
            catch(Exception ex)
            {
                out.printf("Exception: %1$s: %2$s%m", ex.getClass().getSimpleName(), 
                                                      ex.getMessage());
            }
        }
    }
    private void getRequestInfo(HttpServletRequest request)
    {
        eduPersonPrincipalName = (String) request.getAttribute("eduPersonPrincipalName");
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = request.getHeader("eduPersonPrincipalName");
        }
        if (eduPersonPrincipalName == null || eduPersonPrincipalName.isEmpty())
        {
            eduPersonPrincipalName = "local.admin";
        }
        cn = (String) request.getAttribute("cn");
        if (cn == null || cn.isEmpty())
        {
            cn = request.getHeader("cn");
        }
        if (cn == null || cn.isEmpty())
        {
            cn = "Local Admin";
        }
        mail = (String) request.getAttribute("mail");
        if (mail == null || mail.isEmpty())
        {
            mail = request.getHeader("mail");
        }
        mailFwdAddr = (String) request.getAttribute("mailForwardingAddress");
        if (mailFwdAddr == null || mailFwdAddr.isEmpty())
        {
            mailFwdAddr = request.getHeader("mailForwardingAddress");
        }
        givenName = (String) request.getAttribute("givenName");
        if (givenName == null || givenName.isEmpty())
        {
            givenName = request.getHeader("AJP_givenName");
        }
        isMemberOf = (String) request.getAttribute("isMemberOf");
        if (isMemberOf == null || isMemberOf.isEmpty())
        {
            isMemberOf = request.getHeader("AJP_isMemberOf");
        }
        remoteIP = request.getRemoteAddr();
        userAgent = request.getHeader("user-agent");
    }
    /**
     * Tests the lowest privilege level. If they aren't "valid" we won't do anything
     *
     * @return true if they meet these requirements
     *
     */
    public boolean isValid()
    {
        boolean ret = isLocalHost();
        if (isMemberOf != null)
        {
            ret |= isMemberOf.contains("LSCVirgoLIGOGroupMembers");
            ret |= isMemberOf.contains("ldvwAdmin");
            ret |= isMemberOf.contains("ldvwRobots");
        }
        return ret;
    }

    public boolean isLocalHost()
    {
        boolean ret = remoteIP.equalsIgnoreCase("127.0.0.1");       // to allow testing
        ret |= remoteIP.equalsIgnoreCase("0:0:0:0:0:0:0:1%0");
        ret |= remoteIP.equalsIgnoreCase("0:0:0:0:0:0:0:1");
        return ret;
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
