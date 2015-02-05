/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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

import viewerplugin.GUISupport;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Utils;
import commonUI.ChannelsUI;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvplugin.HelpManager;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * GUI to allow selections of one or more channels from database
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChannelSelector extends GUISupport
{
    private int strt = 0;
    private final int cnt = 25;       ///< number of rows per page
    private HashSet<Integer> selections;
    private String ifo;
    private String subsys;
    private final HttpServletRequest request;

    
    public ChannelSelector(HttpServletRequest request, HttpServletResponse response, Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
        setParamMap(request.getParameterMap());
        this.request = request;
    }

    /**
     * Present the basic Channel selection GUI (after the retrieve button)
     * 
     * @param submitAct
     * @param homePage
     * @throws WebUtilException 
     */
    public void selectChannels(String submitAct, boolean homePage) throws WebUtilException
    {
        try
        {
            vpage.setTitle("liogdv-web - select channels");
            HelpManager hm = new HelpManager(db, vpage, vuser);
            hm.setContextPath(contextPath);
            hm.setServletPath(servletPath);
            hm.setParamMap(paramMap);
            PageItem hlpBtn = hm.getHelpButton("chanNameFilter");
            PageForm chanSel = addChanSelector(homePage, vuser.isAdmin(),hlpBtn);
            selections = getSelections();
            addSelections(chanSel, selections);
            vpage.add(chanSel);
            vpage.addBlankLines(2);
            
            doFormFilter(submitAct);
        }
        catch (WebUtilException | LdvTableException | SQLException ex)
        {
            throw new WebUtilException("Select channels", ex);
        }

    }
    /**
     * Take the results of the channel filter form and add the list of matching channels as selection form
     * to the current page.
     * 
     * @param submitAct
     * @throws SQLException
     * @throws LdvTableException
     * @throws WebUtilException 
     */
    public void doFormFilter(String submitAct) throws SQLException, LdvTableException, WebUtilException
    {
        ChannelTable ct = new ChannelTable(db);
        selections = getSelections();

        int nsel = selections.size();
        if (nsel > 0)
        {
            vpage.add(String.format("%1$,d channel%2$s selected", nsel, (nsel > 1 ? "s are" : " is")));
        }
        int nMatch;
        
        if (submitAct != null && ! submitAct.equalsIgnoreCase("selMore"))
        {
            String where = makeWhereClause(submitAct);
            nMatch = ct.getMatchCount(where);

            strt = Math.min(strt, nMatch-1);
            strt = Math.max(0,strt);
            int last = strt + cnt;
            last = Math.min(last, nMatch-1);
            last = Math.max(last, 0);
            int nPages = (nMatch + cnt - 1) / cnt;
            int curPage = strt / cnt + 1;

            vpage.add(String.format("%1$,d channels match query.", nMatch));
            vpage.includeJS("setChkBoxByClass.js");

            if (nMatch > 0)
            {
                vpage.add(String.format("  Displaying channels %1$,d through %2$,d, ", strt + 1, last+1));
            }
            vpage.addBlankLines(2);

            PageForm pf = new PageForm();
            pf.setName("chanFilter");
            pf.setId("chanFilter");
            pf.setMethod("get");
            pf.setAction(getServletPath());
            pf.addHidden("act", "chanlist");
            pf.addHidden("strt", Integer.toString(strt));
            // add in the hidden variables from the channel parts dialog
            String[] partsVariables = { "server", "ifo", "subsys", "fsCmp", "fs", "ctype", "dtype", "chnamefilt"};

            for(String var : partsVariables)
            {
                String val = request.getParameter(var);
                if (val != null && ! val.isEmpty())
                {
                    pf.addHidden(var, val);
                }
            }
            pf.setNoSubmit(true);

            addSelections(pf, selections);

            PageTable pgCntrlBar = getPageControlBar(last, nMatch, nPages, curPage, false);
            pf.add(pgCntrlBar);
            pf.add(new PageItemBlanks());

            PageTable cpt = getChanListTable(0, strt, cnt, true, where, selections);
            pf.add(cpt);
        
            if (nMatch > 10)
            {   // if the list is long enough add another control bar at the end
                pf.add(new PageItemBlanks());
                PageTable pgCntrlBar2 = getPageControlBar(last, nMatch, nPages, curPage, true);
                pf.add(pgCntrlBar2);
            }

            vpage.add(pf);
        }
        vpage.addBlankLines(2);

    }
    public int getMatchCount(String submitAct)
    {
        int nMatch;
        try
        {
            String where = makeWhereClause(submitAct);

            ChannelTable ct = new ChannelTable(db);
        
            nMatch = ct.getMatchCount(where);
            
        }
        catch (WebUtilException | SQLException ex)
        {
            nMatch=0;
        }
        return nMatch;
    }
    private String makeWhereClause(String submitAct) throws WebUtilException
    {
        // extract fields to filter from the request
        String server = request.getParameter("server");
        ifo = request.getParameter("ifo");
        subsys = request.getParameter("subsys");
        String fsCmp = request.getParameter("fsCmp");
        String fs = request.getParameter("fs");
        String ctype = request.getParameter("ctype");
        String dtype = request.getParameter("dtype");
        String chnamefilt = request.getParameter("chnamefilt");
        
        String strtStr = request.getParameter("strt");
        if (strtStr != null && strtStr.length() > 0)
        {
            strt = Integer.parseInt(strtStr);
        }

        if (submitAct.equalsIgnoreCase("next"))
        {
            strt += cnt;
        }
        else if (submitAct.equalsIgnoreCase("prev"))
        {
            strt -= cnt;
            if (strt < 0)
            {
                strt = 0;
            }
        }
        else if (submitAct.toLowerCase().equals("go"))
        {
            String pageNum = request.getParameter("pageNum");
            if (pageNum.matches("^\\d+$"))
            {
                strt = (Integer.parseInt(pageNum) - 1) * cnt;
                strt = Math.max(strt, 0);
            }
        }
        else if (submitAct.toLowerCase().equals("go2"))
        {
            String pageNum = request.getParameter("pageNum2");
            if (pageNum.matches("^\\d+$"))
            {
                strt = (Integer.parseInt(pageNum) - 1) * cnt;
                strt = Math.max(strt, 0);
            }
        }

        // construct a where clause
        StringBuilder where = new StringBuilder();
        if (server != null && server.length() > 0 && !server.equalsIgnoreCase("any"))
        {
            where.append(String.format("server=\"%1$s\"", server));
        }

        boolean gotIFO = false;
        String nameMatcher = "";
        if (ifo != null && ifo.length() > 0 && !ifo.equalsIgnoreCase("any"))
        {
            nameMatcher = ifo + ":";
            gotIFO=true;
        }
        if (subsys != null && subsys.length() > 0 && !subsys.equalsIgnoreCase("any")) 
        {
            nameMatcher += gotIFO ? "" : "%:";
            nameMatcher += subsys;
        }        
        if (fs != null && fs.length() > 0 && !fs.equalsIgnoreCase("any") && fsCmp != null && fsCmp.length() > 0)
        {
            if (where.length() > 0)
            {
                where.append(" AND ");
            }
            //@todo probably should convert sample rate to a float and add it to the query that way
            switch (fsCmp)
            {
                case ">=":
                    where.append(String.format(" sampleRate >= %1$s ", fs));
                    break;
                case ">":
                    where.append(String.format(" sampleRate > %1$s ", fs));
                    break;
                case "<=":
                    where.append(String.format(" sampleRate <= %1$s ", fs));
                    break;
                case "<":
                    where.append(String.format(" sampleRate < %1$s ", fs));
                    break;
                case "~=":
                    where.append(String.format(" abs(sampleRate - %1$s) >= .01 ", fs));
                    break;
                default:
                    where.append(String.format(" abs(sampleRate - %1$s) < .01 ", fs));
                    break;
            }

        }
        if (submitAct.equalsIgnoreCase("referenceplot") ||
                (ctype != null && ctype.length() > 0 && !ctype.equalsIgnoreCase("any")))
        {
            //@todo we probably want to be smarter about trend data
            if (where.length() > 0)
            {
                where.append(" AND ");
            }
            if (submitAct.equalsIgnoreCase("referenceplot"))
            {
                where.append("cType = 'raw'");
            }
            else
            {
                where.append(String.format("cType = \"%1$s\" ", ctype));
            }
        }

        if (dtype != null && dtype.length() > 0 && !dtype.equalsIgnoreCase("any"))
        {
            //@todo we probably want to be smarter about trend data
            if (where.length() > 0)
            {
                where.append(" AND ");
            }

            where.append(String.format("dtype = \"%1$s\" ", dtype));
        }
        if (submitAct.equalsIgnoreCase("referenceplot"))
        {
            if (where.length() > 0)
            {
                where.append(" AND ");
            }

            String reqChan = request.getParameter("channelName");
            reqChan=Utils.sqlQuote(reqChan);
            where.append("name=").append(reqChan).append(" ");
            
        }
        else
        {
            String cnf = decodeFilterString(nameMatcher, chnamefilt);
            if (cnf.toLowerCase().startsWith("error"))
            {
                vpage.add("Error in channel name filter.  I promise details in a later version.");
                vpage.addBlankLines(1);
                vpage.add("It is ignored in this query.");
                vpage.addBlankLines(2);
            }
            else if (!cnf.isEmpty())
            {
                if (where.length() > 0)
                {
                    where.append(" AND ");
                }
                where.append(cnf);
            }
        }
        return where.toString();
    }

    private String decodeFilterString(String nameMatcher, String chnamefilt)
    {
        // <filter> :== null | <and clause> [ | <and clause> ...]
        // <and clause :== <term> [ <white space> <term> ...]
        // <term> :== [!] <alnum>*

        String ret = "";
        String nm = nameMatcher;
        if (!nm.isEmpty())
        {
            // check for ifo or subsystem conflict
             
        }
        if (chnamefilt != null && !chnamefilt.isEmpty())
        {
            String work = chnamefilt.trim();
            Pattern p = Pattern.compile("([^\\!\\s\\|]*)([\\!\\s\\|]*)(.*)$");
            Pattern ifoPat = Pattern.compile("^\\w+:");
            ArrayList<String> andList = new ArrayList<>();
            boolean gotNot = false;
            boolean gotOr = false;
            boolean gotErr = false;

            while (work.length() > 0 && !gotErr)
            {
                Matcher m = p.matcher(work);
                if (m.find())
                {
                    String a = m.group(1);
                    String b = m.group(2);
                    work = m.group(3);

                    if (a != null && !a.isEmpty())
                    {
                        String comp = gotNot ? " NOT LIKE" : " LIKE ";
                        String clause = " name " + comp + "\"" ;
                        Matcher ifoMat = ifoPat.matcher(a);
                        if (!ifoMat.find())
                        {   // add leading % iff ifo not specified.
                            clause += "%";
                        }
                        clause += a + "%\"";
                        andList.add(clause);
                        gotNot = false;
                    }
                    if ((b == null || b.isEmpty()) && (a == null || a.isEmpty()))
                    {
                        gotErr = true;
                    }
                    else if (!b.matches("\\s"))
                    {
                        b = b.trim();
                        if (b.length() > 1)
                        {
                            gotErr = true;
                        }
                        else if (b.equals("!"))
                        {
                            if (gotNot)
                            {
                                gotErr = true;
                            }
                            else
                            {
                                gotNot = true;
                            }
                        }
                        else if (b.equals("|"))
                        {
                            if (!andList.isEmpty())
                            {
                                String al = "";
                                for (String s : andList)
                                {
                                    if (!al.isEmpty())
                                    {
                                        al += " AND ";
                                    }
                                    al += s;
                                }
                                if (!ret.isEmpty() && gotOr)
                                {
                                    ret += " OR ";
                                }
                                ret += " (" + al + ") ";
                                andList.clear();
                                gotOr = true;
                            }

                        }
                    }
                }
            }
            if (gotErr)
            {
                ret = "error";
            }
            else if (!andList.isEmpty())
            {
                String al = "";
                for (String s : andList)
                {
                    if (!al.isEmpty())
                    {
                        al += " AND ";
                    }
                    al += s;
                }
                if (!ret.isEmpty() && gotOr)
                {
                    if (!nm.isEmpty())
                    {
                        ret = "(name LIKE '" + nm + "%'" + (ret.isEmpty() ? "" : " AND " + ret + ") ");
                    }
                    ret += " OR ";
                    if (!nm.isEmpty())
                    {
                        al = "name LIKE '" + nm + "%'" + " AND " + al;
                    }
                }
                ret += " (" + al + ") ";
            }
        }
        
        if (!ret.isEmpty() )
        {
            if (!nm.isEmpty())
            {
                ret = " ( name like '" + nm + "%' ) AND (" + ret + ")" ;
            }
            else
            {
                ret = " (" + ret + ") ";
            }
            
        }
        else if (!nm.isEmpty())
        {
            ret = " ( name like '" + nm + "%' ) ";
        }
        return ret;
    }

    private PageTable getPageControlBar(int last, int nMatch, int nPages, int curPage, boolean isBottomBar) throws WebUtilException
    {
        PageTable pgCntrlBar = new PageTable();
        PageTableRow pgCntrlRow = new PageTableRow();
        
        
        PageFormButton prevBtn = new PageFormButton("submitAct", "<", "Prev");
        prevBtn.setEnabled(strt > 0);
        pgCntrlRow.add(prevBtn);
        
        PageItemList pageSel = new PageItemList();
        pageSel.add("Page: ");
        String botSuffix = isBottomBar ? "2" : "";
        PageFormText pageNum = new PageFormText("pageNum" + botSuffix, String.format("%1$,d", curPage));
        pageNum.setSize(3);
        pageNum.setMaxLen(6);
        pageNum.addEvent("onchange", "historySubmit('Go" + botSuffix + "', this);");
        pageSel.add(pageNum);
        pageSel.add(String.format(" of %,d ", nPages));
        pgCntrlRow.add(pageSel);

        PageFormButton nextBtn = new PageFormButton("submitAct", ">", "next");
        nextBtn.setEnabled(curPage < nPages);
        pgCntrlRow.add(nextBtn);
        
        PageFormButton selAll = new PageFormButton("selAll", "Select all", "selall");
        selAll.setType("button");
        selAll.addEvent("onclick", "setChkBoxByClass('selBox',true)");
        pgCntrlRow.add(selAll);

        PageFormButton clrAll = new PageFormButton("selAll", "Clear all", "clrall");
        clrAll.setType("button");
        clrAll.addEvent("onclick", "setChkBoxByClass('selBox', false)");
        pgCntrlRow.add(clrAll);
        
        PageFormButton selMore = new PageFormButton("selMore", "Select more", "selMore");
        selMore.setType("submit");
        pgCntrlRow.add(selMore);
        pgCntrlRow.add();

        PageFormSubmit timeSpecBtn = new PageFormSubmit("submitAct", "Continue &raquo;");
        timeSpecBtn.setId("continueButton");
        pgCntrlRow.add(timeSpecBtn);
        pgCntrlRow.setClassAll("noborder");
        pgCntrlBar.addRow(pgCntrlRow);
        return pgCntrlBar;
    }
    /**
     * Make the form to select channels
     *
     * @param mainPage - affects the submit button format
     * @param advanced - determines which parameters are included
     * @param chanNameHelp - a help button to add to form
     * @return the channel selector form to add to your page 
     * @throws WebUtilException - probably my bug
     */
    public PageForm addChanSelector(boolean mainPage,boolean advanced, PageItem chanNameHelp) 
            throws WebUtilException
    {
        PageForm pf = new PageForm();
        pf.setName("chanFilter");
        pf.setMethod("get");
        pf.setAction(getServletPath());
        pf.addHidden("act", "chanlist");

        pf.setNoSubmit(true);

        boolean multipleSelections = false;
        PageTable chanFiltSpec = new PageTable();

        chanFiltSpec.setClassName("SelectorTable");


        addListSelector(chanFiltSpec, null, "Server: ", "server", ChanParts.getServers(), 
                        multipleSelections, request.getParameter("server"), true);

        addListSelector(chanFiltSpec, null, "IFO: ", "ifo", ChanParts.getIFOList(), 
                        multipleSelections, request.getParameter("ifo"), true);
        addListSelector(chanFiltSpec, null, "Subsys: ", "subsys", ChanParts.getSubSystems(), multipleSelections, request.getParameter("subsys"), true);

        PageItemList compare = (PageItemList) PageItemList.getListSelector("", "fsCmp", ChanParts.getSampleRateCmp(), 
                                                        false, request.getParameter("fsCmp"), false, 0);
        compare.setUseDiv(false);
        addListSelector(chanFiltSpec, compare, "Sample Frequency: ", "fs", ChanParts.getSampleRates(), 
                        multipleSelections, request.getParameter("fs"), true);

        addListSelector(chanFiltSpec, null, "Channel Type: ", "ctype", ChanParts.getChanTypes(), 
                        multipleSelections, request.getParameter("ctype"), true);
        if (advanced)
        {
            addListSelector(chanFiltSpec, null, "Data Type: ", "dtype", ChanParts.getDataTypes(), 
                            multipleSelections, request.getParameter("dtype"), true);
        }

        PageTableRow row = new PageTableRow();
        // first column is a label for this parameter(s)
        PageItemString lbl = new PageItemString("Channel name filter: ");
        lbl.setAlign(PageItem.Alignment.RIGHT);
        row.add(lbl);

        PageFormText filt = new PageFormText("chnamefilt", request.getParameter("chnamefilt"));
        filt.setMaxLen(255);
        filt.setSize(40);
        row.add(filt);
        if (chanNameHelp != null)
        {
            row.add(chanNameHelp);
        }
        else
        {
            row.add();
        }
        row.setClassAll("noborder");
        chanFiltSpec.addRow(row);

        PageFormSubmit getChanListBtn;
        if (mainPage)
        {
            getChanListBtn = new PageFormSubmit("submitAct", "Retrieve Channel List &raquo;");
            getChanListBtn.setId("continueButton"); // emphasize it's what we do next
        }
        else
        {
            // sligthly different.  Here it's just an ordinary button
            getChanListBtn = new PageFormSubmit("submit", "Retrieve Channel List");
            getChanListBtn.setId("continueBtnAux");
        }
        row = new PageTableRow();
        row.add();
        row.add(getChanListBtn);
        row.setClassAll("noborder");
        chanFiltSpec.addRow(row);

        pf.add(chanFiltSpec);

        return pf;

    }

    private static void addListSelector(PageTable specTbl, PageItem aux, String label,
                                        String frmItmName, String[] options,
                                        boolean multipleSelections, String defaultVal,
                                        boolean addAny) throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        // first column is a lable for this parameter(s)
        PageItemString lbl = new PageItemString(label, false);
        lbl.setAlign(PageItem.Alignment.RIGHT);
        row.add(lbl);

        PageItemList pil = new PageItemList();
        String id = "chanSelList";
        if (aux != null)
        {
            pil.add(aux);
            id = "chanSelList2";
        }

        pil.add(getListSelector(id, "", frmItmName, options, multipleSelections,
                                defaultVal, addAny));
        pil.setUseDiv(false);
        row.add(pil);
        row.add();
        row.setClassAll("noborder");
        specTbl.addRow(row);

    }

    /**
     * Create a drop down menu
     * @param id
     * @param label
     * @param paramName
     * @param vals
     * @param allowMult
     * @param defVal
     * @param addAny
     * @return 
     */
    public static PageItem getListSelector(String id, String label, String paramName, String[] vals, boolean allowMult, String defVal, boolean addAny)
    {

        PageFormSelect fs = new PageFormSelect(paramName);

        fs.setClassName(id);
        fs.setMultAllowed(allowMult);
        if (addAny)
        {
            fs.add("any");
        }
        for (String s : vals)
        {
            fs.add(s);
        }
        if (defVal != null && defVal.length() > 0)
        {
            fs.setSelected(defVal);
        }

        PageItemList pil3 = new PageItemList();
        pil3.setUseDiv(false);
        pil3.add(new PageItemString(label, false));
        pil3.add(fs);

        return pil3;
    }
    /**
     * Get a table to select channels from a database query
     *
     * @param nCols
     * @param strt
     * @param count
     * @param sel
     * @param filter
     * @param selections
     * @return a table object ready for display with selection
     * @throws LdvTableException
     * @throws WebUtilException
     */
    public PageTable getChanListTable(int nCols, int strt, int count, boolean sel,
                                  String filter, HashSet<Integer> selections) throws LdvTableException, WebUtilException
    {
        ChannelTable ct;
        try
        {
            ct = new ChannelTable(db);
        }
        catch (SQLException ex)
        {
            throw new WebUtilException("Getting channel selection table", ex);
        }
        ArrayList<ChanInfo> cList;
        cList = ct.getFilterChanList(strt, count, sel, filter);

        ChannelsUI cs = new ChannelsUI(contextPath);
        PageTable ret = cs.getSelector(cList, selections);
        return ret;
    }


}
