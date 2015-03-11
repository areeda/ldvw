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
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class PluginNumberArrayParameter extends PluginParameter
{
    private final ArrayList<Double> values;
    
    private String stringDef;
    
    public PluginNumberArrayParameter(String name, String formName, String comment)
    {
        super(name, formName, comment);
        values = new ArrayList<>();
        nDecimals = 0;
        stringDef = "";
    }

    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        PageTableRow ptr;
        String formItemName = namespace + "_" + getFormName();
        ptr = GUISupport.getTxtRow(formItemName, getFormLabel(), getComment(), 16, getStringVal());
        return ptr;
    }
    
    public PluginParameter setStringVal(String val)
    {
        String[] vals;
        if (val.contains(","))
        {
            vals = val.split(",");
        }
        else if (val.trim().contains(" "))
        {
            vals = val.split(" ");
        }
        else
        {
            vals = new String[1];
            vals[0] = val;
        }
        for(String v : vals)
        {
            v = v.trim();
            Double dval = null;
            if (v.matches(fpRegex))
            {
                dval = Double.parseDouble(v);
            }
            else if (v.equalsIgnoreCase("inf"))
            {
                dval = Double.POSITIVE_INFINITY;
            }
            if (!values.contains(dval))
            {
                values.add(dval);
            }
        }
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
            if (values.isEmpty() && ! stringDef.isEmpty())
            {
                setStringVal(stringDef);
            }
            if (values.size() > 1)
            {
                for(Double val: values)
                {
                    ret += ret.isEmpty() ? "" : " ";
                    if (val.isInfinite())
                    {
                        ret += "inf";
                    }
                    else
                    {
                        ret += String.format(fmt,val);
                    }
                }
            }
            else if (values.size() == 1)
            {
                ret = String.format(fmt,values.get(0));
            }
        }
        return ret;
    }
    @Override
    public PluginParameter setStringDefault(String def)
    {
        stringDef = def;
        return this;
    }
    
}
