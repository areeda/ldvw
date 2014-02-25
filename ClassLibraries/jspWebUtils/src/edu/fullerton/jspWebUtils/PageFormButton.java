/*
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
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
 * Class representing a button element, depending on type it may be similar to a input type submit
 * but is more flexible in content
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageFormButton  extends PageFormItem
{
    private String type;
    private String text;
    private String value;
    private PageItemImage icon;
    private boolean enabled;
    
    public PageFormButton(String name)
    {
        this.name = name;
        text = "";
        value = "";
        icon = null;
        enabled = true;
        type="submit";
    }
    public PageFormButton(String name, String text, String value)
    {
        this.name = name;
        this.text = text;
        this.value= value;
        icon = null;
        enabled = true;
        type="submit";
    }

    public void setType(String type) throws WebUtilException
    {
        if (type.equalsIgnoreCase("submit") || type.equalsIgnoreCase("button")  
            || type.equalsIgnoreCase("reset"))
        {
            this.type = type;
        }
        else
        {
            throw new WebUtilException("Button type: " + type + " is not valid.");
        }
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public void setIcon(PageItemImage iconUrl)
    {
        this.icon = iconUrl;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
    
    
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder ret = new StringBuilder();
        ret.append("<button name='").append(name).append("' ");
        if (!enabled)
        {
            ret.append(" disabled ");
        }
        ret.append(" type='").append(type).append("' ");
        if (!value.isEmpty())
        {
            ret.append(" value='").append(value).append("' ");
        }
        ret.append(getAttributes());
        if ((text == null || text.isEmpty()) && icon == null)
        {
            ret.append("></button>\n");
        }
        else
        {
            ret.append(">");
            if (icon != null)
            {
                ret.append(icon.getHtml());
            }
            if (text != null && !text.isEmpty())
            {
                ret.append(text);
            }
            ret.append("</button>\n");
        }
        return ret.toString();
    }
    
}
