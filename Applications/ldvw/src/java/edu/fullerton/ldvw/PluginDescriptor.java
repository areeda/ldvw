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

import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ViewUser;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
class PluginDescriptor extends Table
{
    protected HttpServletRequest request = null;
   
    protected Page vpage = null;
    protected ViewUser vuser = null;
    public enum ResultType
    {

        IMAGE, TEXT, HTML
    };
    String name;
    String publicName;
    ResultType rType;
    ArrayList<PlgFldDesc> flds = new ArrayList<>();

    PluginDescriptor(HttpServletRequest request, Database db, Page vpage, ViewUser vuser)
    {
        this.request = request;
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    void makeNew() throws WebUtilException
    {
        vpage.add(new PageItemHeader("Define new plugin", 3));
        
    }
    
}
