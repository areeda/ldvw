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
package edu.fullerton.ldvw;

import edu.fullerton.ldvjutils.LdvTableException;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvtables.*;
import java.sql.SQLException;
import javax.servlet.http.HttpServletRequest;

/**
 * Summarize the use log in a (hopefully) human readable fashion
 * @author Joseph Areeda <joe@areeda.com>
 */
class SiteStats
{
    HttpServletRequest request;
    Database db;
    
    SiteStats(HttpServletRequest request, Database db)
    {
        this.request = request;
        this.db = db;
    }

    /**
     * Produce a page of all stats on users and pages
     * @return
     * @throws WebUtilException
     * @throws SQLException
     * @throws LdvTableException 
     */
    PageItem getStats(String imgHistUrl) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        try
        {
            // Users
            ViewUser vu = new ViewUser(request,db);
            ret.add(vu.getStats(imgHistUrl));
            ret.addBlankLines(1);

            // pages
            ret.add(new PageItemHeader("Page summary:", 3));
            ret.addBlankLines(1);

            UseLog ul = new UseLog(db);
            ret.add(ul.getStats());
            ret.addBlankLines(1);
        }
        catch (Exception ex)
        {
            String ermsg = "Generating user and page view stats: " + ex.getClass().getSimpleName() +
                           " - " + ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
        return ret;
    }

    PageItem  getChanStats() throws SQLException, WebUtilException
    {
        String saveName = "ChannelStats";
        
        PageItemList ret = new PageItemList();
        PageItemCache pic = new PageItemCache(db);
        String savedHtml = pic.getContents(saveName);
        if (savedHtml.isEmpty())
        {
            PageItemString tblTitle = new PageItemString("By channel type:");
            tblTitle.addStyle("font-weight", "bold");
            ret.add(tblTitle);
            ret.addBlankLines(2);

            ChannelTable ct = new ChannelTable(db);
            ret.add(ct.getStatsByCType());
            ret.addBlankLines(1);

            tblTitle = new PageItemString("By IFO:Subsystem:");
            tblTitle.addStyle("font-weight", "bold");
            ret.add(tblTitle);
            ret.addBlankLines(2);

            ret.add(ct.getStatsByIfoSubsys());
            ret.addBlankLines(1);
            pic.save(saveName,ret);
        }
        else
        {
            ret.add(new PageItemString(savedHtml, false));
        }
        return ret;
    }
    
}
