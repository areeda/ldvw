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

package edu.fullerton.plugindefn;

import edu.fullerton.jspWebUtils.PageItemString;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TrendPlotDefinition extends PluginController
{

    @Override
    public void init()
    {
        if (!inited)
        {
            String[] scaleType = { "percentile", "abs", "percent" };
            
            setName("Trend Plot");
            setDescription("for long term trends from frames, accepts gaps in data");
            setNamespace("trndplt");

            String note = "<br>Note:  These plots can take a long time to produce, especially"
                    + "if some of the frames are on tape.<br>"
                          + "You can use the email option to be notified when complete.<br><br>";
            setNotes(new PageItemString(note, false));
            
            addAttribute(new PluginAttribute("stackable", true));
            addAttribute(new PluginAttribute("needsDataXfer", false));
            addAttribute(new PluginAttribute("program", "gsissh ldas-pcdev2.ligo.caltech.edu /home/areeda/condor/trendPlot "));
            addAttribute(new PluginAttribute("useEquals", false));
            addAttribute(new PluginAttribute("nDashes", 2));
            
            PluginParameter p;

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "baseChannel", null, null);
            p.setArgumentName("chan");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "start", null, null);
            p.setArgumentName("start");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "end", null, null);
            p.setArgumentName("end");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "geometry", null, null);
            p.setArgumentName("geometry");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "tempdir", null, null);
            p.setArgumentName("datdir");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "email", null, null);
            p.setArgumentName("email");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "userName", null, null);
            p.setArgumentName("user");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.LIST, "Scaling ", "scale", 
                                            "How to scale y axis");
            p.setVal(scaleType).setMultiSel(false);
            p.setArgumentName("scale");
            p.setStringDefault("percentile");
            p.setListStyle("comma");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "min", "min",
                                            "Y-axis min");
            p.setArgumentName("min");
            p.setStringDefault("0.05");
            p.setnDecimals(2);
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "max", "max",
                                            "Y-axis max");
            p.setArgumentName("max");
            p.setStringDefault("99.95");
            p.setnDecimals(2);
            addParameter(p);

            
            inited = true;
        }
    }
    
}
