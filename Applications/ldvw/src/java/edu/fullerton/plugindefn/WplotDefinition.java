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
 * Define the User Interface and command line parameters for this version of Omega scan
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class WplotDefinition extends PluginController
{
    
    @Override
    public void init()
    {
        if (!inited)
        {
            String[] plotType =
            {
                "spectrogram_whitened", "eventgram_whitened", "eventgram_autoscaled", "spectrogram_raw",
                "eventgram_raw","spectrogram_autoscaled", "timeseries_raw",
                "timeseries_whitened", "timeseries_highpassed"
            };

            setName("Omega scan");
            setDescription("As implemented by dmt_wplot");
            setNamespace("wplt");
            
            String note = "<br>Note:  The start time specified above is used as the mid point of the Omega scan<br>"
                        + "and duration is specified with the Search time range parameter<br><br>";
            setNotes(new PageItemString(note, false));

            addAttribute(new PluginAttribute("stackable", false));
            addAttribute(new PluginAttribute("needsDataXfer", false));
            addAttribute(new PluginAttribute("program", "/usr/local/ldvw/bin/wplot dmt_wplot frameType='NDS2' "));
            addAttribute(new PluginAttribute("useEquals", true));
            addAttribute(new PluginAttribute("nDashes", 0));
            addAttribute(new PluginAttribute("listType", "dmt"));

            PluginParameter p;

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "channel", null, null);
            p.setArgumentName("channelName");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "server", null, null);
            p.setArgumentName("ndsServer");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "startDbl", null, null);
            p.setArgumentName("eventTime");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.STANDARD, "tempDir", null, null);
            p.setArgumentName("outDir");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.LIST, "Plot type", "plttyp", "One or more plots");
            p.setVal(plotType).setMultiSel(true);
            p.setArgumentName("plotType");
            p.setStringDefault("spectrogram_whitened");
            p.setListStyle("comma");
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBERARRAY, "Plot time ranges", "plttimes",
                                             "One or more plot durations separated by commas");
            p.setArgumentName("plotTimeRanges");
            p.setStringDefault("1, 4, 16");
            p.setnDecimals(0);  // these are integers
            addParameter(p);

            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Sample frequency", "smplfrq", 
                            "Frequency to which the raw time series is down-sampled before analysis.");
            p.setArgumentName("sampleFrequency");
            p.setStringDefault("4096");
            p.setnDecimals(0);
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBERARRAY, "Plot frequency range",
                                            "pltfrq", "Plot frequency min, max");
            p.setArgumentName("plotFrequencyRange");
            p.setStringDefault("0, inf");
            p.setnDecimals(0);
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBERARRAY, 
                                            "Normalized energy range", "pltenergyrange", 
                                            "Normalized energy min/max");
            p.setArgumentName("plotNormalizedEnergyRange");
            p.setStringDefault("0, 25.5");
            p.setnDecimals(1);
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Search time range", 
                          "srchtime", "Length of time on which the omega transform is calculated");
            p.setArgumentName("searchTimeRange");
            p.setnDecimals(0);
            p.setStringDefault("64");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBERARRAY, 
                                            "Search frequency range", "srchfrqrng", 
                                            "Frequency range for the Omega transform tiling");
            p.setArgumentName("searchFrequencyRange");
            p.setnDecimals(0);
            p.setStringDefault("0, inf");
            addParameter(p);
            
            p = ParameterFactory.buildParam(PluginParameter.Type.NUMBERARRAY, "Search Q range", 
                                            "srchqrng", "Q range of the Omega transform tiling");
            p.setArgumentName("searchQRange");
            p.setnDecimals(1);
            p.setStringDefault("4, 64");
            addParameter(p);
            
            
        }
        inited = true;
    }


    
}
