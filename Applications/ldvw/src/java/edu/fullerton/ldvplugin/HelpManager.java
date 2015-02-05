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
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormButton;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.HelpInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.HelpTextTable;
import edu.fullerton.ldvtables.ViewUser;
import viewerplugin.GUISupport;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;

/**
 * Help text is kept in a database table and provided as JSON for AJAX clients.
 * This class supports, create, edit, delete, retrieve/serve functions
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class HelpManager  extends GUISupport
{
    private final HelpTextTable htt;
    
    private String name;
    private String location;
    private String title;
    private String helpTxt;
    

    /**
     * Constructor with necessary objects for our servlets
     * @param db
     * @param vpage
     * @param vuser
     * @throws java.sql.SQLException
     */
    public HelpManager(Database db, Page vpage, ViewUser vuser) throws SQLException
    {
        super(db, vpage, vuser);
        
        htt = new HelpTextTable(db);
        
        // Initialize the current help fields
        name = "";
        location = "";
        title = "";
        helpTxt = "";
        
    }

    public void editHelp() throws WebUtilException, LdvTableException
    {
        // let them add a new one
        showForm();
    }
    
    public void editHelp(String hlpName) throws WebUtilException, LdvTableException
    {
        if (hlpName != null && !hlpName.isEmpty() && !hlpName.equalsIgnoreCase("new"))
        {
            HelpInfo hi = htt.getHelpInfo(hlpName);
            name = hi.getName();
            title = hi.getTitle();
            location = hi.getLocation();
            helpTxt = hi.getHelpTxt();
        }

        // let them add a new one
        showForm();
    }
    public boolean showForm() throws WebUtilException, LdvTableException
    {
        PageForm pf = new PageForm();
        pf.setName("edithelp");
        pf.setMethod("get");
        pf.setAction(getServletPath());
        pf.addHidden("act", "saveHelpText");
        pf.setNoSubmit(true);
        
        PageTable tbl = new PageTable();
        PageTableRow row;

        row = new PageTableRow();
        row.add();
        PageFormSelect chooser = new PageFormSelect("helpName");
        ArrayList<String> names = htt.getAllNames();
        chooser.add("New");
        chooser.add(names);
        chooser.addEvent("onchange", "this.form.submit()");
        if (!name.isEmpty())
        {
            chooser.setSelected(name);
        }
        row.add(chooser);
        row.add("Select to edit");
        row.setClassAll("noborder");
        tbl.addRow(row);
        
        row = getLabelTxtRow("name", "Name", "help key (must be unique)", 48, name);
        tbl.addRow(row);
        
        row = getLabelTxtRow("location", "Location", "Where it's used", 48, location);
        tbl.addRow(row);
        
        row = getLabelTxtRow("title", "Title", "Dialog title", 48, title);
        tbl.addRow(row);
        
        row = GUISupport.getEditTextArea("helpTxt", "Text", "Help text", 48, 8, "");
        tbl.addRow(row);
        if (!helpTxt.isEmpty())
        {
            String editorContent = helpTxt.replaceAll("'", "&#39;").replaceAll("\\n", "");
            editorContent = editorContent.replaceAll("\\r", "");
            String script = String.format("tinyMCE.get('helpTxt').setContent('%1$s');",editorContent);
            vpage.addLoadJS(script);
        }
        
        row = new PageTableRow();
        row.add();
        PageFormButton saveBtn = new PageFormButton("save", "Save", "save");
        row.add(saveBtn);
        row.add(getHelpButton("helpEditor"));
        row.setClassAll("noborder");
        tbl.addRow(row);
        
        pf.add(tbl);
        vpage.add(pf);
        
        return true;
    }

    public void saveHelpText() throws LdvTableException
    {
        String[] save = paramMap.get("save");
        
        if (save != null)
        {
            name = paramMap.get("name")[0];
            location = paramMap.get("location")[0];
            title = paramMap.get("title")[0];
            helpTxt = paramMap.get("helpTxt")[0];

            if (!name.isEmpty() && ! location.isEmpty() && ! title.isEmpty() && ! helpTxt.isEmpty())
            {
                HelpInfo hi = new HelpInfo(name, location, title, helpTxt);
                htt.save(hi);
            }
            name="";
            location="";
            title="";
            helpTxt = "";
        }
    }
    
    public PageItemList getHelpButton(String name) throws LdvTableException, WebUtilException
    {
        HelpInfo hi = htt.getHelpInfo(name);
        PageItemList helpDiv = new PageItemList();
        helpDiv.add(new PageItemString(hi.getHelpTxt(), false));
        PageFormButton closeBtn = new PageFormButton("close", "Close", "close");
        closeBtn.addEvent("onclick", "jQuery('#" + hi.getName() + "').dialog('close')");
        closeBtn.setClassName("closeButton");
        helpDiv.addBlankLines(1);
        helpDiv.add(closeBtn);
        helpDiv.setClassName("helpTxt");
        helpDiv.setId(hi.getName());
        PageItemImage btn = new PageItemImage(getContextPath() + "/help.png", "help", "");
        btn.setDim(14, 14);
        
        String dispTitle = hi.getTitle();
        if (vuser.isAdmin())
        {
            dispTitle += " (" + hi.getName() + ")";
        }
        btn.addEvent("onclick", "showHelpDiv('#" + hi.getName() +"','" + dispTitle + "');");
        
        vpage.includeJS("showHelp.js");
        
        PageItemList ret = new PageItemList();
        ret.setClassName("inlineText");
        ret.add(btn);
        ret.add(helpDiv);
        return ret;
    }
}
