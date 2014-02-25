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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Drop down menu item Class
 * 
 * The items in the list can object of any class derived from PageItem and may be used anywhere 
 * a PageItem can be used
 * such as a table cell.
 * 
 * @author joe areeda
 */
public class PageItemList extends PageItem
{
    protected ArrayList<PageItem> contents = new ArrayList<PageItem>();
    protected boolean useDiv = true;
    /**
     * Add a PageItem to the list of menu choices
     * @param pi Item to add
     */
    public void add(PageItem pi)
    {
        contents.add(pi);
    }
    
    /**
     * Add a general object to the list
     * @see  PageItemString#Constructor(Object) for details
     * @param o  Object to add
     */
    public void add(Object o)
    {
        add(new PageItemString(o));
    }
    /**
     * Add a string as a separate line
     * @param s string to add
     */
    public void addLine(String s)
    {
        add(new PageItemString(s + "<br>", false));
    }
    public void addBlankLines(int n)
    {
        add(new PageItemBlanks(n));
    }
    public void addHorizontalRule()
    {
        add(new PageItemString("<hr/>\n", false));
    }

    public void setUseDiv(boolean useDiv)
    {
        this.useDiv = useDiv;
    }
    /**
     * If the items need any javascript or style sheet add it to the page so it can go out in the header
     * before we send our contents.
     *
     * @param page the html we're building
     */
    @Override
    public void updateHeader(Page page)
    {
        for (PageItem pi : contents)
        {
            pi.updateHeader(page);
        }
    }
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        String attr = getAttributes();
        String closer="";
        if (useDiv || !attr.isEmpty())
        {
            sb.append("<div ");
            if (attr.length() > 0)
            {
                sb.append(attr);
            }
            sb.append(">\n");
            closer = "</div>\n";
        }
        
        
        for(PageItem pi : contents)
        {
            sb.append(pi.getHtml());
        }
        if(closer.length()>0)
        {
            sb.append(closer);
        }
        return sb.toString();
    }
    /**
     * A convenience function for adding a selector to a form
     * @param label - String to precede the menu
     * @param paramName - name of returned value
     * @param vals - Array of items in the menu
     * @param allowMult - Allow them to select multiple items
     * @param defVal - default selection
     * @param addAny - Add the string "Any" as the first item
     * @param size - Number of items in drop down before scroll
     * @return 
     */
    public static PageItem getListSelector(String label, String paramName, Collection<String> vals, 
                                           boolean allowMult, String defVal, boolean addAny,int size)
    {
        String[] v = new String[vals.size()];
        int i=0;
        for(String it : vals)
        {
            v[i] = it;
            i++;
        }
        return getListSelector(label, paramName,v,allowMult,defVal,addAny,size);
    }
    /**
     * A convenience function for adding a selector to a form
     *
     * @param label - String to precede the menu
     * @param paramName - name of returned value
     * @param vals - Array of items in the menu
     * @param allowMult - Allow them to select multiple items
     * @param defVal - default selection
     * @param addAny - Add the string "Any" as the first item
     * @param size - Number of items in drop down before scroll
     * @return
     */
    public static PageItem getListSelector(String label, String paramName, String[] vals, 
                                           boolean allowMult, String defVal, boolean addAny, int size)
    {

        PageFormSelect fs = new PageFormSelect(paramName);
        fs.setMultAllowed(allowMult);
        if (addAny)
        {
            fs.add("any");
        }
        for (String s : vals)
        {
            fs.add(s);
        }
        if (defVal != null && defVal.length() > 0)
        {
            fs.setSelected(defVal);
        }
        if (size > 0)
        {
            fs.setSize(size);
        }

        PageItemList pil3 = new PageItemList();
        pil3.add(new PageItemString(label, false));
        pil3.add(fs);

        return pil3;
    }
}
