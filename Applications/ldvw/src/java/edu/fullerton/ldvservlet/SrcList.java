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
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageFormButton;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelIndex;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.ldvw.ChanSourceData;
import edu.fullerton.viewerplugin.PluginSupport;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.TreeSet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import viewerconfig.ViewerConfig;

/**
 * Query a list of servers and produce a list of available source frames
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SrcList extends HttpServlet
{
    private ViewerConfig viewerConfig;
    private long loadTime;
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
            "edu.fullerton.ldvtables.ChannelTable",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        ServletSupport servSupport = new ServletSupport();
        servSupport.checkDb(tableNames);
        viewerConfig = servSupport.getViewerConfig();
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
    
    * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, WebUtilException
    {
        response.setContentType("text/html;charset=UTF-8");
        
        ServletSupport servletSupport;

        servletSupport = new ServletSupport();
        servletSupport.init(request, viewerConfig, false);

        Page vpage = servletSupport.getVpage();
        vpage.includeJS("showByClass.js");
        vpage.setTitle("Channel source list");
        servletSupport.addStandardHeader("");
        servletSupport.addNavBar();
        String contextPath = request.getContextPath();
        
        try 
        {
            ChannelTable ctbl = new ChannelTable(servletSupport.getDb());
            ChannelIndex cindx = new ChannelIndex(servletSupport.getDb());
            ChanPointerTable cptrs = new ChanPointerTable(servletSupport.getDb());
            ArrayList<ChanSourceData> csdList = new ArrayList<>();
            
            String[] baseIds = request.getParameterValues("baseid");
            int tblNum = 1;
            PageItemList tables = new PageItemList();
            
            if (baseIds != null)
            {
                List<Integer> chanList = new ArrayList<>();

                for(String baseId : baseIds)
                {
                    ChanIndexInfo cii;

                    if (baseId.trim().matches("^\\d+$"))
                    {
                        int bid = Integer.parseInt(baseId);
                        cii = cindx.getInfo(bid);

                        String[] trtype = { "mean" };
                        if (cii.hasRaw())
                        {
                            chanList.addAll( cptrs.getChanList(bid, "raw") );
                        }
                        if (cii.hasRds())
                        {
                            chanList.addAll( cptrs.getChanList(bid, "rds"));
                        }
                        if (cii.hasMtrends())
                        {
                            chanList.addAll( cptrs.getChanList(bid, "minute-trend", trtype));
                        }
                        if (cii.hasStrends())
                        {
                            chanList.addAll( cptrs.getChanList(bid, "second-trend", trtype));
                        }
                        if (cii.hasStatic())
                        {
                            chanList.addAll( cptrs.getChanList(bid, "static"));
                        }
                        tblNum = addChanSource(tables, chanList, ctbl, tblNum, csdList);
                        PageItem plots = makePlots(csdList, cii.getName(), servletSupport.getDb(),
                                                   servletSupport.getVpage(), 
                                                   servletSupport.getVuser(), contextPath);
                        vpage.add(plots);
                        vpage.add(tables);

                    }
                }
                        
            }
            
            servletSupport.showPage(response);
        }
        catch ( LdvTableException | SQLException ex)
        {
            String ermsg = String.format("Error showing channel source data. %1$s - %2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            vpage.add(ermsg);
            servletSupport.showPage(response);
        }
        finally
        {
            servletSupport.close();
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
        try
        {
            processRequest(request, response);
        }
        catch (WebUtilException ex)
        {
            String ermsg = String.format("Error displaying source data %1$s - %2$s", 
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new ServletException(ermsg);
        }
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
        try
        {
            processRequest(request, response);
        }
        catch (WebUtilException ex)
        {
            String ermsg = String.format("Error displaying source data %1$s - %2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new ServletException(ermsg);
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "Get source list for a channel";
    }// </editor-fold>

    

    private int addChanSource(PageItemList tables, List<Integer> chanList, ChannelTable chanTbl, 
                              int tblNum, ArrayList<ChanSourceData> csdList
                               ) throws LdvTableException, WebUtilException
    {
        TreeSet<ChanInfo> channels = new TreeSet<>();
        for ( Integer chanId : chanList)
        {
            ChanInfo ci = chanTbl.getChanInfo(chanId);
            for (ChanInfo ci2 : channels)
            {
                boolean closeEnough = ci.getChanName().contentEquals(ci2.getChanName());
                closeEnough &= ci.getcType().contentEquals(ci2.getcType());
                closeEnough &= ci.getServer().contentEquals(ci2.getServer());
                if (closeEnough)
                {
                    ci = null;
                    break;
                }
            }
            if (ci != null)
            {
                channels.add(ci);
            }

        }
        
        for(ChanInfo ci : channels)
        {
            ChanSourceData csd = new ChanSourceData();
            csd.pullData(ci);
            
            csd.mergeIntervals();
            csd.calcGraphData();
            csdList.add(csd);
            PageItem table = makeTable(csd, tblNum);
            tblNum++;
            tables.add(table);
            String errors = csd.getErrors();
            if (!errors.isEmpty())
            {
                String ermsg = errors.replaceAll("\n", "<br>\n");
                tables.addBlankLines(1);
                tables.add(new PageItemString(ermsg, false));
            }
        }
        return tblNum;
    }

    private PageItem makeTable(ChanSourceData csd, int tblNum) throws WebUtilException
    {
        PageItemList ret;
        ret = new PageItemList();
        String cInfo = csd.getChanInfo().toString();
        ret.add(new PageItemHeader(cInfo, 3));
        ret.addBlankLines(1);
        
        String[] frmTypes = csd.getFrameTypes();
        String frmStr = "";
        for (String s : frmTypes)
        {
            frmStr += frmStr.isEmpty() ? s : (",  " + s);
        }
        
        
        PageTable tbl = new PageTable();
        
        TreeSet<TimeInterval> mergedIntervals = csd.getMergedIntervals();
        long minGps = Long.MAX_VALUE;
        long maxGps = Long.MIN_VALUE;
        long totData = 0;
        int nIntervals = 0;
        PageTableRow row;
        boolean odd = true;
        
        String[] colHdrs = { "Start Gps", "Start UTC", "Stop Gps", "Stop UTC", "len (s)", "HH:MM:SS d"};
        PageTableRow hdr = new PageTableRow(colHdrs, true);
        hdr.setRowType(PageTableRow.RowType.HEAD);
        tbl.addRow(hdr);
        for(TimeInterval ti : mergedIntervals)
        {
            Long strtGps = ti.getStartGps();
            Long stopGps = ti.getStopGps();
            
        
            String strtUtc = TimeAndDate.dateAsUtcString(TimeAndDate.gps2date(strtGps));
            String stopUtc = TimeAndDate.dateAsUtcString(TimeAndDate.gps2date(stopGps));
            minGps = Math.min(minGps, strtGps);
            maxGps = Math.max(maxGps, stopGps);
            long len = stopGps - strtGps;
            totData += len;
            nIntervals++;
            
            row = new PageTableRow();

            row.add(strtGps.toString());
            row.add(strtUtc);
            row.add(stopGps.toString());
            row.add(stopUtc);
            
            PageTableColumn lenCol = new PageTableColumn(String.format("%1$,d", len));
            lenCol.setAlign(PageItem.Alignment.RIGHT);
            row.add(lenCol);
            
            PageTableColumn hrTimeCol = new PageTableColumn(TimeAndDate.hrTime(len, true));
            hrTimeCol.setAlign(PageItem.Alignment.RIGHT);
            row.add(hrTimeCol);
            
            row.setClassName(odd ? "odd" : "even");
            odd = !odd;
            tbl.addRow(row);
        }
        float pct = totData > 0 ? 100.f * totData / (maxGps - minGps) : 0;
        String summary;
        if (nIntervals > 0)
        {
            String ftypeInfo = "Data is in frames of type: " + frmStr;
            ret.add(ftypeInfo);
            ret.addBlankLines(1);
            summary = String.format("Available from %1$s (%2$d) to "
                                    + "%3$s (%4$d), gaps: %5$d, coverage %6$.1f%%",
                                    TimeAndDate.dateAsUtcString(TimeAndDate.gps2date(minGps)), minGps,
                                    TimeAndDate.dateAsUtcString(TimeAndDate.gps2date(maxGps)), maxGps,
                                    nIntervals - 1, pct);
            ret.add(summary);
            ret.addBlankLines(2);
            String tblId = String.format("tbl%1$02d",tblNum);
            String btnId = String.format("btn%1$02d",tblNum);
            String jsCall = String.format("toggleShowById('%1$s','#%2$s')",tblId, btnId);
            PageFormButton optBtn = new PageFormButton("ShowHideBtn");
            optBtn.setText("Show interval table");
            optBtn.addEvent("onclick", jsCall);
            optBtn.setId(btnId);
            optBtn.setType("button");
            optBtn.setClassName("showCmd");
            ret.add(optBtn);
            
            tbl.setClassName("showHide");
            tbl.setId(tblId);
            ret.add(tbl);
            ret.addBlankLines(2);

        }
        else
        {
            ret.add("No source data found."); 
        }
        return ret;
    }

    private PageItem makePlots(ArrayList<ChanSourceData> csdList, String name, Database db, 
                               Page vpage, ViewUser vuser, String contextPath) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new DateAxis("Date/Time (UTC)"));
        plot.setGap(10.0);
        
        
        String baseName="";
        StringBuilder errors = new StringBuilder();
        int plotNum=0;
        Color[] colors = { 
            Color.RED, 
            Color.BLUE, 
            Color.MAGENTA, 
            Color.ORANGE, 
            Color.DARK_GRAY,
            Color.GREEN                                                           
        };
        
        for(ChanSourceData csd : csdList)
        {
            baseName = csd.getChanInfo().getBaseName();
            TimeSeriesCollection mtds = new TimeSeriesCollection();
            String server = csd.getChanInfo().getServer().replace(".caltech.edu", "");
            String legend = String.format("Type: %1$s at %2$s", csd.getChanInfo().getcType(), server);
            TimeSeries ts;
            double[][] data = csd.getGraphData();
            if (data == null)
            {
                data = new double[2][2];
                data[0][0] = TimeAndDate.utc2gps(System.currentTimeMillis() / 1000) - 600;
                data[1][0] = TimeAndDate.utc2gps(System.currentTimeMillis() / 1000) - 599;
                data[0][1] = data[1][1] = 0;
                errors.append("Error getting data for: ").append(legend).append("<br>");
            }
            for(double[] d : data)
            {
                d[1]= d[1] == 0 ? .1 : d[1];
            }
            ts = getTimeSeries(data, legend);
            
            mtds.addSeries(ts);
            XYAreaRenderer renderer=  new XYAreaRenderer(XYAreaRenderer.AREA);
            

            BasicStroke str = new BasicStroke(2);
            LogAxis yAxis = new LogAxis("% Avail");
            XYPlot subplot = new XYPlot(mtds, null, yAxis, renderer);
            plot.add(subplot);
            

        }
        ChartPanel cpnl;
        JFreeChart chart;
        String gtitle = String.format("Available data for %1$s ", baseName);
        
        plot.setOrientation(PlotOrientation.VERTICAL);

        chart = new JFreeChart(gtitle, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        cpnl = new ChartPanel(chart, false, false, false, false, false);
        cpnl.setPreferredSize(new Dimension(720, 720));
        
        PluginSupport psupport = new PluginSupport();
        psupport.setup(db, vpage, vuser);
        int imgId;
        PageItemImage img = null;
        try
        {
            imgId = psupport.saveImageAsPNG(cpnl);
            String url = String.format("%1$s/view?act=getImg&amp;imgId=%2$d", contextPath, imgId);
            
            img = new PageItemImage(url, "availability", baseName);

        }
        catch (SQLException | IOException | NoSuchAlgorithmException ex)
        {
            String ermsg = String.format("Error creating or saving image: %1$s - $2$s",
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            errors.append(ermsg);
            
        }
        if (errors.length() > 0)
        {
            ret.add(errors.toString());
        }
        if (img != null)
        {
            ret.add(img);
        }
        return ret;
    }
    private TimeSeries getTimeSeries(double[][] data, String legend)
    {
        TimeSeries ts;
        ts = new TimeSeries(legend, Millisecond.class);
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        
        for (double[] data1 : data)
        {
            long gps = Math.round(data1[0]);
            long utcms = TimeAndDate.gps2utc(gps) * 1000;
            Date t = new Date(utcms);
            ts.addOrUpdate(new Millisecond(t, utctz), data1[1]);
        }
        return ts;
    }

}
