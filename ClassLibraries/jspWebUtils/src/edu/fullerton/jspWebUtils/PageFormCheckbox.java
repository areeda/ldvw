/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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
package edu.fullerton.jspWebUtils;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageFormCheckbox extends PageFormItem
{
    private String text;
    private boolean checked;
    
    public PageFormCheckbox(String name, String txt)
    {
        this.name = name;
        text = txt;
        checked = false;
    }

    public PageFormCheckbox(String name, String txt, boolean checked)
    {
        this.name = name;
        text = txt;
        this.checked = checked;
    }
    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }

    @Override
    public String getHtml()
    {
        String ret = "<input type=\"checkbox\" name=\"" + name + "\" value=\"" + text + "\" ";
        if (checked)
        {
            ret += "checked";
        }
        ret += " " + getAttributes();
        ret += "/> " + text + "\n";

        return ret;

    }
    
}
