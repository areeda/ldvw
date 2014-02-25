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

import edu.fullerton.jspWebUtils.PageTableRow.RowType;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PageTableColumn extends PageItem
{
    private PageItem contents;
    private int colSpan;
    private int rowSpan;
    private RowType type;

    public PageTableColumn(Object o) throws WebUtilException
    {
        if (o instanceof PageItem)
        {
            contents = (PageItem)o;
        }
        else if (o instanceof Integer || o instanceof Float || o instanceof Double)
        {
            contents = new PageItemString(o);
            contents.setAlign(Alignment.RIGHT);
        }
        else
        {
            contents = new PageItemString(o);
        }
        type = RowType.BODY;
    }

    public void setSpan(int span)
    {
        this.colSpan = span;
    }

    public void setRowSpan(int rowSpan)
    {
        this.rowSpan = rowSpan;
    }


    public void setType(RowType rowType)
    {
        type = rowType;
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        
        String tag = type == RowType.HEAD ? "th " : "td ";
        sb.append("    <").append(tag);
        sb.append(getAttributes());
        if (colSpan > 1)
        {
            sb.append(String.format(" colspan=\"%1$d\"", colSpan));
        }
        if(rowSpan > 1)
        {
            sb.append(String.format(" rowspan=\"%1$d\"", rowSpan));
        }
        sb.append(">\n      ");
        sb.append(contents.getHtml());

        sb.append("\n    </").append(tag).append(">\n");
        return sb.toString();
    }
    public void updateHeader(Page page)
    {
        contents.updateHeader(page);
    }
}
