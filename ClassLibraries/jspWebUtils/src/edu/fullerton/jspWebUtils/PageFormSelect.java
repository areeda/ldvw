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
import java.util.Collection;

/**
 * An HTML drop down menu
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageFormSelect extends PageFormItem
{
    /**
     * represents a single item in drop down list
     */
    public static class Option
    {
        String name;    
        String value;
        boolean selected;
        /**
         * construct a new option object
         * @param name - text displayed
         * @param value - identifier return in form data
         * @param selected - is this item selected (more than one is valid only if multiple selection specified)
         */
        public Option(String name, String value, Boolean selected)
        {
            this.name = name;
            this.value = value;
            this.selected = selected;
        }
    }
    
    private ArrayList<Option> options = new ArrayList<Option>();
    private boolean multAllowed = false;
    private boolean escapeOptions = true;
    
    public PageFormSelect(String name)
    {
        this.name=name;
    }
    public PageFormSelect(String name, ArrayList<Option> options)
    {
        this.name = name;
        this.options = options;
    }
    public PageFormSelect(String name, String[] opt)
    {
        this.name = name;
        for(String o: opt)
        {
            Option option = new Option(o,o,false);
            options.add(option);
        }
    }
    /**
     * Add a single item to the menu with default options
     * @param it item to add
     */
    public void add(String it)
    {
        add(it, it, false);
    }
    /**
     * Add an array of items to the menu with default options
     * 
     * @param itemArray items to add
     */
    public void add(String[] itemArray)
    {
        for(String s : itemArray)
        {
            add(s);
        }
    }
    /**
     * Add a collection of items to the menu with default options
     * 
     * @param itemArray  items to add
     */
    public void add(Collection<String> itemArray)
    {
        for (String s : itemArray)
        {
            add(s);
        }
    }
    public void add(String name, String value, Boolean selected)
    {
        options.add(new Option(name,value,selected));
    }
    public void add(Option option)
    {
        options.add(option);
    }

    public boolean isMultAllowed()
    {
        return multAllowed;
    }

    public void setMultAllowed(boolean multAllowed)
    {
        this.multAllowed = multAllowed;
    }

    public void setSelected(String nam)
    {
        for(Option o : options)
        {
            if (o.name.equals(nam))
            {
                o.selected = true;
            }
        }
    }
    public static Option newOption(String name, String value, boolean selected)
    {
        return new Option(name, value, selected);
    }
    @Override
    public String getHtml()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("<select name=\"").append(name).append("\" ");
        if (multAllowed)
            sb.append(" multiple ");
        if (size > 0)
            sb.append(" size=").append(String.format("%1$d",size));
        sb.append(getAttributes());

        sb.append( ">\n");

        for(Option o: options)
        {
            sb.append( "<option ");
            if (o.value.length() > 0)
                sb.append( " value=\"").append(escapeOptions ? escape(o.name) :o.name).append("\" ");
            if (o.selected)
                sb.append(" selected=\"selected\" ");
        
            sb.append(">").append(escapeOptions ? escape(o.name) :o.name);
            sb.append("</option>\n");
        }
        sb.append( "</select>\n");

        return sb.toString();

    }

    /**
     * If options contain html character codes ( for example &Delta;) set this to false
     * 
     * @param escapeOptions if false option text will not be escaped
     */
    public void setEscapeOptions(boolean escapeOptions)
    {
        this.escapeOptions = escapeOptions;
    }
    
}
