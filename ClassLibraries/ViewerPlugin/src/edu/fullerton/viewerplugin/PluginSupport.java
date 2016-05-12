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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.DefaultFontMapper;
import com.itextpdf.text.pdf.FontMapper;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfTemplate;
import com.itextpdf.text.pdf.PdfWriter;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TreeSet;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;


/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PluginSupport 
{
    protected Database db = null;
    protected Page vpage = null;
    protected ViewUser vuser = null;
    protected int width=640;
    protected int height=480;
    protected Map<String, String[]> parameterMap; // all request parameters for a plot request
    private String xAxisLabel;
    protected String enableKey;

    
    public PluginSupport()
    {
        enableKey="no way should the parameter map contain this";
    }
    public boolean isSelected()
    {
        boolean ret = false;
        if (parameterMap != null)
        {
            ret = parameterMap.containsKey(enableKey);
        }
        return ret;
    }
    public String getEnableKey()
    {
        return enableKey;
    }
    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    /**
     * Use information from the buffer to create a title for the plot
     * @param dbufs - data being plotted
     * @param compact - if it's a small plot don't tell them too much
     * @return
     * @throws LdvTableException
     */
    public String getTitle(ArrayList<ChanDataBuffer> dbufs, boolean compact) throws LdvTableException
    {
        String ret;
        if (dbufs.size() == 1)
        {
            ret = getLegend(dbufs.get(0), compact);
        }
        else if (dbufs.size() > 1)
        {
            TreeSet<String> chans = new TreeSet<>();
            for(ChanDataBuffer dbuf: dbufs)
            {
                chans.add(dbuf.getChanInfo().getChanName());
            }
            ret = "";
            for (String cname : chans)
            {
                if (ret.length() > 0)
                {
                    ret += ", ";
                }
                ret += cname;
            }
        }
        else
        {
            ret = "Looks like we don't have any data??";
        }
        return ret;
    }

    /**
     * From the single time series create text for a legend
     * @param dbuf
     * @param compact
     * @return
     * @throws LdvTableException
     */
    public String getLegend(ChanDataBuffer dbuf, boolean compact) throws LdvTableException
    {
        String ret;
        String chName = dbuf.getChanInfo().getChanName();
        float fs = dbuf.getChanInfo().getRate();
        String fsStr;
        if (fs > 0.999)
        {
            fsStr = String.format("%1$.0f",fs);
        }
        else
        {
            fsStr = String.format("%1$.3f",fs);
        }
        long startGps = dbuf.getTimeInterval().getStartGps();
        String startUTCstr = TimeAndDate.gpsAsUtcString(startGps);
        long stopGps = dbuf.getTimeInterval().getStopGps();
        int duration = (int) (stopGps-startGps);
        String durationStr = TimeAndDate.hrTime(duration);
        if (compact)
        {
            ret = String.format("%1$s %2$d (%3$s)",chName,startGps,durationStr);
        }
        else
        {
            ret = String.format("%1$s t=%3$s at %2$sHz \n%4$s UTC (%5$d)", 
                             chName, fsStr, durationStr, startUTCstr, startGps);
        }
        return ret;
    }
    /**
     * Create an image from the Chart Panel and add it the database
     * @param cp input plot
     * @return image ID of newly added row
     * @throws IOException
     * @throws SQLException
     * @throws NoSuchAlgorithmException 
     */
    public int saveImageAsPNG(ChartPanel cp) throws IOException, SQLException, NoSuchAlgorithmException
    {
        JFreeChart chart = cp.getChart();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsPNG(bos, chart, width, height);

        ImageTable itbl = new ImageTable(db);
        
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        int imgId = itbl.addImg(vuser.getCn(), bis, "image/png");
        return imgId;
    }
    public void saveImageAsPNGFile(ChartPanel cp, String fname) throws WebUtilException 
    {
        FileOutputStream fos = null;
        try
        {
            JFreeChart chart = cp.getChart();

            fos = new FileOutputStream(fname);
            ChartUtilities.writeChartAsPNG(fos, chart, width, height);
            fos.close();
            fos=null;
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
    /**
     * Create a Scalable Vector Graphics (SVG) file from a JFree Chart
     * @param chart the plot ready for saving
     * @param filename the output filename
     * @throws WebUtilException 
     */
    public void saveImageAsSvgFile(JFreeChart chart, String filename) throws WebUtilException
    {
        // THE FOLLOWING CODE BASED ON THE EXAMPLE IN THE BATIK DOCUMENTATION...
        // Get a DOMImplementation
        DOMImplementation domImpl;
        domImpl = SVGDOMImplementation.getDOMImplementation();
        
        // Create an instance of org.w3c.dom.Document
        Document document = domImpl.createDocument(null, "svg", null);
        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        // set the precision to avoid a null pointer exception in Batik 1.5
        svgGenerator.getGeneratorContext().setPrecision(6);
        // Ask the chart to render into the SVG Graphics2D implementation
        chart.draw(svgGenerator, new Rectangle2D.Double(0, 0, width, height), null);
        // Finally, stream out SVG to a file using UTF-8 character to
        // byte encoding
        boolean useCSS = true;
        Writer out;
        try
        {
            out = new OutputStreamWriter(
                    new FileOutputStream(new File(filename)), "UTF-8");
            svgGenerator.stream(out, useCSS);
            out.close();
        }
        catch (IOException  ex)
        {
            throw new WebUtilException("Writing SVG image", ex);
        }
    }
    public void saveImageAsPdfFile(JFreeChart chart, String filename) throws WebUtilException
    {
        try
        {
            OutputStream out = new BufferedOutputStream(new FileOutputStream(filename));
            Rectangle pagesize = new Rectangle(width, height);
            com.itextpdf.text.Document document;
            document = new com.itextpdf.text.Document(pagesize, 50, 50, 50, 50);
            try
            {
                PdfWriter writer = PdfWriter.getInstance(document, out);
                FontMapper mapper = new DefaultFontMapper();
                document.addAuthor("JFreeChart");
                document.addSubject("Demonstration");
                document.open();
                PdfContentByte cb = writer.getDirectContent();
                PdfTemplate tp = cb.createTemplate(width, height);
                Graphics2D g2 = tp.createGraphics(width, height, mapper);
                Rectangle2D r2D = new Rectangle2D.Double(0, 0, width, height);
                chart.draw(g2, r2D);
                g2.dispose();
                cb.addTemplate(tp, 0, 0);
            }
            catch (DocumentException de)
            {
                throw new WebUtilException("Saving as pdf", de);
            }
            document.close();
        }
        catch (FileNotFoundException ex)
        {
            throw new WebUtilException("Saving plot as pdf: ", ex);
        }

    }
    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }
    public void setParameters(Map<String, String[]> parameterMap)
    {
        this.parameterMap = parameterMap;
    }
    public static void getRangeLimits(TimeSeriesCollection mtds,Double rng[])    
    {   
        Double minx,miny,maxx,maxy;
    
        minx = miny = Double.MAX_VALUE;
        
        maxx = maxy = -Double.MAX_VALUE;
        for (Iterator it = mtds.getSeries().iterator(); it.hasNext();)
        {
            TimeSeries ds = (TimeSeries) it.next();
            for (int item = 1; item < ds.getItemCount() - 1; item++)
            {
                TimeSeriesDataItem dataItem = ds.getDataItem(item);
                RegularTimePeriod period = dataItem.getPeriod();

                double y = dataItem.getValue().doubleValue();
                double x = period.getFirstMillisecond();
                
                minx = Math.min(minx, x);
                miny = Math.min(miny, y);
                maxx = Math.max(maxx, x);
                maxy = Math.max(maxy, y);
            }
        }
        rng[0] = minx;
        rng[1] = miny;
        rng[2] = maxx;
        rng[3] = maxy;
    }
    public static void getRangeLimits(XYSeriesCollection mtds, Double[] rng, int skip)
    {
        getRangeLimits(mtds,rng,skip,-Float.MAX_VALUE, Float.MAX_VALUE);
    }
    public static void getRangeLimits(XYSeriesCollection mtds, Double[] rng, int skip, float xmin, float xmax)
    {
        Double minx, miny, maxx, maxy;

        minx = miny = Double.MAX_VALUE;

        maxx = maxy = -Double.MAX_VALUE;
        for (Iterator it = mtds.getSeries().iterator(); it.hasNext();)
        {
            
            XYSeries ds = (XYSeries) it.next();
            for (int item = skip; item < ds.getItemCount() - skip; item++)
            {
                double x = ds.getX(item).doubleValue();
                double y = ds.getY(item).doubleValue();

                if (x >= xmin && x <= xmax)
                {
                    minx = Math.min(minx, x);
                    miny = Math.min(miny, y);
                    maxx = Math.max(maxx, x);
                    maxy = Math.max(maxy, y);
                }
            }
        }
        rng[0] = minx;
        rng[1] = miny;
        rng[2] = maxx;
        rng[3] = maxy;  
    }
    public static int scaleRange(TimeSeriesCollection mtds, Double miny, Double maxy)
    {
        int exp = PluginSupport.getExp(miny, maxy);
        double scale = Math.pow(10,exp);
        for (Iterator it = mtds.getSeries().iterator(); it.hasNext();)
        {
            TimeSeries ds = (TimeSeries) it.next();
            for (int item = 0; item < ds.getItemCount(); item++)
            {
                TimeSeriesDataItem dataItem = ds.getDataItem(item);
                RegularTimePeriod period = dataItem.getPeriod();
                
                double y = dataItem.getValue().doubleValue();
                y *= scale;
                ds.update(period, y);
            }
        }
        return exp;
    }
    static int getExp(Double miny, Double maxy)
    {
        int mine = miny == 0 ? 0 : (int) Math.floor(Math.log10(Math.abs(miny)));
        int maxe = maxy == 0 ? 0 : (int) Math.ceil(Math.log10(Math.abs(maxy)));
        int rng = maxe - mine;
        int exp = -(maxe + mine) / 2;
        
        double min=0;
        if (miny != 0)
        {
            min = Math.log10(Math.abs(miny));
        }
        double max = 0;
        if (maxy != 0)
        {
            max = Math.log10(Math.abs(maxy));
        }
        if (Math.abs(max) > Math.abs(min))
        {
            exp = - (int) Math.round(max);
        }
        else
        {
            exp = - (int) Math.round(min);
        }
        return exp;
    }
    public static int scaleRange(XYSeriesCollection mtds, Double miny, Double maxy)
    {
        int exp = PluginSupport.getExp(miny, maxy);
        if (exp > 0 && exp < 100)
        {
            int nseries = mtds.getSeriesCount();
            XYSeries[] newSeries = new XYSeries[nseries];
            
            double scale = Math.pow(10, exp);
            for (int s=0; s < nseries; s++)
            {
                XYSeries ds = (XYSeries) mtds.getSeries(s);
                Comparable skey = mtds.getSeriesKey(s);
                XYSeries nds = new XYSeries(skey, true);
                for (int item = 0; item < ds.getItemCount(); item++)
                {
                    double x = ds.getX(item).doubleValue();
                    double y = ds.getY(item).doubleValue();

                    y *= scale;
                    nds.add(x, y);
                }
                newSeries[s] = nds;
            }
            mtds.removeAllSeries();
            for (int s=0; s < nseries; s++)
            {
                mtds.addSeries(newSeries[s]);
            }
        }
        else
        {
            exp =0;
        }
        return exp;
    }

    /**
     * Assume that the product wants the PlotManager to do the data transfer. Override for special cases
     * @return always true
     */
    public boolean needsDataXfer()
    {
        return true;
    }
    /**
     * Generate a JFreeChart TimeSeries object from a ChanDataBuffer
     * Note:  the time axis is in UTC.  For GPS or delta T use XY series.
     * @param dbuf - ldvw data buffer
     * @param legend - plot legend for this series
     * @return JFreeChart time series for adding to a plot
     */
    public TimeSeries getTimeSeries(ChanDataBuffer dbuf, String legend, int sum) throws LdvTableException
    {
        sum = sum < 1 ? 1 : sum;
        TimeSeries ts;
        ts = new TimeSeries(legend, Millisecond.class);
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");

        float rate = dbuf.getChanInfo().getRate();
        double msPerSample = 1000 / rate;
        long startMs = TimeAndDate.gps2utc(dbuf.getTimeInterval().getStartGps()) * 1000;
        float[] data = dbuf.getData();
        for (int i = 0; i < dbuf.getDataLength(); i+=sum)
        {
            float td = 0.f;
            int nsum = 0;
            for(int j=0;j<sum && i+j < dbuf.getDataLength(); j++)
            {
                td += data[i+j];
                nsum++;
            }
            td /= nsum;
            
            long curMs = Math.round(msPerSample * i + startMs);
            Date t = new Date(curMs);
            ts.addOrUpdate(new Millisecond(t, utctz), td);
            if (msPerSample >= 1000)
            {
                // this plots trend data as stair steps
                long endMs = Math.round(curMs + msPerSample - 1);
                Date t1 = new Date(endMs);
                ts.addOrUpdate(new Millisecond(t1, utctz), td);
            }
        }
        return ts;
    }

    /**
     * Convert a ldvw data buffer to JFreeChart XY-series
     * 
     * @param dbuf - ldvw data buf
     * @param legend - String to identify series
     * @param dt if true X axis is 0-t in [hopefully] intelligent units else it's gps time
     * @param sum
     * @param xAxisLabel this argument is returned set to a label for the X-axis
     * @return A series to be added to a JFreeChart plot
     */
    public XYSeries addXySeries(ChanDataBuffer dbuf, String legend, boolean dt, int sum) throws LdvTableException
    {
        sum = sum < 1 ? 1 : sum;
        XYSeries xys = new XYSeries(legend, false);

        float rate = dbuf.getChanInfo().getRate();

        long startSec = dbuf.getTimeInterval().getStartGps();
        long durSec = dbuf.getTimeInterval().getStopGps() - dbuf.getTimeInterval().getStartGps();
        double scale = 1;
        double t0 = startSec;
        xAxisLabel = "GPS time";
        String units;
        if (dt)
        {
            t0 = 0;
            // put dt plots into reasonable units
            if (durSec < 1800)
            {
                scale = 1.;
                units = "sec";
            }
            else if (durSec < 3 * 3600)
            {
                scale = 1 / 60.;
                units = "min";
            }
            else if (durSec < 48 * 3600)
            {
                scale = 1 / 3600.;
                units = "hrs";
            }
            else
            {
                scale = 1.0 / (24 * 3600);
                units = "days";
            }
            xAxisLabel = String.format("dt from %1$,d (%2$s)", startSec, units);
        }
        
        float[] data = dbuf.getData();
        for (int i = 0; i < dbuf.getDataLength(); i += sum)
        {
            double x = (i / rate + t0) * scale; // handle gps vs dt and dt units
            double y = 0;
            int nsum=0;
            for(int j=0; j<sum && i+j < dbuf.getDataLength(); j++)
            {
                y += data[i+j];
                nsum++;
            }
            y /= nsum;
            xys.add(x, y);
        }

        return xys;
    }

    public String getxAxisLabel()
    {
        return xAxisLabel;
    }
    /**
     * As part of remembering where we came from, form values are passed back and forth to select
     * more. Here we use the previous value or default for the specified key
     *
     * @param key - Parameter name for this field
     * @param idx - Index into value array, 0 if only 1 value allowed
     * @param def - default value if no parameter or parameter is empty
     * @return
     */
    public String getPrevValue(String key, int idx, String def)
    {
        String ret = def;
        String[] prev = parameterMap.get(key);
        if (prev != null && prev.length > idx && !prev[0].isEmpty())
        {
            ret = prev[idx];
        }
        return ret;
    }
    /**
     * Checkboxes are a bit difficult because their key only gets sent if it's checked.  So we 
     * don't really know if it's the first time thru with no values for anything or they unchecked
     * it.
     * 
     * @param key - parameter name
     * @return true if parameter is available
     */
    public boolean getPrevValue(String key)
    {
        boolean ret = parameterMap.containsKey(key);
        return ret;
    }
}
