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

package crossspectrumapp;

import com.areeda.jaDatabaseSupport.Database;
import com.mathworks.toolbox.javabuilder.MWException;
import com.mathworks.toolbox.javabuilder.MWNumericArray;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.matiface.CrossSpectrum;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import java.io.FileOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CrossSpectrumApp
{
    private CrossSpectrumCommandLine cscl;
    private CrossSpectrumApp csa;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        CrossSpectrumApp csa = new CrossSpectrumApp();
        csa.go(args);
    }
    private Database db;
    private ChannelTable chanTbl;
    private String title;
    private long dataTransferEnd;

    private void go(String[] args)
    {
        long startMs = System.currentTimeMillis();
        try
        {
            cscl = new CrossSpectrumCommandLine();
            if (cscl.parseCommand(args, "CrossSpectrumApp", "0.0.0"))
            {
                doPlot();
            }
                
        }
        catch (Exception ex)
        {
            System.err.format("Error generating Cross Spectrum Plot: %1$s - %2$s \n",
                              ex.getClass().getSimpleName(), ex.getLocalizedMessage());
        }
        float elapsedTimeSeconds = (System.currentTimeMillis() - startMs)/1000.f;
        System.out.format("Elapsed time: %1$.1fs\n", elapsedTimeSeconds);
    }
    private void doPlot() throws SQLException, LdvTableException, ViewConfigException, 
                                 WebUtilException
    {
        HashSet<Integer> selections = getSelections();
        if (selections.size() < 2)
        {
            System.err.println("We need 2 channels to calculate Cross Spectrum.");
        }
        else
        {
            ArrayList<TimeInterval> times = getTimes();
            ArrayList<ChanDataBuffer> data = getData(selections, times);
            dataTransferEnd = System.currentTimeMillis();
            if (data.size() != 2)
            {
                System.err.println("We need 2 channels to calculate Cross Spectrum.");
            }
            else
            {
                float fftlen = cscl.getSecPerFFT();
                float ovlap = cscl.getOverlap();
                double[] power = null;
                double[] phase = null;
                double[] frequency = null;
                
                CrossSpectrum crossSpectrum = null;
                try
                {
                    crossSpectrum = new CrossSpectrum();
                    Object[] results = crossSpectrum.crossSpectrum(3, data.get(0).getData(), 
                                       data.get(0).getChanInfo().getRate(), 
                                       data.get(1).getData(), data.get(1).getChanInfo().getRate(), 
                                       fftlen, ovlap);
                    power = ((MWNumericArray) results[0]).getDoubleData();
                    phase = ((MWNumericArray) results[1]).getDoubleData();
                    frequency = ((MWNumericArray) results[2]).getDoubleData();
                }
                catch (MWException ex)
                {
                    Logger.getLogger(CrossSpectrumApp.class.getName()).log(Level.SEVERE, null, ex);
                }
                finally
                {
                    // release native resources (MCR stuff) that were allocated by object creation
                    if (crossSpectrum != null)
                    {
                        crossSpectrum.dispose();
                    }

                }
                if (power != null && phase != null && frequency != null)
                {
                    ChanInfo ci1 = data.get(0).getChanInfo();
                    ChanInfo ci2 = data.get(1).getChanInfo();
                    String subTitle1 = String.format("%1$s (%2$.0fHz) vs. %3$s (%4$.0f)", 
                                          ci1.getChanName(), ci1.getRate(),
                                          ci2.getChanName(), ci2.getRate());
                    long startGps = times.get(0).getStartGps();
                    String startStr = TimeAndDate.gpsAsUtcString(startGps);
                    String subTitle2 = String.format("%1$s (%2$d) t=%3$ds",startStr,startGps, 
                                          times.get(0).getStopGps()-times.get(0).getStartGps());
                    
                    plotData(frequency, power, phase, "Cross spectrum", subTitle1, subTitle2);
                }
            }
        }
    }
    
    private HashSet<Integer> getSelections() throws SQLException, LdvTableException, ViewConfigException
    {
        HashSet<Integer> ret = new HashSet<>();
        long strt = System.currentTimeMillis();
        getDbTables();

        {
            int n;
            String[] channelNames = cscl.getChannelNames();
            if (channelNames == null || channelNames.length == 0)
            {
                throw new IllegalArgumentException("No Channel specified");
            }
            if (channelNames.length != 2)
            {
                throw new IllegalArgumentException("2 and only 2 channels need to be specified.");
            }
            String server = cscl.getServer();
            String cType = cscl.getcType();
            
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

    private ArrayList<TimeInterval> getTimes()
    {
        Integer startGPS = cscl.getStartGPS();
        Integer duration = cscl.getDuration();
        
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
                if (cscl.isDetrend())
                {
                    dbuf.detrend();
                }
            }
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException(ex);
        }
        return bufList;
    }

    private void plotData(double[] frequency, double[] power, double[] phase, String title, 
                          String subTitleText1, String subTitleText2) throws WebUtilException
    {
        XYSeries asdSeries = new XYSeries("asd");
        XYSeries phiSeries = new XYSeries("phase");
        
        for(int i=1;i<frequency.length; i++)
        {
            double f = frequency[i];
            double pwr = power[i];
            double phi = phase[i];
            asdSeries.add(f, pwr);
            phiSeries.add(f, phi);
        }
        
        XYSeriesCollection asdCollection = new XYSeriesCollection();
        asdCollection.addSeries(asdSeries);
        XYSeriesCollection phiCollection = new XYSeriesCollection();
        phiCollection.addSeries(phiSeries);
        
        // create the chart
        XYItemRenderer asdRenderer=  new StandardXYItemRenderer();
        LogAxis asdAxis = new LogAxis("ASD counts/ \u221AHz");
        XYPlot asdSubplot = new XYPlot(asdCollection, null, asdAxis, asdRenderer);
        asdSubplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        XYItemRenderer phiRenderer=  new StandardXYItemRenderer();
        NumberAxis phiAxis = new NumberAxis("Phase degrees");
        XYPlot phiSubplot = new XYPlot(phiCollection, null, phiAxis, phiRenderer);
        asdSubplot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        
        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new LogAxis("Frequency (Hz)"));
        plot.setGap(10.0);

        // add the subplots...
        plot.add(asdSubplot, 2);
        plot.add(phiSubplot, 1);
        plot.setOrientation(PlotOrientation.VERTICAL);
        
        JFreeChart chart = new JFreeChart(title,
                       JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        Title subTitle = new TextTitle(subTitleText1);
        chart.addSubtitle(subTitle);
        subTitle = new TextTitle(subTitleText2);
        chart.addSubtitle(subTitle);
        ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
        panel.setPreferredSize(new java.awt.Dimension(cscl.getOutX(), cscl.getOutY()));

        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(cscl.getOfileName());
            ChartUtilities.writeChartAsPNG(fos, chart, cscl.getOutX(), cscl.getOutY());
            fos.close();
            fos = null;
        }
        catch (Exception ex)
        {
            throw new WebUtilException("Saving image: " + ex.getClass() + " - " + ex.getLocalizedMessage());
        }
        finally
        {
            try
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
            catch (Exception ex)
            {
                throw new WebUtilException("Saving image: " + ex.getClass() + " - " + ex.getLocalizedMessage());
            }
        }



    }

}
