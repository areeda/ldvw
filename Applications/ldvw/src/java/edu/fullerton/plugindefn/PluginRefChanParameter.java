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

import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.viewerplugin.GUISupport;
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PluginRefChanParameter extends PluginParameter
{
    private String refChanname;

    public PluginRefChanParameter(String formLabel, String formName, String comment)
    {
        super(formLabel, formName, comment);
    }

    @Override
    PageTableRow getSelectorRow(String namespace) throws WebUtilException
    {
        if (baseSelections == null || baseSelections.isEmpty())
        {
            throw new WebUtilException("Request for reference channel selector with empty chan list");
        }
        ArrayList<String> names = new ArrayList<>();
        for (BaseChanSelection bcs : baseSelections)
        {
            String cname = bcs.getName();
            names.add(cname);
        }
        String[] str = new String[0];
        String formItemName = namespace + "_" + getFormName();

        PageFormSelect refChanSelect = new PageFormSelect(formItemName, names.toArray(str));
        PageTableRow ptr = GUISupport.getObjRow(refChanSelect, getFormLabel(), getComment());
        
        return ptr;
    }

    /**
     * Get the name of the reference channel
     * @return the base name of the channel
     */
    @Override
    public String getStringVal()
    {
        return refChanname;
    }

    /**
     * Set the base name of the reference channel
     * @param val - channel name
     * @return this object so setters may be cascaded
     */
    @Override
    public PluginParameter setStringVal(String val)
    {
        refChanname = val;
        return this;
    }

}
