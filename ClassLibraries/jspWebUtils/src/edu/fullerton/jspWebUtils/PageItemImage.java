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
 * A page item that contains a displayable image.  Mime types image/* and application/pdf are supported
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageItemImage extends PageItem
{
    private final String url;
    private final String altText;
    private int width=0, height=0;
    private String mime;
    
    public PageItemImage(String url, String alt, String title)
    {
        this.url = url;
        this.altText = alt;
        this.title = title == null ? "" : title;
        mime="image/png";
    }

    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder ret = new StringBuilder();
        if (mime.contains("image"))
        {
            ret.append(String.format("<img %1$s src=\"%2$s\" ", getAttributes(),url));
            if (!altText.isEmpty())
            {
                ret.append(String.format(" alt=\"%1$s\" ", altText));
            }
            if (width > 0)
            {
                ret.append(String.format(" width=\"%1$d\" ", width));
            }
            if (height > 0)
            {
                ret.append(String.format(" height=\"%1$d\" ", height));
            }
            ret.append("/>\n");
        }
        else if (mime.contains("pdf"))
        {
            ret.append("<object ");
            ret.append(getAttributes());
            ret.append(" data=\"").append(url);
            ret.append("\" type=\"application/pdf\"");
            
//            if (width > 0)
//            {
//                ret.append(String.format(" width=\"%1$d\" ", width));
//            }
//            if (height > 0)
//            {
//                ret.append(String.format(" height=\"%1$d\" ", height));
//            }
            ret.append("/>\n");
            ret.append("<p>");
            if (!altText.isEmpty())
            {
                ret.append(String.format(" Your browser does not appear to have the "
                        + "ability to display this pdf alt=\"%1$s\" ", altText));
            }
            ret.append("<a href=\"");
            ret.append(url);
            ret.append("\">Click here </a>to download the pdf.\n</p>\n");
            ret.append("</object>\n");
        }
        else
        {
            ret.append("<p>");
            if (!altText.isEmpty())
            {
                ret.append(String.format(" alt=\"%1$s\" ", altText));
            }
            ret.append("<a href=\"");
            ret.append(url);
            ret.append("\">to the PDF!</a>\n</p>\n");
            ret.append("</object>\n");
        }
        
        return ret.toString();
    }
    
    public void setDim(int width, int height)
    {
        this.width=width;
        this.height=height;

    }

    public void setMime(String mime)
    {
        this.mime = mime;
    }
    
}
