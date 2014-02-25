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

import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageItemTextLink;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;

/**
 * Control access to the near real time plots
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class LivePlotManager
{
    public static PageItemList getSelector() throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        ret.add("The links below will open a new page/tab in your browser that will update"
                + " regularly showing plots of the selected channels that are 5 to 15 minutes old");
        ret.addBlankLines(2);
        
        PageTable links = new PageTable();
        links.setClassName("noborder");
        
        addLink(links,"https://ldvw.ligo.caltech.edu/secure/LivePlots/index.html",
                "Overview", "All Available Plots");
        
        addBlank(links, "Single site:");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveH1-PSL.html", 
                "Hanford", "All plots for Hanford PSL/IMC");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveL1-PSL.html", 
                "Livingston", "All plots for Livingston PSL/IMC");
        
        addBlank(links,"Individual Plots:");
        addLink(links,"https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveODC-H1-PSL.html", 
                "H1:PSL-ODC", "Single ODC plot");
        addLink(links,"https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveODC-L1-PSL.html", 
                "L1:PSL-ODC", "Single ODC plot");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpec-H1-PSL.html", 
                "H1:PSL-ISS-PDA-OUT-DQ", "Spectrogram of out of loop photodiode");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpec-L1-PSL.html", 
                "L1:PSL-ISS-PDA-OUT-DQ", "Spectrogram of out of loop photodiode");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpectrum-H1-PSL.html", 
                "H1:PSL-PDA/PDB", "Specturum of in loop and out of loop photodiode");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpectrum-L1-PSL.html", 
                "L1:PSL-PDA/PDB", "Specturum of in loop and out of loop photodiode");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpec-H1-IMC.html",
                "H1:IMC-F_OUT_DQ", "Spectrogram of important IMC photodiode (we need a better description of this channel)");
        addLink(links, "https://ldvw.ligo.caltech.edu/secure/LivePlots/LiveSpec-L1-IMC.html",
                "L1:IMC-F_OUT_DQ", "Spectrogram of important IMC photodiode (we need a better description of this channel)");
        
        
        ret.add(links);
        return ret;
    }

    private static void addLink(PageTable links, String url, String label, String description) 
            throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        row.setClassName("linkRow");
        PageItemTextLink link = new PageItemTextLink(url, label, "_blank");
        link.setClassName("buttonLink");
        row.add(link);
        row.add(description);
        row.setClassAll("linkRow");
        links.addRow(row);
    }

    private static void addBlank(PageTable links, String sectionTitle) throws WebUtilException
    {
        PageTableRow row = new PageTableRow();
        row.add();
        row.add();
        row.add();
        row.setClassAll("linkRow");
        links.addRow(row);
        
        row = new PageTableRow();
        row.setClassName("linkRow");
        PageItemString title = new PageItemString(sectionTitle);
        title.addStyle("font-weight", "bold");
        row.add(title);
        row.add();
        row.add();
        row.setClassAll("linkRow");
        links.addRow(row);
    }
}
