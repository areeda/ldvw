/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ViewUser;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.SimpleTimeZone;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Plot a simple time series
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class TsPlot extends PluginSupport implements PlotProduct
{
    private boolean addLinFit;
    private String timeAxis;
    private String xAxisLabel="Time";
    private int lineThickness=2;
    private boolean wantStacked;

    public TsPlot()
    {
    }

    public TsPlot(Database db, Page vpage, ViewUser vuser)
    {
        
    }
    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws WebUtilException
    {
        int imageId;
        try
        {
            if (parameterMap.containsKey("ts_newplt"))
            {
                imageId = makeAddPlotFiles(dbufs, compact);
            }
            else
            {
                String gtitle = getTitle(dbufs,compact);

                XYSeriesCollection xyds = new XYSeriesCollection();
                TimeSeriesCollection mtds = new TimeSeriesCollection();

                compact = dbufs.size() > 2 ? false : compact;
                for (ChanDataBuffer dbuf : dbufs)
                {
                    if (timeAxis.equalsIgnoreCase("utc"))
                    {
                        addTimeSeries(dbuf, compact, mtds);
                    }
                    else
                    {
                        addXySeries(dbuf,compact,xyds);
                    }
                }
                Double minx,miny,maxx,maxy;
                Double[] rng = new Double[4];

                if (timeAxis.equalsIgnoreCase("utc"))
                {
                    PluginSupport.getRangeLimits(mtds, rng);
                }
                else
                {
                    PluginSupport.getRangeLimits(xyds, rng, 0);
                }
                minx = rng[0];
                miny = rng[1];
                maxx = rng[2];
                maxy = rng[3];

                int exp;
                if (timeAxis.equalsIgnoreCase("utc"))
                {
                    exp = PluginSupport.scaleRange(mtds,miny,maxy);
                }
                else
                {
                    exp = PluginSupport.scaleRange(xyds,miny,maxy);
                }

                ChartPanel cpnl;
                DefaultXYDataset ds = new DefaultXYDataset();
                JFreeChart chart;
                if (timeAxis.equalsIgnoreCase("utc"))
                {
                    chart = ChartFactory.createTimeSeriesChart(gtitle, "Time (UTC)", "Amplitude (Counts)", ds, true, true, false);
                }
                else
                {
                    chart = ChartFactory.createXYLineChart(gtitle, xAxisLabel, "Amplitude (Counts)", ds, PlotOrientation.VERTICAL, true, false, false);
                }

                XYPlot plot = (XYPlot) chart.getPlot();
                NumberAxis rangeAxis = new NumberAxis("Amplitude (Counts)");
                ScaledAxisNumberFormat sanf = new ScaledAxisNumberFormat();
                sanf.setExp(exp);
                if ( maxy != 0 && Math.abs(maxy-miny) <= Math.abs(maxy) * 1e-25)
                {
                    // this garbage is to get jFreeChart to put labels on the Y axis
                    double dt = Math.abs(miny)/10;
                    double scaledMin = (miny - dt) * Math.pow(10., exp);
                    double scaledMax = (maxy + dt) * Math.pow(10., exp);
                    rangeAxis.setRange(scaledMin, scaledMax);
                    NumberTickUnit unit = new NumberTickUnit((scaledMax  - scaledMin)/10.);
                    rangeAxis.setTickUnit(unit);
                    rangeAxis.setAutoRange(false);
                }
//                else
//                {
//                    sanf.setMinMax(miny, maxy);
//                    rangeAxis.setRange(miny, maxy);
//                    NumberTickUnit unit = new NumberTickUnit((maxy  - miny)/6.);
//                    rangeAxis.setTickUnit(unit);
//                    rangeAxis.setAutoRange(false);
//                }
                rangeAxis.setNumberFormatOverride(sanf);
                rangeAxis.setAutoRangeIncludesZero(false);
                plot.setRangeAxis(rangeAxis);

                if (timeAxis.equalsIgnoreCase("utc"))
                {
                    plot.setDataset(0, mtds);

                }
                else
                {
                    plot.setDataset(0, xyds);
                }
                // Set the line thickness
                XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
                BasicStroke str = new BasicStroke(lineThickness);
                int n = plot.getSeriesCount();
                for (int i = 0; i < n; i++)
                {
                    r.setSeriesStroke(i, str);
                }
                plot.setBackgroundPaint(Color.WHITE);
                // add 
                plot.setDomainGridlinesVisible(true);
                plot.setDomainGridlinePaint(Color.BLACK);
                plot.setRangeGridlinesVisible(true);
                plot.setRangeGridlinePaint(Color.BLACK);

                r.setBaseFillPaint(Color.WHITE);
                if (compact)
                {
                    chart.removeLegend();
                }

                chart.setBackgroundPaint(Color.WHITE);

                cpnl = new ChartPanel(chart);
                imageId = saveImageAsPNG(cpnl);
            }
        }
        catch (LdvTableException | NoSuchAlgorithmException | SQLException | IOException  ex)
        {
            throw new WebUtilException("Making time series plot: ", ex);
        }
        ArrayList<Integer> ret = new ArrayList<Integer>();
        ret.add (imageId);
        return ret;
    }

    /**
     * we can take all datasets at once, so return what the user wants
     *
     * @return flag saying so
     */
    @Override
    public boolean isStackable()
    {
        return true;
    }

    @Override
    public String getProductName()
    {
        return "Time series plot";
    }

    @Override
    public void setDispFormat(String dispFormat)
    {
        if (dispFormat.equalsIgnoreCase("Stacked"))
        {
            wantStacked = true;
        }

    }
    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        this.parameterMap = parameterMap;
        String[] t;
        t = parameterMap.get("tsLinFit");
        if (t != null)
        {
            addLinFit = true;
        }
        t = parameterMap.get("ts_timeaxis");
        if (t == null || t[0].equalsIgnoreCase("utc"))
        {
            timeAxis="utc";
        }
        else if (t[0].equalsIgnoreCase("gps"))
        {
            timeAxis="gps";
        }
        else
        {
            timeAxis="dt";
        }
        
        t = parameterMap.get("ts_linethickness");
        if (t == null || ! t[0].trim().matches("^\\d+$"))
        {
            lineThickness = 2;
        }
        else
        {
            lineThickness = Integer.parseInt(t[0]);
        }
    }

    private TimeSeries LinFit(TimeSeries ts)
    {
        TimeSeries ret = new TimeSeries("lin fit",Millisecond.class);
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        int n = ts.getItemCount();
        double[] x = new double[n];
        double[] y = new double[n];
        TimeSeriesDataItem it;
        for(int i=0;i<n;i++)
        {
            it = ts.getDataItem(i);
            x[i]=it.getPeriod().getFirstMillisecond() + 0.;
            y[i] = it.getValue().doubleValue();
        }
        LinearRegression lr=new LinearRegression(x,y);
       double b = lr.getIntercept();
       double m = lr.getSlope();
       double fit,t;
        for (int i = 0; i < n; i++)
        {
            it = ts.getDataItem(i);
            t = it.getPeriod().getFirstMillisecond() + 0.;
            fit = m * t + b;
            
            ret.addOrUpdate(it.getPeriod(), fit);
        }
        
        return ret;
    }

    private XYSeries LinFit(XYSeries ts)
    {
        XYSeries ret = new XYSeries("lin fit",false);
        
        int n = ts.getItemCount();
        double[] x = new double[n];
        double[] y = new double[n];
        XYDataItem it;
        for (int i = 0; i < n; i++)
        {
            it = ts.getDataItem(i);
            x[i] = it.getXValue();
            y[i] = it.getYValue();
        }
        LinearRegression lr = new LinearRegression(x, y);
        double b = lr.getIntercept();
        double m = lr.getSlope();
        double fit, t;
        for (int i = 0; i < n; i++)
        {
            
            it = ts.getDataItem(i);
            t = it.getXValue();
            fit = m * t + b;

            ret.add(t,fit);
        }

        return ret;
    }

    /**
     * Generate the UI object to select parameters for this plot
     * 
     * @param enableKey form item name to select this plot
     * @param nSel number of channels selected
     * @param multDisp
     * @return
     * @throws WebUtilException 
     */
    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        String[] timeAxisOptions = { "&Delta;t", "UTC", "GPS" };
        String[] lineThicknessOptions = { "1", "2", "3", "4" };
        
        this.enableKey = enableKey;
        
        PageItemList ret = new PageItemList();
        String enableText = "Generate time series plot";
        enableText += nSel > 1 ? "s" : "";
        boolean selected = getPrevValue(enableKey);
        PageFormCheckbox cb =new PageFormCheckbox(enableKey, enableText, selected);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick" ,fun);
        ret.add(cb);
        ret.addBlankLines(1);
        ret.add("Set apprpriate parameters.");
        ret.addBlankLines(1);
        ret.addBlankLines(1);
        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        
        PageTableRow ptr;

        selected = getPrevValue("tsLinFit");
        PageFormCheckbox linfit = new PageFormCheckbox("tsLinFit", "Add linear fit", selected);
        ptr = GUISupport.getObjRow(linfit, "", "");
        product.addRow(ptr);
        
        selected = getPrevValue("doDetrend");
        PageFormCheckbox detrend = new PageFormCheckbox("doDetrend", "Detrend", selected);
        ptr = GUISupport.getObjRow(detrend, "", "");
        product.addRow(ptr);
        
        PageFormSelect timeAxisSelector = new PageFormSelect("ts_timeaxis", timeAxisOptions);
        String val = getPrevValue("ts_timeaxis", 0, timeAxisOptions[0]);
        timeAxisSelector.setSelected(val);
        timeAxisSelector.setEscapeOptions(false);
        ptr = GUISupport.getObjRow(timeAxisSelector, "Time axis" , "");
        product.addRow(ptr);
        
        PageFormSelect lineThicknessSelector = new PageFormSelect("ts_linethickness", lineThicknessOptions);
        val = getPrevValue("ts_linethickness", 0, "2");
        lineThicknessSelector.setSelected(val);
        ptr = GUISupport.getObjRow(lineThicknessSelector, "Line thickness: ", "");
        product.addRow(ptr);
        
        ret.add(product);
        ret.addBlankLines(1);

        return ret;
    }

    @Override
    public boolean needsImageDescriptor()
    {
        return true;
    }

    private void addTimeSeries(ChanDataBuffer dbuf, boolean compact,  TimeSeriesCollection mtds) throws LdvTableException
    {
        String legend = getLegend(dbuf, compact);
        TimeSeries ts;
        ts = new TimeSeries(legend, Millisecond.class);
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");

        float rate = dbuf.getChanInfo().getRate();
        double msPerSample = 1000 / rate;
        long startMs = TimeAndDate.gps2utc(dbuf.getTimeInterval().getStartGps()) * 1000;
        double[] data = dbuf.getDblData();
        for (int i = 0; i < dbuf.getDataLength(); i++)
        {
            long curMs = Math.round(msPerSample * i + startMs);
            Date t = new Date(curMs);
            ts.addOrUpdate(new Millisecond(t, utctz), data[i]);
            if (msPerSample >= 1000)
            {
                // this plots trend data as stair steps
                long endMs = Math.round(curMs + msPerSample - 1);
                Date t1 = new Date(endMs);
                ts.addOrUpdate(new Millisecond(t1, utctz), data[i]);
            }
        }

        mtds.addSeries(ts);
        if (addLinFit)
        {
            TimeSeries linTs = LinFit(ts);
            mtds.addSeries(linTs);
        }

    }

    private void addXySeries(ChanDataBuffer dbuf, boolean compact, XYSeriesCollection xyds) throws LdvTableException
    {
        String legend = getLegend(dbuf, compact);
        XYSeries xys = new XYSeries(legend,false);
        
        float rate = dbuf.getChanInfo().getRate();
        
        long startSec = dbuf.getTimeInterval().getStartGps();
        long durSec  = dbuf.getTimeInterval().getStopGps() - dbuf.getTimeInterval().getStartGps();
        double scale = 1;
        double t0 = startSec;
        xAxisLabel = "GPS time";
        if (timeAxis.equalsIgnoreCase("dt"))
        {
            t0 = 0;
            // put dt plots into reasonable units
            if (durSec < 1800)
            {
                scale = 1.;
                xAxisLabel = "t (sec)";
            }
            else if (durSec < 3 * 3600)
            {
                scale = 1/60.;
                xAxisLabel = "t (min)";
            }
            else if (durSec < 48 * 3600)
            {
                scale = 1/3600.;
                xAxisLabel = "t (hrs)";
            }
            else
            {
                scale = 1.0 / (24 * 3600);
                xAxisLabel = "t (days)";
            }
            
        }
        
        double[] data = dbuf.getDblData();
        for (int i = 0; i < dbuf.getDataLength(); i++)
        {
            double x =  (i / rate + t0) * scale; // handle gps vs dt and dt units
            double y = data[i];
            xys.add(x, y);
        }
        
        xyds.addSeries(xys);
        if (addLinFit)
        {
            XYSeries linTs = LinFit(xys);
            xyds.addSeries(linTs);
        }
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }

    
    private int makeAddPlotFiles(ArrayList<ChanDataBuffer> dbufs, boolean compact)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setChanList(List<BaseChanSelection> baseChans)
    {
        // we don't need this for our plot, but it's part of the interface
    }
}
