/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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

package edu.fullerton.plugindefn;

/**
 * Base class for common sets of parameters used in gwpy-ldvw plots
 * 
 * Defines groups of parameters for efficency and consistency.  It
 * is up to the derived class to determine which of these is appropriate.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public abstract class GWpyBaseDefn extends PluginController
{
    private PluginParameter p;
    /**
     * initialize the plugin definition tables
     */
    @Override
    protected abstract void init();    
    
    /**
     * These define how we construct the command and call the program
     */
    protected void addCommonAttributes()
    {
        addAttribute(new PluginAttribute("needsDataXfer", false));
        addAttribute(new PluginAttribute("program", "/usr/local/ldvw/bin/gwpy-plot"));
        addAttribute(new PluginAttribute("useEquals", false));
        addAttribute(new PluginAttribute("nDashes", 2));
        addAttribute(new PluginAttribute("useQuotes", false));
    }
    
    /**
     * Time and Channel arguments define the dataset(s) to plot
     */
    protected void addTimeChannel()
    {
        // standard parameters come from globals like channels and times
        p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "channel", null, null);
        p.setArgumentName("chan");
        p.setListStyle("python");
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "start", null, null);
        p.setArgumentName("start");
        p.setListStyle("python");
        addParameter(p);
        
        p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "duration", null, null);
        p.setArgumentName("duration");
        p.setListStyle("python");
        addParameter(p);
        
        p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "tempFile", ".png", null);
        p.setArgumentName("out");
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "geometry", null, null);
        p.setArgumentName("geometry");
        addParameter(p);
    }
    
    /**
     * FFTs are defined by time span and overlap
     */
    // @todo add window and scaling
    protected void addFFT()
    {
        addFFT(1.0f, 0.5f);
    }
    protected void addFFT(float secpfftDefault, float overlapDefault)
    {
        
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Seconds per fft", "secpfft",
                                        String.format("Default = %1$.1f", secpfftDefault));
        p.setArgumentName("secpfft");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Overlap", "ovlap",
                                        String.format("Overlap fraction [0-1), default = %1$.1f",
                                                      overlapDefault));
        p.setArgumentName("overlap");
        p.setnDecimals(3);
        addParameter(p);
    }
    /**
     * Pre-filtering options
     */
    // @todo add other filters available from GWpy
    protected void addPreFilter()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "High pass filter", "hpfilt",
                                        "Cut off frequency, default = no filter");
        p.setArgumentName("highpass");
        p.setnDecimals(2);
        addParameter(p);
    }
    /**
     * Default frequency axis defaults to log, let them make it liniear.
     */
    protected void addLogFaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear F-axis", "nologf",
                                        "Default = log");
        p.setArgumentName("nologf");
        addParameter(p);
        addFaxisLimits();
    }
    /**
     * Let them specify limits of Frequency axis.  May be X or Y.
     */
    protected void addFaxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "F-minimum", "fmin",
                                        "Default is auto");
        p.setArgumentName("fmin");
        p.setnDecimals(0);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "F-maximum", "fmax",
                                        "Default is auto");
        p.setArgumentName("fmax");
        p.setnDecimals(0);
        addParameter(p);
    }
    /**
     * Y-axis defaults to log let them specify linear
     */
    protected void addLogYaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear Y-axis", "nology",
                                        "Default is log");
        p.setArgumentName("nology");
        addParameter(p);
        addYaxisLimits();
    }
    /**
     * X-axis defaults to linear let them specify log.
     */
    protected void addLinearXaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Log X-axis", "logx",
                                        "Default is linear");
        p.setArgumentName("logx");
        addParameter(p);
        addXaxisLimits();
    }
    /**
     * Allow them to specify X-axis limits
     */
    protected void addXaxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-minimum", "xmin",
                                        "Seconds (float) from start or GPS, default = auto");
        p.setArgumentName("xmin");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-maximum", "xmax",
                                        "Seconds (float) from start or GPS, default = auto");
        p.setArgumentName("xmax");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-axis zero", "epoch",
                                        "Seconds (float) from start or GPS, default = auto");
        p.setArgumentName("epoch");
        p.setnDecimals(3);
        addParameter(p);
    }
    /**
     * Y-axis defaults to linear.  Let them make it log.
     */
    protected void addLinearYaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Log Y", "logy",
                                        "Default is linear");
        p.setArgumentName("logy");
        addParameter(p);
        addYaxisLimits();
    }
    /**
     * Allow them to make fix the Y-axis limits
     */
    protected void addYaxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Y-minimum", "ymin",
                                        "Default is auto");
        p.setArgumentName("ymin");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Y-maximum", "ymax",
                                        "Default is auto");
        p.setArgumentName("ymax");
        p.setnDecimals(3);
        addParameter(p);
    }
    /**
     * Intensity axis defaults to log let them make it linear, if they want
     */
    protected void addLogIntensity()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear color bar", "nologi",
                                        "Default is log");
        p.setArgumentName("lincolors");
        addParameter(p);

        addIntAxisLimits();
    }
    /**
     * Intensity axis defaults to linear let them make it log, if they want
     */
    protected void addLinearIntensity()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Log color bar", "logi",
                                        "Default is linear");
        p.setArgumentName("lincolors");
        addParameter(p);

        addIntAxisLimits();
    }
    /**
     * The Intensity axis aka Colorbar.  Let them set the limits
     */
    protected void addIntAxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Colorbar-minimum", "imin",
                                        "Default = auto");
        p.setArgumentName("imin");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Colorbar-maximum", "imax",
                                        "Default = auto");
        p.setArgumentName("imax");
        p.setnDecimals(3);
        addParameter(p);
        
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Normalize", "norm",
                                        "Normalize intensity relative to mean");
        p.setArgumentName("norm");
        addParameter(p);
    }
    /** 
     * paired products like coherence need a reference channel
     */
    protected void addRefChan()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.REFCHAN, "Reference channel", 
                                        "refchan", "Calculation always uses this channel.");
        p.setArgumentName("ref");
        addParameter(p);
        
    }
    
    
}
