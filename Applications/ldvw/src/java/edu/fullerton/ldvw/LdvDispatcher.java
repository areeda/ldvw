/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvplugin.DBManager;
import edu.fullerton.ldvplugin.HelpManager;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.GUISupport;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ndsmonitor.NdsMonitor;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author Joseph Areeda<joseph.areeda@ligo.org>
 */
public class LdvDispatcher extends GUISupport
{
    private final long startTime;
    private String mainHelpUrl;
    
    private HelpManager helpManager;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public LdvDispatcher(HttpServletRequest request, HttpServletResponse response, Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
        startTime = System.currentTimeMillis();
        setParamMap(request.getParameterMap());
        this.request = request;
        this.response = response;
    }

    /**
     * Calls the appropriate module to handle the action requested by the user.
     * Note: most actions build the output in the vpage variable passed to pretty much everybody 
     *       However some module send data directly to the browser such as data or image downloads.
     *       That's what the return value specifies
     * @return true if our caller is responsible for sending html to the client.  False if we already did it
     * @throws WebUtilException we repackage any exceptions for consistent error handling.
     * @throws java.sql.SQLException
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public boolean dispatchCommand() throws WebUtilException, SQLException, LdvTableException
    {
        boolean ret = true;
        
        helpManager = new HelpManager(db, vpage, vuser);
        helpManager.setContextPath(contextPath);
        helpManager.setServletPath(servletPath);
        helpManager.setParamMap(paramMap);
        
        addNavBar();
        
        if (request.getMethod().equalsIgnoreCase("post") &&  ServletFileUpload.isMultipartContent(request))
        {
            
        }
        else
        {
            String act;
            act = request.getParameter("act");
            act = act == null ? "main" : act;
            act = act.toLowerCase();
            switch (act)
            {
                case "basechan":
                    baseChan();
                    break;
                case "chanlist":
                    chanList();
                    break;
                case "channelstats":
                    channelStats();
                    break;
                case "contactus":
                    contactForm();
                    break;
                case "contactsubmit":
                    sendRpt();
                    break;
                case "dbstats": 
                    if (vuser.isAdmin())
                    {
                        dbStats();
                    }
                    break;
                case "defineplugin":
                    definePlugin();
                    break;
                case "doplot":
                    ret = doPlot();
                    break;
                case "edithelp":
                    if ( vuser.isAdmin())
                    {
                        ret = editHelp();
                    }       
                    break;
                case "getimg":
                    ret = getImg();
                    break;
                case "getjson":
                    ret = getJson();
                    break;
                case "imagehistory":
                    imageHistory();
                    break;
                case "main":
                    startPage();
                    break;
                case "ndshistory":
                    ndsHistory();
                    break;
                case "ndsstatus":
                    ndsStatus();
                    break;
                case "procsrvfrm":
                    if (vuser.isAdmin())
                    {
                        serverManager();
                    }
                    break;
                case "referenceplot":
                    referencePlot();
                    break;
                case "savehelptext":
                    saveHelpText();
                    break;
                case "servermanager":
                    if (vuser.isAdmin())
                    {
                        serverManager();
                    }
                    break;
                case "specialplot":
                    vpage.setTitle("Special Plots");
                    PluginManager pmanage = new PluginManager( db, vpage, vuser, paramMap);
                    pmanage.setContextPath(contextPath);
                    pmanage.setServletPath(servletPath);
                    ret = pmanage.specialPlot();
                    break;
                case "stats":
                    if (vuser.isAdmin())
                    {
                        userStats();
                    }
                    break;
                case "upload":
                    uploadFiles();
                    break;
                default:
                    vpage.addLine(String.format("Unknow action requested: [%1$s]", act));
                    break;  // just in case somebody adds something after this
            }
        }
        return ret;     // NB: ret is a boolean that says content is in vpage to be sent
        // otherwise we already sent a different mime type, like an image or pdf
    }

    /**
     * Add a table of links to our main pages.
     *
     * @param imgCnt total number of records in selection
     */
    private void addNavBar() throws WebUtilException
    {
        // Top row commands 2 element rows "Text", "get parameters" or "http*://"
        String[][] commands =
        {
            { "Home",           "main"         },
            { "Saved Plots",    "ImageHistory&amp;size=med" },
            { "Chan Stats",     "ChannelStats" },
            { "NDS Status",     "ndsStatus"    },
            { "Upload",         "upload"       },
            { "Help",           mainHelpUrl    },
            { "Contact Us",     "contactUs"    }
        };
        // Commands only available to admin group
        String[][] adminCommands =
        {
            { "User Stats",     "Stats"         },
            { "Edit Help",      "EditHelp"      },
            { "DB stats",       "dbstats"       },
            { "New Chan sel",   "baseChan"      },
            { "Servers",        "serverManager" }
        };

        PageItem navBar;

        PageItemBSNavBar bsnav = new PageItemBSNavBar();
        String baseUrl = servletPath + "?act=";
        String cmdUrl;

        for (String[] command : commands)
        {
            if (!command[0].isEmpty() && !command[1].isEmpty())
            {
                if (command[1].matches("^http.?://.*"))
                {
                    bsnav.addLink(command[1], command[0], "_blank");
                }
                else
                {
                    cmdUrl = baseUrl + command[1];
                    bsnav.addLink(cmdUrl, command[0]);
                }
            }
        }

        if (vuser.isAdmin())
        {
            bsnav.createNewSubmenu("Admin");

            for (String[] command : adminCommands)
            {
                cmdUrl = baseUrl + command[1];
                bsnav.addSubmenuLink(cmdUrl, command[0]);
            }
            bsnav.addCurSubmenu();
        }

        navBar = bsnav;

        if (paramMap.get("embed") == null)
        {
            vpage.add(navBar);
            vpage.addBlankLines(2);
        }
    }

    /**
     * An external request for a plot.  
     * Depending on how much information they provided, we will go to different places:
     * Nothing: main page
     * Channel name: if not unique select channels, else specify times
     * Add server: better chance of being unique
     * time, type of plot and unique channel display plot.
     */
    private void referencePlot() throws WebUtilException
    {
        try
        {
            String chanName = request.getParameter("channelName");
            String server = request.getParameter("server");
            String plot = request.getParameter("plot");
            String start = request.getParameter("start");
            String duration = request.getParameter("duration");
            if (server == null)
            {
                server = "";
            }

            vpage.setTitle("Reference Plot");

            if (chanName == null || chanName.isEmpty())
            {   // nothing specified means go to main page
                vpage.setTitle("Ligodv-web");
                
                PageItem hlpBtn = helpManager.getHelpButton("chanNameFilter");
                ChannelSelector cs = new ChannelSelector(request, response, db, vpage, vuser);
                cs.setServletPath(servletPath);
                PageForm chanSel = cs.addChanSelector(true, vuser.isAdmin(),hlpBtn);
                vpage.add(chanSel);
                vpage.add("Start by narrowing down the list of channels."); 
            }
            else
            {
                ChannelSelector clf = new ChannelSelector(request, response, db, vpage, vuser);
                clf.setContextPath(contextPath);
                clf.setServletPath(servletPath);
                int nmatch = clf.getMatchCount("referenceplot");
                if (nmatch == 0)
                {
                    vpage.add("No channel matches specifier: " + chanName);
                    startPage();
                }
                else 
                {
                    ChannelTable ct = new ChannelTable(db);
                    TreeSet<ChanInfo> sel = ct.getAsSet(server, chanName, "raw", 1);
                    HashSet<Integer> selint = new HashSet<>();
                    for (ChanInfo ci : sel)
                    {
                        Integer i = ci.getId();
                        selint.add(i);
                    }
                    if (nmatch > 1)
                    {   // non-unique channel specified go to channel selection
                        vpage.setTitle("liogdv-web - select channels");
                        PageItem hlpBtn = helpManager.getHelpButton("chanNameFilter");
                        ChannelSelector cs = new ChannelSelector(request, response, db, vpage, vuser);
                        cs.setServletPath(servletPath);
                        PageForm chanSel = cs.addChanSelector(false, vuser.isAdmin(),hlpBtn);
                        vpage.add(chanSel);
                        vpage.addBlankLines(2);
                        clf.doFormFilter("referencePlot");
                    }
                    else
                    {
                        // one and only one match let's see what else they specified
                        
                        vpage.setTitle("ligodv-web - specify times");
                        TimeAndPlotSelector tps = new TimeAndPlotSelector(db, vpage, vuser);
                        tps.setContextPath(contextPath);
                        tps.setParamMap(paramMap);
                        tps.addSelections(selint);
                        tps.showForm("selchan");
                    }
                }
            }
        }
        catch (SQLException | LdvTableException | WebUtilException ex)
        {
            throw new WebUtilException("Reference plot", ex);
        }
    }
    /**
     * Channels have been selected individually, next step is time and plots
     * @throws WebUtilException 
     */
    private void specifyTimesAndPlots(String selType) throws WebUtilException
    {
        try
        {
            vpage.setTitle("LigoDV-web - Specify times");
            TimeAndPlotSelector tps = new TimeAndPlotSelector(db, vpage, vuser);
            tps.setParamMap(paramMap);
            tps.setContextPath(contextPath);
            tps.setServletPath(servletPath);
            tps.showForm(selType);
        }
        catch (SQLException | WebUtilException | LdvTableException ex)
        {
            throw new WebUtilException("Specify times", ex);
        }
    }

    private void definePlugin() throws WebUtilException
    {
        vpage.setTitle("Plugin Setup");
        PluginDescriptor pd = new PluginDescriptor(request, db, vpage, vuser);
        pd.makeNew();
    }

    /**
     * Process the plot request, generating as many images as necessary
     * @return true if output is to be sent by our caller, false if we already sent another mime type
     * @throws WebUtilException 
     */
    private boolean doPlot() throws WebUtilException
    {
        boolean ret = true;
        String isDownload = request.getParameter("download");
        
        if (request.getParameter("selMore") != null)
        {
            ChannelSelector clf = new ChannelSelector(request, response, db, vpage, vuser);
            clf.setContextPath(contextPath);
            clf.setServletPath(servletPath);
            clf.selectChannels("selMore", false);
        }
        else
        {
            if (isDownload==null)
            {
                vpage.setTitle("Ligodv-web results");
            }
            else
            {
                vpage.setTitle("Data download");
            } 
            try
            {
                PluginManager pmanage = new PluginManager(db, vpage, vuser, paramMap);
                pmanage.setContextPath(contextPath);
                pmanage.setServletPath(servletPath);
                pmanage.setResponse(response);
                ret = pmanage.doPlots();
            }
            catch(SQLException | WebUtilException ex)
            {
                String ermsg = "Calling PluginManager to create plots or send other mime: " + ex.getClass().getSimpleName()
                        + " - " + ex.getLocalizedMessage();
                throw new WebUtilException(ermsg);
            }
        }
        return ret;
    }

    private boolean getImg() throws WebUtilException
    {
        boolean ret = true;
        try
        {
            String imgIdStr = request.getParameter("imgId");
            String widthStr = request.getParameter("width");
            int width=0;
            if (widthStr!=null && !widthStr.isEmpty())
            {
                if (widthStr.matches("^\\d+$"))
                {
                    width = Integer.parseInt(widthStr);
                }
            }

            if (imgIdStr != null && !imgIdStr.isEmpty())
            {
                if (imgIdStr.toLowerCase().matches("^\\d+$"))
                {
                    int imgId = Integer.parseInt(imgIdStr);
                    ImageTable itbl = new ImageTable(db);
                    itbl.sendImg(response, imgId,width);
                    ret = false;
                    Long timeMs = System.currentTimeMillis() - startTime;
                    vuser.logPage(String.format("Sent image #%1$d", imgId), itbl.getLastImageSize(), timeMs.intValue());
                }
                else
                {
                    vpage.add("getImg has an invalid id.");
                }
            }
            else
            {
                vpage.add("getImg has no image id.");
            }
        }
        catch (NumberFormatException | SQLException | IOException ex)
        {
            throw new WebUtilException("Get saved image", ex);
        }
        return ret;
    }

    private void imageHistory() throws WebUtilException
    {
        try
        {
            vpage.setTitle("Image History");
            ImageHistory ih = new ImageHistory(request, response, db, vpage, vuser);
            ih.setContextPath(contextPath);
            ih.setServletPath(servletPath);
            ih.show();
        }
        catch (SQLException | WebUtilException | LdvTableException ex)
        {
            throw new WebUtilException("Display image history", ex);
        }
    }

    /**
     * Home page for the website
     * 
     * @throws WebUtilException probably a programming bug defining html
     */
    private void startPage() throws WebUtilException, SQLException, LdvTableException
    {
        vpage.setTitle("Ligodv-web");

        
        PageItemString genPlotIntro = new PageItemString(
                "<b><u>General plots:</u></b><br/><br/>"
                + "Start by narrowing down the list of channels.  "
                + "All fields are optional but they speed the search and reduce the number of matches.  "
                + "Then hit then \"Retrieve Channel List\" button.<br/>"
                ,false);
        vpage.add(genPlotIntro);
        PageItem hlpBtn = helpManager.getHelpButton("chanNameFilter");
        
        vpage.addBlankLines(2);
        ChannelSelector cs = new ChannelSelector(request, response, db, vpage, vuser);
        cs.setServletPath(servletPath);
        PageForm chanSel = cs.addChanSelector(true, vuser.isAdmin(),hlpBtn);
        vpage.add(chanSel);
        vpage.addBlankLines(1);
        
        vpage.addHorizontalRule();
        
        PluginManager pmanage = new PluginManager( db, vpage, vuser, paramMap);
        pmanage.setContextPath(contextPath);
        pmanage.setServletPath(servletPath);
        pmanage.specialPlotSelector(vpage);
    }

    private void userStats() throws WebUtilException
    {
        vpage.setTitle("Stats");
        SiteStats ss = new SiteStats(request, db);
        String imgHistBaseUrl=request.getContextPath() + "?act=ImageHistory&amp;size=med&amp;usrSel=";
        vpage.add(ss.getStats( imgHistBaseUrl ));
    }

    private void channelStats() throws WebUtilException
    {
        try
        {
            vpage.setTitle("LigoDv-web - Channel Table Stats");
            SiteStats ss = new SiteStats(request, db);
            vpage.add(ss.getChanStats());
        }
        catch (WebUtilException | SQLException ex)
        {
            throw new WebUtilException("Display stats page", ex);
        }

    }

    private void uploadFiles() throws WebUtilException
    {
        String submitAct = request.getParameter("submitAct");
        if(submitAct == null || ! submitAct.equalsIgnoreCase("doupload"))
        {
            vpage.setTitle("LigoDV-web - Upload Image(s)");
            ImageUploader iu = new ImageUploader(db, vpage, vuser);
            iu.setServletPath(servletPath);
            iu.setContextPath(contextPath);
            iu.addForm();

        }
        else
        {
            
            vpage.setTitle("Image Upload Error");
            vpage.add("Old style uri used for upload images.");
        }
    }

    /**
     * Summarize the nds monitor stats
     * @throws WebUtilException error formatting results
     * @throws SQLException database problems
     */
    private void ndsStatus() throws WebUtilException, SQLException
    {
        vpage.setTitle("LigoDV-web - NDS Status");
        NdsMonitor nm = new NdsMonitor(db);
        nm.timeCounts();
        String sTitle = String.format("Status of NDS servers on %1$s UTC:",TimeAndDate.nowAsUtcString(0));
        PageItemString secTitle = new PageItemString(sTitle);
        secTitle.setId("secTitle");
        vpage.add(secTitle);
        vpage.addBlankLines(2);
        String baseUrl = "/viewer/?act=ndsHistory";
        vpage.add(nm.getStatsAsTable(baseUrl));
        vpage.addBlankLines(2);
        vpage.add(nm.getSummary());
    }
    
    private void ndsHistory() throws LdvTableException, WebUtilException
    {
        String site = request.getParameter("site");
        if (site == null || site.isEmpty())
        {
            vpage.setTitle("LigoDV-web - NDS Stats request error");
            vpage.add("Invalid request to ndsHistory");
        }
        else
        {
            vpage.setTitle("LigoDV-web - NDS Server History");
            NdsMonitor nm = new NdsMonitor(db);
            vpage.add(nm.getHistory(site));
        }        
    }

    /**
     * General routine to format something as JSON and return it
     * @return true if we are to display the error message in out page, false if json data is already sent
     */
    private boolean getJson()
    {
        boolean ret = false;
        
        AjaxHelper ah = new AjaxHelper();
        String ermsg = ah.processRequest(request,response);
        if (!ermsg.isEmpty())
        {
            vpage.add("Error: " + ermsg);
            ret = true;
        }
        return ret;
    }

    /**
     * Present a bug report form
     */
    private void contactForm() throws WebUtilException
    {
        BugReportForm brf = new BugReportForm(db,vpage, vuser, request);
        brf.setServletPath(servletPath);
        brf.addForm();
    }

    /**
     * Add their bug report to the ticket system
     */
    private void sendRpt() throws WebUtilException
    {
        BugReportForm brf = new BugReportForm(db,vpage, vuser, request);
        brf.setServletPath(servletPath);
        brf.sendRpt();
    }

    /**
     * Add new or edit existing help text
     * @return flag to send created page 
     * @throws WebUtilException 
     */
    private boolean editHelp() throws WebUtilException, LdvTableException
    {
        vpage.setTitle("Help Text Editor");
        helpManager.editHelp();
        
        return true;
    }

    private void saveHelpText() throws WebUtilException
    {
        vpage.setTitle("Help Text Editor");
        try
        {
            String saveit = request.getParameter("save");
            if(saveit != null)
            {
                helpManager.saveHelpText();
                editHelp();
            }
            else
            {
                helpManager.editHelp(request.getParameter("helpName"));
            }
        }
        catch (WebUtilException | LdvTableException ex)
        {
            throw new WebUtilException("SaveHelpText command", ex);
        }
        
    }

    private void dbStats() throws WebUtilException
    {
        vpage.setTitle("Database statistic");
        DBManager dbm = new DBManager(db, request, vpage, vuser);
        dbm.addStats();
    }
    
    /**
     * handle V2 of the channel filter/selection process
     */
    private void baseChan() throws WebUtilException, SQLException, LdvTableException
    {
        BaseChannelSelector bsc = new BaseChannelSelector(request, response, db, vpage, vuser);
        bsc.setContextPath(contextPath);
        bsc.setServletPath(servletPath);
        String submitAct = request.getParameter("submitAct");
        submitAct = submitAct == null ? "" : submitAct;

        if (submitAct.isEmpty())
        {
            vpage.setTitle("LigoDV-web");
            PageForm chanSel = bsc.addSelector(true);
            vpage.add(chanSel);
            vpage.addBlankLines(1);
        }
        else if (submitAct.toLowerCase().contains("retrieve"))
        {
            bsc.processFilterRequest();
        }
        else if (submitAct.toLowerCase().contains("continue"))
        {
            specifyTimesAndPlots("selbchan");
        }
        else
        {
            vpage.setTitle("LigoDV-web select channels");
            bsc.processFilterRequest();
        }
       
    }
    /**
     * Individual channel selection (v1)
     * submitAct = null or "" -> specify channels 
     * submitAct = "continue" -> pass selected channels to select times and plots.
     * @throws WebUtilException 
     */
    private void chanList() throws WebUtilException
    {
        String submitAct = request.getParameter("submitAct");
        submitAct = submitAct == null ? "" : submitAct;

        if (submitAct.toLowerCase().contains("continue"))
        {
            specifyTimesAndPlots("selchan");
        }
        else
        {
            ChannelSelector clf = new ChannelSelector(request, response, db, vpage, vuser);
            clf.setContextPath(contextPath);
            clf.setServletPath(servletPath);
            clf.selectChannels(submitAct, false);
        }
    }

    /**
     * The main help URL is part of the global configuration.
     * We put it on our nav bar.
     * 
     * @param helpUrl - usually the full URL to a wiki page
     */
    void setMainHelp(String helpUrl)
    {
        mainHelpUrl = helpUrl;
    }

    private void serverManager() throws LdvTableException, WebUtilException
    {
        vpage.setTitle("NDS Server Manager");
        ServerManager sm = new ServerManager(db, vpage, vuser);
        sm.setServletPath(servletPath);
        sm.setContextPath(contextPath);
        sm.setParamMap(paramMap);
        
        String[] submitActs = paramMap.get("submit");
        String submitAct= submitActs == null || submitActs.length == 0 ? "" : submitActs[0];
        if (submitAct == null || submitAct.isEmpty())
        {
            sm.showForm();
        }
        else
        {
            sm.procForm();
        }
    }
}
