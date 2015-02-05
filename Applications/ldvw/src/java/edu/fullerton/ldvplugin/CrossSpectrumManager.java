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
import edu.fullerton.plugindefn.CrossSpectrumDefinition;
import viewerplugin.ChanDataBuffer;
import viewerplugin.PlotProduct;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CrossSpectrumManager extends ExternalPlotManager implements PlotProduct
{
    private File tempFile;
    private final CrossSpectrumDefinition csd;

    public CrossSpectrumManager(Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
        width = height = 0;
        csd = new CrossSpectrumDefinition();
        csd.init();
    }

    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) throws WebUtilException
    {
        try
        {
            if (width > 200 && height > 100)
            {
                String[] geom = {String.format("%1$dx%2$d", width, height)};
                paramMap.put("geom", geom);
            }
            csd.setFormParameters(paramMap);
            String cmd = csd.getCommandLine(dbuf, paramMap);
            vpage.add(cmd);
            vpage.addBlankLines(2);
            ArrayList<Integer> ret = new ArrayList<>();
            if (runExternalProgram(cmd))
            {
                String txtOutput = String.format("%1$s Output:<br>%2$s", getProductName(), getStdout());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                txtOutput = String.format("%1$s <br>Stderr: %2$s", getProductName(), getStderr());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                tempFile = csd.getTempFile();
                if (tempFile != null && tempFile.canRead())
                {
                    String desc = "";
                    int imgId = importImage(tempFile, "image/png", desc);
                    if (imgId > 0)
                    {
                        ret.add(imgId);
                    }
                }
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
            throw new WebUtilException("Making cross spectrum plot:", ex);
        }

    }

    @Override
    public boolean isStackable()
    {
        return csd.getBoolAttribute("isStackable", true);
    }

    @Override
    public boolean needsImageDescriptor()
    {
        return true;
    }

    @Override
    public String getProductName()
    {
        return "Cross Spectrum";
    }

    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        PageItemList ret = csd.getSelector(enableKey, nSel);
        tempFile = csd.getTempFile();
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
        // ignore it 
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
        return true;
    }

    @Override
    public boolean isPaired()
    {
        return true;
    }
    
}
