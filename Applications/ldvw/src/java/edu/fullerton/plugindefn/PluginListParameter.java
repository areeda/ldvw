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

import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.viewerplugin.GUISupport;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Corresponds to a pull down menu
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class PluginListParameter extends PluginParameter
{

    private final ArrayList<String> val;
    private boolean multiSel;
    private String defaultVal="";
    private String style="";
    
    public PluginListParameter(String formLabel, String formName, String comment)
    {
        super(formLabel, formName, comment);
        val = new ArrayList<>();
    }
    
    @Override
    public PluginParameter setVal(ArrayList<String> val)
    {
        this.val.addAll(val);
        return this;
    }
    @Override
    public PluginParameter setVal(String[] val)
    {
        this.val.addAll(Arrays.asList(val));
        return this;
    }
    @Override
    public String[] getStringArrayValue()
    {
        String[] ret = {};
        ret = val.toArray(ret);
        return  ret;
    }
    
    @Override
    public boolean getMultiSel()
    {
        return multiSel;
    }
    
    @Override
    public PluginParameter  setMultiSel(boolean val)
    {
        multiSel = val;
        return this;
    }

    @Override
    public PluginParameter setStringDefault(String val)
    {
        defaultVal = val;
        return this;
    }
    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        PageTableRow ptr;
        String formItemName = namespace + "_" + getFormName();
        PageFormSelect sel = new PageFormSelect(formItemName, getStringArrayValue());
        sel.setMultAllowed(getMultiSel());
        if (! defaultVal.isEmpty())
        {
            sel.setSelected(defaultVal);
        }
        ptr = GUISupport.getObjRow(sel, getComment(), "");
        return ptr;
    }
    @Override
    public PluginParameter setListStyle(String val)
    {
        style = val;
        return this;
    }

    @Override
    public String getListStyle()
    {
        return style;
    }
}
