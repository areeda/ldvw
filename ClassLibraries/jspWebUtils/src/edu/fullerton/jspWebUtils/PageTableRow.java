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
public class PageTableRow extends PageItem
{
    public enum RowType { HEAD, BODY, FOOT};
    
    private RowType rowType;
    private ArrayList<PageTableColumn> cols;
    
    public PageTableRow()
    {
        rowType=RowType.BODY;
        cols = new ArrayList<PageTableColumn>();
    }
    public PageTableRow(Object r) throws WebUtilException
    {
        rowType = RowType.BODY;
        cols = new ArrayList<PageTableColumn>();
        add(r);
    }
    public PageTableRow(String[] strs,boolean escape) throws WebUtilException
    {
        rowType = RowType.BODY;
        cols = new ArrayList<PageTableColumn>();
        for(String s : strs)
        {
            add(s,escape);
        }
    }
    public void add() throws WebUtilException
    {
        PageTableColumn t = new PageTableColumn(new PageItemString("&nbsp;", false));
        cols.add(t);
    }
    public void add(Object r) throws WebUtilException
    {
        add(r,1);
    }
    public void add(Object r, int span) throws WebUtilException
    {
        PageTableColumn col;
        
        if (r == null)
        {
            col = new PageTableColumn("");
            if (span > 1)
            {
                col.setSpan(span);
            }
            cols.add(col);
        }
        else if (r instanceof PageTableColumn)
        {
            col = (PageTableColumn)r;
            if (span > 1)
            {
                col.setSpan(span);
            }
            cols.add(col);
        }
        else if (r instanceof Iterable)
        {
            for(Object o: (Iterable)r)
            {
                PageTableColumn c = new PageTableColumn(o);
                if (span > 1)
                {
                    c.setSpan(span);
                }
                cols.add(c);
            }
        }
        else if (r.getClass().isArray())
        {
            for(Object o: (Object[])r)
            {
                PageTableColumn c = new PageTableColumn(o);
                if (span > 1)
                {
                    c.setSpan(span);
                }
                cols.add(c);                
            }
        }
        else
        {
            PageTableColumn c = new PageTableColumn(r);
            if (span > 1)
            {
                c.setSpan(span);
            }
            cols.add(c);
        }
    }
    
    public void add(String str, boolean escape) throws WebUtilException
    {
        PageTableColumn t = new PageTableColumn(new PageItemString(str, escape));
        cols.add(t);
    }

    /**
     * Set row type to Header
     */
    public void setHeader()
    {
        rowType=RowType.HEAD;
    }
    /**
     * Set row type to Footer
     */
    public void setFooter()
    {
        rowType=RowType.FOOT;
    }

    public RowType getRowType()
    {
        return rowType;
    }

    public void setRowType(RowType rowType)
    {
        this.rowType = rowType;
    }
    
    public int getColumnCount()
    {
        return cols.size();
    }
    
    /**
     * Set the alignment option for each column in the row
     * @param algn
     */
    @Override
    public void setAlign(PageItem.Alignment algn) throws WebUtilException
    {
        for(PageTableColumn col : cols)
        {
            col.setAlign(algn);
        }
    }
    /**
     * Set the class of this row and all columns in the row
     * @param className name of the CSS class
     */
    public void setClassAll(String className)
    {
        for(PageTableColumn col : cols)
        {
            col.setClassName(className);
        }
        setClassName(className);
    }
    public void addStyleAll(String style, String val) throws WebUtilException
    {
        for(PageTableColumn col : cols)
        {
            col.addStyle(style, val);
        }
    }
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        if (cols.size() > 0)
        {
            sb.append("  <tr ").append(getAttributes()).append(">\n");
            for(PageTableColumn c : cols)
            {
                c.setType(rowType);
                sb.append(c.getHtml());
            }
            sb.append("  </tr>\n");
        }
        return sb.toString();
    }
    @Override
    public void updateHeader(Page page)
    {
        for(PageItem col : cols)
        {
            col.updateHeader(page);
        }
    }

}
