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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ViewUser;
import java.awt.BasicStroke;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


/**
 * Generic plot (at least for ldvw)
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class XYPlotter
{
    private String title;
    private String legend;
    private String xLabel;
    private String yLabel;
    private int width;
    private int height;
    private boolean logYaxis = false;
    private boolean logXaxis = false;
    private Database db;
    private Page vpage;
    private ViewUser vuser;
    private Float fmin;
    private Float fmax;
    private int lineThickness = 2;

    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }
    
    public int plotAndSave(String title, String legend, String xLabel, String yLabel, double[][] data) throws WebUtilException
    {
        this.title = title;
        this.legend = legend;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
        
        return plotAndSave(data);
    }
    
    public int plotAndSave(double[][] data) throws WebUtilException
    {
        ChartPanel cpnl = getPanel(data);
        int imageId = 0;
        try
        {
            PluginSupport psup = new PluginSupport();
            psup.setup(db, vpage, vuser);
            psup.setSize(width, height);
            imageId = psup.saveImageAsPNG(cpnl);
        }
        catch (Exception ex)
        {
            throw new WebUtilException("Creating XY plot: " + ex.getClass().getSimpleName()
                                       + ": " + ex.getLocalizedMessage());
        }
        return imageId;
    }
    private ChartPanel getPanel(double[][] data) throws WebUtilException
    {
        ChartPanel ret = null;
        try
        {
            XYSeries xys;
            XYSeriesCollection mtds = new XYSeriesCollection();
            String mylegend = legend == null || legend.isEmpty() ? "series 1" : legend;

            xys = new XYSeries(legend);

            int len = data.length;
            double minx = Double.MAX_VALUE;
            double maxx = Double.MIN_VALUE;
            double miny = Double.MAX_VALUE;
            double maxy = Double.MIN_VALUE;
            boolean gotZeroX = false;
            boolean gotZeroY = false;
            
            for(int i=0;i<len;i++)
            {
                double x = data[i][0];
                double y = data[i][1];
                if (x == 0)
                {
                    gotZeroX = true;
                }
                else
                {
                    minx = Math.min(minx, x);
                    maxx = Math.max(maxx, x);
                }
                if (y == 0)
                {
                    gotZeroY = true;
                }
                else
                {
                    miny = Math.min(miny,y);
                    maxy = Math.max(maxy, y);
                }
            }
            // this kludge lets us plot a 0 on a log axis
            double fakeZeroX=0.;
            double fakeZeroY=0.;
            if (gotZeroX)
            {
                if (logXaxis)
                {
                    fakeZeroX = minx/10;
                }
                else
                {
                    minx = Math.min(0, minx);
                    maxx = Math.max(0, maxx);
                }
            }
            if (gotZeroY)
            {
                if (logYaxis)
                {
                    fakeZeroY = miny/10;
                }
                else
                {
                    miny = Math.min(0,miny);
                    maxy = Math.max(0,maxy);
                }
            }
            for (int i = 0; i < len; i++)
            {
                double x = data[i][0];
                double y = data[i][1];
                x = x == 0 ? fakeZeroX : x;
                y = y == 0 ? fakeZeroY : y;
                xys.add(x, y);
            }
            mtds.addSeries(xys);


            DefaultXYDataset ds = new DefaultXYDataset();

            int exp;
            if (maxy == 0. && miny == 0.)
            {
                miny = -1.;
                exp = 0;
                logYaxis = false;
            }
            else
            {
                
                maxy = maxy > miny ? maxy : miny * 10;
                exp = PluginSupport.scaleRange(mtds, miny, maxy);
                if (!logYaxis && exp > 0)
                {
                    yLabel += " x 1e-" + Integer.toString(exp);
                }
            }
            JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, ds, PlotOrientation.VERTICAL, true, false, false);
            org.jfree.chart.plot.XYPlot plot = (org.jfree.chart.plot.XYPlot) chart.getPlot();
            if (logYaxis)
            {
                LogAxis rangeAxis = new LogAxis(yLabel);
                double smallest = miny * Math.pow(10, exp);
                rangeAxis.setSmallestValue(smallest);
                rangeAxis.setMinorTickCount(9);

                LogAxisNumberFormat lanf = new LogAxisNumberFormat();
                lanf.setExp(exp);
                rangeAxis.setNumberFormatOverride(lanf);
                rangeAxis.setRange(smallest, maxy * Math.pow(10, exp));
                plot.setRangeAxis(rangeAxis);
            }
            if (logXaxis)
            {
                LogAxis domainAxis = new LogAxis(xLabel);
                domainAxis.setMinorTickCount(9);
                domainAxis.setSmallestValue(minx);
                domainAxis.setNumberFormatOverride(new LogAxisNumberFormat());
                plot.setDomainAxis(domainAxis);
            }
            ValueAxis domainAxis = plot.getDomainAxis();
            if (fmin != null && fmin > 0)
            {
                domainAxis.setLowerBound(fmin);
            }
            if (fmax != null && fmax > 0)
            {
                domainAxis.setUpperBound(fmax);
            }
            plot.setDomainAxis(domainAxis);
            plot.setDataset(0, mtds);
            
            // Set the line thickness
            XYLineAndShapeRenderer r = (XYLineAndShapeRenderer) plot.getRenderer();
            BasicStroke str = new BasicStroke(lineThickness);
            int n = plot.getSeriesCount();
            for (int i = 0; i < n; i++)
            {
                r.setSeriesStroke(i, str);
            }
            
            if (legend == null || legend.isEmpty())
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

    public void setTitle(String title)
    {
        this.title = title;
    }

    public void setLegend(String legend)
    {
        this.legend = legend;
    }

    public void setxLabel(String xLabel)
    {
        this.xLabel = xLabel;
    }

    public void setyLabel(String yLabel)
    {
        this.yLabel = yLabel;
    }

    public void setLogYaxis(boolean logYaxis)
    {
        this.logYaxis = logYaxis;
    }

    public void setLogXaxis(boolean logXaxis)
    {
        this.logXaxis = logXaxis;
    }
    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    public void setXrange(Float fmin, Float fmax)
    {
        if (fmin > 0 || fmax > 0)
        {
            this.fmin = fmin;
            this.fmax = fmax;
        }
    }

    public void setLineThickness(int lineThickness)
    {
        this.lineThickness = lineThickness;
    }
    
}
