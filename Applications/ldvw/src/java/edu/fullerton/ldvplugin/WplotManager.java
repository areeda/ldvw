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
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.plugindefn.WplotDefinition;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.io.FilenameUtils;

/**
 * Generate Omega scans. All the work is done by dmt_wplot
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class WplotManager extends ExternalPlotManager 
{

    public WplotManager(Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
    }
    
    /**
     * Produce the plot's option pane for the UI
     * @param enableKey
     * @param nSel
     * @param multDisp
     * @return div to be inserted in the accordion
     * @throws WebUtilException
     */
    @Override
    public PageItemList getSelector(String enableKey,int nSel, String[] multDisp) throws WebUtilException
    {
        WplotDefinition wpd = new WplotDefinition();
        wpd.init();
        this.enableKey = enableKey;
        wpd.setFormParameters(paramMap);
        PageItemList ret = wpd.getSelector(enableKey, nSel);
        return ret;
    }

    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) 
            throws WebUtilException
    {
        try
        {
            WplotDefinition wpd = new WplotDefinition();
            wpd.init();
            
            // make sure wplot's sample frequency <= acquired sample frequency
            String smplFrqName = wpd.getNamespace() + "_" + "smplfrq";
            String[] sampleFrequency = paramMap.get(smplFrqName);
            if (sampleFrequency != null && sampleFrequency.length == 1)
            {
                try {
                    String strFreq = sampleFrequency[0].trim();
                    if (! strFreq.matches("^\\d+$"))
                    {
                        throw new WebUtilException(String.format("Sample frequency (%1$s) is not an integer."
                                ,strFreq));
                    }
                    Integer requested = Integer.parseInt(strFreq);
                    Float rate = dbuf.get(0).getChanInfo().getRate();
                    if(requested > rate)
                    {
                        sampleFrequency[0] = String.format("%1$.0f", rate);
                        paramMap.put(smplFrqName, sampleFrequency);
                    }
                }
                catch (LdvTableException ex)
                {
                    throw new WebUtilException("Making omega plot", ex);
                }
            }
            
            wpd.setFormParameters(paramMap);
            
            List<String> cmd = wpd.getCommandArray(dbuf,paramMap);
            String cmdStr = wpd.getCommandLine(dbuf, paramMap);
            vpage.add(cmdStr);
            vpage.addBlankLines(2);
            ArrayList<Integer> ret = new ArrayList<>();
            if (runExternalProgram(cmd, ""))
            {
                String txtOutput = String.format("%1$s Output:<br>%2$s",getProductName(),getStdout());
                vpage.add(new PageItemString(txtOutput,false));
                vpage.addBlankLines(1);
                txtOutput = String.format("%1$s <br>Stderr: %2$s", getProductName(), getStderr());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                
                File outDir = wpd.getTempDir();
                ArrayList<File> imgs = getAllImgFiles(outDir);
                for(File file : imgs)
                {
                    String desc = getDescription(file);
                    String mime = "image/" + FilenameUtils.getExtension(file.getAbsolutePath());
                    int imgId = importImage(file, mime, desc);
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
                vpage.add(String.format("%1$s returned and error: %2$d", getProductName(),stat));
                vpage.addBlankLines(1);
                PageItemString ermsg = new PageItemString(stderr, false);
                vpage.add(ermsg);
            }
            
            return ret;
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException("Making Omega plot", ex);
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
    public String getProductName()
    {
        return "Omega scan (dmt_wplot)";
    }

    @Override
    public boolean needsDataXfer()
    {
        return false;
    }


    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        paramMap = new TreeMap<>();
        paramMap.putAll(parameterMap);
    }

    private String getDescription(File file)
    {
        String desc = FilenameUtils.getBaseName(file.getName()) + "<br>";
        return desc;
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }
}
