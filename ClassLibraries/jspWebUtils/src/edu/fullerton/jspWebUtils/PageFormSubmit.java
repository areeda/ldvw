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
 * Object representing a input type submit html tag
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageFormSubmit extends PageFormItem
{
    private String value;

    public PageFormSubmit(String name, String value)
    {
        this.name = name;
        this.value = value;
    }

    @Override
    public String getHtml()
    {
        
        String ret = "<input type=\"submit\" name='" + name +"' value=\"" + value +  "\"";
        if (!id.isEmpty())
        {
            ret += " id='" + id + "' ";
        }
        if (!className.isEmpty())
        {
            ret += " class='" + className + "' ";
        }
        ret += "/>\n";

        return ret;
    }
    
}
