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

import edu.fullerton.viewerplugin.GUISupport;
import com.areeda.jaDatabaseSupport.Database;
import commonUI.ChannelsUI;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.BaseChanSingle;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;

import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvplugin.HelpManager;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates the PageForm to present to the client to select time interval, product, and their options
 * 
 * @author Joseph Areeda<joseph.areeda@ligo.org>
 */
public class TimeAndPlotSelector extends GUISupport
{
    private final String[] durUnits = { "seconds", "minutes", "hours", "days", "weeks"};
    private final String[] pltGroups = { "time", "channel", "all","none" };
    private final String[] dwnldFmts = { "LigoDV export", "CSV - 1 dataset only", "WAV - 1 dataset only" };
    private HashSet<Integer> selections;
    private Map<Integer, BaseChanSelection> baseSelections;
    private boolean goterr;
    private ChannelTable ct;
    private int nSel;       // number of channels selected (base or classic)
    
    /**
     * Constructs a new Time and Plot selector
     * @param db
     * @param vpage
     * @param vuser 
     */
    public TimeAndPlotSelector( Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
        selections = new HashSet<>();
    }
    /**
     * add a collection of selected channels, used by features like reference plot to add
     * channels not selected by the user to a plot
     * 
     * @param sels table ids of channels to add
     */
    public void addSelections(Collection<Integer> sels)
    {
        if (!sels.isEmpty())
        {
            selections.addAll(sels);
        }
    }
    /**
     * process channel selection input form if applicable and present the time and plot selector
     * 
     * @param selType "selchan" for individual, "selbchan" for base channels
     * @throws SQLException
     * @throws WebUtilException 
     * @throws LdvTableException
     */
    public void showForm(String selType) throws SQLException, WebUtilException, LdvTableException
    {
        Integer stepNo = 1;     /// numbering for what to do

        PluginManager pmanage = new PluginManager(db, vpage, vuser, paramMap);
        pmanage.setContextPath(contextPath);
        pmanage.setServletPath(servletPath);
        pmanage.setParamMap(paramMap);

        PageItem chanSelector;
        if (selType.equalsIgnoreCase("selchan"))
        {
            chanSelector = getClassicChanSelector();
        }
        else
        {
            chanSelector = getBaseChanSelector();
        }
        if (chanSelector == null)
        {
            vpage.addBlankLines(3);
            vpage.add("No Channels are selected.  Please use back button.");            
        }
        else
        {
            PageForm pf = new PageForm();
            pf.setName("timePlotSelect");
            pf.setMethod("get");
            pf.setAction(getServletPath());
            pf.addHidden("act", "doplot");
            pf.setNoSubmit(true);

            if (selType.equalsIgnoreCase("selbchan"))
            {
                pf.addHidden("baseSelector", "true");
            }
            
            vpage.add(String.format("%1$d channel%2$s selected.", nSel, nSel > 1 ? "s are" : " is"));
            vpage.addBlankLines(2);

            PageItem selClrBar = getChanSelBar(nSel);
            pf.add(selClrBar);

            pf.add(chanSelector);
            
            if (selections.size() > 10)
            {
                pf.add(selClrBar);
            }

            addProcStep(pf, String.format("%1$d. Select time(s)", stepNo));
            stepNo++;

            PageTable timeSpecTbl = getTimeSpecTable();

            PageTableRow tsRow = new PageTableRow();

            // if all channels are on-line we can auto refresh
            boolean allOnline = true;

            for (BaseChanSelection bcs : baseSelections.values())
            {
                boolean isOnline = bcs.isOnline();
                allOnline &= isOnline;
            }
            if (allOnline)
            {
                PageFormCheckbox autoRefreshCB = new PageFormCheckbox("autoRefresh", "Auto refresh (use repeat count).");
                PageTableRow arRow = new PageTableRow();
                arRow.add();
                arRow.add(autoRefreshCB);
                arRow.setClassAll("noborder");
                timeSpecTbl.addRow(arRow);
            }

            tsRow.add("Group by:  ");
            PageFormSelect pltGroup = new PageFormSelect("plotGroup");
            String grp = getPrevValue("plotGroup", 0, pltGroups[0]);
            pltGroup.add(pltGroups);
            pltGroup.setSelected(grp);
            tsRow.add(pltGroup);

            HelpManager helpManager = new HelpManager(db, vpage, vuser);
            helpManager.setContextPath(contextPath);
            helpManager.setParamMap(paramMap);

            PageItem hlpBtn = helpManager.getHelpButton("groupByMenu");
            tsRow.add(hlpBtn);
            tsRow.setClassAll("noborder");
            timeSpecTbl.addRow(tsRow);

            pf.add(timeSpecTbl);
            pf.add(new PageItemBlanks(2));

            ArrayList<BaseChanSelection> baseChans = new ArrayList<>(baseSelections.values());
            pf.add(pmanage.getSelector(baseChans, stepNo));

            addProcStep(pf, String.format("To proceed, click on plot or download"));
            stepNo++;

            pf.add(new PageItemBlanks(2));
            PageFormSubmit plotBtn = new PageFormSubmit("plot", "Plot &raquo;");
            plotBtn.setId("continueButton"); // emphasize it's what we do next
            pf.add(plotBtn);

            pf.add(new PageItemString("Download format:"));
            PageFormSelect fmtSel = new PageFormSelect("dwnFmt");
            fmtSel.add(dwnldFmts);
            pf.add(fmtSel);
            PageFormSubmit downloadBtn = new PageFormSubmit("download", "Download");
            downloadBtn.setId("continueButton");
            pf.add(downloadBtn);
            vpage.add(pf);
            vpage.addBlankLines(2);
        }
    }
    private PageItem getClassicChanSelector() throws SQLException, WebUtilException, LdvTableException
    {
        PageItem ret;
        nSel = 0;
        selections = getSelections();
        
        if (selections.isEmpty())
        {
            ret = null;
        }
        else
        {
            nSel = selections.size();

            ct = new ChannelTable(db);
            ChannelIndex chanIndex = new ChannelIndex(db);
            ChanPointerTable cpt = new ChanPointerTable(db);
            if (baseSelections == null)
            {
                baseSelections = new HashMap<>();
            }
            for(int cidx : selections)
            {
                BaseChanSelection bcs = new BaseChanSelection();
                ChanInfo ci = ct.getChanInfo(cidx);
                int baseId = cpt.getBaseId(cidx);
                ChanIndexInfo baseInfo = chanIndex.getInfo(baseId);
                bcs.init(baseInfo);
                bcs.addSingleChan(ci);
                baseSelections.put(baseId, bcs);
            }
            ret = getChannelSelectTable(selections);
        }
        return ret;
    }
    private PageItem getBaseChanSelector() throws SQLException, WebUtilException, LdvTableException
    {
        PageItem ret;
        nSel = 0;
        baseSelections = getBaseSelections();
        BaseChannelSelector bcs = new BaseChannelSelector( db, vpage, vuser, getContextPath());
        bcs.setParamMap(paramMap);
        if (baseSelections.isEmpty())
        {
            ret = new PageItemString("No channels were selected.");
        }
        else
        {
            ret = bcs.getSelector(baseSelections);
        }
        nSel = 0;
        for (BaseChanSelection bc : baseSelections.values())
        {
            nSel += bc.getNsel();
        }
        return ret;
    }

    /**
     * Produce the common Time Interval specification table
     * @return an item to add to the page where applicable
     * @throws WebUtilException probably a programming bug in this routine. 
     * @throws edu.fullerton.ldvjutils.LdvTableException
     * @throws SQLException
     */
    public PageTable getTimeSpecTable() throws WebUtilException, SQLException, LdvTableException
    {
        int itemWidth = 35;
        PageTable timeSpecTbl = new PageTable();
        timeSpecTbl.setClassName("SelectorTable");
        
        HelpManager helpManager = new HelpManager(db, vpage, vuser);
        helpManager.setContextPath(contextPath);
        helpManager.setParamMap(paramMap);

        PageItem strtHelpBtn = helpManager.getHelpButton("startTime");
        String strtStr = TimeAndDate.nowAsUtcString(-10 * 60 * 1000);
        strtStr = getPrevValue("strtTime",0,strtStr);
        timeSpecTbl.addRow(GUISupport.getTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", 
                                          itemWidth, strtStr,strtHelpBtn));
        PageTableRow moreTimes;
        int nTimes=4;
        for (int i=1;i<= nTimes; i++)
        {
            strtStr=getPrevValue("strtTime", i, "");
            moreTimes = getLabelTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", 
                                       itemWidth, strtStr);
            moreTimes.setClassName("moreTimes");
            moreTimes.addStyle("display", "none");
            timeSpecTbl.addRow(moreTimes);
        }
        
        PageItem durHelpBtn = helpManager.getHelpButton("duration");

        String durStr=getPrevValue("duration", 0, "20");
        timeSpecTbl.addRow(GUISupport.getTxtRow("duration", "Duration", "(day HH:MM:SS or sec)", 
                                                itemWidth, durStr, durHelpBtn));

        PageTableRow rpt = new PageTableRow();
        PageItemString rptLbl = new PageItemString("Repeat every:");
        rptLbl.setAlign(PageItem.Alignment.RIGHT);
        rpt.setClassName("repeatOptions");
        rpt.addStyle("display", "none");
        rpt.add(rptLbl);
        
        PageItemList rptLst = new PageItemList();

        PageFormText pft = new PageFormText("repeatDur", "");
        pft.setMaxLen(255);
        pft.setSize(itemWidth-10); 
        String val=getPrevValue("repeatDur", 0, "0");
        pft.setDefaultValue(val);
        rptLst.add(pft);

        PageFormSelect rptUnit = new PageFormSelect("repeatUnit");
        rptUnit.add(durUnits);
        val = getPrevValue("repeatUnit", 0, durUnits[0]);
        rptUnit.setSelected(val);
        rptLst.add(rptUnit);
        rpt.add(rptLst);
        rpt.add("");
        rpt.setClassAll("noborder");
        timeSpecTbl.addRow(rpt);
        
        val = getPrevValue("rptCnt", 0, "1");
        rpt = getLabelTxtRow("rptCnt", "Repeat count:", "" , itemWidth, val);
        rpt.setClassName("repeatOptions");
        rpt.addStyle("display", "none");
        timeSpecTbl.addRow(rpt);
        
        PageTableRow opt = new PageTableRow();
        opt.add();
        
        PageItemString optBtn = new PageItemString("+More start times");
        optBtn.addEvent("onclick", "showByClass('moreTimes',this)");
        optBtn.setClassName("showCmd");
        opt.add(optBtn);
        
        optBtn = new PageItemString("+Repeat options");
        optBtn.addEvent("onclick", "showByClass('repeatOptions',this);");
        optBtn.setClassName("showCmd");
        opt.add(optBtn);
        opt.setClassAll("noborder");
        timeSpecTbl.addRow(opt);
        
        vpage.includeJS("showByClass.js");

        return timeSpecTbl;
    }
    /**
     * Process form parameters that specify time intervals
     * @throws edu.fullerton.jspWebUtils.WebUtilException *
     * @return time intervals specified
     */
    public ArrayList<TimeInterval> getTimesFromForm() throws WebUtilException 
    {
        goterr = false;
        ArrayList<TimeInterval> times = new ArrayList<>();
        
        String[] ourParams =
        {
            "strtTime", "duration", "repeatDur", "repeatUnit", "rptCnt"
        };
        Double[] startGps;
        long duration;
        long repeatDur;
        long repeatUnit;  // converted to seconds
        long rptCnt;

        String[] str = paramMap.get(ourParams[0]);
        startGps = TimeAndDate.getGPSDouble(str);
        if (startGps == null || startGps.length == 0)
        {
            String dateStr = "";
            if (str != null && str.length > 0)
            {
                String it = str[0].trim();
                if (!it.isEmpty())
                {
                    dateStr = it;
                }
            }           
            vpage.add("Error parsing date.[");
            vpage.add(dateStr);
            vpage.add("] You can use the browser back button to correct it.");
            vpage.addBlankLines(2);
            goterr = true;
        }
        else
        {
            duration = getDuration(ourParams[1]);
            if (duration < 0)
            {   // negative duration means time is end time
                for(int i=0;i<startGps.length;i++)
                {
                    startGps[i] += duration;
                }
                duration = -duration;
            }
            repeatDur = getLong(ourParams[2]);
            repeatUnit = getUnit(ourParams[3]);
            rptCnt = getLong(ourParams[4]);
            if (repeatUnit == 0 || rptCnt < 1 || paramMap.get("autoRefresh") != null)
            {
                rptCnt = 1;
            }
            else if (rptCnt > 100)
            {
                vpage.addLine("Maximum number of repeats is 100.  Using 100");
                rptCnt = 100;
            }

            for (int r = 0; r < rptCnt; r++)
            {
                for(int i=0;i<startGps.length;i++)
                {
                    Double strt = startGps[i] + r * repeatDur * repeatUnit;
                    Double stop = strt + duration;
                    TimeInterval ti = new TimeInterval(strt, stop);
                    times.add(ti);
                }
            }
        }
        if (goterr)
        {
            throw new WebUtilException("Error in time interval specification.");
        }
        // validate times as best we can
        long nowGps = TimeAndDate.utc2gps(System.currentTimeMillis()/1000);
        long firstFrameTime = 624011000; // at least what I could find in archive
        for (TimeInterval ti : times)
        {
            if (ti.getStartGps() < firstFrameTime || ti.getStopGps() > nowGps)
            {
                vpage.add("Error parsing start or end time.  At least one is out of the reasonable range");
                vpage.add("] You can use the browser back button to correct it.");
                vpage.addBlankLines(2);
                goterr = true;
            }
        }
        return times;
    }
    
    /**
     * Duration can be specified as Days HH:MM:SS or as a long int any of the fields may be null. So
     * 3600 = 1:0:0 = 60:0 ...
     *
     * @param string user entry to be converted to seconds
     * @return time specified in seconds
     */
    private long getDuration(String paramName) 
    {
        long ret;
        boolean isNegative;
        Pattern inpDays = Pattern.compile("^(\\d+)\\s+(\\d+)\\s*:\\s*(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpHrs = Pattern.compile("^(\\d+)\\s*:\\s*(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpMin = Pattern.compile("^(\\d+)\\s*:\\s*(\\d+)$");
        Pattern inpSec = Pattern.compile("^(\\d+)$");
        int days = 0;
        int hrs = 0;
        int min = 0;
        int sec = 0;
        String[] ina = paramMap.get(paramName);
        String in = (ina == null || ina.length == 0) ? null : ina[0];
        if (in == null || in.isEmpty())
        {
            ret = 0;
            isNegative = false;
        }
        else
        {
            in=in.trim();
            isNegative = false;
            if(in.startsWith("-"))
            {
                isNegative = true;
                in = in.substring(1);
            }
            Matcher m = inpDays.matcher(in);
            if (m.find())
            {
                days = Integer.parseInt(m.group(1));
                hrs = Integer.parseInt(m.group(2));
                min = Integer.parseInt(m.group(3));
                sec = Integer.parseInt(m.group(4));
            }
            else
            {
                m = inpHrs.matcher(in);
                if (m.find())
                {
                    hrs = Integer.parseInt(m.group(1));
                    min = Integer.parseInt(m.group(2));
                    sec = Integer.parseInt(m.group(3));
                }
                else
                {
                    m = inpMin.matcher(in);
                    if (m.find())
                    {
                        min = Integer.parseInt(m.group(1));
                        sec = Integer.parseInt(m.group(2));
                    }
                    else
                    {
                        m = inpSec.matcher(in);
                        if (m.find())
                        {
                            sec = Integer.parseInt(m.group(1));
                        }
                        else
                        {
                            vpage.add("Error parsing duration.[");
                            vpage.add(in);
                            vpage.add("] You can use the browser back button to correct it.");
                            vpage.addBlankLines(2);
                            goterr = true;
                        }
                    }
                }
            }
            ret = days * 3600 * 24 + hrs * 3600 + min * 60 + sec;
        }
        if (isNegative)
        {
            ret = -ret;
        }
        return ret;
    }
    
    /**
     * Common action, format and add instructions for this step
     * @param pf - form to add to
     * @param stepName text of the step name/instructrions
     */
    private void addProcStep(PageForm pf, String stepName) throws WebUtilException
    {
        PageItemString procStep;
        procStep = new PageItemString(stepName);
        procStep.setClassName("processStep");
        pf.add(new PageItemBlanks());
        pf.add(procStep);
        pf.add(new PageItemBlanks());
    }
    private void addListSelector(PageTable specTbl, PageItem aux, String label, String desc,
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
        if (aux != null)
        {
            pil.add(aux);
        }

        pil.add(PageItemList.getListSelector("", frmItmName, options, multipleSelections,
                                             defaultVal, addAny, 0));
        row.add(pil);
        row.add(desc);
        row.setClassAll("noborder");
        specTbl.addRow(row);

    }
    public void setVuser(ViewUser vuser)
    {
        this.vuser = vuser;
    }
    public void setVpage(Page vpage)
    {
        this.vpage = vpage;
    }

    /**
     * Add the table of preselected channels allowing another chance to limit what gets plotted
     * 
     * @param selections
     * @return a formatted list of selects ready to add to the form
     * @throws SQLException
     * @throws WebUtilException
     */
    public PageTable getChannelSelectTable(Set<Integer> selections) throws SQLException, WebUtilException
    {
        PageTable ret;
        Set<ChanInfo> cList = ct.getAsSet(selections);
        ChannelsUI cs = new ChannelsUI(contextPath);
        ret = cs.getSelector(cList, selections);
        return ret;
    }
    private final String[] hdr =
    {
        "", "Name", "Sample<br/>Rate", "Type", "Server", "Data<br/>Type", "CIS"
    };

    private PageTableRow getHdrRow() throws WebUtilException
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

    private PageTableColumn getNumericCol(Float fs) throws WebUtilException
    {
        String str;
        if (fs > 0.9999)
        {
            str = String.format("%1$.0f", fs);
        }
        else
        {
            str = String.format("%1$.3f", fs);
        }
        PageTableColumn fsc = new PageTableColumn(str);
        fsc.setAlign(PageItem.Alignment.RIGHT);
        return fsc;
    }
    private PageItem getChanSelBar(int nSel) throws WebUtilException
    {
        vpage.includeJS("setChkBoxByClass.js");

        PageTable selClrBar = new PageTable();
        PageTableRow selClrRow = new PageTableRow();

        PageFormButton selAll = new PageFormButton("selAll", "Select all", "selall");
        selAll.setType("button");
        selAll.addEvent("onclick", "setSelByClasses('selBox', true, '.trendChoice', 1)");
        selClrRow.add(selAll);

        PageFormButton clrAll = new PageFormButton("selAll", "Clear all", "clrall");
        clrAll.setType("button");
        clrAll.addEvent("onclick", "setSelByClasses('selBox', false, '.trendChoice', 0)");
        selClrRow.add(clrAll);

        PageFormButton selMore = new PageFormButton("selMore", "Select more", "selMore");
        selMore.setType("submit");
        selClrRow.add(selMore);

        selClrRow.setClassAll("noborder");
        selClrBar.addRow(selClrRow);

        return selClrBar;
    }

    /**
     * As part of remembering where we came from, form values are passed back and forth to 
     * select more.  Here we use the previous value or default for the specified key
     * @param key - Parameter name for this field
     * @param idx - Index into value array, 0 if only 1 value allowed
     * @param def - default value if no parameter or parameter is empty
     * @return 
     */
    private String getPrevValue(String key, int idx, String def)
    {
        String ret = def;
        String[] prev = paramMap.get(key);
        if (prev != null && prev.length > idx && !prev[0].isEmpty())
        {
            ret = prev[idx];
        }
        return ret;
    }
}
