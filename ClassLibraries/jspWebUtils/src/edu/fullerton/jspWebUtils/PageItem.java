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

import java.util.Map.Entry;
import java.util.TreeMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 *
 * @author Joseph Areeda<joseph.areeda@ligo.org>
 */
abstract public class PageItem
{

    public enum Alignment
    {

        DEFAULT, LEFT, CENTER, RIGHT
    };
    private static String[] events =
    {
        "onkeydown",
        "onkeypress",
        "onkeyup",
        "onblur",
        "onchange",
        "onfocus",
        "onreset",
        "onselect",
        "onsubmit",
        "onclick",
        "ondblclick",
        "onmousedown",
        "onmousemove",
        "onmouseover",
        "onmouseout",
        "onmouseup",
        "onabort"
    };
    protected String name = "unknown";
    
    protected String id = "";
    protected String className = "";
    protected TreeMap<String,String> style = new TreeMap<String,String>();
    protected TreeMap<String,String> event = new TreeMap<String,String>();
    protected String title = "";
    /**
     * Each type must override this method to return the completed html tag
     *
     * @return full tag must be a plain string or opened and closed tag.
     */
    abstract public String getHtml() throws WebUtilException;
    
    /**
     * If this item needs a javascript or a style sheet then override this method. Base class does
     * nothing.
     *
     * @param page the object to add the things to.
     */
    public void updateHeader(Page page)
    {
    }



    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

   

    public void setAlign(Alignment align) throws WebUtilException
    {
        String val = "left";
        switch(align)
        {
            case CENTER:
                val = "center";
                break;
            case RIGHT:
                val = "right";
                break;
        }
        this.addStyle("text-align", val);
    }

    public void addEvent(String evt, String call) throws WebUtilException
    {
        int idx = ArrayUtils.indexOf(events, evt, 0);
        if (idx == ArrayUtils.INDEX_NOT_FOUND)
        {
            throw new WebUtilException("Unknown event.(" + evt + ")");
        }
        else
        {
            if (event == null)
            {
                event = new TreeMap<String,String>();
            }
            if (event.containsKey(evt))
            {
                throw new WebUtilException("Event: " + evt + " already assigned");
            }
            
            event.put(evt, call);
        }
    }
    
    public void addStyle(String st, String val) throws WebUtilException
    {
        if (style == null)
        {
            style = new TreeMap<String,String>();
        }
        if (style.containsKey(st))
        {
            throw new WebUtilException("Style: " + st + " already assigned");
        }
        
        style.put(st, val);
    }

    /**
     * Attributes are common to most tags so their functions are in the base class.
     * 
     * @return a String containing all common attributes
     */
    public String getAttributes()
    {
        StringBuilder ret = new StringBuilder();
        
        if (className.length() > 0)
            ret.append("class =\"").append(className).append("\" ");
        if (id.length() > 0)
            ret.append("id =\"").append(id).append("\" ");
        if (style.size() > 0)
        {
            ret.append("style=\"");
            for(String st: style.keySet())
            {
                ret.append(st).append(":").append(style.get(st)).append(";");
            }
            ret.append("\" ");
        }
        
        if (title.length() > 0)
            ret.append("title= \"").append(title).append("\" ");
        
        if (event.size() > 0)
        {
            for (Entry<String,String> entry : event.entrySet())
            {
                ret.append(entry.getKey()).append("=\"").append(entry.getValue()).append("\" ");
            }
        }
        return ret.toString();
    }
    public String escape(String it)
    {
        return StringEscapeUtils.escapeHtml4(it);
    }
}
