/*
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
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
 * Builds a Top level navigation bar with buttons submenus
 * It is dependent on Bootstrap and jQuery
 * 
 * @see http://getbootstrap.com/2.3.2/components.html#navbar
 * @see http://bootswatch.com/
 * @see http://jquery.com/
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageItemBSNavBar extends PageItem
{
    private ArrayList<PageItemListItem> elements;
    private String curSubmenuName;
    private ArrayList<PageItem> curSubmenuItems;
    
    public PageItemBSNavBar()
    {
        elements = new ArrayList<>();
        curSubmenuName = null;
        curSubmenuItems = null;
    }
    public void addLink(String url, String label)
    {
        addLink(url, label, "");
    }
    public void addLink(String url, String label, String target)
    {
        PageItemTextLink link;
        if (target != null && ! target.isEmpty())
        {
            link = new PageItemTextLink(url, label, target);
        }
        else
        {
            link = new PageItemTextLink(url, label);
        }
        PageItemListItem pili = new PageItemListItem(link);
        elements.add(pili);
    }
    public void createNewSubmenu(String label) throws WebUtilException
    {
        if (curSubmenuName != null)
        {
            throw new WebUtilException("Attempt to create new submenu without closing previous");
        }
        curSubmenuName = label;
        curSubmenuItems = new ArrayList<>();
    }
    public void addSubmenuLink(String url, String label) throws WebUtilException
    {
        addSubmenuLink(url, label, "");
    }
    public void addSubmenuLink(String url, String label, String target) throws WebUtilException
    {
        if (curSubmenuItems == null)
        {
            throw new WebUtilException("Attempt to add an item to a submenu wihout creating one");
        }
        PageItemTextLink link;
        if (target != null && !target.isEmpty())
        {
            link = new PageItemTextLink(url, label, target);
        }
        else
        {
            link = new PageItemTextLink(url, label);
        }
        curSubmenuItems.add(link);
    }
    public void addCurSubmenu() throws WebUtilException
    {
        if (curSubmenuName == null || curSubmenuItems == null)
        {
            throw new WebUtilException("Attempt to add null submenu to nav bar.");
        }
        PageItemList pil = new PageItemList();
        pil.setUseDiv(false);
        PageItemDropDownToggle piddt = new PageItemDropDownToggle(curSubmenuName);
        pil.add(piddt);
        
        PageItemUList submenu = new PageItemUList();
        submenu.setClassName("dropdown-menu");
        submenu.setUseDiv(false);
        for(PageItem pi : curSubmenuItems)
        {
            submenu.add(pi);
        }
        pil.add(submenu);
        
        PageItemListItem pili = new PageItemListItem(pil);
        pili.setClassName("dropdown");
        elements.add(pili);
        
        curSubmenuName = null;
        curSubmenuItems = null;
    }
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder html = new StringBuilder();
        html.append("<!--Add a bootstrap navbar-->\n");
        html.append("<nav class=\"navbar navbar-default\" role=\"navigation\">\n" +
            "<!-- Brand and toggle get grouped for better mobile display -->\n" +
            "  <div class=\"navbar-header\">\n" +
            "    <button type=\"button\" class=\"navbar-toggle\" data-toggle=\"collapse\" data-target=\".navbar-ex1-collapse\">\n" +
            "        <span class=\"sr-only\">Toggle navigation</span>\n" +
            "        <span class=\"icon-bar\"></span>\n" +
            "        <span class=\"icon-bar\"></span>\n" +
            "        <span class=\"icon-bar\"></span>\n" +
            "    </button>\n" +
            "  </div>\n");
        html.append("  <div class=\"collapse navbar-collapse navbar-ex1-collapse\">\n");
        
        PageItemUList navBar = new PageItemUList();
        navBar.setClassName("nav navbar-nav");
        navBar.setUseDiv(false);
        
        for(PageItemListItem pili : elements)
        {
            navBar.add(pili);
        }

        html.append(navBar.getHtml());
        html.append("  </div><!-- /.navbar-collapse -->\n"
                + "  </nav>\n");
        
        return html.toString();
    }
    /**
     * add the necessary javascript and css files to the page
     * @param page 
     */
    @Override
    public void updateHeader(Page page)
    {
        page.includeCSS("jquery/bootstrap.min.css");
        page.includeJS("bootstrap.min.js");
    }
    
}
