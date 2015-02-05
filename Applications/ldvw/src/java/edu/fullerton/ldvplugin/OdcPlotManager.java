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
package edu.fullerton.ldvplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageFormSubmit;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemBlanks;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageCoordinateTbl;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.ldvw.TimeAndPlotSelector;
import viewerplugin.ChanDataBuffer;
import viewerplugin.PlotProduct;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ODC Plots are implemented as a stand alone program.  This class wraps that program with 
 * the web gui
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class OdcPlotManager extends ExternalPlotManager implements PlotProduct
{
    
    public OdcPlotManager( Database db, Page vpage, ViewUser vuser)
    {
        super(db,vpage,vuser);
    }

    public int doPlot(String chanName, String server, ArrayList<TimeInterval> times) throws WebUtilException
    {
        File tmpDir = new File("/tmp");
        //@TODO get the path to our programs from config file
        String odcDir = "/usr/local/ldvw/OdcPlot";
        String prog = String.format("java -Xmx1g -jar %s/OdcPlot.jar", odcDir);
        String cmd;
        int imgId=0;
        for(TimeInterval t : times)
        {
            try
            {
                String[] plotSizes = paramMap.get("odcPlotSize");
                if (plotSizes == null || plotSizes.length < 1)
                {
                    plotSizes = paramMap.get("plotSize");
                }
                String geometry = "";
                if (plotSizes != null && plotSizes.length == 1)
                {
                    Pattern gPat = Pattern.compile("(\\d+x\\d+)");
                    Matcher gMat = gPat.matcher(plotSizes[0]);
                    if (gMat.find())
                    {
                        geometry="--geom " + gMat.group(1);
                    }
                }
                if (geometry.isEmpty() && width >0 && height > 0)
                {
                    geometry=String.format("--geom %1$dx%2$d", width, height);
                }
                Long start = t.getStartGps();
                Long stop = t.getStopGps();
                File outFile = File.createTempFile("odc", ".png", tmpDir);
                cmd = String.format("%1$s --server %2$s --chan %3$s --start %4$d --dur %5$d "
                        + "%6$s --outfile %7$s",
                        prog, server, chanName, start, (stop-start), geometry,
                        outFile.getCanonicalPath());
                
                imgId = callProgramSaveOutput(cmd, outFile, prog, chanName);
                PageItemList desc = addIntro(t, chanName, server);
                ImageTable imgTbl2 = new ImageTable(db);
                imgTbl2.addDescription(imgId, desc.getHtml());
                
                String url = String.format("%1$s?act=getImg&imgId=%2$d", servletPath, imgId);
                PageItemImage piImg = new PageItemImage(url, prog, "");

                ImageCoordinateTbl ict = new ImageCoordinateTbl(db);
                ImageCoordinate imgCord = ict.getCoordinate(imgId);
                if (imgCord != null)
                {
                    vpage.includeJS("ShowTimeUnderMouse.js");
                    vpage.addLoadJS(imgCord.getHeadJS());
                    String className = imgCord.getClassName();
                    String imageIdname = String.format("%1$s_%2$d", className, imgId);
                    String timeId = String.format("time_%1$d", imgId);
                    piImg.setClassName(className);
                    piImg.setId(imageIdname);
                    vpage.addBodyJS(imgCord.getInitJS(imageIdname, timeId));
                    vpage.add(piImg);
                    PageItemString timeStr = new PageItemString("Click in image for time under mouse.");
                    timeStr.setId(timeId);
                    vpage.add(timeStr);
                }
                else
                {
                    vpage.add(piImg);
                }
            }
            catch (IOException | WebUtilException | LdvTableException | SQLException ex)
            {
                throw new WebUtilException("Creating OdcPlot: " + ex.getLocalizedMessage());
            }
                    
        }
        return imgId;
    }
    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        this.paramMap = parameterMap;
    }
    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        String enableText = "Generate ODC Plot";
        enableText += nSel > 1 ? "s<br><br>" : "<br><br>";
        
        this.enableKey = enableKey;
        boolean enabled = getPrevValue(enableKey);

        PageFormCheckbox cb = new PageFormCheckbox(enableKey, enableText, enabled);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick", fun);
        ret.add(cb);

        ret.add(new PageItemString("No special options for ODC plots"));
        return ret;
    }
    public static TreeMap<String, String> getOdcPlotChannelMap()
    {
        TreeMap<String, String> it = new TreeMap<>();
        
        it.put("H1:HPI-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM1_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM2_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM3_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ITMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:IMC-ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-HAM2_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-HAM3_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ITMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-TST_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:PSL-ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-BSTST_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC1_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC2_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC3_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC_MASTER_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-OMC_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PR2_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PR3_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PRM_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-QUADTST_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SR2_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SR3_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SRM_ODC_CHANNEL_OUT_DQ", "nds.ligo-wa.caltech.edu");

        it.put("L1:HPI-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ETMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM1_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM2_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM3_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM4_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM5_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM6_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ITMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:IMC-ODC_CHANNEL_OUT_DQ ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ETMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM2_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM3_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM4_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM5_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM6_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ITMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:PSL-ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-BS_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ETMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ETMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ITMX_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ITMY_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC1_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC2_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC3_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC_MASTER_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-OMC_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PR2_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PR3_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PRM_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SR2_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SR3_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SRM_ODC_CHANNEL_OUT_DQ", "nds.ligo-la.caltech.edu");

        // these are pretty much the same channels but sampled at 16 Hz not 32KHz
/* removed the 16Hz channels because of errors in the frame writing
        
        it.put("H1:HPI-BS_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM1_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM2_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-HAM3_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ITMX_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:HPI-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-BS_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-HAM2_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-HAM3_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ITMX_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:ISI-TST_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:PSL-ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-BSTST_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-BS_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC1_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC2_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC3_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-MC_MASTER_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-OMC_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PR2_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PR3_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-PRM_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-QUADTST_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SR2_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SR3_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");
        it.put("H1:SUS-SRM_ODC_CHANNEL_OUTMON", "nds.ligo-wa.caltech.edu");

        it.put("L1:HPI-BS_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ETMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM1_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM2_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM3_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM4_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM5_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-HAM6_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ITMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:HPI-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-BS_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ETMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM2_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM3_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM4_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM5_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-HAM6_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ITMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:ISI-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:PSL-ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-BS_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ETMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ETMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ITMX_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-ITMY_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC1_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC2_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC3_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-MC_MASTER_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-OMC_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PR2_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PR3_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-PRM_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SR2_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SR3_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
        it.put("L1:SUS-SRM_ODC_CHANNEL_OUTMON", "nds.ligo-la.caltech.edu");
*/
        
        return it;
    }
    private PageItemList addIntro(TimeInterval t, String chanName, String server) throws WebUtilException
    {
        PageItemList intro = new PageItemList();
        PageItemString pname = new PageItemString("ODC Plot");
        pname.addStyle("font-weight", "bold");
        intro.add(pname);
        intro.addBlankLines(1);
            intro.add(chanName + " from " + server + ". ");
            String timeDescription = t.getTimeDescription();
            intro.add(timeDescription);
            intro.addBlankLines(1);

        vpage.add(intro);
        return intro;
    }
    /**
     * Create a PageForm that contains all the items needed to specify one or more ODC Plots
     * @param vpage
     * @return object to be added to the page
     * @throws WebUtilException probably a programming bug in this routine
     * @throws java.sql.SQLException
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public PageForm getSelector(Page vpage) throws WebUtilException, SQLException, LdvTableException
    {
        String[] plotSize =
        {
            "Tiny (400x300)", "Small(640x480)", "Medium (1024x550)", "Large (1280x960)",
            "Larger (1920x1080)", "Huge (2200x1400)"
        };
        PageForm pf = new PageForm();

        pf.setName("specialPlot");
        pf.setMethod("get");
        pf.setAction(servletPath);
        pf.addHidden("act", "specialPlot");
        pf.setNoSubmit(true);

        TimeAndPlotSelector tsp = new TimeAndPlotSelector(db, vpage, vuser);
        tsp.setContextPath(contextPath);
        tsp.setParamMap(paramMap);
        tsp.setVpage(vpage);
        PageTable selBar = tsp.getTimeSpecTable();

        // Channel selector
        PageTableRow selRow = new PageTableRow();
        PageItemString chlbl = new PageItemString("Channel(s):");
        chlbl.setAlign(PageItem.Alignment.RIGHT);
        selRow.add(chlbl);

        TreeSet<String> cSelSet = new TreeSet<>(OdcPlotManager.getOdcPlotChannelMap().keySet());
        cSelSet.add("All");

        PageItem cListSel = PageItemList.getListSelector("", "chanName",
                                                         cSelSet, true, "All", false, 14);
        
        selRow.add(cListSel);
        selRow.add();
        selRow.setClassAll("noborder");
        selBar.addRow(selRow);

        // Plot size
        selRow = new PageTableRow();
        PageItemString psizLbl = new PageItemString("Plot size: ");
        psizLbl.setAlign(PageItem.Alignment.RIGHT);
        selRow.add(psizLbl);

        PageFormSelect psizSel = new PageFormSelect("odcPlotSize", plotSize);
        psizSel.setSelected(plotSize[2]);

        selRow.add(psizSel);
        selRow.add();

        selRow.setClassAll("noborder");
        selBar.addRow(selRow);
        pf.add(selBar);
        pf.add(new PageItemBlanks());

        PageFormSubmit odcBtn = new PageFormSubmit("odcplot", "OdcPlot");
        odcBtn.setId("continueBtnAux");
        pf.add(odcBtn);

        return pf;
    }

    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) throws WebUtilException
    {
        ArrayList<Integer> ret = new ArrayList<>();
        for(ChanDataBuffer buf : dbuf)
        {
            try
            {
                ArrayList<TimeInterval> tis = new ArrayList<>();
                tis.add(buf.getTimeInterval());
                int imgId = doPlot(buf.getChanInfo().getChanName(), buf.getChanInfo().getServer(), tis);
                if (imgId > 0)
                {
                    ret.add(imgId);
                }
            }
            catch (LdvTableException ex)
            {
                throw new WebUtilException("Making ODC plot: ", ex);
            }
        }
        return ret;
    }

    @Override
    public boolean isStackable()
    {
        return false;
    }

    @Override
    public boolean isPaired()
    {
        return false;
    }
    
    @Override
    public boolean needsImageDescriptor()
    {
        return false;
    }

    @Override
    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean needsDataXfer()
    {
        return false;
    }

    @Override
    public void setDispFormat(String dispFormat)
    {
        
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }

    @Override
    public String getProductName()
    {
        return "ODC Plot";
    }
}
