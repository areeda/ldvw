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

import edu.fullerton.viewerplugin.PlotProduct;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.GUISupport;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Calculate and plot the coherence of 2 time series
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CoherenceManager extends ExternalPlotManager implements PlotProduct
{
    private final String name = "Coherence";
    private boolean logXaxis=true, logYaxis=false;
    private final String prog = "/usr/local/ldvw/bin/gw_coher.py";

    private String ref_name;
    private String ref_server;
    private String scale;
    private float secpfft;
    private final Pattern fltPat;
    private Float ovlap;
    private Float fmin;
    private Float fmax;
    private String yLabel="Coherence";
    private int lineThickness;
    /**
     * Manage the external program that calculates coherence of two series
     * @param db
     * @param vpage
     * @param vuser
     */
    public CoherenceManager( Database db, Page vpage, ViewUser vuser)
    {
        super( db, vpage, vuser);
        fltPat = Pattern.compile("(([1-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?");
    }
    /**
     * Create a coherence plot of the reference series vs. each of the others
     * @param dbuf data buffers to plot
     * @param compact if we should make text as short as possible
     * @return always returns zero because we save the images ourselves
     * @throws WebUtilException 
     */
    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) throws WebUtilException
    {
        try
        {
            int imageId;
            ArrayList<File> files = new ArrayList<>();

            getParameters();
            
            ArrayList<Integer> imageIds = new ArrayList<>();
            if (dbuf.size() < 2)
            {
                throw new WebUtilException("Coherence calculations need at least 2 datasets");
            }
            ChanDataBuffer ref = getRefBuf(dbuf);
            
            int len= ref.getDataLength();
            Float refRate = ref.getChanInfo().getRate();
            
            
            try
            {
                String refChanName = ref.getChanName();
                String otherChanNames = "";
                ArrayList<String> cmd = new ArrayList<>();
                cmd.add(prog);
                cmd.add("--chan");
                cmd.add(refChanName);
                
                for (ChanDataBuffer b : dbuf)
                {
                    if (b != ref)
                    {
                        cmd.add("--chan");
                        cmd.add(b.getChanName());
                        if (! otherChanNames.isEmpty())
                        {
                            otherChanNames += ", ";
                        }
                        otherChanNames += b.getChanName();
                    }
                }
                cmd.add("--start");
                cmd.add(String.format("%1$d", ref.getTimeInterval().getStartGps()));
                cmd.add("--duration");
                cmd.add(String.format("%1$d", ref.getTimeInterval().getDuration()));
                cmd.add("--fftlen");
                cmd.add(String.format("%1$.2f",secpfft));
                cmd.add("--geometry");
                cmd.add(String.format("%1$dx%2$d", width,height));
                if (logXaxis)
                {
                    cmd.add("--logf");
                }
                if (logYaxis)
                {
                    cmd.add("--logy");
                }
                if (fmin > 0 )
                {
                    cmd.add("--fmin");
                    cmd.add(String.format("%1$.2f", fmin));
                }
                if (fmax > 0)
                {
                    cmd.add("--fmax");
                    cmd.add(String.format("%1$.2f", fmax));
                }
                
                // @todo Implement in gwpy stuff            xyp.setLineThickness(lineThickness);
                            
                String title = String.format("Coherence:  %1$s vs %2$s", refChanName,otherChanNames);
                cmd.add("--suptitle");
                cmd.add(title);
                
//                TimeInterval ti = ref.getTimeInterval();
//                long duration = ti.getDuration();
//                float nfft;
//                nfft = (float) Math.floor(duration/(secpfft*(1-ovlap)) - 1);
//                String xLabel = String.format("Frequency (Hz) sec/fft: %1$.1f "
//                        + "bin-width: %2$.3f, #-fft: %3$.0f", secpfft, 1/secpfft,
//                        nfft);
//                cmd.add("--xlabel");
//                cmd.add(xLabel);
  
                File outImage = getTempFile("ldvw-coh-", ".png");
                files.add(outImage);
                cmd.add("--out");
                cmd.add(outImage.getCanonicalPath());
                if (runExternalProgram(cmd))
                {
                    imageId = addImg2Db(outImage, db, vuser.getCn());
                    if (imageId > 0)
                    {
                        PageItem descItem = makeDescription(this, dbuf);
                        String descHtml = descItem.getHtml();
                        getImageTable(db);
                        imgTbl.addDescription(imageId, descHtml);
                        imageIds.add(imageId);
                    }
                    else
                    {
                        vpage.add("Coherence program returned normal status but produced no output.");
                        vpage.add(getErrorMessage("Coherence plot",cmd));
                    }
                }
                else
                {
                    vpage.add(getErrorMessage("Coherence plot",cmd));
                }
                //   removeTempFiles(files);
                files.clear();
            }
            catch (WebUtilException | IOException | SQLException ex)
            {
                if (!files.isEmpty())
                {
                    removeTempFiles(files);
                }
                throw new WebUtilException("Error making coherence calculations: " + ex.getLocalizedMessage());
            }

            return imageIds;
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException("Make coherence plot", ex);
        }
    }

    @Override
    public boolean isStackable()
    {
        return true;
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
        return name;
    }

    /**
     * Get the html objects to use for selecting this product and setting options.
     * 
     * @param enableKey - parameter for the checkbox
     * @param nSel - number of channels selected
     * @param multDisp
     * @return
     * @throws WebUtilException 
     */
    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        String[] scalingNames =
        {
            "Linear", "Log", "1/(1-c)", "log(1/(1-c)"
        };
        String[] lineThicknessOptions = { "1", "2", "3", "4" };
        
        this.enableKey = enableKey;
        
        PageItemList ret = new PageItemList();
        String enableText = "Generate Coherence Plots";
        boolean enabled = getPrevValue(enableKey);
        PageFormCheckbox cb = new PageFormCheckbox(enableKey, enableText, enabled);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick", fun);
        ret.add(cb);
        
        ret.addBlankLines(1);
        ret.add("Set appropriate parameters below:");
        ret.addBlankLines(1);

        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        PageTableRow ptr;

        PageFormSelect scaleSel = new PageFormSelect("coh_scaling", scalingNames);
        String prevVal = getPrevValue("coh_scaling", 0, scalingNames[0]);
        scaleSel.setSelected(prevVal);
        ptr = GUISupport.getObjRow(scaleSel, "Scaling:", "");
        product.addRow(ptr);
        
        PageFormSelect lineThicknessSelector = new PageFormSelect("coh_linethickness", lineThicknessOptions);
        prevVal = getPrevValue("coh_linethickness", 0, "2");
        lineThicknessSelector.setSelected(prevVal);
        ptr = GUISupport.getObjRow(lineThicknessSelector, "Line thickness: ", "");
        product.addRow(ptr);

        // length and overlap off fft
        prevVal = getPrevValue("coh_secperfft", 0, "1");
        ptr = GUISupport.getTxtRow("coh_secperfft", "Sec/fft:", "", 16, prevVal);
        product.addRow(ptr);

        prevVal = getPrevValue("coh_fftoverlap", 0, "");
        ptr = GUISupport.getTxtRow("coh_fftoverlap", "Overlap [0-1):", "Leave blank for auto", 16, 
                                    prevVal);
        product.addRow(ptr);

        // frequncy axis limits
        prevVal = getPrevValue("coh_fmin", 0, "");
        ptr = GUISupport.getTxtRow("coh_fmin", "Min freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);

        prevVal = getPrevValue("coh_fmax", 0, "");
        ptr = GUISupport.getTxtRow("coh_fmax", "Max freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);

        // do they want axis to be linear
        enabled = getPrevValue("coh_linfreq");
        PageFormCheckbox logx = new PageFormCheckbox("coh_linfreq", "Freq axis linear", enabled);
        ptr = GUISupport.getObjRow(logx, "", "");
        product.addRow(ptr);

        
        // allow them to select the reference channel
        if (!baseChans.isEmpty())
        {
           
            ArrayList<String> chlist = new ArrayList<>();
            for(BaseChanSelection bcs : baseChans)
            {
                String selStr = bcs.getName();
                chlist.add(selStr);
            }
            String[] chlistStr = new String[0];
            chlistStr = chlist.toArray(chlistStr);

            prevVal = getPrevValue("coh_Ref", 0, chlist.get(0));
            PageFormSelect refChan = new PageFormSelect("coh_Ref", chlistStr);
            refChan.setSelected(prevVal);
            ptr = GUISupport.getObjRow(refChan, "Reference Channel:", "");
            product.addRow(ptr);

        }
        ret.add(product);
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
        this.dispFormat = dispFormat;
    }

    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        this.paramMap = parameterMap;
        // decode the reference channel name + optional server
        String[] coh_ref = paramMap.get("coh_Ref");
        if (coh_ref != null && coh_ref.length == 1)
        {
            int atIdx = coh_ref[0].indexOf("@");
            if (atIdx > 0)
            {
                ref_name=coh_ref[0].substring(0, atIdx).trim();
                ref_server=coh_ref[0].substring(atIdx+1).trim();
            }
            else
            {
                ref_name = coh_ref[0].trim();
                ref_server="";
            }
        }
        String[] t = paramMap.get("coh_linethickness");
        if (t == null || !t[0].trim().matches("^\\d+$"))
        {
            lineThickness = 2;
        }
        else
        {
            lineThickness = Integer.parseInt(t[0]);
        }
    }

    private void removeTempFiles(Collection<File> files)
    {
        for(File f : files)
        {
            if (f.exists())
            {
                try
                {
                    f.delete();
                }
                catch (Exception ex)
                {
                    
                }
            }
        }
    }

    @Override
    public boolean needsImageDescriptor()
    {
        return false;
    }


    /**
     * Get the data buffer for the reference channel.  
     * If we do not have data for the one they asked for use the first one.
     * @param dbuf data buffers (with data)
     * @return the reference channel's data buffer
     */
    private ChanDataBuffer getRefBuf(ArrayList<ChanDataBuffer> dbuf) throws LdvTableException
    {
        ChanDataBuffer ref = null;
        for(ChanDataBuffer it : dbuf)
        {
            if (it.getChanInfo().getChanName().contentEquals(ref_name))
            {
                ref = it;
            }
        }
        if (ref == null)
        {
            ref = dbuf.get(0);
            String rchan = ref_name + (ref_server.isEmpty() ? "" : " @ " + ref_server);
            String uchan = ref.getChanInfo().getChanName() + (ref_server.isEmpty() ? "" : " @ " + 
                           ref.getChanInfo().getServer());
            String ermsg = String.format("The selected reference channel: %1$s does not have any data"
                    + "using %2$s",rchan,uchan);
            vpage.add(ermsg);
            vpage.addBlankLines(1);
        }
        return ref;
    }

    /**
     * Get the request parameters and set the appropriate fields in the object
     */
    private void getParameters() throws WebUtilException
    {
        if (paramMap == null)
        {
            throw new WebUtilException("CoherenceManager.getParameters: parameters have not been set");
        }
        String[] scl = paramMap.get("coh_scaling");
        
        if (scl != null && scl.length > 0)
        {
            scale=scl[0];
        }
        else
        {
            scale="log";
        }
        if (scale.toLowerCase().contains("log"))
        {
            logYaxis = true;
        }
        secpfft = getVal("coh_secperfft",1.f);
        ovlap = getVal("coh_fftoverlap",.5f);
        fmin = getVal("coh_fmin",0.f);
        fmax = getVal("coh_fmax",0.f);
        
        logXaxis = paramMap.get("coh_linfreq") == null;
    }
    private Float getVal(String pname, Float def)
    {
        Float ret = def;
        String[] pa = paramMap.get(pname);
        
        if (pa != null && pa[0].matches("(([1-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?"))
        {
            ret=Float.parseFloat(pa[0]);
        }
        return ret;
    }

    /**
     * based on their request there's a few ways to display this data
     * 
     * @param res 
     */
    private void scaleRes(double[][] res) throws WebUtilException
    {
        if (scale.equalsIgnoreCase("linear"))
        {
            logYaxis = false;
            yLabel = "Coherence";
        }
        else if (scale.equalsIgnoreCase("log"))
        {
            dealWithZeroes(res);
            logYaxis = true;
            yLabel = "Coherence (log)";
        }
        else if (scale.equalsIgnoreCase("1/(1-c)"))
        {
            logYaxis = false;
            calcRatio1(res);
            yLabel = "Coherence (scaled as 1/(1-c))";
        }
        else if (scale.equalsIgnoreCase("log(1/(1-c)"))
        {
            logYaxis = true;
            calcRatio1(res);
            dealWithZeroes(res);
            yLabel = "Coherence (scaled as log(1/(1-c)))";
        }
        else
        {
            
        }
    }

    /**
     * When taking the log beware of zeroes
     * @param res 
     */
    private void dealWithZeroes(double[][] res)
    {
        int l = res.length;
        double mn = Double.MAX_VALUE;
        for (int i = 0; i < l; i++)
        {
            double x = res[i][1];
            if (x > 0)
            {
                mn = Math.min(mn, x);
            }
        }
        double newmin = mn / 10;
        for (int i = 0; i < l; i++)
        {
            if (res[i][1] < mn)
            {
                res[i][1] = newmin;
            }
        }
    }

    /**
     * when calculating the ratio near one beware of divide by zero
     * @param res 
     */
    private void calcRatio1(double[][] res)
    {
        int l = res.length;
        // calculate the ratio, ignore divide by zero
        for (int i = 0; i < l; i++)
        {
            res[i][1] = 1 / (1 - res[i][1]);
        }
        // the divide by zero are now infinity
        // find the max < infinity
        double mx=Double.MIN_VALUE;
        for (int i = 0; i < l; i++)
        {
            double x = res[i][1];
            if (x != Double.POSITIVE_INFINITY)
            {
                mx = Math.max(mx, x);
            }
        }
        // replace inifinity with something
        double newmax = logYaxis ? mx * 10 : mx * 2;
        for (int i = 0; i < l; i++)
        {
            if (res[i][1] == Double.POSITIVE_INFINITY)
            {
                res[i][1] = newmax;
            }
        }
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }
}
