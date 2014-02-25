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
public class PageItemTextLink extends PageItem
{
    private String url;
    private String text;
    private String target;
    
    private PageItemTextLink()
    {
       
    }

    public PageItemTextLink(String url, String text)
    {
        this.url = url;
        this.text = text;
    }
    public PageItemTextLink(String url, String text, String target)
    {
        this.url = url;
        this.text = text;
        this.target = target;
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        if (url == null || text == null)
            throw new WebUtilException("Text link is missing the url and/or text.");
        
        String ret = "<a " + getAttributes() + "href=\"";
        ret += url + "\"";
        if (target != null && target.length() > 0)
            ret += " TARGET=\"" + target + "\"";
        
        ret += ">" + text + "</a>";
        return ret;
    }
    
}
