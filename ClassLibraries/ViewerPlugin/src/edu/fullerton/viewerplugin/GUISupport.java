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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChanPointerTable;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class GUISupport
{
    protected Database db = null;
    protected Page vpage = null;
    protected ViewUser vuser = null;
    protected Map<String, String[]> paramMap;
    protected String contextPath;
    protected String servletPath;
    
    private GUISupport() 
    {
        
    }

    public GUISupport (Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }
    
    public void setParamMap(Map<String, String[]> pmap)
    {
        paramMap = pmap;
    }

    /**
     * The context path is where we get root files like css and js
     * @param context 
     */
    public void setContextPath(String context)
    {
        contextPath=context;
    }

    public String getContextPath()
    {
        return contextPath;
    }

    public String getServletPath()
    {
        return servletPath;
    }

    public void setServletPath(String servletPath)
    {
        this.servletPath = servletPath;
    }
    
    public HashSet<Integer> getSelections() throws WebUtilException
    {
        return getSelections("selchan");
    }
    public HashSet<Integer> getSelections(String selType) throws WebUtilException
    {
        HashSet<Integer> selections = new HashSet<>();
        
        if (selType.equalsIgnoreCase("selchan"))
        {   // channels are selected individually
            
            ChannelTable ct;
            try
            {
                ct = new ChannelTable(db);
            }
            catch (SQLException ex)
            {
                throw new WebUtilException("Searching for channel by name:", ex);
            }
            for(Entry<String, String[]> ent : paramMap.entrySet())
            {
                String pname = ent.getKey();
                if (pname.startsWith("selchan_"))
                {
                    Integer cnum = Integer.parseInt(pname.substring(8));
                    selections.add(cnum);
                }
                else if (pname.equals("chanName"))
                {
                    String[] cnames=ent.getValue();
                    for(String cname: cnames)
                    {
                        int cnum;
                        try
                        {
                            cnum = ct.getBestMatch(cname);
                        }
                        catch (SQLException ex)
                        {
                            throw new WebUtilException("getBestMatch problem: ", ex);
                        }
                        if (cnum == 0)
                        {
                            throw new WebUtilException("Channel requested by name not found");
                        }
                        selections.add(cnum);
                    }
                }
            }
        }
        else if (selType.equalsIgnoreCase("selbchan"))
        {
            // base channels and perhaps channel type are selected
            getBaseChanSelections(selections);
        }
        else
        {
            throw new WebUtilException("Unknown channel selection type.");
        }
        return selections;
    }

    /**
     * Add current selections to the form as hidden items
     * 
     * @param pf the form items will be added to
     * @param selections any Collection of id numbers to add
     * @throws WebUtilException 
     */
    public void addSelections(PageForm pf, Collection<Integer> selections) throws WebUtilException
    {
        for (Integer sel : selections)
        {
            String selStr = String.format("selchan_%1$d", sel);
            pf.addHidden(selStr, selStr);
        }
    }    

    public PageItemList getLabelTxt(String fldName, String label, int size)
    {
        return getLabelTxt(fldName, label, size, "");
    }

    public PageItemList getLabelTxt(String fldName, String label, int size, String defaultVal)
    {
        PageItemList pil = new PageItemList();
        pil.add(new PageItemString(label, false));
        PageFormText pft = new PageFormText(fldName, "");
        pft.setMaxLen(255);
        if (size > 0)
        {
            pft.setSize(size);
        }
        if (defaultVal != null && !defaultVal.isEmpty())
        {
            pft.setDefaultValue(defaultVal);
        }
        pil.add(pft);
        return pil;
    }

    /**
     * A member function to call the static method, just a convenience
     * @param fldName
     * @param label
     * @param aux
     * @param size
     * @param defaultVal
     * @return
     * @throws WebUtilException 
     */
    public PageTableRow getLabelTxtRow(String fldName, String label, String aux, int size, String defaultVal) throws WebUtilException
    {
        return GUISupport.getTxtRow(fldName,label,aux,size,defaultVal);
    }
    public static PageTableRow getTxtRow(String fldName, String label, String aux, int size, String defaultVal) throws WebUtilException
    {
        return GUISupport.getTxtRow(fldName,label,aux,size,defaultVal,null);
    }
    public static PageTableRow getTxtRow(String fldName, String label, String aux, int size, 
                                         String defaultVal, PageItem help) throws WebUtilException
    {
        PageFormText pft = new PageFormText(fldName, "");
        pft.setMaxLen(255);
        if (size > 0)
        {
            pft.setSize(size);
        }
        if (defaultVal != null && !defaultVal.isEmpty())
        {
            pft.setDefaultValue(defaultVal);
        }
        PageItemString lbl = new PageItemString(label);
        
        PageItemList comment = new PageItemList();
        comment.setClassName("inlineText");
        comment.add(new PageItemString(aux));
        
        if (help != null)
        {
            comment.add(help);
        }
        return GUISupport.getObjRow(pft, lbl, comment);
    }
    public static PageTableRow getEditTextArea(String name, String label, String aux, int cols, int rows, String defaultVal) throws WebUtilException
    {
        PageFormText pft = new PageFormText(name, "");
        pft.setId(name);
        
        if (cols > 0)
        {
            pft.setSize(cols);
        }
        if (rows > 0)
        {
            pft.setnLines(rows);
        }
        if (defaultVal != null && !defaultVal.isEmpty())
        {
            pft.setDefaultValue(defaultVal);
        }
        pft.setUseEditor(true);
        
        return GUISupport.getObjRow(pft, label, aux);
    }
    public static PageTableRow getObjRow(PageItem obj, String label, String comment) throws WebUtilException
    {
        PageItemString lbl = new PageItemString(label,false);
        PageItemString auxInfo = new PageItemString(comment, false);
        return getObjRow(obj,lbl,auxInfo);
    }
    /**
     * The general case where all columns can be arbitrary page items
     * @param obj
     * @param lbl
     * @param auxInfo
     * @return
     * @throws WebUtilException 
     */
    public static PageTableRow getObjRow(PageItem obj, PageItem lbl, PageItem auxInfo) throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        
        lbl.setAlign(PageItem.Alignment.RIGHT);
        row.add(lbl);
        
        row.add(obj);
        
        if (auxInfo != null)
        {
            
            auxInfo.setAlign(PageItem.Alignment.LEFT);
            row.add(auxInfo);
        }
        else
        {
            row.add("");
        }

        row.setClassAll("noborder");
        
        return row;
    }
    /**
     * Convert a parameter from submitted form and convert it to a long
     * @param paramName name of the form element
     * @return value or 0
     */
    protected long getLong(String paramName)
    {
        long ret = 0;
        String[] ita = paramMap.get(paramName);
        String it = ita == null ? null : ita[0];
        if (it != null && !it.isEmpty())
        {
            if (it.matches("^[+\\-\\d\\.eE]+$"))
            {
                Double t = Double.parseDouble(it);
                t = Math.ceil(t);
                ret = t.longValue();
            }
        }
        return ret;
    }

    protected long getUnit(String paramName)
    {
        String[] ita = paramMap.get(paramName);
        String it = ita == null ? null : ita[0];
        long ret;
        if (it == null || it.isEmpty())
        {
            ret = 0;
        }
        else if (it.equalsIgnoreCase("seconds"))
        {
            ret = 1;
        }
        else if (it.equalsIgnoreCase("minutes"))
        {
            ret = 60;
        }
        else if (it.equalsIgnoreCase("hours"))
        {
            ret = 60 * 60;
        }
        else if (it.equalsIgnoreCase("days"))
        {
            ret = 24 * 60 * 60;
        }
        else if (it.equalsIgnoreCase("weeks"))
        {
            ret = 7 * 24 * 60 * 60;
        }
        else
        {
            ret = 0;
        }
        return ret;
    }

    private void getBaseChanSelections(HashSet<Integer> selections) throws WebUtilException
    {
        String cType = null;
        if (paramMap != null)
        {
            String[] ctyps = paramMap.get("ctype");
            if (ctyps != null)
            {
                cType = ctyps[0];
            }
        }
        if (cType == null)
        {
            throw new WebUtilException("Base channel selection without a channel type. Invalid call or a bug.");
        }
        
        ChanPointerTable cpt;
        try
        {
            cpt = new ChanPointerTable(db);
            Pattern trendPat = Pattern.compile("(second|minute)-trend_(\\d+)");
            Pattern singlePat = Pattern.compile("(raw|online|static|testpoint|rds)_(\\d+)");
            
            for (Entry<String, String[]> ent : paramMap.entrySet())
            {
                String pname = ent.getKey();
                Matcher trendMatch = trendPat.matcher(pname.toLowerCase());
                Matcher singleMatch = singlePat.matcher(pname.toLowerCase());
                
                if (pname.startsWith("selbchan_") )
                {
                    if (cType.equalsIgnoreCase("any"))
                    {
                        throw new WebUtilException("Base channel selected without channel type. Joe's bug.");
                    }
                    Integer indexID = Integer.parseInt(pname.substring(9));
                    List<Integer> tcList = cpt.getChanList(indexID, cType);
                    selections.addAll(tcList);
                }
                else if (trendMatch.find())
                {
                    String trType = trendMatch.group(1) + "-trend";
                    Integer indexID = Integer.parseInt(trendMatch.group(2));
                    String[] trends = ent.getValue();
                    if (trends.length == 1)
                    {
                        String[] trendList = trends[0].split(",");
                        for(int i=0; i<trendList.length;i++)
                        {
                            trendList[i] = trendList[i].trim();
                        }
                        if (! (trendList.length < 1 || trendList[0].equalsIgnoreCase("none")))
                        {
                            List<Integer> tcList = cpt.getChanList(indexID, trType, trendList);
                            selections.addAll(tcList);
                        }
                    }
                }
                else if (singleMatch.find())
                {
                    String myCtype = singleMatch.group(1);
                    Integer indexID = Integer.parseInt(singleMatch.group(2));
                    if (myCtype.equalsIgnoreCase("rds"))
                    {   // sic or maybe in this case sick
                        myCtype = myCtype.toUpperCase();
                    }
                    List<Integer> tcList = cpt.getChanList(indexID, myCtype);
                    selections.addAll(tcList);
                }
            }
        }
        catch (WebUtilException | LdvTableException | NumberFormatException ex)
        {
            throw new WebUtilException("Searching for channel by name:", ex);
        }
    }
}
