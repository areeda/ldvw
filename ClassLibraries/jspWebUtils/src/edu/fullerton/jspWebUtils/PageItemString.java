/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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
 * @author Joseph Areeda<joseph.areeda@ligo.org>
 */
public class PageItemString extends PageItem
{
    private String contents="";

    public PageItemString()
    {
        contents="";
    }
    public PageItemString(Object it)
    {
        if (it instanceof String)
        {
            contents = escape((String) it);
        }
        else if (it instanceof Double)
        {
            contents = String.format("%1$.5f", (Double)it);
        }
        else if (it instanceof Float)
        {
            contents = String.format("%1$.3f", (Float)it);
        }
        else if (it instanceof Integer)
        {
            contents = WebUtils.commaFormat((Integer)it);
        }
        else if (it instanceof Long)
        {
            contents = WebUtils.commaFormat((Long)it);
        }
        else
        {
            contents = it.toString();
        }
    }
    public PageItemString(String it,boolean escapeit)
    {
        if (escapeit)
            contents = escape(it);
        else
            contents = it;
    }
    public PageItemString(boolean it)
    {
        contents = it ? "TRUE" : "FALSE";
    }
    public PageItemString(int it)
    {
        contents = String.format("%1$d", it);
    }
    public PageItemString(float it)
    {
        contents = String.format("%1$.3f", it);
    }
    public PageItemString(double it)
    {
        contents = String.format("%1$.3f", it);
    }

    public void setText(String t)
    {
        contents =  t==null ? "" : t; 
    }
    @Override
    public String getHtml()
    {
        String start = "", end="";
        String attr = getAttributes();
        if (attr.length() > 0)
        {
            start = "<div " + attr + "> ";
            end = "</div>\n";
        }
        return start + contents + end;
    }
    
}
