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

import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import viewerplugin.GUISupport;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class PluginSwitchParameter extends PluginParameter
{
    boolean val=false;
    /**
     * Rendered as an html checkbox in a 3 column row
     * | __blank__ | [] name | comment
     * @param name
     * @param formName
     * @param comment 
     */
    PluginSwitchParameter(String name, String formName, String comment)
    {
        super(name,formName,comment);
        if (name == null || name.isEmpty())
        {
            throw new IllegalArgumentException("formName can't be null for a switch "
                    + "otherwise we end up with a naked checkbox");
        }
    }
    
    @Override
    public boolean getBoolValue()
    {
        boolean ret = val;
        
        return val;
    }

    @Override
    public PluginParameter setVal(boolean val)
    {
        this.val = val;
        return this;
    }

    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        PageTableRow ptr;
        String formItemName = namespace + "_" + getArgumentName();
        PageFormCheckbox cbox = new PageFormCheckbox(formItemName, getFormLabel(), getBoolValue());
        ptr = GUISupport.getObjRow(cbox, "", getComment());
        return ptr;
    }
    
}
