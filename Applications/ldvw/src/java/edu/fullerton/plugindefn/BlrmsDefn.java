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
 * UI definition for Band-limited root mean square plots
 * calls gwpy-blrms written by Andrew Lundgren
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class BlrmsDefn extends GWpyBaseDefn
{

    @Override
    public void init()
    {
        if (!inited)
        {
            // general stuff
            setName("BLRMS");
            setDescription("As implemented by Andrew Lundgren");
            setNamespace("blrms");

            // Attributes define the type of Plot product we are
            addAttribute(new PluginAttribute("stackable", false));
            addAttribute(new PluginAttribute("needsDataXfer", false));
            addAttribute(new PluginAttribute("program", "/usr/local/ldvw/bin/gwpy-brms"));
            addAttribute(new PluginAttribute("useEquals", false));
            addAttribute(new PluginAttribute("nDashes", 2));
            addAttribute(new PluginAttribute("useQuotes", false));
            
            addTimeChannel();
            addBandFilter();
            PluginParameter p2;
            p2 = ParameterFactory.buildParam(PluginParameter.Type.NUMBER, "Stride second", "stride",
                                            "Length of each RMS calculation");
            p2.setArgumentName("stride");
            p2.setnDecimals(2);
            addParameter(p2);            
        }
        inited = true;

    }

}
