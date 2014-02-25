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
public class PageItemHeader  extends PageItem
{
    private String contents;
    private Integer level;
    
    public PageItemHeader(String it)
    {
        contents = escape(it);
        level = 1;
    }
    
    public PageItemHeader(String it, int lev,boolean escapeIt) throws WebUtilException
    {
        
        contents = escapeIt ? escape(it) : it;
        if (lev < 1 || lev > 6)
        {
            throw new WebUtilException(String.format("Invalid level (%1$d) for PageItemHeader", level));
        }
        level = lev;
    }
    public PageItemHeader(String it, int lev) throws WebUtilException
    {
         contents = escape(it);
        if (lev < 1 || lev > 6)
        {
            throw new WebUtilException(String.format("Invalid level (%1$d) for PageItemHeader", level));
        }
        level = lev;
    }
    @Override
    public String getHtml()
    {
        return String.format("<h%1$1d %2$s>%3$s</h%4$d>\n",level,getAttributes(),contents,level);
    }
    
}
