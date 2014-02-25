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
public class PageItemImage extends PageItem
{
    private String url;
    private String altText;
    private int width=0, height=0;
    
    public PageItemImage(String url, String alt, String title)
    {
        this.url = url;
        this.altText = alt;
        this.title = title == null ? "" : title;
    }

    @Override
    public String getHtml() throws WebUtilException
    {
        String ret = String.format("<img %1$s src=\"%2$s\" ", getAttributes(),url);
        if (!altText.isEmpty())
            ret += String.format(" alt=\"%1$s\" ",altText);
        if (width > 0)
            ret += String.format(" width=\"%1$d\" ", width);
        if (height > 0)
            ret += String.format(" height=\"%1$d\" ", height);
        ret += "/>\n";
        return ret;
    }
    
    public void setDim(int width, int height)
    {
        this.width=width;
        this.height=height;

    }
}
