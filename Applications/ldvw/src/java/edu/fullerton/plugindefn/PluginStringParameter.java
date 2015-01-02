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
class PluginStringParameter extends PluginParameter
{
    private String val;
    private String defaultVal="";
    
    public PluginStringParameter(String name, String formName, String comment)
    {
        super(name, formName, comment);
    }
    
    @Override
    public String getStringVal()
    {
        String ret = val;
        if (lastVal != null && lastVal.length > 0)
        {
            ret = lastVal[0];
        }
        return ret;
    }
    
    @Override
    public PluginParameter setStringVal(String val)
    {
        this.val = val;
        return this;
    }

    @Override
    public PluginParameter setStringDefault(String val)
    {
        this.defaultVal = val;
        return this;
    }
    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        PageTableRow ptr;
        String formItemName = namespace + "_" + getFormLabel();
        ptr = GUISupport.getTxtRow(formItemName, getFormLabel(), getComment(), 16, defaultVal);
        return ptr;
    }
}
