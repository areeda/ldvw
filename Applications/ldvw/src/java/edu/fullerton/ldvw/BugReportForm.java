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
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormButton;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageFormText;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ViewUser;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;

/**
 * Provide and easy on line way to submit a ticket.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class BugReportForm 
{
    private final Database db;
    private final Page vpage;
    private final ViewUser vuser;
    private final HttpServletRequest request;
    private String submitAct;
    private String email;
    private String rptType;
    private String rptInfo;
    private PageForm frm;
    private String agent;
    private String name;
    private String remoteIP;
    private Properties fMailServerConfig;
    private String subj;
    private final String[] recipients = { "joe@areeda.com", "josmith@exchange.fullerton.edu" };
    private String servletPath;

    public BugReportForm(Database db, Page vpage, ViewUser vuser, HttpServletRequest request) 
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
        this.request = request;
        agent = request.getHeader("User-Agent");
    }

    public void setServletPath(String servletPath)
    {
        this.servletPath = servletPath;
    }

    /**
     * Present a form for them to fill out
     */
    void addForm() throws WebUtilException
    {
        vpage.add("The LigoDV-web development team welcomes comments, suggestions, "
                + "and bug reports to help improve the product.");
        vpage.addBlankLines(2);
        
        frm = new PageForm();
        
        String frmName = "contactForm";
        frm.setName(frmName);
        frm.setId(frmName);
        frm.setMethod("get");
        frm.setAction(servletPath);
        frm.addHidden("act", "contactSubmit");
        frm.setNoSubmit(true);
        
        PageTable contactData = new PageTable();
        addNextRow(contactData,"Name:",vuser.getCn());
        String mail = vuser.getMail();
        if (mail != null && !mail.isEmpty())
        {
            addNextRow(contactData,"Email:",mail);
        }
        else
        {
            addNextRow(contactData,"Email:", new PageFormText("email", ""));
        }
        addNextRow(contactData,"Agent:",agent);

        String[] rptTypelst = {"Bug report", "Help request", "Enhancement request", "Comment"};
        addNextRow(contactData, "Report type:", new PageFormSelect("rptType", rptTypelst));
        addNextRow(contactData, "Subject", new PageFormText("subj", "", 48));
        PageFormText info = new PageFormText("rptInfo", "", 64);
        info.setnLines(10);
        addNextRow(contactData, "Additional info:", info);
        
        PageFormButton submitBtn = new PageFormButton("submitAct", "Submit", "sendRpt");
        PageFormButton cancelBtn = new PageFormButton("submitAct", "Cancel", "cancelRpt");
        PageTableRow row = new PageTableRow();
        row.add(submitBtn);
        row.add(cancelBtn);
        row.setClassAll("noborder");
        contactData.addRow(row);
        frm.add(contactData);
        vpage.add(frm);
    }
    
    private void addNextRow(PageTable contactData, String label, String fixedVal) throws WebUtilException
    {
        String lbl = label.toLowerCase();
        if (lbl.endsWith(":"))
        {
            lbl = lbl.substring(0, lbl.length()-1);
        }
        if (frm != null)
        {
            frm.addHidden(lbl, fixedVal);
        }
        PageItemString val = new PageItemString(fixedVal);
        addNextRow(contactData,label,val);
        
    }

    private void addNextRow(PageTable contactData, String label, PageItem value) throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        PageItemString lbl = new PageItemString(label);
        lbl.setAlign(PageItem.Alignment.RIGHT);
        row.add(lbl);
        
        row.add(value);

        row.setClassAll("noborder");
        contactData.addRow(row);
    }

    /**
     * If they confirmed send the bug report to the ticket system and log it here
     */
    void sendRpt() throws WebUtilException
    {
        processParameters();
        if (submitAct.equalsIgnoreCase("sendRpt"))
        {
            if (email.isEmpty())
            {
                vpage.add("We don't have an email to respond to.");
                vpage.addBlankLines(1);
            }
            if (rptInfo.isEmpty())
            {
                vpage.add("You didn't seem to enter anything in additional info, so we don't know what the problem is.");
                vpage.addBlankLines(1);
            }
            vpage.add("This is what we sent to the development team:");
            vpage.addBlankLines(2);
            
            PageTable report = new PageTable();
            addNextRow(report, "Name:", name);
            addNextRow(report, "Email:",email);
            addNextRow(report, "Remote address:", remoteIP);
            addNextRow(report, "Agent:", agent);
            addNextRow(report, "Report Type:", rptType);
            PageItemString htmlTxt = new PageItemString(rptInfo.replaceAll("\n", "<br>\n"), false);
            addNextRow(report, "Additional Info:", htmlTxt);
            vpage.add(report);
            vpage.addBlankLines(2);
            vpage.add("Thank you for helping us to make this a better service for LIGO!");
            
            String subject = rptType + ": " + subj;
            sendEmail(email, subject, report.getHtml());
        }
        else
        {
            vpage.add("Report canceld nothing was sent.  Choose from navigation bar, please.");
        }
    }

    private void processParameters()
    {
        submitAct = getParam("submitAct");
        remoteIP = request.getRemoteAddr();
        email = getParam("email");
        rptType = getParam("rptType");
        rptInfo = getParam("rptInfo");
        agent = getParam("agent");
        name = getParam("name");
        subj = getParam("subj");
    }

    private String getParam(String param)
    {
        String it = request.getParameter(param);
        it = it == null ? "" : it;
        return it;
    }
    private void sendEmail(
            String aFromEmailAddr,String aSubject, String aBody) throws WebUtilException
    {
        //Here, no Authenticator argument is used (it is null).
        //Authenticators are used to prompt the user for user
        //name and password.
        getProperties();
        
        
        Session session = Session.getDefaultInstance(fMailServerConfig, null);
        MimeMessage message = new MimeMessage(session);
        try
        {
            //the "from" address may be set in code, or set in the
            //config file under "mail.from" ; here, the latter style is used
            message.setFrom( new InternetAddress("root@ldvw.ligo.caltech.edu") );
            for (String toAddr : recipients)
            {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toAddr));
            }
            message.setSubject(aSubject);
            message.setContent(aBody, "text/html; charset=utf-8");
            Transport.send(message);
        }
        catch (MessagingException ex)
        {
            throw new WebUtilException("Cannot send email. " + ex);
        }
    }

    private void getProperties()
    {
        fMailServerConfig = new Properties();
        fMailServerConfig.setProperty("mail.host", "localhost");
        fMailServerConfig.setProperty("mail.from", "ldvw");
        
    }
}
