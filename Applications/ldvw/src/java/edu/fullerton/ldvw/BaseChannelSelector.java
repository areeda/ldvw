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
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageFormSubmit;
import edu.fullerton.jspWebUtils.PageFormText;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemBlanks;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemImageLink;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanParts;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvplugin.HelpManager;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ViewUser;
import static edu.fullerton.ldvw.ChannelSelector.getListSelector;
import edu.fullerton.viewerplugin.GUISupport;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UI class to filter channel lists and select channels (V2)
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class BaseChannelSelector extends GUISupport
{
    private int strt = 0;
    private final int cnt = 25;       ///< number of rows per page
    private HashMap<String, String> selections;
    private String submitAct;
    
    // for the set of methods that get a selection row
    private boolean oddRow = true;
    
    private PageTableColumn selcbCol;
    private PageTableColumn cnameCol;
    private PageTableColumn trendSel;
    private PageTableColumn rawRates;
    private PageTableColumn rdsRates;
    private PageTableColumn singleRates;
    private PageTableColumn infoLink;
    private PageTableColumn typeChoices;
    
    private final String baseCISurl = "https://cis.ligo.org/channel/byname/";
    private final String infoIconDescUrl;
    private final String infoIconNoDescUrl;
    private PageItemImage infoDescIcon = null;
    private PageItemImage infoNoDescIcon = null;
    
    private PageItemImage pemIcon = null;
    private final String basePemUrl = "http://pem.ligo.org/channelinfo/index.php?channelname=";
    
    private PageTableRow selRow;
    private PageTableRow selRow2;
    private PageTableColumn cTypeCol;

    private final HttpServletRequest request;

    public BaseChannelSelector(HttpServletRequest request, HttpServletResponse response, Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
        setParamMap(request.getParameterMap());
        setContextPath(request.getContextPath());
        this.request = request;
        infoIconDescUrl = request.getContextPath() + "/infoicon3.png";
        infoIconNoDescUrl = request.getContextPath() + "/infoicon4.png";
        pemIcon = new PageItemImage(request.getContextPath() + "/pemIcon.png", "pem icon", "pem");
        pemIcon.setDim(24, 24);
    }
    
    /**
     * Generate the form with the channel list filter parameters
     * @param mainPage - flag to specify what the retrieve button looks like
     * @return the form to be added to the page.
     * @throws WebUtilException
     * @throws SQLException
     * @throws LdvTableException 
     */
    public PageForm addSelector(boolean mainPage) throws WebUtilException, SQLException, LdvTableException
    {
        PageForm pf = new PageForm();
        pf.setName("baseChanFilter");
        pf.setMethod("get");
        pf.setAction(getServletPath());
        pf.addHidden("act", "baseChan");
        
        // if we're on a "select more" command, save all previous selections
        if (request.getParameter("selMore") != null)
        {
            selections = getBaseChanSelections();
            addSelections(pf);
        }

        pf.setNoSubmit(true);

        boolean multipleSelections = false;
        PageTable chanFiltSpec = new PageTable();

        chanFiltSpec.setClassName("SelectorTable");

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
        
        PageTableRow row = new PageTableRow();
        // first column is a label for this parameter(s)
        PageItemString lbl = new PageItemString("Channel name filter: ");
        lbl.setAlign(PageItem.Alignment.RIGHT);
        row.add(lbl);

        PageFormText filt = new PageFormText("chnamefilt", request.getParameter("chnamefilt"));
        filt.setMaxLen(255);
        filt.setSize(32);
        row.add(filt);
        
        HelpManager hm = new HelpManager(db, vpage, vuser);
        hm.setContextPath(contextPath);
        PageItem hlpBtn = hm.getHelpButton("baseNameFilter");
        row.add(hlpBtn);
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
    /**
     * Process the filter parameters entered and create a list of indexID's
     */
    public void processFilterRequest() throws SQLException, LdvTableException, WebUtilException
    {
        String ifo = getParam("ifo");
        String subsys = getParam("subsys");
        String fsCmp = getParam("fsCmp");
        String fsStr = getParam("fs");
        String cType = getParam("ctype");
        String chnamefilt = getParam("chnamefilt");
        submitAct = getParam("submitAct");
        Pattern ifoSubsysPat = Pattern.compile("(^.+):((.+?)[_-])?");
        
        // optimize the search iff they specify channel name with ifo and or subsys that contain
        // no wild cards but have wild cards in the rest.
        
        Matcher ifoSubsysMat = ifoSubsysPat.matcher(chnamefilt);
        if ( ( chnamefilt.contains("*") || chnamefilt.contains("?") ) && ifoSubsysMat.find())
        {
            String myIfo = ifoSubsysMat.group(1);
            String mySubsys = ifoSubsysMat.group(3);
            if (myIfo != null && ifo.isEmpty() && ! myIfo.contains("*") && ! myIfo.contains("?"))
            {
                ifo = myIfo;
            }
            
            if (mySubsys != null && subsys.isEmpty() && ! mySubsys.contains("*") && ! mySubsys.contains("?"))
            {
                subsys = mySubsys;
            }
        }
        // do the query
        selections = getBaseChanSelections();   // see if they selected any channels so far

        int nsel = selections.size();
        if (nsel > 0)
        {
            vpage.add(String.format("%1$,d channel%2$s selected.  ", nsel, (nsel > 1 ? "s are" : " is")));
        }
        
        Float fs = fsStr.isEmpty() ? 0.f : Float.parseFloat(fsStr);
        
        ChannelIndex cidx = new ChannelIndex(db);
        int nMatch = cidx.getMatchCount(ifo, subsys, fsCmp, fs, cType, chnamefilt);
        
        if (nMatch == 0)
        {
            vpage.add("Query did not return any matches, please use back button");
        }
        else
        {
            processPageControls();
            
            strt = Math.min(strt, nMatch - 1);
            strt = Math.max(0, strt);
            int last = strt + cnt;
            last = Math.min(last, nMatch - 1);
            last = Math.max(last, 0);
            int nPages = (nMatch + cnt - 1) / cnt;
            int curPage = strt / cnt + 1;

            ArrayList<ChanIndexInfo> matches = cidx.search(ifo, subsys, fsCmp, fs, cType, chnamefilt, strt, cnt);
            
            vpage.add(String.format("%1$,d channels match query.", nMatch));
            vpage.includeJS("setChkBoxByClass.js");

            if (nMatch > 0)
            {
                vpage.add(String.format("  Displaying channels %1$,d through %2$,d, ", strt + 1, last + 1));
            }
            vpage.addBlankLines(2);

            PageForm pf = new PageForm();
            pf.setName("baseChanFilter");
            pf.setId("chanFilter");
            pf.setMethod("get");
            pf.setAction(getServletPath());
            pf.addHidden("act", "baseChan");
            pf.addHidden("strt", Integer.toString(strt));
            // add in the hidden variables from the channel parts dialog
            String[] partsVariables =
            {
                "server", "ifo", "subsys", "fsCmp", "fs", "ctype", "dtype", "chnamefilt"
            };

            for (String var : partsVariables)
            {
                String val = request.getParameter(var);
                if (val != null && !val.isEmpty())
                {
                    pf.addHidden(var, val);
                }
            }
            pf.setNoSubmit(true);


            PageTable pgCntrlBar = getPageControlBar(last, nMatch, nPages, curPage, false);
            pf.add(pgCntrlBar);
            pf.add(new PageItemBlanks());

            PageTable cpt = getSelectTable( matches,cType);

            pf.add(cpt);
            addSelections(pf);
            
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
    
    //--------------- Private utility methods --------------
    private void addListSelector(PageTable specTbl, PageItem aux, String label,
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
     * get a request parameter but return empty string if not available or set to any
     * @param name parameter name (what the form called it)
     * @return value or empty string, never null
     */
    private String getParam(String name)
    {
        String p = request.getParameter(name);
        if (p == null || p.equalsIgnoreCase("any"))
        {
            p = "";
        }
        return p;
    }

    /**
     * Search through the form parameters and save channel selections
     * @return parameters we want to remember for next time
     */
    private HashMap<String,String> getBaseChanSelections()
    {
        Pattern trendPat = Pattern.compile("(second|minute)-trend_(\\d+)");
        Pattern singlePat = Pattern.compile("(raw|online|static|testpoint|rds)_(\\d+)");

        HashMap<String,String> sels = new HashMap<>();
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (Entry<String,String[]> ent : parameterMap.entrySet())
        {
            String pname = ent.getKey();
            String[] val = ent.getValue();
            Matcher trendMatch = trendPat.matcher(pname.toLowerCase());
            Matcher singleMatch = singlePat.matcher(pname.toLowerCase());

            if (pname.startsWith("selbchan_"))
            {
                sels.put(pname, val[0]);
            }
            else if (trendMatch.find() && ! val[0].equalsIgnoreCase("none") )
            {
                sels.put(pname, val[0]);
            }
            else if (singleMatch.find())
            {
                sels.put(pname, val[0]);
            }
        }
        return sels;
    }
    private void getPagingParams()
    {
        submitAct = request.getParameter("submitAct");
        
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
        selAll.addEvent("onclick", "setSelByClasses('selBox',true, '.trendChoice',1)");
        pgCntrlRow.add(selAll);

        PageFormButton clrAll = new PageFormButton("selAll", "Clear all", "clrall");
        clrAll.setType("button");
        clrAll.addEvent("onclick", "setSelByClasses('selBox',false, '.trendChoice',0)");
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

    private PageTable getSelectTable( ArrayList<ChanIndexInfo> matches, String cType) 
            throws WebUtilException
    {
        String[] hdrAll=     { "Name", "Raw rate(s)", "Rds rate(s)", "CIS"};
        String[] hdrRawRds = { "Sel", "Name","Type", "Rate(s)", "CIS" };
        String[] hdrTrend =  { "Name","Type", "Trends", "CIS" };
        
        PageTable selTbl = new PageTable();
        PageTableRow hdr;
        if (cType.isEmpty())
        {
            hdr = getHdrRow(hdrAll);
        }
        else if (cType.equalsIgnoreCase("Raw") || cType.equalsIgnoreCase("rds"))
        {
            hdr = getHdrRow(hdrRawRds);
        }
        else if (cType.toLowerCase().contains("trend"))
        {
            hdr = getHdrRow(hdrTrend);
        }
        else
        {
            hdr = getHdrRow(hdrRawRds);
        }
        hdr.addStyleAll("text-align", "center");
        selTbl.addRow(hdr);
        
        
        for(ChanIndexInfo cii : matches )
        {
            prepareSelectRow(cii, cType);
            if (cType.equalsIgnoreCase("raw"))
            {
                singleRates = rawRates;
            }
            else if (cType.equalsIgnoreCase("rds"))
            {
                singleRates = rdsRates;
            }
            selRow = new PageTableRow();        //  contains data for all channels
            
            // change color on alternating rows of table
            selRow.setClassName(oddRow ? "odd" : "even");
            if (cType.isEmpty())
            {
                selRow2 = new PageTableRow();       //  contains type selector if we need it
                selRow2.setClassName(oddRow ? "odd" : "even");
            }
            oddRow = !oddRow;

            if (cType.isEmpty())
            {
                getRowAll(selTbl);
            }
            else if (cType.equalsIgnoreCase("Raw") || cType.equalsIgnoreCase("rds"))
            {
                getRowSingle(selTbl);
            }
            else if (cType.toLowerCase().contains("trend"))
            {
                getRowTrend(selTbl);
            }
            else
            {
                getRowSingle(selTbl);
            }
        }
        return selTbl;
    }
    private PageTableRow getHdrRow(String[] hdr) throws WebUtilException
    {
        PageTableRow r = new PageTableRow();
        for (String h : hdr)
        {
            if (h.isEmpty())
            {
                h = "&nbsp;";
            }
            r.add(new PageItemString(h, false));
        }
        r.setRowType(PageTableRow.RowType.HEAD);

        return r;
    }


    private final String[] trendSelChoices = // NB: item 0 is used by clear all, item 1 by select all
                            { "none", "min, mean, max", "mean only", "min only", "max only" };
    /**
     * Generate all the possible columns for this Channel Index entry, we decide later which ones to add
     * @param cii Channel index entry info
     * @param cType Type of channel they selected if any
     * @throws WebUtilException 
     */
    private void prepareSelectRow(ChanIndexInfo cii, String cType) throws WebUtilException
    {
        int indexID = cii.getIndexID();
        String selName = String.format("selbchan_%1$d", indexID);
        PageFormCheckbox selcb = new PageFormCheckbox(selName, "");
        selcb.setClassName("selBox");
        if (selections.containsKey(selName))
        {
            selcb.setChecked(true);
            selections.remove(selName);
        }
        PageItemString cname = new PageItemString(cii.getName());
        selcbCol = new PageTableColumn(selcb);
        cnameCol = new PageTableColumn(cname);
        cTypeCol = new PageTableColumn(cType);
        
        String trndSelName = String.format("seltrnd_%1$d", indexID);
        PageFormSelect trndChoice = new PageFormSelect(trndSelName, trendSelChoices);
        trndChoice.setClassName("trendChoice");
        if (selections.containsKey(trndSelName))
        {
            trndChoice.setSelected(selections.get(trndSelName));
            selections.remove(trndSelName);
        }
        trendSel = new PageTableColumn(trndChoice);
        
        rawRates = getRateCol(cii.getMinRawRate(), cii.getMaxRawRate());
        rdsRates = getRateCol(cii.getMinRdsRate(), cii.getMaxRdsRate());
        
        PageItem cisInfo;
        if (cii.getCisAvail().equalsIgnoreCase("a") || cii.getCisAvail().equalsIgnoreCase("d"))
        {
            cisInfo = getCisLink(cii);
        }
        else
        {
            cisInfo = new PageItemString("&nbsp;",false);
        }
        PageItem pemInfo = getPemLink(cii);
        if (pemInfo != null)
        {
            PageItemList infoLinks = new PageItemList();
            infoLinks.add(cisInfo);
            infoLinks.add(new PageItemString("&nbsp;",false));
            infoLinks.add(pemInfo);
            infoLink = new PageTableColumn(infoLinks);
        }
        else
        {
            infoLink = new PageTableColumn(cisInfo);
        }

        PageItemList typSelList = new PageItemList();
        for(String type : cii.getTypeList())
        {
            String typSelName= String.format("%1$s_%2$d", type, indexID);
            
            if (type.toLowerCase().contains("trend"))
            {
                PageFormSelect trndLst = new PageFormSelect(typSelName, trendSelChoices);
                trndLst.setClassName("trendChoice");
                typSelList.add(type + ": ");
                typSelList.add(trndLst);
            }
            else
            {
                PageFormCheckbox cb = new PageFormCheckbox(typSelName, type + ". ");
                cb.setClassName("selBox");
                if (selections.containsKey(typSelName))
                {
                    cb.setChecked(true);
                    selections.remove(typSelName);
                }
                typSelList.add(cb);
            }
        }
        typeChoices = new PageTableColumn(typSelList);
    }

    /**
     * Add the appropriate columns to the table this one is a double row
     * because we are also selecting channel type
     * "selCB", "Name", "Raw rate(s)", "Rds rate(s)", "CIS"
     * -      , "          type selector           ",   -
     */
    private void getRowAll(PageTable selTbl) throws WebUtilException
    {
        
        selRow.add(cnameCol);
        selRow.add(rawRates);
        selRow.add(rdsRates);
        infoLink.setRowSpan(2);
        selRow.add(infoLink);
        
        typeChoices.setSpan(selRow.getColumnCount()-1);
        selRow2.add(typeChoices);
        
        selTbl.addRow(selRow);
        selTbl.addRow(selRow2);
    }

    /**
     * Chanel select row with only 1 channel type
     * "selCB", "Name", "Rate(s)", "CIS"
     * @param selTbl 
     */
    private void getRowSingle(PageTable selTbl) throws WebUtilException
    {
        selRow.add(selcbCol);
        selRow.add(cnameCol);
        selRow.add(cTypeCol);
        selRow.add(singleRates);
        selRow.add(infoLink);
        selTbl.addRow(selRow);
    }

    /**
     * Channel select row for a (single) trend
     * "selCB", "trend selector", "Name", "CIS"
     * @param selTbl 
     */
    private void getRowTrend(PageTable selTbl) throws WebUtilException
    {

        selRow.add(cnameCol);
        selRow.add(cTypeCol);
        selRow.add(trendSel);
        selRow.add(infoLink);
        selTbl.addRow(selRow);
    }

    private PageTableColumn getRateCol(float minRate, float maxRate) throws WebUtilException
    {
        String minS, maxS;
        if (minRate < 0.999f)
        {
            minS = String.format("%1$.3f", minRate);
        }
        else
        {
            minS = String.format("%1$.0f", minRate);
        }
        if (maxRate < 1)
        {
            maxS = String.format("%1$.3f", maxRate);
        }
        else
        {
            maxS = String.format("%1$.0f", maxRate);
        }
        String it;
        if (minRate == maxRate)
        {
            if (minRate <= .0001)
            {
                it = "";
            }
            else
            {
                it=minS;
            }
        }
        else
        {
            it = minS + "-" + maxS;
        }
        PageItemString str = new PageItemString(it);
        PageTableColumn ret = new PageTableColumn(str);
        ret.setAlign(minRate == maxRate ? PageItem.Alignment.CENTER : PageItem.Alignment.RIGHT);
        return ret;
    }
    /**
     * Link into the Channel Information system
     *
     * @param ci our channel info object
     * @return an image link
     */
    
    private PageItemImageLink getCisLink(ChanIndexInfo ci)
    {
        if (infoDescIcon == null)
        {
            infoDescIcon = new PageItemImage(infoIconDescUrl, "chan info", "CIS");
            infoDescIcon.setDim(24, 24);
        }
        if (infoNoDescIcon == null)
        {
            infoNoDescIcon = new PageItemImage(infoIconNoDescUrl, "chan info", "CIS");
            infoNoDescIcon.setDim(24, 24);
        }

        String cname = ci.getName();
        
        String cisUrl = baseCISurl + cname;
        PageItemImageLink link;
        if (ci.getCisAvail().equalsIgnoreCase("d"))
        {
            link = new PageItemImageLink(cisUrl, infoDescIcon, "_blank");
        }
        else
        {
            link = new PageItemImageLink(cisUrl, infoNoDescIcon, "_blank");
        }

        return link;
    }

    /**
     * adjust start and last values if they did a prev, next ...
     */
    private void processPageControls()
    {
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

    }

    private void addSelections(PageForm pf) throws WebUtilException
    {
        for(Entry<String,String> ent : selections.entrySet())
        {
            pf.addHidden(ent.getKey(), ent.getValue());
        }
    }
    /**
     * if this channel probably has a PEM link add an icon for it
     * @param cii
     * @return 
     */
    private PageItem getPemLink(ChanIndexInfo cii)
    {
        PageItem ret = null;
        String cname=cii.getName();
        
        if (cname.matches("[HL]1:PEM-.*_DQ"))
        {
            String pemName = cname.replace("_DQ", "");
            
            String pemUrl = basePemUrl + pemName;
            PageItemImageLink link;
            ret = new PageItemImageLink(pemUrl, pemIcon, "_blank");
        }
        return ret;
    }
    
}
