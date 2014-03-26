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
public class CrossSpectrumDefinition  extends PluginController
{

    @Override
    public void init()
    {
        if (!inited)
        {
            setName("Cross Spectrum Plot");
            setDescription("for long term trends from frames, accepts gaps in data");
            setNamespace("trndplt");

            String note = "<br>Note:  Cross spectral analysis requires 2 channels<br><br>";
            setNotes(new PageItemString(note, false));

            addAttribute(new PluginAttribute("stackable", true));
            addAttribute(new PluginAttribute("needsDataXfer", false));
            addAttribute(new PluginAttribute("program", "/usr/local/ldvw/CrossSpectrumApp/run_CrossSpectrumApp.sh "));
            addAttribute(new PluginAttribute("useEquals", false));
            addAttribute(new PluginAttribute("useQuotes", false));
            addAttribute(new PluginAttribute("nDashes", 2));

            PluginParameter p;

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "channel", null, null);
            p.setArgumentName("chan");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "start", null, null);
            p.setArgumentName("start");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "duration", null, null);
            p.setArgumentName("dur");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "geometry", null, null);
            p.setArgumentName("geom");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "tempFile", ".png", null);
            p.setArgumentName("outfile");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Sec/fft", "secpfft", "seconds per fft)");
            p.setArgumentName("secpfft");
            p.setnDecimals(2);
            p.setStringVal("1.0");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Overlap", "ovlap", "fft overlap [0-1)");
            p.setArgumentName("overlap");
            p.setStringVal("0.5");
            p.setnDecimals(2);
            addParameter(p);
        }
        inited = true;
    }
    
}
