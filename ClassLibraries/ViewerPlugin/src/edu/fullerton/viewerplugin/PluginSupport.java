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

import edu.fullerton.viewerplugin.ChanDataBuffer;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

    
    public PluginSupport()
    {
        
    }
    
    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    public String getTitle(ArrayList<ChanDataBuffer> dbufs, boolean compact)
    {
        String ret;
        if (dbufs.size() == 1)
        {
            ret = getLegend(dbufs.get(0), compact);
        }
        else if (dbufs.size() > 1)
        {
            TreeSet<String> chans = new TreeSet<String>();
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

    
    public String getLegend(ChanDataBuffer dbuf, boolean compact)
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
    static void getRangeLimits(XYSeriesCollection mtds, Double[] rng, int skip)
    {
        getRangeLimits(mtds,rng,skip,-Float.MAX_VALUE, Float.MAX_VALUE);
    }
    static void getRangeLimits(XYSeriesCollection mtds, Double[] rng, int skip, float xmin, float xmax)
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
    static int scaleRange(TimeSeriesCollection mtds, Double miny, Double maxy)
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
    static int scaleRange(XYSeriesCollection mtds, Double miny, Double maxy)
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
}
