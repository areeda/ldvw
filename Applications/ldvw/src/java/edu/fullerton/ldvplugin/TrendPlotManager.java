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

package edu.fullerton.ldvplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.plugindefn.TrendPlotDefinition;
import viewerplugin.ChanDataBuffer;
import viewerplugin.PlotProduct;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Control the external TrendPlot program
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class TrendPlotManager extends ExternalPlotManager implements PlotProduct
{
    private int width;
    private int height;
    
    public TrendPlotManager(Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
    }

    /**
     * Submit a condor job to do the work
     * 
     * @param dbuf
     * @param compact
     * @return
     * @throws WebUtilException 
     */
    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) throws WebUtilException
    {
        try
        {
            TrendPlotDefinition tpd = new TrendPlotDefinition();
            tpd.init(); 
            tpd.setFormParameters(paramMap);
            tpd.setVuser(vuser);
            
            String cmd = tpd.getCommandLine(dbuf, paramMap);
            vpage.add(cmd);
            vpage.addBlankLines(2);
            ArrayList<Integer> ret = new ArrayList<>();
            if (runExternalProgram(cmd))
            {
                String hdr = String.format("Your %1$s is being queued for processing, and email will be "
                        + "sent to %2$s with a link to the results when finished.<br>",
                                       getProductName(), vuser.getMail());
                vpage.add(new PageItemString(hdr, false));
                hdr = "Please be patient, currently there is only one batch queue processing these plots.<br>"
            + "Processing time depends on how much data you requested and what else is running.";
                vpage.add(new PageItemString(hdr, false));
                String txtOutput = String.format("%1$s Output:<br>%2$s", getProductName(), getStdout());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                txtOutput = String.format("%1$s Stderr: <br>%2$s", getProductName(), getStderr());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
            }
            else
            {
                int stat = getStatus();
                String stderr = getStderr();
                vpage.add(String.format("%1$s returned and error: %2$d", getProductName(), stat));
                vpage.addBlankLines(1);
                PageItemString ermsg = new PageItemString(stderr, false);
                vpage.add(ermsg);
            }
            
            return ret;
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException("Making trend plot:", ex);
        }

    }

    @Override
    public boolean isStackable()
    {
        return false;
    }

    @Override
    public boolean isPaired()
    {
        return false;
    }

    @Override
    public boolean needsImageDescriptor()
    {
        return false;
    }

    @Override
    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public String getProductName()
    {
        return "Trend plot";
    }

    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        TrendPlotDefinition tpd = new TrendPlotDefinition();

        PageItemList ret = tpd.getSelector(enableKey, nSel);
        return ret;
    }

    @Override
    public boolean needsDataXfer()
    {
        return false;
    }

    @Override
    public void setDispFormat(String dispFormat)
    {
        // ignore it we don't control it
    }

    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        paramMap = new TreeMap<>();
        paramMap.putAll(parameterMap);
    }

    @Override
    public boolean hasImages()
    {
        return false;
    }
    
}
