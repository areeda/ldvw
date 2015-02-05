/*
 * Copyright (C) 2012-2014 Joseph Areeda <joe@areeda.com>
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
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.SpectrumCalc.Scaling;
import edu.fullerton.viewerplugin.WindowGen.Window;
import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Generate a spectrum from a time series
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class SpectrumPlot extends PluginSupport implements PlotProduct
{

    private boolean wantStacked = false;
    private float secperfft = 1.0f;
    private float overlap = 0.5f;
    private float fmin,fmax, fsMax;

    
    private Window window = Window.NONE;
    private Scaling pwrScale = Scaling.ASD;
    private boolean doDetrend = true;
    private boolean logXaxis = false;
    private boolean logYaxis = false;
    private double smallestX = 1e-3;
    private double smallestY = 1e-10;
    private int lineThickness;
    private int nfft;

    public SpectrumPlot()
    {
    }

    public SpectrumPlot(Database db, Page vpage, ViewUser vuser)
    {
        
    }
    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws WebUtilException
    {
        ArrayList<Integer> ret = new ArrayList<>();
        if (parameterMap.containsKey("sp_newplt"))
        {
            try
            {
                ret = makeAddPlotFiles(dbufs, compact);
            }
            catch (LdvTableException ex)
            {
                throw new WebUtilException("Making spectrum plot: ", ex);
            }
        }
        else
        {
            int imageId;
            ChartPanel cpnl = getPanel(dbufs,compact);
            try
            {
                imageId = saveImageAsPNG(cpnl);
                ret.add(imageId);
            }
            catch (IOException | NoSuchAlgorithmException | SQLException ex)
            {
                throw new WebUtilException("Making spectrum plot: ", ex);
            }
        }
        
        
        return ret;

    }
    public void makePlotFile(ArrayList<ChanDataBuffer> dbufs, boolean compact, String fname) throws WebUtilException
    {
        ChartPanel cpnl = getPanel(dbufs, compact);
        try
        {
            saveImageAsPNGFile(cpnl,fname);
        }
        catch (WebUtilException ex)
        {
            throw new WebUtilException("Creating spectrum plot: " + ex.getClass().getSimpleName()
                                       + ": " + ex.getLocalizedMessage());
        }
    }
    private ChartPanel getPanel(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws WebUtilException
    {
        ChartPanel ret = null;
        try
        {
            float tfsMax = 0;
            for (ChanDataBuffer buf : dbufs)
            {
                ChanInfo ci = buf.getChanInfo();
                float fs = ci.getRate();
                tfsMax = Math.max(fs, tfsMax);
            }
            setFsMax(tfsMax);
            String gtitle = getTitle(dbufs, compact);
            int nbuf = dbufs.size();
            XYSeries[] xys = new XYSeries[nbuf];
            XYSeriesCollection mtds = new XYSeriesCollection();

            int cnum = 0;
            compact = dbufs.size() > 2 ? false : compact;
            float bw = 1.f;
            for (ChanDataBuffer dbuf : dbufs)
            {
                String legend = getLegend(dbuf, compact);

                xys[cnum] = new XYSeries(legend);

                bw = calcSpectrum(xys[cnum], dbuf);

                mtds.addSeries(xys[cnum]);
            }
            
            DefaultXYDataset ds = new DefaultXYDataset();
            String yLabel = pwrScale.toString();
            DecimalFormat dform=new DecimalFormat("0.0###");
            String xLabel;
            xLabel = String.format("Frequency Hz - (bw: %1$s, #fft: %2$,d, s/fft: %3$.2f, ov: %4$.2f)",
                                   dform.format(bw), nfft,secperfft, overlap);
            
            
            Double minx, miny, maxx, maxy;
            Double[] rng = new Double[4];
            if (fmin <= 0)
            {
                fmin = bw;
            }
            float searchFmax = fmax;
            if (fmax <= 0 || fmax == Float.MAX_VALUE)
            {
                fmax = tfsMax/2;
                searchFmax = tfsMax/2 * 0.8f;
            }
            PluginSupport.getRangeLimits(mtds, rng,2,fmin,searchFmax);
            minx = rng[0];
            miny = rng[1];
            maxx = rng[2];
            maxy = rng[3];
            
            findSmallest(mtds);
            int exp;
            if ( maxy==0. && miny == 0.)
            {
                miny = -1.;
                exp = 0;
                logYaxis = false;
            }
            else
            {
                miny = miny > 0 ? miny : smallestY;
                maxy = maxy > 0 ? maxy : miny*10;
                exp = PluginSupport.scaleRange(mtds, miny, maxy);
                if (!logYaxis)
                {
                    yLabel += " x 1e-" + Integer.toString(exp); 
                }
            }    
            JFreeChart chart = ChartFactory.createXYLineChart(gtitle, xLabel, yLabel, ds, PlotOrientation.VERTICAL, true, false, false);
            XYPlot plot = (XYPlot) chart.getPlot();
            if (logYaxis)
            {
                LogAxis rangeAxis = new LogAxis(yLabel);
                double smallest = miny * Math.pow(10,exp);
                rangeAxis.setSmallestValue(smallest);
                rangeAxis.setMinorTickCount(9);
                
                LogAxisNumberFormat lanf = new LogAxisNumberFormat();
                lanf.setExp(exp);
                
                rangeAxis.setNumberFormatOverride(lanf);
                rangeAxis.setRange(smallest, maxy*Math.pow(10,exp));
                rangeAxis.setStandardTickUnits(LogAxis.createLogTickUnits(Locale.US));
                plot.setRangeAxis(rangeAxis);
                plot.setRangeGridlinesVisible(true);
                plot.setRangeGridlinePaint(Color.BLACK);
            }
            if (logXaxis)
            {
                LogAxis domainAxis = new LogAxis(xLabel);
                domainAxis.setBase(2);
                domainAxis.setMinorTickCount(9);
                domainAxis.setMinorTickMarksVisible(true);
                domainAxis.setSmallestValue(smallestX);
                domainAxis.setNumberFormatOverride(new LogAxisNumberFormat());
                //domainAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
                plot.setDomainAxis(domainAxis);
                plot.setDomainGridlinesVisible(true);
                plot.setDomainGridlinePaint(Color.BLACK);
            }
            ValueAxis domainAxis = plot.getDomainAxis();
            if (fmin > Float.MIN_VALUE)
            {
                domainAxis.setLowerBound(fmin);
            }
            if (fmax != Float.MAX_VALUE)
            {
                domainAxis.setUpperBound(fmax);
            }
            plot.setDomainAxis(domainAxis);
            plot.setDataset(0, mtds);
            plot.setDomainGridlinePaint(Color.DARK_GRAY);
            plot.setRangeGridlinePaint(Color.DARK_GRAY);

            // Set the line thickness
            XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
            BasicStroke str = new BasicStroke(lineThickness);
            int n = plot.getSeriesCount();
            for (int i = 0; i < n; i++)
            {
                r.setSeriesStroke(i, str);
            }
            plot.setBackgroundPaint(Color.WHITE);
            if (compact)
            {
                chart.removeLegend();
            }
            ret = new ChartPanel(chart);
        }
        catch (Exception ex)
        {
            throw new WebUtilException("Creating spectrum plot" + ex.getLocalizedMessage());
        }
        return ret;

    }

    @Override
    public boolean isStackable()
    {
        return true;
    }

    @Override
    public boolean isPaired()
    {
        return false;
    }

    @Override
    public String getProductName()
    {
        String name = "Spectrum";
        return name;
    }

    @Override
    public void setDispFormat(String dispFormat)
    {
        if (dispFormat.equalsIgnoreCase("Stacked"))
        {
            wantStacked = true;
        }
    }
    public double[][] calcSpectrum(ChanDataBuffer dbuf) throws LdvTableException
    {
        
        long dlen = dbuf.getDataLength();
        float rate = dbuf.getChanInfo().getRate();

        int flen = Math.round(rate * secperfft);
        int ov = Math.round(rate * overlap);
        //@todo verify all this will work

        float[] data = dbuf.getData();

        DoubleFFT_1D fft = new DoubleFFT_1D(flen);
        double[] fftd = new double[flen * 2];

        double[] result = new double[flen / 2 + 1];
        for (int i = 0; i < flen / 2 + 1; i++)
        {
            result[i] = 0;
        }

        double[] win = WindowGen.getWindow(window, flen);

        nfft = 0;      // number of ffts summed into result
        float[] dtemp = new float[flen];
        for (int idx = 0; idx + flen <= dlen; idx += flen - ov)
        {
            // copy this segment into temp buffer
            for (int i2 = 0; i2 < flen; i2++)
            {
                dtemp[i2] = data[i2 + idx];
            }
            detrend(dtemp);     // subtract a linear fit to the data

            // apply window function
            for (int i2 = 0; i2 < flen; i2++)
            {
                dtemp[i2] *= win[i2];
            }
            for (int i2 = 0; i2 < flen; i2++)
            {
                fftd[i2 * 2] = dtemp[i2];
                fftd[i2 * 2 + 1] = 0.f;
            }
            fft.complexForward(fftd);
            nfft++;
            double r;
            double im;
            for (int i3 = 0; i3 < flen / 2; i3++)
            {
                r = fftd[i3 * 2];
                im = fftd[i3 * 2 + 1];

                result[i3] += (r * r + im * im);
            }
        }
        // calculate scale factor
        double winsum = 0., winsumsq = 0;
        for (int idx = 0; idx < flen; idx++)
        {
            winsum += win[idx];
            winsumsq += win[idx] * win[idx];
        }
        double scale = 1.;
        if (pwrScale == Scaling.AS)
        {
            scale = Math.sqrt(2.) / winsum;
        }
        else if (pwrScale == Scaling.ASD)
        {
            scale = Math.sqrt(2. / (winsumsq * rate));
        }
        else if (pwrScale == Scaling.PS)
        {
            scale = 2 / (winsum * winsum);
        }
        else if (pwrScale == Scaling.PSD)
        {
            scale = 2 / (rate * winsumsq);
        }

        // create data series for plotting
        double df = 1 / secperfft;       // frequency separation for each fft bin
        double[][] spectrum = new double[flen/2+1][2];
        
        for (int idx = 0; idx < flen / 2 + 1; idx++)
        {
            double x = df * idx;     // frequency of this bin
            double y = result[idx] / nfft;
            if (pwrScale == Scaling.AS || pwrScale == Scaling.ASD)
            {
                y = (Math.sqrt(y) * scale);
            }
            else
            {
                y *= scale;
            }
            spectrum[idx][0] = x;
            spectrum[idx][1] = y;
        }
        return spectrum;
    }
    private float calcSpectrum(XYSeries xySeries, ChanDataBuffer dbuf) throws LdvTableException
    {
        double[][] spectrum = calcSpectrum(dbuf);
        for (double[] spectrum1 : spectrum)
        {
            xySeries.add(spectrum1[0], spectrum1[1]);
        }
        float df = 1 / secperfft;
        return df;
    }

    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        this.parameterMap = parameterMap;
        String[] t;
        t = parameterMap.get("window");
        String it = t != null && t.length > 0 ? t[0] : "";

        if (it.isEmpty() || it.equalsIgnoreCase("none"))
        {
            window = Window.NONE;
        }
        else if (it.equalsIgnoreCase("hanning"))
        {
            window = Window.HANNING;
        }
        else if (it.equalsIgnoreCase("flattop"))
        {
            window = Window.FLATTOP;
        }
        else
        {
            window = Window.HANNING;
        }


        t = parameterMap.get("scaling");
        it = t != null && t.length > 0 ? t[0] : "";
        if (it.equalsIgnoreCase("Amplitude spectrum"))
        {
            pwrScale = Scaling.AS;
        }
        else if (it.equalsIgnoreCase("Amplitude spectral density"))
        {
            pwrScale = Scaling.ASD;
        }
        else if (it.equalsIgnoreCase("Power specturm"))
        {
            pwrScale = Scaling.PS;
        }
        else if (it.equalsIgnoreCase("Power spectral density"))
        {
            pwrScale = Scaling.PSD;
        }

        t = parameterMap.get("sp_linethickness");
        if (t == null || !t[0].trim().matches("^\\d+$"))
        {
            lineThickness = 2;
        }
        else
        {
            lineThickness = Integer.parseInt(t[0]);
        }
        
        t = parameterMap.get("secperfft");
        if (t==null)
        {
            secperfft = 1;
        }
        else if (t[0].matches("[\\d\\.]+"))
        {
            this.secperfft = (float) Double.parseDouble(t[0]);
        }
        else
        {
            vpage.add("Invalid entry for seconds per fft, using 1");
        }

        t = parameterMap.get("fftoverlap");
        if (t == null)
        {
            overlap = secperfft/2;
        }
        else if (t[0].matches("[\\d\\.]+"))
        {
            double tmp = Double.parseDouble(t[0]);
            if (tmp >= 0 && tmp < 1)
            {
                this.overlap = (float) (tmp * secperfft);
            }
            else
            {
                vpage.add("Invalid entry for overlap, using 0.5");
                overlap = secperfft / 2;
            }                
        }
        else
        {
            vpage.add("Invalid entry for overlap, using 0.5");
            overlap = secperfft/2;
        }

        t=parameterMap.get("fmin");
        if (t==null)
        {
            fmin = Float.MIN_VALUE;
        }
        else if (t[0].matches("[\\d\\.]+"))
        {
            fmin = (float) Double.parseDouble(t[0]);
        }
        else 
        {
            fmin = Float.MIN_VALUE;
        }
        
        t=parameterMap.get("fmax");
        if (t == null)
        {
            fmax = Float.MAX_VALUE;
        }
        else if (t[0].matches("[\\d\\.]+"))
        {
            fmax = (float) Double.parseDouble(t[0]);
        }
        else 
        {
            fmax = Float.MAX_VALUE;
        }
        t = parameterMap.get("sp_logx");
        logXaxis = t != null;

        t = parameterMap.get("sp_logy");
        logYaxis = t != null;
    }

    private void findSmallest(XYSeriesCollection mtds)
    {
        double minx = Double.MAX_VALUE;
        double miny = Double.MAX_VALUE;
        for (Iterator it = mtds.getSeries().iterator(); it.hasNext();)
        {
            XYSeries ds = (XYSeries) it.next();
            for (int item = 1; item < ds.getItemCount() - 1; item++)
            {
                double x = ds.getX(item).doubleValue();
                double y = ds.getY(item).doubleValue();
                minx = Math.min(minx, x);
                miny = Math.min(miny, y);
            }
        }
        smallestX = minx;
        smallestY = 1e-40;
        double t;
        for (int e = 40; e > -40; e--)
        {
            t = Math.pow(10., e);
            if (miny >= t)
            {
                smallestY = t;
                break;
            }
        }


    }

    public void detrend(float[] data)
    {
        int dataLength = data.length;
        double[] x = new double[dataLength];
        double[] y = new double[dataLength];

        for (int i = 0; i < dataLength; i++)
        {

            x[i] = i;
            y[i] = data[i];
        }
        LinearRegression lr = new LinearRegression(x, y);
        double b = lr.getIntercept();
        double m = lr.getSlope();
        double fit, t;
        for (int i = 0; i < dataLength; i++)
        {
            data[i] = (float) (data[i] - (m * i + b));
        }
    }
    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        String[] windows =
        {
            "Hanning", "Flattop", "None",
        };
        String[] scalingNames =
        {
            "Amplitude spectral density", "Amplitude spectrum", "Power specturm", "Power spectral density"
        };
        String[] lineThicknessOptions = { "1", "2", "3", "4" };
        
        this.enableKey = enableKey;     // save for fancy page formatting
        
        PageItemList ret = new PageItemList();
        String enableText = "Generate spectrum plot";
        enableText += nSel > 1 ? "s" : "";
        boolean enabled = getPrevValue(enableKey);
        PageFormCheckbox cb = new PageFormCheckbox(enableKey, enableText, enabled);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick", fun);
        ret.add(cb);

        ret.addBlankLines(1);
        ret.add("Set appropriate parameters.");
        ret.addBlankLines(1);
        ret.addBlankLines(1);
        
        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        PageTableRow ptr;

        // window and scaling
        PageFormSelect win = new PageFormSelect("window", windows);
        String prevVal = getPrevValue("window", 0, windows[0]);
        win.setSelected(prevVal);
        ptr = GUISupport.getObjRow(win, "Window:", "");
        product.addRow(ptr);

        PageFormSelect scale = new PageFormSelect("scaling", scalingNames);
        prevVal = getPrevValue("scaling", 0, scalingNames[0]);
        ptr = GUISupport.getObjRow(scale, "Scaling:", "");
        product.addRow(ptr);
        
        PageFormSelect lineThicknessSelector = new PageFormSelect("sp_linethickness", lineThicknessOptions);
        prevVal = getPrevValue("sp_linethickness", 0, "2");
        lineThicknessSelector.setSelected(prevVal);
        ptr = GUISupport.getObjRow(lineThicknessSelector, "Line thickness: ", "");
        product.addRow(ptr);

        // length and overlap off fft
        prevVal = getPrevValue("secperfft", 0, "1.0");
        ptr = GUISupport.getTxtRow("secperfft", "Sec/fft:", "", 16, prevVal);
        product.addRow(ptr);
        
        prevVal = getPrevValue("fftoverlap", 0, "0.5");
        ptr = GUISupport.getTxtRow("fftoverlap", "Overlap fraction [0-1):", "", 16, prevVal);
        product.addRow(ptr);

        // frequncy axis limits
        prevVal = getPrevValue("fmin", 0, "");
        ptr = GUISupport.getTxtRow("fmin", "Min freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);

        prevVal = getPrevValue("fmax", 0, "");
        ptr = GUISupport.getTxtRow("fmax", "Max freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);
        
        // do we want axis to be logarithmic
        PageFormCheckbox logx = new PageFormCheckbox("sp_logx", "Freq axis logarithmic", true);
        ptr = GUISupport.getObjRow(logx, "", "");
        product.addRow(ptr);
        
        PageFormCheckbox logy = new PageFormCheckbox("sp_logy", "Range axis logarithmic", true);
        ptr = GUISupport.getObjRow(logy, "", "");
        product.addRow(ptr);
        
        enabled = getPrevValue("sp_dnld");
        PageFormCheckbox dnld = new PageFormCheckbox("sp_dnld", "Download spectrum as CSV", enabled);
        ptr = GUISupport.getObjRow(dnld, "", "No plots will be produced and 1 series only can be selected");
        product.addRow(ptr);

        PageFormCheckbox newPlt = new PageFormCheckbox("sp_newplt", "Use new plot functions", true);
        ptr = GUISupport.getObjRow(newPlt, "", "Uncheck to use classic plots, leave for new routines");
        product.addRow(ptr);

        ret.add(product);

        return ret;
    }

    public void setWantStacked(boolean wantStacked)
    {
        this.wantStacked = wantStacked;
    }

    public void setSecperfft(float secperfft)
    {
        this.secperfft = secperfft;
    }

    public void setOverlap(float overlap)
    {
        this.overlap = overlap;
    }

    public void setFmin(float fmin)
    {
        this.fmin = fmin;
    }

    public void setFmax(float fmax)
    {
        this.fmax = fmax;
    }

    public void setFsMax(float fsMax)
    {
        this.fsMax = fsMax;
    }

    
    public void setWindow(Window window)
    {
        this.window = window;
    }

    public void setScaling(SpectrumCalc.Scaling scaling)
    {
        this.pwrScale = scaling;
    }

    public void setDoDetrend(boolean doDetrend)
    {
        this.doDetrend = doDetrend;
    }

    public void setLogXaxis(boolean logXaxis)
    {
        this.logXaxis = logXaxis;
    }

    public void setLogYaxis(boolean logYaxis)
    {
        this.logYaxis = logYaxis;
    }

    @Override
    public boolean needsImageDescriptor()
    {
        return true;
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }

    @Override
    public void setChanList(List<BaseChanSelection> baseChans)
    {
        // we don't need this for our plot, but it's part of the interface
    }

    private class genPlotInfo
    {
        public File spFile;
        public String title;
        public String legend;
        
        genPlotInfo(File spFile, String title, String legend)
        {
            this.spFile = spFile;
            this.title = title;
            this.legend = legend;
        }
    }
    /**
     * use the external program genPlot.py to generate the graph
     * @param dbufs input spec
     * @param compact minimize label because output image will be small
     * @return 
     */
    private ArrayList<Integer> makeAddPlotFiles(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws WebUtilException, LdvTableException
    {
        ExternalProgramManager epm = new ExternalProgramManager();
        ArrayList<Integer> ret = new ArrayList<>();
        float fsMax=0;
        try
        {
            ArrayList<String> cmd = new ArrayList<>();
            ArrayList<genPlotInfo> spectra = new ArrayList<>();
            
            File tempDir = epm.getTempDir("sp_");
            File outFile = epm.getTempFile("sp_plot_", ".png");
            
            // we need to know for labeling
            boolean sameChannel = true;
            boolean sameTime = true;
            boolean multiPlot = dbufs.size() > 1;
            
            Set<ChanInfo> cis = new TreeSet<>();
            Set<TimeInterval> tis = new TreeSet<>();

            if (multiPlot)
            {
                for (ChanDataBuffer buf : dbufs)
                {
                    cis.add(buf.getChanInfo());
                    tis.add(buf.getTimeInterval());
                }
                sameChannel = cis.size() == 1;
                sameTime = tis.size() == 1;
            }
            
            for (ChanDataBuffer buf : dbufs)
            {
                double[][] spectrum = calcSpectrum(buf);
                File spFile = epm.writeTempCSV("Spectrum_", spectrum);
                String fTitle = "";
                String fLegend = "";
                float fs = buf.getChanInfo().getRate();
                fsMax = Math.max(fsMax, fs);
                if (!sameChannel)
                {
                    String chanName = buf.getChanInfo().getChanName();
                    String chanFs;
                    if (fs >= 1)
                    {
                        chanFs = String.format("%1$.0f Hz", fs);
                    }
                    else
                    {
                        chanFs = String.format("%1$.3f Hz", fs);
                    }
                    if (fTitle.length() > 0)
                    {
                        fTitle += ", ";
                    }
                    fLegend += String.format("%1$s at %2$s ", chanName, chanFs);
                }
                if (!sameTime)
                {
                    long gps = buf.getTimeInterval().getStartGps();
                    String utc = TimeAndDate.gpsAsUtcString(gps);
                    fLegend += String.format("%1$s (%2$d)",utc, gps);
                }
                spectra.add(new genPlotInfo(spFile, fTitle, fLegend));
            }
            File outImg=epm.getTempFile("spPlot_", ".png");
            
            cmd.add("/usr/local/ldvw/bin/genPlot.py");
            // add input files
            for (genPlotInfo gpi : spectra)
            {
                File f = gpi.spFile;
                cmd.add("--infile");
                cmd.add(f.getCanonicalPath());
                if (!sameChannel || !sameTime )
                {
                    if (gpi.title.length() > 0)
                    {
                        cmd.add("--title");
                        cmd.add(gpi.title);
                    }
                    if (gpi.legend.length() > 0)
                    {
                        cmd.add("--legend");
                        cmd.add(gpi.legend);
                    }
                }
            }
            // add and output file
            cmd.add("--out");
            cmd.add(outFile.getCanonicalPath());
            // add options
            if (parameterMap.containsKey("sp_logy"))
            {
                cmd.add("--logy");
            }
            if (parameterMap.containsKey("sp_logx"))
            {
                cmd.add("--logx");
            }
            if (height > 100 && width > 100)
            {
                cmd.add("--geometry");
                cmd.add(String.format("%1$dx%2$d", width, height));
            }
            if (fmin > 0)
            {
                cmd.add("--xmin");
                cmd.add(String.format("%1$.2f", fmin));
            }
            if (fmax < fsMax && fmax > 0)
            {
                cmd.add("--xmax");
                cmd.add(String.format("%1$.2f",fmax));
            }
            // add the super title
            String supTitle = "Spectrum plot";
            long dur = dbufs.get(0).getTimeInterval().getDuration();            
            String durStr = String.format("%1$,d s", dur);
            if (dur >= 3600)
            {
                durStr = TimeAndDate.hrTime(dur);
            }
            if (!multiPlot)
            {
                supTitle = getTitle(dbufs, false);
            }
            else if (!sameChannel && sameTime)
            {
                long gps = dbufs.get(0).getTimeInterval().getStartGps();

                supTitle = String.format("%1$s (%2$d) t = %3$s",TimeAndDate.gpsAsUtcString(gps),
                                         gps,durStr);
            }
            else if (sameChannel && !sameTime)
            {
                String chanFs;
                float fs = dbufs.get(0).getChanInfo().getRate();
                if (fs > 1)
                {
                    chanFs = String.format("%1$.0f",fs);
                }
                else
                {
                    chanFs = String.format("%1$.3f", fs);
                }
                supTitle = String.format("%1$s at %2$s Hz, t=%3$s", 
                                         dbufs.get(0).getChanInfo().getChanName(), chanFs, durStr);
            }
            else if (!sameChannel && !sameTime)
            {
                supTitle = "Spectrum plot";
            }
            cmd.add("--suptitle");
            cmd.add(supTitle);
            // axis labels
            DecimalFormat dform = new DecimalFormat("0.0###");
            float bw = 1/secperfft;
            
            cmd.add("--xlabel");
            cmd.add(String.format("Frequency Hz,  bw: %1$s, "
                    + "fft: %2$,d, s/fft: %3$.2f, ov: %4$.2f",
                                   dform.format(bw), nfft,secperfft, overlap));
            pwrScale.setTex(true);
            cmd.add("--ylabel");
            cmd.add(pwrScale.toString());
            //cmd.add("--test");
            if (epm.runExternalProgram(cmd))
            {
                int imgId = epm.addImg2Db(outFile, db, vuser.getCn());
                ret.add(imgId);
            }
            else
            {
                vpage.add("Problem generating plot of spectrum.");
                vpage.addBlankLines(2);
                vpage.add("Command line: ");
                vpage.addBlankLines(1);
                vpage.add(new PageItemString(cmd.toString(), false));
                vpage.addBlankLines(2);
                vpage.add("Stderr:");
                vpage.addBlankLines(1);
                vpage.add(new PageItemString(epm.getStderr(),false));
                vpage.addBlankLines(2);
                vpage.add("Stdout:");
                vpage.addBlankLines(1);
                vpage.add(new PageItemString(epm.getStdout(),false));
                vpage.addBlankLines(2);
            }
            
        }
        catch (IOException ex)
        {
            throw new WebUtilException("Spectrum plot (genPlot.py):", ex);
        }
        finally
        {
            epm.removeTemps();
        }
        return ret;
    }

}
