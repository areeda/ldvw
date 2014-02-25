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
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageFormRadio extends PageFormItem
{
    
    public class Option
    {

        String name;
        String value;
        boolean selected;

        public Option(String name, String value, Boolean selected)
        {
            this.name = name;       // what the user sees
            this.value = value;     // what is returned
            this.selected = selected; // better be only one
        }
    }
    private ArrayList<PageFormRadio.Option> options = new ArrayList<PageFormRadio.Option>();
    private boolean multAllowed = false;

    public PageFormRadio(String name)
    {
        this.name = name;
    }

    public PageFormRadio(String name, ArrayList<PageFormRadio.Option> options)
    {
        this.name = name;
        this.options = options;
    }

    public PageFormRadio(String name, String[] opt)
    {
        this.name = name;
        options = new ArrayList<PageFormRadio.Option>();
        for (String o : opt)
        {
            PageFormRadio.Option option = new PageFormRadio.Option(o, o, false);
            options.add(option);
        }
    }

    public void add(String it)
    {
        add(it, it, false);
    }

    public void add(String[] itemArray)
    {
        for (String s : itemArray)
        {
            add(s);
        }
    }

    public void add(String name, String value, Boolean selected)
    {
        options.add(new PageFormRadio.Option(name, value, selected));
    }

    public void add(PageFormRadio.Option option)
    {
        options.add(option);
    }


    public void setSelected(String nam)
    {
        for (PageFormRadio.Option o : options)
        {
            if (o.name.equals(nam))
            {
                o.selected = true;
            }
        }
    }

    @Override
    public String getHtml()
    {
        StringBuilder sb = new StringBuilder();

        for (PageFormRadio.Option o : options)
        {
            sb.append("<input type=\"radio\" name=\"").append(getName()).append("\" ");
            if (o.value.length() > 0)
            {
                sb.append(" value=\"").append(o.value).append("\" ");
            }
            if (o.selected)
            {
                sb.append(" checked ");
            }

            sb.append("/>").append(o.name);
            sb.append("<br>\n");
            
        }
        

        return sb.toString();

    }

}
