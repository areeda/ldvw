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
package edu.fullerton.ldvplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.jspWebUtils.WebUtils;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class DBManager
{
    private Database db;
    private HttpServletRequest request;
    private Page vpage;
    private ViewUser vuser;
    
    public DBManager(Database db, HttpServletRequest request, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.request = request;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    public void addStats() throws WebUtilException
    {
        String[][] columns = 
        {
            // col name         col title           format
            { "table_name",     "Table",            "str"},
            { "update_time",    "Last mod (UTC)",   "time"},
            { "row_format",     "Row type",         "str"},
            { "table_rows",     "# rows",           "num"},
            { "avg_row_length", "Avg len",          "nbytes"},
            { "data_length",    "Table size",       "nbytes"},
            { "index_length",   "Index size",       "nbytes"},
            { "data_free",      "Unused space",     "nbytes"}
        };
        try
        {
            
            PageTable statTable = new PageTable();
            statTable.setCellpadding(5);
            
            PageTableRow hdr = new PageTableRow();
            
            for(String[] colSpec : columns)
            {
                hdr.add(colSpec[1], false);
            }
            hdr.setRowType(PageTableRow.RowType.HEAD);
            statTable.addRow(hdr);
            
            ResultSet stats = db.getISStats();
            boolean odd = true;
            int nTables=0;
            long tblSize=0;
            long idxSize=0;
            
            while(stats.next())
            {
                PageTableRow tblRow = new PageTableRow();
                PageTableColumn c;
                nTables ++;
                for (String[] colSpec : columns)
                {
                    String fmt = colSpec[2].toLowerCase();
                    switch(fmt)
                    {
                        case "str":
                            String it = stats.getString(colSpec[0]);
                            it = it == null ? " " : it;
                            tblRow.add(it, true);
                            break;
                        case "num":
                            long lit = stats.getLong(colSpec[0]);
                            c = new PageTableColumn(String.format("%1$,d",lit));
                            c.setAlign(PageItem.Alignment.RIGHT);
                            tblRow.add(c);
                            break;
                        case "nbytes":
                            long nit = stats.getLong(colSpec[0]);
                            c = new PageTableColumn(WebUtils.hrInteger(nit)+"B");
                            c.setAlign(PageItem.Alignment.RIGHT);
                            tblRow.add(c);
                            switch(colSpec[0])
                            {
                                case "data_length":
                                    tblSize += nit;
                                    break;
                                case "index_length":
                                    idxSize += nit;
                                    break;
                            }
                            break;
                        case "time":
                            Timestamp tit = stats.getTimestamp(colSpec[0]);

                            if (tit != null)
                            {
                                Date d = new Date(tit.getTime());
                                tblRow.add(TimeAndDate.dateAsUtcString(d), true);
                            }
                            else
                            {
                                tblRow.add();
                            }
                            break;
                        default:
                            tblRow.add("Error unknown format", true);
                            break;
                    }
                }
                tblRow.setClassName(odd ? "odd" : "even");
                tblRow.setClassAll(odd ? "odd" : "even");
                odd = ! odd;
                statTable.addRow(tblRow);
            }
            vpage.add(statTable);
            vpage.addBlankLines(2);
            vpage.add("Summary:");
            vpage.addBlankLines(1);
            PageTable summary = new PageTable();
            PageTableRow srow = new PageTableRow();
            PageTableColumn c;
            
            srow.add("# tables", false);
            c = new PageTableColumn(String.format("%1$,d", nTables));
            c.setAlign(PageItem.Alignment.RIGHT);
            srow.add(c);
            summary.addRow(srow);
            
            srow = new PageTableRow();
            srow.add("total size of tables", false);
            c = new PageTableColumn(WebUtils.hrInteger(tblSize)+"B");
            c.setAlign(PageItem.Alignment.RIGHT);
            srow.add(c);
            summary.addRow(srow);
            
            srow = new PageTableRow();
            srow.add("total size of indexes", false);
            c = new PageTableColumn(WebUtils.hrInteger(idxSize) + "B");
            c.setAlign(PageItem.Alignment.RIGHT);
            srow.add(c);
            summary.addRow(srow);

            vpage.add(summary);
            
        }
        catch (Exception ex)
        {
            
            throw new WebUtilException("Generating database statistics", ex);
        }
    }
    
}
