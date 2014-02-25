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
 * An item in an ordered (OL tag) or unordered list (UL tag)
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageItemListItem extends PageItem
{
    PageItem contents;
    
    public PageItemListItem(PageItem content)
    {
        contents = content;
    }
    public PageItemListItem(String content)
    {
        contents = new PageItemString(content);
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        String ret = "    <LI ";
        ret += getAttributes();
        ret += ">";
        ret += contents.getHtml();
        ret += "</LI>\n";
        return ret;
    }
    
}
