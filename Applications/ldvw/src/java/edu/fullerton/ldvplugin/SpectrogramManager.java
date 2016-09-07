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

import edu.fullerton.viewerplugin.GUISupport;
import edu.fullerton.viewerplugin.PlotProduct;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageFormSelect;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Spectrograms are generated by an external program.  This class deals with the UI and controls
 * that program.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SpectrogramManager extends ExternalPlotManager implements PlotProduct
{
    private final String name = "Spectrogram";
    private final String nameSpace = "spg";
    
    /**
     * Manage the external Spectrogram program
     * @param db
     * @param vpage
     * @param vuser
     */
    public SpectrogramManager( Database db, Page vpage, ViewUser vuser)
    {
        super( db, vpage, vuser);
    }

    
    @Override
    public String getProductName()
    {
        return name;
    }
    /**
     * The external program transfers data itself, so we don't want to duplicate it if we don't have to
     * @return false, we don't want it
     */
    @Override
    public boolean needsDataXfer()
    {
        return false;
    }
    @Override
    public PageItemList getSelector(String enableKey,int nSel, String[] multDisp) throws WebUtilException
    {
        String[] windows =
        {
            "Hanning", "Flattop", "None",
        };
        String[] scalingNames =
        {
            "ASD", "AS", "PS", "PSD"
        };
        String[] colorTableNames =
        {
            "Jet", "hot", "bw"
        };

        this.enableKey = enableKey;
        PageItemList ret = new PageItemList();
        String enableText = "Generate Spectrogram";
        enableText += nSel > 1 ? "s<br><br>" : "<br><br>";
        boolean enabled=getPrevValue(enableKey);
        PageFormCheckbox cb = new PageFormCheckbox(enableKey, enableText, enabled);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick", fun);
        ret.add(cb);

        //ret.addBlankLines(1);
        ret.add("Set appropriate parameters below:");
        ret.addBlankLines(1);

        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        PageTableRow ptr;

        // window and scaling
        PageFormSelect win = new PageFormSelect("spg_window", windows);
        String prevVal = getPrevValue("spg_window", 0, windows[0]);
        win.setSelected(prevVal);
        ptr = GUISupport.getObjRow(win, "Window:", "");
        product.addRow(ptr);

        PageFormSelect scale = new PageFormSelect("spg_scaling", scalingNames);
        prevVal = getPrevValue("spg_scaling", 0, scalingNames[0]);
        scale.setSelected(prevVal);
        ptr = GUISupport.getObjRow(scale, "Scaling:", "");
        product.addRow(ptr);

        // length and overlap off fft
        prevVal=getPrevValue("spg_secperfft", 0, "1");
        ptr = GUISupport.getTxtRow("spg_secperfft", "Sec/fft:", "", 16, prevVal);
        product.addRow(ptr);

        prevVal=getPrevValue("spg_fftoverlap", 0, "");
        ptr = GUISupport.getTxtRow("spg_fftoverlap", "Overlap [0-1):", "Leave blank for auto", 16, 
                                   prevVal);
        product.addRow(ptr);

        // frequncy axis limits
        prevVal = getPrevValue("spg_fmin", 0, "");
        ptr = GUISupport.getTxtRow("spg_fmin", "Min freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);

        prevVal = getPrevValue("spg_fmax", 0, "");
        ptr = GUISupport.getTxtRow("spg_fmax", "Max freq:", "Leave blank for auto", 16, prevVal);
        product.addRow(ptr);

        // do they want axis to be logarithmic
        enabled = getPrevValue("spg_linfreq");
        PageFormCheckbox logx = new PageFormCheckbox("spg_linfreq", "Freq axis linear (default=log)", enabled);
        ptr = GUISupport.getObjRow(logx, "", "");
        product.addRow(ptr);
        
        // do they want a logarithmic color scale
        enabled = getPrevValue("spg_linintensity");
        PageFormCheckbox logIntensity = new PageFormCheckbox("spg_linintensity", 
                "Color scale linear (default=log)", enabled);
        ptr = GUISupport.getObjRow(logIntensity, "", "");
        product.addRow(ptr);

        // normalize -> divide each frequency by mean
        enabled = getPrevValue("spg_norm");
        PageFormCheckbox norm = new PageFormCheckbox("spg_norm", "Normalize", enabled);
        ptr = GUISupport.getObjRow(norm, "", "Divide each FFT by the mean FFT");
        product.addRow(ptr);
        
        // want to smooth?
        enabled = getPrevValue("spg_smooth");
        PageFormCheckbox smooth = new PageFormCheckbox("spg_smooth", "Smooth in time and freq", 
                                                        enabled);
        ptr = GUISupport.getObjRow(smooth, "", "");
        product.addRow(ptr);

        // want to interpolate
        enabled = getPrevValue("spg_interp");
        PageFormCheckbox interp = new PageFormCheckbox("spg_interp", "Interpolate resulting image", 
                                                        enabled);
        ptr = GUISupport.getObjRow(interp, "", "");
        product.addRow(ptr);
        
        // histogram operations on the intensity scale
        prevVal = getPrevValue("spg_log", 0, "0.2");
        ptr = GUISupport.getTxtRow("spg_lo", "Low:", "Min pixel (percentile 0 <= low < 1)", 16, prevVal);
        product.addRow(ptr);
        
        prevVal = getPrevValue("spg_up", 0, "1.0");
        ptr = GUISupport.getTxtRow("spg_up", "Up:", "Max pixel (percentile 0 < up <= 1)", 16, 
                                   prevVal);
        product.addRow(ptr);

        // Select a color scale
        PageFormSelect color = new PageFormSelect("spg_color", colorTableNames);
        prevVal = getPrevValue("spg_color", 0, colorTableNames[0]);
        color.setSelected(prevVal);
        ptr = GUISupport.getObjRow(color, "Color table:", "");
        product.addRow(ptr);
        
        // Allow them the choice of gwpy (new) or java (old) processor
//        PageFormCheckbox useNew = new PageFormCheckbox("spg_gwpy", "Use new process", true);
//        ptr = GUISupport.getObjRow(useNew, "", "");
//        product.addRow(ptr);
        
        // add the option table and return the selector
        ret.add(product);
        return ret;
    }

    /**
     * Take the parameters and generate the command line then run the program.
     * 
     * @param dbuf - data to process, we ignore it
     * @param compact small image or lots of channel/intervals so minimize additonal text
     * @return
     * @throws WebUtilException 
     */
    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) 
            throws WebUtilException
    {
        try
        {
            int imageId = 0;    // error in setup
            StringBuilder cmd = new StringBuilder();
            cmd.append("java -Xmx3g -jar /usr/local/ldvw/Spectrogram/Spectrogram.jar ");
            if (dbuf.size() != 1)
            {
                String ermsg = "Spectrogram: one and only one channel/time can be passed to makePlot. ";
                ermsg += "We received " + Integer.toString(dbuf.size());
                throw new WebUtilException(ermsg);
            }
            ChanDataBuffer cdb = dbuf.get(0);
            ChanInfo ci;
            ci = cdb.getChanInfo();

            cmd.append(" --chan ").append(ci.getChanName());
            cmd.append(" --server ").append(ci.getServer());
            cmd.append(" --ctype ").append(ci.getcType());
            float sampleRate = ci.getRate();
            String rateStr;
            if (sampleRate > 0.9999)
            {
                rateStr = String.format("%1$.0f", sampleRate);
            }
            else
            {
                rateStr = String.format("%1$.4f", sampleRate);
            }
            cmd.append(" --rate ").append(rateStr);
            
            TimeInterval ti = cdb.getTimeInterval();
            long startGPS = ti.getStartGps();
            long stopGPS = ti.getStopGps();
            cmd.append(" --start ").append(Long.toString(startGPS));
            cmd.append(" --dur ").append(Long.toString(stopGPS - startGPS));
            
            addParam(cmd, "spg_color", "color");
            addParam(cmd, "spg_fmin", "fmin");
            addParam(cmd, "spg_fmax", "fmax");
            
            
            width = Math.max(width, 1280);
            height = Math.max(height,700);
            cmd.append(String.format(" --geom %1$dx%2$d ", width,height));
            
            addParam(cmd, "spg_lo", "lo");
            addParam(cmd, "spg_up", "up");
            
            if (! paramMap.containsKey("spg_linfreq"))
            {
                cmd.append(" --logfreq ");
            }
            if (! paramMap.containsKey("spg_linintensity"))
            {
                cmd.append(" --logintensity ");
            }
            addSwitch(cmd, "spg_norm", "norm");
            addSwitch(cmd, "spg_smooth", "smooth");
            addSwitch(cmd, "spg_interp", "interp");
            
            addParam(cmd, "spg_scaling","scale");
            addParam(cmd, "spg_window", "window");
            addParam(cmd, "spg_secperfft", "secpfft");
            
            addParam(cmd, "spg_fftoverlap", "overlap");
            
            if (paramMap.containsKey("prefilt"))
            {
                String filt = paramMap.get("prefilt")[0];
                if (!filt.equalsIgnoreCase("none"))
                {
                    if (filt.equalsIgnoreCase("high pass"))
                    {
                        filt = "high";
                    }
                    else if (filt.equalsIgnoreCase("low pass"))
                    {
                        filt = "low";
                    }
                    else if (filt.equalsIgnoreCase("band pass"))
                    {
                        filt = "band";
                    }
                    else if (filt.equalsIgnoreCase("band reject"))
                    {
                        filt = "notch";
                    }
                    addParamLiteral(cmd, "filt", filt);
                    addParam(cmd,"Cutoff","cutoff");
                    addParam(cmd,"Order","order");
                }
            }
            try
            {
                File tmpDir = new File("/tmp");
                File outFile;
                outFile = File.createTempFile("spg", ".png", tmpDir);
                cmd.append(" --outfile ").append(outFile.getCanonicalPath());
                
                imageId = callProgramSaveOutput(cmd.toString(), outFile, "Spectrogram", ci.getChanName());
            }
            catch (IOException | WebUtilException | LdvTableException ex)
            {
                throw new WebUtilException("Spectrogram: " + ex.getLocalizedMessage());
            }
            
            ArrayList<Integer> ret = new ArrayList<>();
            ret.add(imageId);
            return ret;
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException("makePlot: ", ex);
        }
    }

    private void addParam(StringBuilder cmd,String mapName, String cname)
    {
        String[] param;
        String cnm = cname == null || cname.isEmpty() ? mapName : cname;

        param = paramMap.get(mapName);
        if (param != null && param.length == 1 && !param[0].isEmpty())
        {
            cmd.append(" --").append(cnm).append(" ").append(param[0]);
        }
    }
    
    private void addParamLiteral(StringBuilder cmd, String paramName, String value)
    {
        cmd.append(" --").append(paramName).append(" ");
        if (value != null && !value.isEmpty())
        {
            cmd.append(value);
        }
    }
    private void addSwitch(StringBuilder cmd, String mapName, String cname)
    {
        String[] param;
        String cnm = cname == null || cname.isEmpty() ? mapName : cname;

        param = paramMap.get(mapName);
        if (param != null && param.length == 1)
        {
            cmd.append(" --").append(cnm);
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
    public void setDispFormat(String dispFormat)
    {
        // we can ignore this because we are not stackable
    }

    @Override
    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        // we can ignore this because it's done in our contructor
    }

   
    @Override
    public boolean needsImageDescriptor()
    {
        return true;
    }

    @Override
    public boolean hasImages()
    {
        return true;
    }

    @Override
    public String getNameSpace()
    {
        return nameSpace;
    }

}
