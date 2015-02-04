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
        addAttribute(new PluginAttribute("program", "/usr/local/ldvw/bin/gwpy-ldvw.py"));
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
     * @todo add window and scaling
     */
    protected void addFFT()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Seconds per fft", "secpfft",
                                        "Default is 1.0");
        p.setArgumentName("secpfft");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Overlap", "ovlap",
                                        "Overlap fraction [0-1), default is 0.5");
        p.setArgumentName("overlap");
        p.setnDecimals(2);
        addParameter(p);
    }
    /**
     * Prefiltering options
     * @todo add other filters available from GWpy
     */
    protected void addPreFilter()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "High pass filter", "hpfilt",
                                        "Cut off frequency, default is no filter.");
        p.setArgumentName("highpass");
        p.setnDecimals(2);
        addParameter(p);
    }
    protected void addLogFaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear F-axis", "nologf",
                                        "Default is log");
        p.setArgumentName("nologf");
        addParameter(p);
        addFaxisLimits();
    }
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
    protected void addLogYaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear Y-axis", "nology",
                                        "Default is log");
        p.setArgumentName("nology");
        addParameter(p);
        addYaxisLimits();
    }
    protected void addLinearXaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Log X-axis", "logx",
                                        "Default is linear");
        p.setArgumentName("logx");
        addParameter(p);
        addXaxisLimits();
    }
    protected void addXaxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-minimum", "xmin",
                                        "Seconds (float) from start) or GPS, default = earliest sample time");
        p.setArgumentName("xmin");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-maximum", "xmax",
                                        "Seconds (float) from start) or GPS, default = last sample time");
        p.setArgumentName("xmax");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "X-axis zero", "epoch",
                                        "Seconds (float) from start) or GPS, default = first sample time");
        p.setArgumentName("epoch");
        p.setnDecimals(3);
        addParameter(p);
    }
    protected void addLinearYaxis()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Log Y", "logy",
                                        "Default is linear");
        p.setArgumentName("logy");
        addParameter(p);
        addYaxisLimits();
    }
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
    protected void addLogIntensity()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Linear color bar", "nologi",
                                        "Default is log");
        p.setArgumentName("lincolors");
        addParameter(p);

        addIntAxisLimits();
    }
    protected void addIntAxisLimits()
    {
        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Colorbar-minimum", "imin",
                                        "Default determined by data");
        p.setArgumentName("imin");
        p.setnDecimals(3);
        addParameter(p);

        p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Colorbar-maximum", "imax",
                                        "Default determined by data");
        p.setArgumentName("imax");
        p.setnDecimals(3);
        addParameter(p);
        
        p = ParameterFactory.buildParam(PluginParameter.Type.SWITCH, "Normalize", "norm",
                                        "Normalize intensity relative to mean");
        p.setArgumentName("norm");
        addParameter(p);
    }
    
}
