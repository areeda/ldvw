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
package edu.fullerton.ldvw;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageFormText;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.Server;
import edu.fullerton.ldvtables.ServerTable;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.GUISupport;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class ServerManager extends GUISupport
{
    private ServerTable serverTable;

    
    public ServerManager(Database db, Page vpage, ViewUser vuser)
    {
        super(db,vpage,vuser);
    }

    void showForm() throws LdvTableException, WebUtilException
    {
        serverTable = new ServerTable(db);
        List<Server> existing = serverTable.getAll();
        Server editing = new Server();
        String[] selections = paramMap.get("selExisting");
        String selection = selections == null  || selections.length == 0 ? null : selections[0];
        
        if (selection != null && ! selection.equalsIgnoreCase("new"))
        {
            int pos = selection.indexOf(",");
            String selName = selection.substring(0, pos);
            editing=serverTable.getByName(selName);
            if (editing == null)
            {
                throw new IllegalArgumentException("Edit server:  Request to edit nonexistant server");
            }
        }
        
        PageForm frm = new PageForm();
        frm.setName("server");
        frm.setAction(servletPath);
        frm.addHidden("act", "procSrvFrm");
        frm.setMethod("POST");
        
        if (! existing.isEmpty())
        {
            PageFormSelect selExisting = new PageFormSelect("selExisting");
            selExisting.add("New");
            for(Server srv : existing)
            {
                String it = srv.getName() + ", " + srv.getFqdn();
                selExisting.add(it);
            }
            frm.add(selExisting);
        }
        
        PageTable newParms = new PageTable();
        newParms.addRow(getStrParamRow("Short name",editing.getName()));
        newParms.addRow(getStrParamRow("Fully qualified domain name",editing.getFqdn()));
        newParms.addRow(getStrParamRow("Est. min available time (sec) ",editing.getMinAvail().toString()));
        newParms.addRow(getStrParamRow("Est. max tim on disk (sec)",editing.getMaxOnline().toString()));
        frm.add(newParms);
        
        vpage.add(frm);
    }

    private PageTableRow getStrParamRow(String name, String defaultValue) throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        row.add(name);
        PageFormText txt = new PageFormText(name, defaultValue, 64);
        row.add(txt);
        return row;
    }

    void procForm()
    {
        if (paramMap.get("submit") != null)
        {
            String[] nicknames = paramMap.get("Short name");
            String[] fqdns = paramMap.get("Fully qualified domain name");
        }
    }
    
    
}
