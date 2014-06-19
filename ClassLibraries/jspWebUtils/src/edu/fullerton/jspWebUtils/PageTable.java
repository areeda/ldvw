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
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageTable extends PageItem
{

    private ArrayList<PageItem> hdrRows;
    private ArrayList<PageItem> rows;
    private ArrayList<PageItem> ftrRows;
    
    // table properties
    private int border  = 1;
    private int cellspacing  = 0;
    private int cellpadding  = 0;
    private String bordercolor  = "#000066";
    private int width  = 0;
    private boolean wPct = false;
    private boolean sortable  = false;
    private int initialSortColumn = -1;
    private int initialSortDir=0;
    private int eqlCols=0;
    
    public PageTable()
    {
        hdrRows = new ArrayList<PageItem>();
        rows = new ArrayList<PageItem>();
        ftrRows = new ArrayList<PageItem>();
        
    }
    @Override
    public String getHtml() throws WebUtilException
    {
        
        StringBuilder sb = new StringBuilder();
        sb.append("<table ");
        sb.append(getAttributes());
        if (width > 0)
        {
            sb.append(Integer.toString(width));
            if (wPct)
            {
                sb.append("%");
            }
        }
        sb.append(">\n");
        if (eqlCols > 0)
        {
            int pwidth = (int) Math.floor(100./eqlCols);
            String cw = String.format("<col style=\"width: %1$d%%\" >%n",pwidth);
            for(int i=0;i<eqlCols;i++)
            {
                sb.append(cw);
            }
        }

        //-- Add the rows to the output
        if (!hdrRows.isEmpty())
        {
            
            sb.append(" <thead>\n");
            for(PageItem pi : hdrRows)
            {
                PageTableRow r = (PageTableRow)pi;
                if (cellpadding > 0)
                {
                    r.addStyleAll("padding", String.format("%1$dpx",cellpadding));
                }
                sb.append(r.getHtml());
            }
            sb.append(" </thead>\n");
        }
        sb.append(" <tbody>\n");
        for (PageItem pi : rows)
        {
            PageTableRow r = (PageTableRow) pi;
            if (cellpadding > 0)
            {
                r.addStyleAll("padding", String.format("%1$dpx", cellpadding));
            }
            sb.append(r.getHtml());
        }
        sb.append(" </tbody>");
        if (!ftrRows.isEmpty())
        {
            sb.append(" <tfoot>\n");
            for (PageItem pi : ftrRows)
            {
                PageTableRow r = (PageTableRow) pi;
                if (cellpadding > 0)
                {
                    r.addStyleAll("padding", String.format("%1$dpx", cellpadding));
                }
                sb.append(r.getHtml());
            }
            sb.append(" </tfoot>\n");
        }
        sb.append("</table>\n");
        
        return sb.toString();
    }
    public int getRowCount()
    {
        int ret = hdrRows.size() + rows.size() + ftrRows.size();
        return ret;
    }
    
    public void addRow(Object r) throws WebUtilException
    {
        addRow(r,1);
    }
    public void addRow(Object r, int span) throws WebUtilException
    {
        if (r instanceof PageTableRow)
        {
            PageTableRow tr=(PageTableRow)r;
            switch(tr.getRowType())
            {
                case HEAD:
                    hdrRows.add(tr);
                    break;
                case FOOT:
                    ftrRows.add(tr);
                    break;
                default:
                    rows.add(tr);
            }
        }
        else
        {
            PageTableRow tr = new PageTableRow();
            PageTableColumn tc = new PageTableColumn(r);
            if (span > 1)
                tc.setSpan(span);
            tr.add(tc);
            rows.add(tr);
        }
    }
    /**
     * If we need any javascript or style sheet add it to the page so it can go out in the header
     * before we send our contents.
     *
     * @param page the html we're building
     */
    @Override
    public void updateHeader(Page page)
    {
        if (sortable)
        {
            if (!getId().isEmpty())
            {
                
                page.includeJS("jquery.tablesorter.js");
                if (initialSortColumn >= 0)
                {
                    String isort=String.format("jQuery(\"#%1$s\").tablesorter({ " +
                                                " sortList: [[%2$d,%3$d]], widgets:['zebra'] " +
                                                " }); ",getId(),initialSortColumn,initialSortDir);
                    page.addReadyJS(isort);
                }
                else
                {
                    String isort = String.format("jQuery(\"#%1$s\")."
                                               + "tablesorter({widgets:['zebra']}); ", getId());
                    page.addReadyJS(isort);
                }
            }
            setClassName("tablesorter");
        }
        updateHdrForRow(page,hdrRows);
        updateHdrForRow(page,ftrRows);
        updateHdrForRow(page,rows);
                
    }
    private void updateHdrForRow(Page page, Collection<PageItem> rows)
    {
        for (PageItem row : rows)
        {
            row.updateHeader(page);
        }
    }
    public int getBorder()
    {
        return border;
    }

    public void setBorder(int border) throws WebUtilException
    {
        
        if (border == 0)
            this.addStyle("border", "0px");
        else
        {
            this.addStyle("border-collapse", "collapse");
            this.addStyle("border", String.format("%1$dpx",border));
        }
        this.border = border;
    }

    public String getBordercolor()
    {
        return bordercolor;
    }

    public void setBordercolor(String bordercolor)
    {
        this.bordercolor = bordercolor;
    }

    public int getCellpadding()
    {
        return cellpadding;
    }

    public void setCellpadding(int cellpadding) throws WebUtilException
    {
        this.cellpadding = cellpadding;
        addStyle("padding", String.format("%1$dpx", cellpadding));
    }

    public int getCellspacing()
    {
        return cellspacing;
    }

    public void setCellspacing(int cellspacing)
    {
        this.cellspacing = cellspacing;
    }

    public int getWidth()
    {
        return width;
    }

    public void setWidth(int width) throws WebUtilException
    {
        addStyle("width", String.format("%1$dpx",width));
    }
    public void setWidthPct(int width) throws WebUtilException
    {
        addStyle("width", String.format("%1$d%%",width));
    }

    public boolean isSortable()
    {
        return sortable;
    }

    public void setSortable(boolean sortable)
    {
        this.sortable = sortable;
    }

    public int getInitialSortColumn()
    {
        return initialSortColumn;
    }

    public void setInitialSortColumn(int initialSortColumn)
    {
        initialSortDir=0;
        this.initialSortColumn = initialSortColumn;
    }
    public void setInitialSortColumn(int initialSortColumn, int dir)
    {
        this.initialSortDir = dir == 0 ? 0 : 1;
        this.initialSortColumn = initialSortColumn;
    }

    /**
     * Defines the table columns with the style setting width to a percent of the window
     * @param ncols 
     */
    public void setColEqualWidth(int ncols)
    {
        eqlCols = ncols;
    }
    
}
