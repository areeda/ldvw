/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
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
package commonUI;

import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemImageLink;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableColumn;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanIndexInfo;
import edu.fullerton.ldvjutils.ChanInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Display interfaces for the Channels table
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChannelsUI
{
    private final String baseCISurl = "https://cis.ligo.org/channel/byname/";
    private final String infoIconDescUrl;
    private final String infoIconNoDescUrl;
    private PageItemImage infoDescIcon = null;
    private PageItemImage infoNoDescIcon = null;

    private PageItemImage pemIcon = null;
    private final String basePemUrl = "http://pem.ligo.org/channelinfo/index.php?channelname=";

    private PageItemImage srcIcon = null;
    private final String baseSrcUrl;

    public ChannelsUI(String contextPath)
    {
        infoIconDescUrl = contextPath + "/infoicon3.png";
        infoIconNoDescUrl = contextPath + "/infoicon4.png";
        pemIcon = new PageItemImage(contextPath + "/pemIcon.png", "pem icon", "PEM diagram containing channel");
        pemIcon.setDim(24, 24);

        srcIcon = new PageItemImage(contextPath + "/clockIcon.png", "src icon", "Get channel source data");
        srcIcon.setDim(24, 24);
        baseSrcUrl = contextPath + "/SrcList";

    }
    public PageTable getSelector(Collection<ChanInfo> chanCollection,
                                 Set<Integer> selections) throws WebUtilException
    {
        ArrayList<ChanInfo> chanList = new ArrayList<>();
        chanList.addAll(chanCollection);
        Collections.sort(chanList);
        PageTable ret = new PageTable();
        ret.setId("channelSelectorTable");
        ret.setSortable(true);
        
        ret.addRow(getHdrRow());
        
        boolean odd = true;
        for (ChanInfo ci : chanList)
        {
            PageTableRow d = new PageTableRow();        // d contains data for all channels
            // change color on alternating rows of table
            d.setClassName(odd ? "odd" : "even");
            odd = !odd;

            Integer myId = ci.getId();
            String selName = String.format("selchan_%1$d", myId);
            PageFormCheckbox selcb = new PageFormCheckbox(selName, "");
            selcb.setClassName("selBox");
            if (selections.contains(myId))
            {
                selcb.setChecked(true);
                selections.remove(myId);
            }
            PageItemString cname = new PageItemString(ci.getChanName());
            PageTableColumn selcbCol = new PageTableColumn(selcb);
            PageTableColumn cnameCol = new PageTableColumn(cname);

            d.add(selcbCol);
            d.add(cnameCol);

            d.add(getRateCol(ci.getRate()));

            String ctyp = ci.getcType();
            PageTableColumn ctc = new PageTableColumn(ctyp);
            ctc.setAlign(PageItem.Alignment.CENTER);
            d.add(ctc);

            d.add(ci.getServer());
            d.add(ci.getdType());

            d.add(getInfoLinks(ci));
            ret.addRow(d);
        }

        return ret;
    }
    
    private PageTableRow getHdrRow() throws WebUtilException
    {
        String[] hdr =
        {
            "", "Name&nbsp;&nbsp;", "Sample&nbsp;&nbsp;&nbsp;&nbsp;<br/>Rate", "Type&nbsp;&nbsp;&nbsp;&nbsp;", 
            "Server&nbsp;&nbsp;", "Data&nbsp;&nbsp;<br/>Type", "Info link&nbsp;&nbsp;"
        };

        PageTableRow r = new PageTableRow();
        for (String h : hdr)
        {
            if (h.isEmpty())
            {
                h = "&nbsp;";
            }
            PageItemString hs = new PageItemString(h, false);
            PageTableColumn c = new PageTableColumn(hs);
            c.setAlign(PageItem.Alignment.CENTER);
            r.add(c);
        }
        r.setRowType(PageTableRow.RowType.HEAD);

        return r;
    }
    private PageTableColumn getRateCol(Float fs) throws WebUtilException
    {
        String str;
        if (fs > 0.9999)
        {
            str = String.format("%1$.0f", fs);
        }
        else
        {
            str = String.format("%1$.3f", fs);
        }
        PageTableColumn fsc = new PageTableColumn(str);
        fsc.setAlign(PageItem.Alignment.RIGHT);
        return fsc;
    }
    private PageItemList getInfoLinks(ChanInfo ci)
    {
        PageItem cisInfo;
        if (ci.getCisAvail().equalsIgnoreCase("a") || ci.getCisAvail().equalsIgnoreCase("d"))
        {
            cisInfo = getCisLink(ci);
        }
        else
        {
            cisInfo = new PageItemString("&nbsp;", false);
        }
        PageItem pemInfo = getPemLink(ci);
        PageItem csrcInfo = getSrcInfoLink(ci);

        PageItemList infoLinks = new PageItemList();
        infoLinks.add(cisInfo);
        infoLinks.add(new PageItemString("&nbsp;", false));
        infoLinks.add(csrcInfo);
        infoLinks.add(new PageItemString("&nbsp;", false));
        if (pemInfo != null)
        {
            infoLinks.add(pemInfo);
        }
        return infoLinks;
    }
    /**
     * Link into the Channel Information system
     *
     * @param ci our channel info object
     * @return an image link
     */

    private PageItemImageLink getCisLink(ChanInfo ci)
    {
        if (infoDescIcon == null)
        {
            infoDescIcon = new PageItemImage(infoIconDescUrl, "chan info", "CIS for channel");
            infoDescIcon.setDim(24, 24);
        }
        if (infoNoDescIcon == null)
        {
            infoNoDescIcon = new PageItemImage(infoIconNoDescUrl, "chan info", "CIS for channel");
            infoNoDescIcon.setDim(24, 24);
        }

        String cname = ci.getBaseName();

        String cisUrl = baseCISurl + cname;
        PageItemImageLink link;
        if (ci.getCisAvail().equalsIgnoreCase("d"))
        {
            link = new PageItemImageLink(cisUrl, infoDescIcon, "_blank");
        }
        else
        {
            link = new PageItemImageLink(cisUrl, infoNoDescIcon, "_blank");
        }

        return link;
    }
    /**
     * if this channel probably has a PEM link add an icon for it
     *
     * @param cii
     * @return
     */
    private PageItem getPemLink(ChanInfo cii)
    {
        PageItem ret = null;
        String cname = cii.getBaseName();

        if (cname.matches("[HL]1:PEM-.*OUT_DQ"))
        {
            String pemName = cname.replace("_OUT_DQ", "");

            String pemUrl = basePemUrl + pemName;
            PageItemImageLink link;
            ret = new PageItemImageLink(pemUrl, pemIcon, "_blank");
        }
        return ret;
    }

    private PageItem getSrcInfoLink(ChanInfo ci)
    {
        PageItem ret;
        String url = String.format("%1$s?chanid=%2$d", baseSrcUrl, ci.getId());
        ret = new PageItemImageLink(url, srcIcon, "_blank");
        return ret;
    }
 
}
