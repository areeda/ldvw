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

import java.util.ArrayList;

/**
 * The unordered list class correspondin to a UL ta
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageItemUList extends PageItem
{
    
    protected ArrayList<PageItem> contents = new ArrayList<>();
    protected boolean useDiv = true;
    
    public void add(PageItem pi)
    {
        contents.add(pi);
    }
    
    /**
     * By default unordered lists are surrounded with <div></div> tags, this allows you to change that.
     * 
     * @param useIt true to use div tags false to omit them
     */
    public void setUseDiv(boolean useIt)
    {
        useDiv = useIt;
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        String ret="";
        if (useDiv)
        {
            ret += "<div>\n";
        }
        ret += "  <UL ";
        ret += getAttributes();
        ret += ">\n";
        for(PageItem pi : contents)
        {
            if (pi instanceof PageItemListItem)
            {
                ret += "   " + pi.getHtml();
            }
            else
            {
                PageItemListItem pili = new PageItemListItem(pi);
                ret += "   " + pili.getHtml();
            }
        }
        ret += "  </UL>\n";
        if (useDiv)
        {
            ret += "</div>\n";
        }
        
        return ret;
    }
}
