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

import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.viewerplugin.GUISupport;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class PluginNumberParameter extends PluginParameter
{
    private Double val;
    private String stringDef;
    
    public PluginNumberParameter(String formLabel, String formName, String comment)
    {
        super(formLabel, formName, comment);
        val=Double.NaN;
    }
    
    @Override
    public PluginParameter setVal(double val)
    {
        this.val = val;
        return this;
    }
    
    @Override
    public double getNumberVal()
    {
        return val;
    }

    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        PageTableRow ptr;
        String formItemName = namespace + "_" + getFormName();
        ptr = GUISupport.getTxtRow(formItemName, getFormLabel(), getComment(), 16, getStringVal());
        return ptr;
    }
    
    @Override
    public PluginParameter setStringVal(String sval)
    {
        if (sval.matches(fpRegex))
        {
            val = Double.parseDouble(sval);
        }
        else
        {
            String ermsg = String.format("Value for %1$s (%2$s) is not a valid number", 
                                         getFormLabel(), sval);
            throw new IllegalArgumentException(ermsg);
        }
        return this;
    }
    @Override
    public PluginParameter setStringDefault(String def)
    {
        stringDef = def;
        return this;
    }
    @Override
    public String getStringVal()
    {
        String ret = "";
        if (lastVal != null && lastVal.length > 0)
        {
            ret = lastVal[0];
        }
        else
        {
            String fmt = String.format("%%1$.%1$df", nDecimals);


            if (val.isNaN() && stringDef != null && !stringDef.isEmpty())
            {
                setStringVal(stringDef);
            }

            if (! val.isNaN())
            {
                ret += ret.isEmpty() ? "" : " ";
                ret += String.format(fmt, val);
            }
        }
        return ret;
    }
}
