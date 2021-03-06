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
 * Define UI for GWpy-ldvw version of time series
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TimeSeriesDefn  extends GWpyBaseDefn
{
    @Override
    public void init()
    {
        if (!inited)
        {
            // general stuff
            setName("Time series");
            setDescription("As implemented by GWpy");
            setNamespace("gwts");

            // Attributes define the type of Plot product we are
            addAttribute(new PluginAttribute("stackable", true));
            addConstant("timeseries");
            addCommonAttributes();
            
            addTimeChannel();
            addPreFilter();
            addLinearXaxis();
            addLinearYaxis();
        }
        inited = true;
    }

}
