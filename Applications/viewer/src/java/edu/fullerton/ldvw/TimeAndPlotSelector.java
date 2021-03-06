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
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvplugin.HelpManager;
import edu.fullerton.ldvtables.ChanDataAvailability;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
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
    private boolean goterr;
    private ChannelTable ct;
    
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
     * add a collection of selected channels
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
        
        selections = getSelections(selType);
        
        if (selections.isEmpty())
        {
            vpage.addBlankLines(3);
            vpage.add("No Channels are selected.  Please use back button.");
        }
        else
        {
            ct = new ChannelTable(db);
            int nSel = selections.size();
            vpage.add(String.format("%1$d channel%2$s selected.", nSel, nSel > 1 ? "s are" : " is" ));
            vpage.addBlankLines(2);

            if (nSel > 2)
            {
                vpage.includeJS("setChkBoxByClass.js");
            }

            PageTable selClrBar = new PageTable();
            PageTableRow selClrRow = new PageTableRow();
            PageForm pf = new PageForm();
            pf.setName("chanFilter");
            pf.setMethod("get");
            pf.setAction(getServletPath());
            pf.addHidden("act", "doplot");
            pf.setNoSubmit(true);

            PageFormButton selAll = new PageFormButton("selAll", "Select all", "selall");
            selAll.setType("button");
            selAll.addEvent("onclick", "setChkBoxByClass('selBox',true)");
            selClrRow.add(selAll);

            PageFormButton clrAll = new PageFormButton("selAll", "Clear all", "clrall");
            clrAll.setType("button");
            clrAll.addEvent("onclick", "setChkBoxByClass('selBox', false)");
            selClrRow.add(clrAll);
            
            
            PageFormButton selMore = new PageFormButton("selMore", "Select more", "selMore");
            selMore.setType("submit");
            selClrRow.add(selMore);
            

            selClrRow.setClassAll("noborder");
            selClrBar.addRow(selClrRow);
            pf.add(selClrBar);
            
            pf.add(getPageTable(selections));
            
            if (selections.size() > 10)
            {
                pf.add(selClrBar);
            }
            
            addProcStep(pf,String.format("%1$d. Select time(s)",stepNo));
            stepNo++;
            
            PageTable timeSpecTbl = getTimeSpecTable();

            PageTableRow tsRow = new PageTableRow();
            
            // if all channels are on-line we can auto refresh
            boolean allOnline=true;
            ArrayList<ChanInfo> selectedCInfo = new ArrayList<>();
            
            for (Integer sel : selections)
            {
                ChanInfo ci = ct.getChanInfo(sel);
                selectedCInfo.add(ci);
                boolean isOnline = ci.getcType().equalsIgnoreCase("online");
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
            pltGroup.add(pltGroups);
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

            PluginManager pmanage = new PluginManager(db, vpage, vuser,paramMap);
            pmanage.setContextPath(contextPath);
            pmanage.setServletPath(servletPath);
            pf.add(pmanage.getSelector(selectedCInfo,stepNo));
            
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
        timeSpecTbl.addRow(GUISupport.getTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", 
                                          itemWidth, TimeAndDate.nowAsUtcString(-10 * 60 * 1000),
                                          strtHelpBtn));
        PageTableRow moreTimes;
        moreTimes = getLabelTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", itemWidth, "");
        moreTimes.setClassName("moreTimes");
        moreTimes.addStyle("display", "none");
        timeSpecTbl.addRow(moreTimes);
        
        moreTimes = getLabelTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", itemWidth, "");
        moreTimes.setClassName("moreTimes");
        moreTimes.addStyle("display", "none");
        timeSpecTbl.addRow(moreTimes);

        moreTimes = getLabelTxtRow("strtTime", "Start time:", "(gps or YYYY-MM-DD HH:MM)", itemWidth, "");
        moreTimes.setClassName("moreTimes");
        moreTimes.addStyle("display", "none");
        timeSpecTbl.addRow(moreTimes);
        
        PageItem durHelpBtn = helpManager.getHelpButton("duration");

        timeSpecTbl.addRow(GUISupport.getTxtRow("duration", "Duration", "(day HH:MM:SS or sec)", 
                                                itemWidth, "20",durHelpBtn));

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
        pft.setDefaultValue("0");
        rptLst.add(pft);

        PageFormSelect rptUnit = new PageFormSelect("repeatUnit");
        rptUnit.add(durUnits);
        rptLst.add(rptUnit);
        rpt.add(rptLst);
        rpt.add("");
        rpt.setClassAll("noborder");
        timeSpecTbl.addRow(rpt);
        
        rpt = getLabelTxtRow("rptCnt", "Repeat count:", "" , itemWidth, "1");
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
        long[] startGps;
        long duration;
        long repeatDur;
        long repeatUnit;  // converted to seconds
        long rptCnt;       // they better not expect a larger value that can be stored in an int

        String[] str = paramMap.get(ourParams[0]);
        startGps = TimeAndDate.getGPS(str);
        if (startGps == null)
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
                    long strt = startGps[i] + r * repeatDur * repeatUnit;
                    long stop = strt + duration;
                    TimeInterval ti = new TimeInterval(strt, stop);
                    times.add(ti);
                }
            }
        }
        if (goterr)
        {
            throw new WebUtilException("Error in time interval specification.");
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
    public PageTable getPageTable(Set<Integer> selections) throws SQLException, WebUtilException
    {
        PageTable t = new PageTable();
        Set<ChanInfo> cList = ct.getAsSet(selections);
        ChanDataAvailability cda = new ChanDataAvailability(db);

        int cnt = 1;
        PageTableRow hdr = ct.getHdrRow();
        t.addRow(hdr);
        boolean odd = true;
        for (ChanInfo ci : cList)
        {
            PageTableRow d = new PageTableRow();        // d contains data for all channels
            PageTableRow d1 = new PageTableRow();       // d1 contains availability if we have it
            String className = odd ? "odd" : "even";
            odd = !odd;
            d.setClassName(className);
            d1.setClassName(className);


            Integer myId = ci.getId();
            String selName = String.format("selchan_%1$d", myId);
            PageFormCheckbox selcb = new PageFormCheckbox(selName, "");
            selcb.setClassName("selBox");
            selcb.setChecked(true);
            PageTableColumn selCol = new PageTableColumn(selcb);
            PageTableColumn cnameCol = new PageTableColumn(ci.getChanName());


            String availability = cda.getAvailability(ci.getChanName(), ci.getServer(), "");
            boolean gotAvailability = availability.length() > 6;
            if (gotAvailability)
            {
                selCol.setRowSpan(2);
                cnameCol.setRowSpan(2);
                PageTableColumn availCol = new PageTableColumn(availability);
                availCol.setSpan(hdr.getColumnCount() - 2);
                d1.add(availCol);
            }
            d.add(selCol);
            d.add(cnameCol);

            float fs = ci.getRate();
            d.add(getNumericCol(fs));
            d.add(ci.getcType());
            d.add(ci.getServer());
            d.add(ci.getdType());

            PageItemImageLink infoLink = ct.getCisLink(ci);
            d.add(infoLink);

            t.addRow(d);
            if (gotAvailability)
            {
                t.addRow(d1);
            }
        }
        return t;
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

}
