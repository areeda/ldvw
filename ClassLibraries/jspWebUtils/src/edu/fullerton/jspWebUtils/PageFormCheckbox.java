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
 * A item that appears as a check box on a form, returns a boolean
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageFormCheckbox extends PageFormItem
{
    private final String text;
    private boolean checked;
    private String value;
    
    public PageFormCheckbox(String name, String txt)
    {
        this.name = name;
        text = txt;
        checked = false;
        value = "";
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

    /**
     * Value is the parameter key returned from form
     * @return current value may be empty but not null
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Value is the parameter key returned from form
     * If empty then text is used
     * @param value 
     */
    public void setValue(String value)
    {
        if (value == null)
        {
            this.value = "";
        }
        else
        {
            this.value = value;
        }
    }

    /**
     * get the HTML representation of this item
     * @return html
     */
    @Override
    public String getHtml()
    {
        String ret = "<input type=\"checkbox\" name=\"" + name + "\" value=\"";
        if (value == null || value.isEmpty())
        {
            ret += text + "\" ";
        }
        else
        {
            ret += value + "\" ";
        }
        if (checked)
        {
            ret += "checked";
        }
        ret += " " + getAttributes();
        ret += "/> " + text + "\n";

        return ret;

    }
    
}
