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

import edu.fullerton.viewerplugin.GUISupport;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageCoordinateTbl;
import edu.fullerton.ldvtables.ImageGroupTable;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Display a page of images from our database
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
class ImageHistory extends GUISupport
{

    private int strt = 0;
    private int cnt = 25;
    private int cols = 1;
    private int imgCnt;
    private final ImageTable imgTbl;
    private final ImageGroupTable imgGrpTbl;
    private final String ourAction = "imagehistory";
    private final String curUser;
    private String userWanted;
    private String groupWanted;
    private TreeSet<Integer> selected;
    private ArrayList<Integer> ids = null;      // images to display
    private boolean keepSelections;
    private String submitAct;
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public ImageHistory(HttpServletRequest request, HttpServletResponse response, Database db, Page vpage, ViewUser vuser) throws SQLException
    {
        super( db, vpage, vuser);
        this.request = request;
        this.response = response;
        imgTbl = new ImageTable(db);
        imgGrpTbl = new ImageGroupTable(db);
        userWanted = vuser.getCn();
        curUser = vuser.getCn();
        groupWanted = "";
        selected = new TreeSet<>();
    }

    /**
     * Process the command and display the images requested
     * 
     * @throws SQLException
     * @throws WebUtilException
     * @throws LdvTableException 
     */
    void show() throws SQLException, WebUtilException, LdvTableException
    {
        vpage.includeJS("setChkBoxByClass.js");
        
        processParams();
       
        imgCnt = getImageList();
        
        PageTable table = new PageTable();
        table.setWidthPct(100);
        table.setColEqualWidth(cols);
        table.setBorder(3);
        table.setCellpadding(15);
        table.setCellspacing(5);
        PageTableRow imgRow = new PageTableRow();
        int width = 0;
        if (cols > 1 && imgCnt > 1)
        {
            width = 1024/cols;
            width = Math.max(width, 128);
        }
        if (ids == null)
        {
            ids = new ArrayList<>();
        }
        for (Integer id : ids)
        {
            PageItemList pil = new PageItemList();
            pil.add(getImageIdLine(id));
            
            String url = String.format("%1$s?act=getImg&amp;imgId=%2$d", getServletPath(), id);
            String imgName = String.format("Image #%1$d", id);
            
            PageItemImage piImg;
            if (width == 0)
            {   // full size just show them the image
                piImg = new PageItemImage(url, imgName, null);
                ImageCoordinateTbl ict = new ImageCoordinateTbl(db);
                ImageCoordinate imgCord = ict.getCoordinate(id);
                if (imgCord != null)
                {
                    vpage.includeJS("ShowTimeUnderMouse.js");
                    vpage.addLoadJS(imgCord.getHeadJS());
                    String className = imgCord.getClassName();
                    String imageIdname = String.format("%1$s_%2$d", className, id);
                    String timeId = String.format("time_%1$d", id);
                    piImg.setClassName(className);
                    piImg.setId(imageIdname);
                    vpage.addBodyJS(imgCord.getInitJS(imageIdname, timeId));
                    pil.add(piImg);
                    PageItemString timeStr = new PageItemString("Click in image for time under mouse.");
                    timeStr.setId(timeId);
                    pil.add(timeStr);
                }
                else
                {
                    pil.add(piImg);
                }
                
            }
            else
            {
                String thumbUrl = url + String.format("&amp;width=%1$d",width);
                piImg = new PageItemImage(thumbUrl, imgName, null);
                piImg.addStyle("width", "100%");
                PageItemImageLink thumb = new PageItemImageLink(url, piImg, "_blank");
                pil.add(thumb);
            }
            pil.addBlankLines(2);
            imgRow.add(pil);
            if (imgRow.getColumnCount() >= cols)
            {
                table.addRow(imgRow);
                imgRow = new PageTableRow();
            }
        }
        int lastRowCount = imgRow.getColumnCount();
        if (lastRowCount < cols && lastRowCount > 0)
        {
            for (int i=lastRowCount;i<= cols;i++)
            {
                imgRow.add();
            }
            table.addRow(imgRow);
            
        }
        PageForm frm = new PageForm();
        String frmName="imgHistory";
        frm.addHidden("strt", Integer.toString(strt));
        frm.setName(frmName);
        frm.setId(frmName);
        frm.setMethod("get");
        frm.setAction(getServletPath());
        frm.addHidden("act", "imagehistory");
        frm.setNoSubmit(true);

        PageItem navBar = getNavBar(imgCnt, false, frmName);
        frm.add(navBar);
        frm.add(new PageItemBlanks(1));

        frm.add(table);
        
        if (imgCnt > 10)
        {
            frm.add(new PageItemBlanks(1));
            PageItem navBar2 = getNavBar(imgCnt, true, frmName);
            frm.add(navBar2);
        }
        frm.add(new PageItemBlanks(2));
        if (keepSelections)
        {
            for(Integer sel : selected)
            {
                String sname = String.format("sel_%1$d",sel);
                frm.addHidden(sname, sname);
            }
        }
        vpage.add(frm);
    }

    //=================
    /**
     * Process the http request and fill in fields with the parameters and defaults
     */
    private void processParams() throws LdvTableException
    {
        keepSelections = false;     // depending on the command we may or may not want to pass
                                    // selections onto the next refresh

        submitAct = request.getParameter("submitAct");
        submitAct = submitAct == null ? "" : submitAct;

        selected = getSelected();
        
        String size = request.getParameter("size");
        size = size == null ? "" : size;
        if (size.equalsIgnoreCase("large"))
        {
            cols = 1;
            cnt = 25;
        }
        else if (size.equalsIgnoreCase("med"))
        {
            cols = 3;
            cnt = 48;
        }
        else if (size.equalsIgnoreCase("small"))
        {
            cols = 5;
            cnt = 75;
        }
        else
        {
            cols = 1;
            cnt = 25;
        }

        String strtStr = request.getParameter("strt");
        if (strtStr != null && strtStr.length() > 0)
        {
            strt = Integer.parseInt(strtStr);
        }
        else
        {
            strt = 0;
        }

        if (submitAct.equalsIgnoreCase("next"))
        {
            strt += cnt;
            keepSelections = true;
        }
        else if (submitAct.equalsIgnoreCase("prev"))
        {
            strt -= cnt;
            if (strt < 0)
            {
                strt = 0;
            }
            keepSelections = true;
        }
        else if (submitAct.equalsIgnoreCase("go"))
        {
            String pageNum = request.getParameter("pageNum").trim();
            if (pageNum.matches("^\\d+$"))
            {
                strt = (Integer.parseInt(pageNum) - 1) * cnt;
            }
            keepSelections = true;
        }
        else if (submitAct.equalsIgnoreCase("go2"))
        {
            String pageNum = request.getParameter("pageNum2").trim();
            if (pageNum.matches("^\\d+$"))
            {
                strt = (Integer.parseInt(pageNum) - 1) * cnt;
            }
            keepSelections = true;
        }
        else if (submitAct.equalsIgnoreCase("delete selected"))
        {
            for (Integer id : selected)
            {
                try
                {
                    imgTbl.deleteById(id);
                }
                catch (SQLException ex)
                {
                    // we'll ignore it for now
                }
            }
            selected.clear();
        }
        
        String userSelected = request.getParameter("usrSel");
        if (userSelected != null && !userSelected.isEmpty())
        {
            int p = userSelected.indexOf("(");
            if (p > 0)
            {
                userSelected=userSelected.substring(0, p).trim();
            }
            userWanted = userSelected;
        }
        String group = request.getParameter("group");
        groupWanted = "";
        if (group != null && !group.isEmpty())
        {
            groupWanted = group;
        }
        processGroupOps();
    }

    /**
     * if they requested a group maintenance operation do it here
     */
    private void processGroupOps() throws LdvTableException
    {
        int nSelected = selected.size();
        String selectedGroup = request.getParameter("op_group");
        
        if (submitAct.equalsIgnoreCase("add to grp"))
        {
            if (nSelected < 1)
            {
                vpage.add("No images were selected for add to group operation");
            }
            else
            {
                imgGrpTbl.addToGroup(curUser, selectedGroup, selected);
            }
        }
        else if (submitAct.equalsIgnoreCase("del from grp"))
        {
            if (nSelected < 1)
            {
                vpage.add("No images were selected for delete from group operation");
            }
            else
            {
                imgGrpTbl.deleteFromGroup(curUser, selectedGroup, selected);
            }
        }
        else if (submitAct.equalsIgnoreCase("new grp"))
        {
            if (nSelected < 1)
            {
                vpage.add("No images were selected for create new group operation");
            }
            else
            {
                String newGrp = request.getParameter("newGrpName");
                if (newGrp == null || newGrp.isEmpty() || newGrp.equalsIgnoreCase("<new group name>"))
                {
                    vpage.add("No name was entered for the new group.");
                }
                else
                {
                    imgGrpTbl.addToGroup(curUser, newGrp, selected);
                }
            }
        }

    }
    /**
     * Get matching record count. 
     * It has some smarts to deal with the possibility of selecting empty users or groups
     * That can happen if:
     *     a) a group is selected for one user and they change to another without that group
     *     b) they delete all images from a user or group
     *     c) this is run before anyone creates an image
     * 
     * @return number of records available for display
     */
    private int getImageList() throws LdvTableException, SQLException
    {
        boolean gotGroup;
        imgCnt=0;
        // check if they wanted a particular group or all from the user
        if (!groupWanted.isEmpty() && !groupWanted.equalsIgnoreCase("all"))
        {
            imgCnt = imgGrpTbl.getCount(userWanted, groupWanted);
            gotGroup = true;
        }
        else
        {
            imgCnt = (int) imgTbl.getDistinctRecordCount(userWanted);
            gotGroup = false;
        }
        // if that didn't work try all images for this user
        if (imgCnt == 0 && !groupWanted.isEmpty() && !groupWanted.equalsIgnoreCase("all"))
        {
            groupWanted="All";
            imgCnt = (int) imgTbl.getDistinctRecordCount(userWanted);
            gotGroup = false;
        }
        // if that didn't work try all images for all users
        if (
                imgCnt == 0 && 
                (groupWanted.isEmpty() || groupWanted.equalsIgnoreCase("all") )
                && !userWanted.equalsIgnoreCase("all") 
            )
        {
            userWanted = "All";
            imgCnt = (int) imgTbl.getDistinctRecordCount(userWanted);
            gotGroup = false;
        }
        if (imgCnt > 0)
        {
            if (imgCnt < cnt)
            {
                strt = 0;
            }
            int stop = Math.min(strt + cnt, imgCnt);
            if (strt >= stop)
            {
                strt = Math.max(0, stop - cnt);
            }
            if (gotGroup)
            {
                ids = imgGrpTbl.getGroupMembers(userWanted, groupWanted, strt, stop);
            }
            else
            {
                ids = imgTbl.getImgList(strt, stop, userWanted);
            }
        }
        
        return imgCnt;
    }

    /**
     * Add a table of buttons/links and info to control display and deletion
     *
     * @param imgCnt total number of records in selection
     */
    private PageItemList getNavBar(int imgCnt, boolean isBottom,String frmName) 
            throws WebUtilException, LdvTableException, SQLException
    {
        PageItemList ret = new PageItemList();
        PageTable navBar = new PageTable();
        PageTableRow cmds = new PageTableRow();

        // add next/prev buttons if applicable
        PageFormButton prevBtn  = new PageFormButton("submitAct", "<", "Prev");
        prevBtn.setEnabled(strt>0);
        cmds.add(prevBtn);
        
        String botSuffix = isBottom ? "2" : "";
        
        int curPage = strt / cnt + 1;
        int nPage = (imgCnt + cnt - 1) / cnt;
        PageItemList pageSel = new PageItemList();
        pageSel.add("Page: ");
        PageFormText pageNum = new PageFormText("pageNum"+botSuffix, String.format("%1$,d", curPage));
        pageNum.setSize(3);
        pageNum.setMaxLen(6);
        pageNum.addEvent("onchange", "historySubmit('Go"+botSuffix +"', this);");
        pageSel.add(pageNum);
        pageSel.add(String.format(" of %d ", nPage));
        cmds.add(pageSel);
        
        PageFormButton nextBtn = new PageFormButton("submitAct", ">", "Next");
        nextBtn.setEnabled(curPage < nPage);
        cmds.add(nextBtn);
        
        int stop = Math.min(strt + cnt, imgCnt);
        String showing = String.format("Showing %1$d-%2$d for:", strt+1,stop);
        cmds.add(showing);
        
        // add selection by user
        String cn = vuser.getCn();

        TreeMap<String, Integer> ucounts = imgTbl.getCountByUser();
        String[] usrs = new String[ucounts.size() + 1];
        String sel = "All";
        usrs[0] = "All";
        int i = 1;
        for (Map.Entry<String, Integer> entry : ucounts.entrySet())
        {
            String u = entry.getKey();
            Integer cnts = entry.getValue();
            String opt = String.format("%1$s (%2$d)", u, cnts);
            if (userWanted.equalsIgnoreCase(u))
            {
                sel = opt;
            }
            usrs[i] = opt;
            i++;
        }
        if (!isBottom)
        {
            // allow them to select another user (or all)
            PageFormSelect usrSel = new PageFormSelect("usrSel", usrs);
            usrSel.setSelected(sel);
            usrSel.addEvent("onchange", "this.form.submit()");
            PageItemList owner = new PageItemList();
            owner.add(new PageItemString("Owner:&nbsp;",false));
            owner.add(usrSel);
            cmds.add(owner);
        
            
            
            // Group selector
            TreeSet<String> groups = imgGrpTbl.getGroups(userWanted);
            if (!groups.isEmpty())
            {
                PageItemList grpPIL = new PageItemList();
                grpPIL.add(new PageItemString("Group:&nbsp;",false));
                groups.add("All");
                PageFormSelect grpSel = new PageFormSelect("group");
                grpSel.add(groups);
                String curGroup = request.getParameter("group");
                if (curGroup != null && !curGroup.isEmpty() && groups.contains(curGroup))
                {
                    grpSel.setSelected(curGroup);
                }
                else
                {
                    grpSel.setSelected("All");
                }
                grpSel.addEvent("onchange", "document." + frmName + ".submit()");
                grpPIL.add(grpSel);
                cmds.add(grpPIL);
            }
        }
        cmds.setClassAll("noborder");
        navBar.addRow(cmds);
        ret.add(navBar);
        
        if (!isBottom)
        {
            // New table because this one has fewer columns and we want to hide it by default
            navBar = new PageTable();
            navBar.setClassName("hidable");
            navBar.addStyle("display", "none");

            // allow them to change image size
            PageTableRow cmd2 = new PageTableRow();
            String[] sizes =
            {
                "original", "small", "med"
            };
            PageFormSelect sizeSel = new PageFormSelect("size", sizes);
            String curSize = request.getParameter("size");
            if (curSize != null && !curSize.isEmpty() && ArrayUtils.contains(sizes, curSize))
            {
                sizeSel.setSelected(curSize);
            }
            sizeSel.addEvent("onchange", "document." + frmName + ".submit()");
            PageItemString lbl;
            lbl = new PageItemString("Size:&nbsp;", false);
            lbl.setAlign(PageItem.Alignment.RIGHT);
            cmd2.add(lbl);
            PageTableColumn col;
            col = new PageTableColumn(sizeSel);
            cmd2.add(col);
            cmd2.add();
            cmd2.add();
            cmd2.setClassAll("noborder");
            navBar.addRow(cmd2);

            cmd2 = new PageTableRow();
            lbl = new PageItemString("Selections:&nbsp;",false);
            lbl.setAlign(PageItem.Alignment.RIGHT);
            cmd2.add(lbl);

            PageFormButton selAll = new PageFormButton("selAll", "Select all","selall");
            selAll.setType("button");
            selAll.addEvent("onclick", "setChkBoxByClass('selBox',true)");
            cmd2.add(selAll);

            PageFormButton clrAll = new PageFormButton("selAll", "Clear all", "clrall");
            clrAll.setType("button");
            clrAll.addEvent("onclick", "setChkBoxByClass('selBox', false)");
            cmd2.add(clrAll);
            cmd2.add();
            cmd2.setClassAll("noborder");
            navBar.addRow(cmd2);

            if (userWanted.equalsIgnoreCase(vuser.getCn()) || vuser.isAdmin())
            {
                cmd2 = new PageTableRow();
                lbl = new PageItemString("Delete images:&nbsp;",false);
                lbl.setAlign(PageItem.Alignment.RIGHT);
                cmd2.add(lbl);

                col = new PageTableColumn(new PageFormSubmit("submitAct", "Delete Selected"));
                cmd2.add(col);
                cmd2.add();
                cmd2.add();
                cmd2.setClassAll("noborder");
                navBar.addRow(cmd2);
            }

            PageTableRow grpRow = new PageTableRow();
            lbl = new PageItemString("My groups:&nbsp;",false);
            lbl.setAlign(PageItem.Alignment.RIGHT);
            grpRow.add(lbl);

            TreeSet<String> myGroup = imgGrpTbl.getGroups(curUser);
            if (!myGroup.contains("Favorites"))
            {
                myGroup.add("Favorites");
            }
            myGroup.remove("Last result");
            PageFormSelect grpSel = new PageFormSelect("op_group");
            grpSel.add(myGroup);
            String curGroup = request.getParameter("groupSel");
            if (curGroup != null && !curGroup.isEmpty() && myGroup.contains(curGroup))
            {
                grpSel.setSelected(curGroup);
            }
            else
            {
                grpSel.setSelected("Favorites");
            }
            grpRow.add(grpSel);
            
            grpRow.add(new PageFormSubmit("submitAct", "Add to grp"));
            grpRow.add(new PageFormSubmit("submitAct", "Del from grp"));
            grpRow.setClassAll("noborder");
            navBar.addRow(grpRow);
            grpRow = new PageTableRow();
            lbl = new PageItemString("New Group:&nbsp;",false);
            lbl.setAlign(PageItem.Alignment.RIGHT);
            grpRow.add(lbl);
            PageFormText grpNameTxt = new PageFormText("newGrpName", "<new group name>");
            grpNameTxt.addEvent("onfocus", "if(this.value == '<new group name>'){ this.value = ''; }");
            grpNameTxt.addEvent("onblur", "if(this.value == ''){ this.value = '<new group name>'; }");
            grpRow.add(grpNameTxt);
            PageFormSubmit newGrpBtn = new PageFormSubmit("submitAct", "New grp");
            grpRow.add(newGrpBtn);
            grpRow.add();
            grpRow.setClassAll("noborder");
            navBar.addRow(grpRow);
            ret.add(navBar);
            
            PageItemString optBtn = new PageItemString("+More options");
            optBtn.addEvent("onclick", "showByClass('hidable',this)");
            optBtn.setClassName("showCmd");
            ret.addBlankLines(1);
            ret.add(optBtn);
        }
        vpage.includeJS("showByClass.js");
        return ret;
    }

    private PageItemList getImageIdLine(int id) throws LdvTableException
    {
        PageItemList idLine = new PageItemList();
        PageFormCheckbox sel = new PageFormCheckbox("sel_" + Integer.toString(id), "Select");
        sel.setClassName("selBox");
        idLine.add(sel);

        idLine.add(String.format(".  Image # %d, - ", id));
        idLine.add(imgTbl.getIdInfo(id));

        return idLine;
    }
    
    /**
     * if they checked anything return a list of image IDs
     * @return IDs of selected images
     */
    private TreeSet<Integer> getSelected()
    {
        TreeSet<Integer> ret = new TreeSet<>();
                                                         
        Map<String, String[]> parameterMap = request.getParameterMap();
        for (String p : parameterMap.keySet())
        {
            if (p.toLowerCase().startsWith("sel_"))
            {
                String sn = p.substring(4);
                if (sn.matches("^\\d+$"))
                {
                    Integer id = Integer.parseInt(sn);
                    ret.add(id);
                }
            }
        }
        return ret;
    }
}
