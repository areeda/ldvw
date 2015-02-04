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
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageCoordinateTbl;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.plugindefn.PluginController;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import edu.fullerton.viewerplugin.ExternalProgramManager;
import edu.fullerton.viewerplugin.PlotProduct;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.FilenameUtils;

/**
 * An abstract base class for plot generators that are external programs
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public abstract class ExternalPlotManager extends ExternalProgramManager implements PlotProduct
{
    protected Map<String, String[]> paramMap;
    protected String contextPath;
    protected String servletPath;
    protected Database db;
    protected Page vpage;
    protected ViewUser vuser;
    protected ImageCoordinate imgCoord;
    protected String enableKey;
    protected String dispFormat;
    protected int width;
    protected int height;
    protected List<BaseChanSelection> baseChans;
    
    public ExternalPlotManager( Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
        enableKey = "no way should this string be a key in the parameter map";
    }

    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    public void setParammap(Map<String, String[]> parammap)
    {
        paramMap = new TreeMap<>();
        paramMap.putAll(parammap);
    }

    @Override
    public void setParameters(Map<String, String[]> parameterMap)
    {
        setParammap(parameterMap);
    }
    @Override
    public void setChanList(List<BaseChanSelection> baseChans)
    {
        this.baseChans = baseChans;
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public void setServletPath(String servletPath)
    {
        this.servletPath = servletPath;
    }

    @Override
    public boolean isSelected()
    {
        boolean ret = false;
        if (paramMap != null)
        {
            ret = paramMap.containsKey(enableKey);
        }
        return ret;
    }
    /**
     * The enable key is the parameter name of the checkbox that activates this progduct
     * 
     * @return key name
     */
    @Override
    public String getEnableKey()
    {
        return enableKey;
    }
    /**
     * As part of remembering where we came from, form values are passed back and forth to select
     * more. Here we use the previous value or default for the specified key
     *
     * @param key - Parameter name for this field
     * @param idx - Index into value array, 0 if only 1 value allowed
     * @param def - default value if no parameter or parameter is empty
     * @return
     */
    public String getPrevValue(String key, int idx, String def)
    {
        String ret = def;
        String[] prev = paramMap.get(key);
        if (prev != null && prev.length > idx && !prev[0].isEmpty())
        {
            ret = prev[idx];
        }
        return ret;
    }

    /**
     * Checkboxes are a bit difficult because their key only gets sent if it's checked. So we don't
     * really know if it's the first time thru with no values for anything or they unchecked it.
     *
     * @param key - parameter name
     * @return true if parameter is available
     */
    public boolean getPrevValue(String key)
    {
        boolean ret = paramMap.containsKey(key);
        return ret;
    }
    /**
     * Call an external (command line) program that produces a single image.  Store the image in our 
     * Image table and add it to the results page
     * 
     * @param cmd the command to run
     * @param outFile the full path to the file that should be produced
     * @param prog name of the program used for error messages
     * @param chan channel name used for error messages
     * @return image id of saved image or 0 if nothing saved
     * @throws WebUtilException any problem in the process
     * @throws edu.fullerton.ldvjutils.LdvTableException problems with our database
     */
    public int callProgramSaveOutput(String cmd, File outFile, String prog, String chan) throws WebUtilException, LdvTableException
    {
        int imgId = 0;
        String erm = "";
        try
        {
            ExternalProgramManager epm = new ExternalProgramManager();
            
            if (epm.runExternalProgram(cmd))
            {
                String stdOut = epm.getStdout();
                boolean hasImgPos;
                hasImgPos = getImgPosInfo(stdOut);
                
                ImageTable itbl = new ImageTable(db);

                // read in the output image
                long len = outFile.length();
                if (len > 1000)
                {
                    byte[] imgBytes = new byte[(int) len];

                    FileInputStream fis = new FileInputStream(outFile);
                    int rlen = fis.read(imgBytes);

                    ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
                    imgId = itbl.addImg(vuser.getCn(), bis, "image/png");
                    if (hasImgPos)
                    {
                        imgCoord.setImgId(imgId);
                        ImageCoordinateTbl ict = new ImageCoordinateTbl(db);
                        ict.add(imgCoord);
                    }
                }
                else
                {
                    erm += prog + " exited normally but output file is invalid.<br/>";
                }
                outFile.delete();
            }
            else
            {
                erm += String.format("Error on chan: %1$s<br/>", chan);
                erm += String.format("Command: %1$s<br/>", cmd);
            }
            if (!erm.isEmpty())
            {
                PageItemString ermStr = new PageItemString(erm, false);
                vpage.add(ermStr);
                vpage.addBlankLines(1);
                PageItemString cmdStr = new PageItemString("Command:<br/>" + cmd, false);
                vpage.add(cmdStr);
                vpage.addBlankLines(2);

                vpage.add(new PageItemString("stderr:<br/>", false));
                vpage.add(new PageItemString(epm.getStderr(),false));

                vpage.addBlankLines(1);
                vpage.add(new PageItemString("stdout:<br/>", false));
                vpage.add(new PageItemString(epm.getStdout(),false));
            }
        }
        catch (NoSuchAlgorithmException | SQLException | IOException ex)
        {
            throw new WebUtilException("Creating " + prog + " output: " + ex.getLocalizedMessage());
        }

        return imgId;
    }
    public ArrayList<File> getAllImgFiles(File dir) throws WebUtilException
    {
        HashSet<String> extensions;
        extensions=new HashSet<>();
        extensions.add("png");
        extensions.add("jpg");
        extensions.add("gif");
        
        ArrayList<File> ret = new ArrayList<>();
        if (!dir.isDirectory() || ! dir.canRead())
        {
            throw new WebUtilException("Can't access output directory: " + dir.getAbsolutePath());
        }
        File[] listFiles = dir.listFiles();
        for(File file : listFiles)
        {
            String ext = FilenameUtils.getExtension(file.getAbsolutePath()).toLowerCase();
            if (extensions.contains(ext))
            {
                // acceptable type
                ret.add(file);
            }
            else
            {
                file.delete();
            }
        }
        return ret;
    }
    /**
     * Create and write data from a ChanDataBuffer to a CSV file for use by the external program as input
     * @param dbuf the data buffer to write
     * @return object representing file with the data
     * @throws IOException some problem writing the file
     */
    public File mkTempInputFile(ChanDataBuffer dbuf) throws IOException
    {
        File out = File.createTempFile("ldvw", ".csv",new File("/tmp"));
        try (FileWriter fw = new FileWriter(out)) 
        {
            float[] data = dbuf.getData();
            for(float d : data)
            {
                fw.append(String.format("%1$.7f\n", d));
            }
        }
        return out;
    }
    /**
     * Create and write data from a ChanDataBuffer to a CSV file for use by the external program as
     * input
     *
     * @param dbuf the data buffer to write
     * @return object representing file with the data
     * @throws IOException some problem writing the file
     */
    public File mkTempBinInputFile(ChanDataBuffer dbuf) throws IOException
    {
        File out = File.createTempFile("ldvw-", ".dat", new File("/tmp"));
        if (out.exists())
        {
            out.delete();
        }
        try (DataOutputStream output = new DataOutputStream(new FileOutputStream(out)))
        {
            float[] data = dbuf.getData();
            int nsample = data.length;
            for (int i = 0; i < nsample; i++)
            {
                double x;
                x = data[i];
                output.writeDouble(x);
            }
        }
        return out;
    }    /**
     * Read a result file with only 1 double value per line
     * @param in the file to read
     * @return values as an array
     * @throws FileNotFoundException you know
     * @throws IOException read problem
     * @throws WebUtilException conversion problem
     */
    public Double[] rdRes1File(File in) throws FileNotFoundException, IOException, WebUtilException
    {
        ArrayList<Double> data = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(in));
        String line;
        while((line=br.readLine()) != null)
        {
            try
            {
                Double it = Double.parseDouble(line);
                data.add(it);
            }
            catch (NumberFormatException ex)
            {
                throw new WebUtilException("rdRes1File:  can't parse input data ("+line+")");
            }
        }
        Double[] ret=null;
        ret = data.toArray(ret);
        return ret;
    }
    public double[][] rdXYFile(File in) throws FileNotFoundException, IOException, WebUtilException
    {
        ArrayList<double[]> data = new ArrayList<>();
        BufferedReader br = new BufferedReader(new FileReader(in));
        String line;
        while ((line = br.readLine()) != null)
        {
            try
            {
                String[] vals = line.split(",");
                double[] it = new double[2];
                it[0] = Double.parseDouble(vals[0]);
                it[1] = Double.parseDouble(vals[1]);
                data.add(it);
            }
            catch (NumberFormatException ex)
            {
                throw new WebUtilException("rdRes1File:  can't parse input data (" + line + ")");
            }
        }
        double[][] ret = new double[1][2];
        ret = data.toArray(ret);
        return ret;
    }
    public double[][] rdBinXYFile(File in) throws FileNotFoundException, IOException, WebUtilException
    {
        ArrayList<double[]> data = new ArrayList<>();
        DataInputStream input = new DataInputStream(new FileInputStream(in));
        
        try
        {
            while(true)
            {
                double[] it = new double[2];
                it[0] = input.readDouble();
                it[1] = input.readDouble();
                data.add(it);
            }
        }
        catch(EOFException eof)
        {
            // we're good
        }
        catch(IOException ex)
        {
            String ermsg = "Reading binary xy data from file: " + ex.getClass().getSimpleName();
            ermsg += " - " + ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
        // return as array
        double[][] ret = new double[1][2];
        ret = data.toArray(ret);
        return ret;
    }

    /**
     * Some plot products contain position and time information for javascript functions
     * These are included in the program output with a line of the form:
     * #imgPos ODC (x0,y0, w,h, strt, dur): 205, 28, 814, 437, 1068422416, 240
     * 
     * This routine looks through the output and if it finds that line it sets fields.
     * @param stdOut the output of the program
     * @return true if the line was found and fields are set
     */
    private boolean getImgPosInfo(String stdOut)
    {
        boolean ret=false;
        BufferedReader br = new BufferedReader(new StringReader(stdOut));
        
        String strtStr="#imgPos\\s+(\\w+)";
        String posStr="(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)(,\\s*(\\d+),\\s*(\\d+))?";
        try
        {
            String line;
            Pattern posPat = Pattern.compile(posStr);
            Pattern strtPat = Pattern.compile(strtStr);
            while ((line = br.readLine()) != null && imgCoord == null)            
            {
                Matcher strtMat = strtPat.matcher(line);
                if (strtMat.find())
                {
                    String prod = strtMat.group(1);
                    Matcher posMat = posPat.matcher(line);
                    if (posMat.find())
                    {
                        imgCoord = new ImageCoordinate();
                        
                        imgCoord.setProduct(prod);
                        imgCoord.setImgX0(Integer.parseInt(posMat.group(1)));
                        imgCoord.setImgY0(Integer.parseInt(posMat.group(2)));
                        imgCoord.setImgWd(Integer.parseInt(posMat.group(3)));
                        imgCoord.setImgHt(Integer.parseInt(posMat.group(4)));
                        imgCoord.setX0(Double.parseDouble(posMat.group(5)));
                        imgCoord.setxN(Double.parseDouble(posMat.group(6)));
                        String y0str = posMat.group(8);
                        if (y0str != null)
                        {
                            imgCoord.setY0(Double.parseDouble(y0str));
                            imgCoord.setyN(Double.parseDouble(posMat.group(9)));
                        }
                        ret = true;
                    }
                }
            }
        }
        catch (IOException | NumberFormatException ex)
        {
            ret = false;
            imgCoord = null;
        }
        return ret;
    }
    public int importImage(File img, String mime) throws WebUtilException 
    {
        return importImage(img,mime,"");
    }
    public int importImage(File img, String mime, String description) throws WebUtilException
    {
        int imgId = 0;

        try
        {
            ImageTable itbl = new ImageTable(db);

            // read in the output image
            long len = img.length();
            if (len > 1000)
            {
                byte[] imgBytes = new byte[(int) len];

                FileInputStream fis = new FileInputStream(img);
                int rlen = fis.read(imgBytes);

                ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
                imgId = itbl.addImg(vuser.getCn(), bis, mime);
                if (!description.isEmpty())
                {
                    itbl.addDescription(imgId, description);
                }
            }
        }
        catch(SQLException | IOException | NoSuchAlgorithmException ex)
        {
            throw new WebUtilException("Importing image to database", ex);
        }
        return imgId;
    }

    public PageItem getErrorMessage(String product,List<String> cmd)
    {
        PageItemList ret = new PageItemList();

        String stderr = getStderr();
        String stdout = getStdout();
        StringBuilder cmdStr = new StringBuilder();

        for (String s : cmd)
        {
            if (s.contains(" "))
            {
                s = "'" + s + "'";
            }
            cmdStr.append(s).append(" ");
        }
        ret.add("There was an error in generating coherence plot.");
        ret.addBlankLines(2);
        ret.add("Command:");
        ret.addBlankLines(1);
        ret.add(cmdStr.toString());
        ret.addBlankLines(2);
        ret.add(String.format("Returned status: %1$d", getStatus()));
        ret.addBlankLines(2);
        ret.addLine("Stdout:");
        PageItemString out = new PageItemString(stdout.replace("\n", "<br>"), false);
        ret.add(out);
        ret.addBlankLines(2);
        ret.addLine("Stderr:");
        PageItemString err = new PageItemString(stderr.replace("\n", "<br>"), false);
        ret.add(err);
        
        return ret;
    }

    /**
     * Create one or more plots from data provided
     *
     * @param dbuf data buffers with full descriptors
     * @param compact flag that image is small so minimize text added
     * @return list of image IDs of product images saved to the database
     * @throws WebUtilException
     */
    @Override
    public abstract ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) 
            throws WebUtilException;

    /**
     * External programs that are based on the PluginController/PluginManager classes all make their
     * plots the same way.  This does the work for the Manager class.
     * @param pc - PluginController contains the form to argument transforms
     * @param dbuf - describes the input data
     * @param compact - flag to make labels as short as possible
     * @return list of image IDs now in our database
     */
    public ArrayList<Integer> makePcPlot(PluginController pc, ArrayList<ChanDataBuffer> dbuf, 
                                         boolean compact) throws WebUtilException
    {
        ArrayList<Integer> ret = new ArrayList<>();
        try
        {
            pc.setFormParameters(paramMap);
            List<String> cmd = pc.getCommandArray(dbuf, paramMap);
            String cmdStr = pc.getCommandLine(dbuf, paramMap);
            vpage.add(cmdStr);
            vpage.addBlankLines(2);
            if (runExternalProgram(cmd, ""))
            {
                String txtOutput = String.format("%1$s Output:<br>%2$s", getProductName(), getStdout());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                txtOutput = String.format("%1$s <br>Stderr: %2$s", getProductName(), getStderr());
                vpage.add(new PageItemString(txtOutput, false));
                vpage.addBlankLines(1);
                File img = pc.getTempFile();
                Integer imgNum = addImg2Db(img, db, vuser.getCn());
                ret.add(imgNum);
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
        }
        catch (LdvTableException ex)
        {
            String ermsg = getProductName() + " failed to save image.  LdvTableException: "
                           + ex.getLocalizedMessage();
            throw new WebUtilException(ermsg);
        }
        return ret;
    }
    /**
     * flag to say whether we can accept all data sets at one time or one for each call default is
     * one per call, override in the plugin if you can take multiples
     *
     * @return true if you want a bunch of datasets
     */
    @Override
    public abstract boolean isStackable();

    /**
     * Determines whether the PluginManager creates an image description for each image in the list
     * or if the product itself creates the description
     *
     * @return true if the PluginManager should do it, false if the product does it
     */
    @Override
    public boolean needsImageDescriptor()
    {
        return true;
    }
    
    /**
     * Set the dimensions of the resulting image
     *
     * @param width in pixels
     * @param height in pixels
     */
    @Override
    public void setSize(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    /**
     * Get the external name of this product for selection and results page
     *
     * @return descriptive name
     */
    @Override
    public abstract String getProductName();

    /**
     * Return html object that displays all user settable parameters
     *
     * @param enableKey - parameter name for the switch that enables this product
     * @param nSel - number of input datasets available
     * @param multDisp - I'm not sure
     * @todo figure out if we still need/want multDisp parameter
     * @return the html object
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    @Override
    public abstract PageItem getSelector(String enableKey, int nSel, String[] multDisp) 
            throws WebUtilException;

    /**
     * Does this product want the PlotManager to get data, or does it do it itself
     *
     * @return true if PlotManager is to get the data
     */
    @Override
    public boolean needsDataXfer()
    {
        return false;
    }

    /**
     * Stacked/single selector
     * 
     * @param dispFormat 
     */
    @Override
    public void setDispFormat(String dispFormat)
    {
        this.dispFormat = dispFormat;
    }

    /**
     * Some products run in the background and do not return a list of images to display.
     *
     * @return true if caller should expect a non-empty list of images.
     */
    @Override
    public boolean hasImages()
    {
        return true;
    }
}
