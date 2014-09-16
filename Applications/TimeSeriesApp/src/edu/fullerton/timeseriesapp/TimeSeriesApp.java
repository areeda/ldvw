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

package edu.fullerton.timeseriesapp;


import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import edu.fullerton.viewerplugin.PluginSupport;
import edu.fullerton.viewerplugin.ScaledAxisNumberFormat;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TimeSeriesApp
{

    private static long startMs;
    // database info

    private Database db;
    private ChannelTable chanTbl;
    private ChanInfo chanInfo;
    // program spec
    private final String version = "0.0.0";
    private final String programName = "SpectrumApp.jar";
    private final int debugLevel = 1;

    // command line parameters
    private boolean logX;
    private boolean logY;
    private boolean dodetrend;

    private Integer outX;
    private Integer outY;
    private Integer startGPS = 0;
    private Integer duration;
    private Integer xTicks;
    private Integer yTicks;

    private Float secPerFFT;
    private Float overlap;
    private Float fmin;
    private Float fmax;

    private String ofileName;

    // input specification
    private String[] channelNames;
    private String server;
    private String cType;               // chan type eg raw, rds, ...


    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int stat;
        startMs = System.currentTimeMillis();
        try
        {
            TimeSeriesApp me = new TimeSeriesApp();

            // decode command line and set parameters
            boolean doit = me.processArgs(args);

            // generate image
            if (doit)
            {
                stat = me.doPlot();
            }
            else
            {
                stat = 10;
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(TimeSeriesApp.class.getName()).log(Level.SEVERE, null, ex);
            stat = 11;
        }
        // timing info
        double elapsedSec = (System.currentTimeMillis() - startMs) / 1000.;

        System.out.format("Run time: %1$.2f sec%n", elapsedSec);
        System.exit(stat);

    }
    private String timeAxis;
    private float lineThickness=2;
    private String outFilename;

    /**
     * Process the command line arguments and set fields
     *
     * @param args the main passed in arguments
     * @return true if we continue false means exit (didn't want to do it here)
     */
    private boolean processArgs(String[] args)
    {
        boolean ret;
        TimeSeriesCommandLine cmd = new TimeSeriesCommandLine();
        if (cmd.parseCommand(args, programName, version))
        {
            channelNames = cmd.getChannelNames();
            server = cmd.getServer();
            cType = cmd.getcType();

            duration = cmd.getDuration() < 1 ? 20 : cmd.getDuration();

            startGPS = cmd.getStartGPS();
            secPerFFT = cmd.getSecPerFFT() <= 0 ? 1 : cmd.getSecPerFFT();

            Float ft = cmd.getFmax();
            fmax = ft == null || ft < 0 ? 0.f : ft;

            ft = cmd.getFmin();
            fmin = ft == null || ft < 0 ? 0.f : ft;

            logX = cmd.isLogX();
            logY = cmd.isLogY();

            ofileName = cmd.getOfileName().isEmpty() ? "/tmp/test.png" : cmd.getOfileName();
            outX = Math.max(1024, cmd.getOutX());
            outY = Math.max(768, cmd.getOutY());


            xTicks = cmd.getxTicks() < 3 ? 7 : cmd.getxTicks();
            yTicks = cmd.getyTicks() < 3 ? 7 : cmd.getyTicks();
            
            timeAxis = cmd.getTimeAxis();
            outFilename = cmd.getOfileName();

            ret = true;
        }
        else
        {
            ret = false;
        }

        return ret;
    }

    private int doPlot() throws SQLException, WebUtilException, LdvTableException, ViewConfigException
    {
        int ret;
        HashSet<Integer> selections = getSelections();
        ArrayList<TimeInterval> times = getTimes();
        ArrayList<ChanDataBuffer> data = getData(selections, times);

        if (data.isEmpty())
        {
            System.err.println("No data to plot.");
            ret = 3;
        }
        else
        {
            makePlot(data, false);
            ret = 0;
        }
        return ret;
    }

    private HashSet<Integer> getSelections() throws SQLException, LdvTableException, ViewConfigException
    {
        HashSet<Integer> ret = new HashSet<>();
        long strt = System.currentTimeMillis();
        getDbTables();

        {
            int n;
            if (channelNames == null || channelNames.length == 0)
            {
                throw new IllegalArgumentException("No Channel specified");
            }
            if ((server == null || server.isEmpty()) && (cType == null || cType.isEmpty()))
            {
                for (String channelName : channelNames)
                {
                    n = chanTbl.getBestMatch(channelName);
                    ret.add(n);
                }
            }
            else
            {
                if (cType == null || cType.isEmpty())
                {
                    cType = "raw";
                }
                for (String channelName : channelNames)
                {
                    TreeSet<ChanInfo> chSet = chanTbl.getAsSet(server, channelName, cType, 10);
                    if (chSet.size() > 1)
                    {
                        System.err.println("Warning: more than one channel matches: " + channelName);
                    }

                    for (ChanInfo ci : chSet)
                    {
                        Integer id = ci.getId();
                        ret.add(id);
                    }
                }
            }
            if (ret.isEmpty())
            {
                for (String channelName : channelNames)
                {
                    System.err.println("Channel requested was not found: " + channelName);
                }
            }
        }
        return ret;

    }

    private ArrayList<TimeInterval> getTimes()
    {

        TimeInterval ti = new TimeInterval(startGPS, startGPS + duration);
        ArrayList<TimeInterval> ret = new ArrayList<>();
        ret.add(ti);
        return ret;
    }

    private ArrayList<ChanDataBuffer> getData(HashSet<Integer> selections, ArrayList<TimeInterval> times) throws WebUtilException
    {

        ArrayList<ChanDataBuffer> bufList;
        try
        {
            bufList = ChanDataBuffer.dataBufFactory(db, selections, times, null, null, false);
            // is there any preprocessing to be done?
            for (ChanDataBuffer dbuf : bufList)
            {
                if (dodetrend)
                {
                    dbuf.detrend();
                }
            }
        }
        catch (Exception ex)
        {
            throw new WebUtilException(ex);
        }

        return bufList;

    }

    /**
     * Connect to the database and create table objects we need
     */
    private void getDbTables() throws LdvTableException, SQLException, ViewConfigException
    {
        if (db == null)
        {
            ViewerConfig vc = new ViewerConfig();
            db = vc.getDb();
            if (db == null)
            {
                throw new LdvTableException("Can't connect to LigoDV-web database");
            }
        }
        if (chanTbl == null)
        {
            chanTbl = new ChannelTable(db);
        }
    }
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws WebUtilException
    {
        int imageId;
        try
        {
            PluginSupport psupport = new PluginSupport();
            String gtitle = psupport.getTitle(dbufs, compact);
            String xAxisLabel="";
            XYSeriesCollection xyds = new XYSeriesCollection();
            TimeSeriesCollection mtds = new TimeSeriesCollection();

            compact = dbufs.size() > 2 ? false : compact;
            for (ChanDataBuffer dbuf : dbufs)
            {
                int npts = dbuf.getDataLength();
                int sum=1;
                if (npts > 2000)
                {
                    sum = npts/2000;
                }
                String legend = psupport.getLegend(dbuf, compact);
                if (timeAxis.equalsIgnoreCase("utc"))
                {
                    TimeSeries ts = psupport.getTimeSeries(dbuf, legend, sum);
                    xAxisLabel="Time (UTC)";
                    mtds.addSeries(ts);
                }
                else
                {
                    boolean isDt=timeAxis.equalsIgnoreCase("dt");
                    XYSeries xys = psupport.addXySeries(dbuf, legend, isDt, sum);
                    xAxisLabel = psupport.getxAxisLabel();
                    xyds.addSeries(xys);
                }
            }
            Double minx, miny, maxx, maxy;
            Double[] rng = new Double[4];

            if (timeAxis.equalsIgnoreCase("utc"))
            {
                PluginSupport.getRangeLimits(mtds, rng);
            }
            else
            {
                int skip=0;
                PluginSupport.getRangeLimits(xyds, rng, skip);
            }
            minx = rng[0];
            miny = rng[1];
            maxx = rng[2];
            maxy = rng[3];

            int exp;
            if (timeAxis.equalsIgnoreCase("utc"))
            {
                exp = PluginSupport.scaleRange(mtds, miny, maxy);
            }
            else
            {
                exp = PluginSupport.scaleRange(xyds, miny, maxy);
            }

            ChartPanel cpnl;
            DefaultXYDataset ds = new DefaultXYDataset();
            JFreeChart chart;
            if (timeAxis.equalsIgnoreCase("utc"))
            {
                chart = ChartFactory.createTimeSeriesChart(gtitle, "Time (UTC)", "Counts", ds, true, true, false);
            }
            else
            {
                chart = ChartFactory.createXYLineChart(gtitle, xAxisLabel, "Counts", ds, PlotOrientation.VERTICAL, true, false, false);
            }
            chart.setBackgroundPaint(Color.WHITE);
            chart.setAntiAlias(true);

            XYPlot plot = (XYPlot) chart.getPlot();
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
            plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

            NumberAxis rangeAxis = new NumberAxis("Counts");
            ScaledAxisNumberFormat sanf = new ScaledAxisNumberFormat();
            sanf.setExp(exp);
            NumberTickUnit tickUnit;
            double plotRange;
            if (maxy != 0 && Math.abs(maxy - miny) < Math.abs(maxy) * 1e-30)
            {
                // this garbage is to get jFreeChart to always put labels on the Y axis
                double dt = Math.abs(miny) / 10;
                double scaledMin = (miny - dt) * Math.pow(10., exp);
                double scaledMax = (maxy + dt) * Math.pow(10., exp);
                rangeAxis.setRange(scaledMin, scaledMax);
                plotRange=scaledMax-scaledMin;
                rangeAxis.setAutoRange(false);
            }
            else
            {
                sanf.setMinMax(miny, maxy);
                plotRange=maxy - miny;
                rangeAxis.setAutoRange(true);
            }
            tickUnit = rangeAxis.getTickUnit();
            double tickSize = tickUnit.getSize();
            int nticks = (int) ((plotRange)/tickSize);
            if (nticks > yTicks)
            {
                double newTickSize = plotRange/yTicks;
                rangeAxis.setTickUnit(new NumberTickUnit(newTickSize));
            }
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

            if (compact)
            {
                chart.removeLegend();
            }
            cpnl = new ChartPanel(chart);
            if (outFilename.isEmpty())
            {
                imageId = psupport.saveImageAsPNG(cpnl);
            }
            else
            {
                imageId = 0;
                psupport.saveImageAsPdfFile(chart, outFilename);
            }

        }
        catch (SQLException | NoSuchAlgorithmException | IOException ex)
        {
            throw new WebUtilException(ex);
        }
        ArrayList<Integer> ret = new ArrayList<>();
        ret.add(imageId);
        return ret;
    }
}
