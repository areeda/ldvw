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

import edu.fullerton.viewerplugin.ChanDataBuffer;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageCoordinateTbl;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.ExternalProgramManager;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A base class for plot generators that are external programs
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ExternalPlotManager extends ExternalProgramManager
{
    protected Map<String, String[]> paramMap;
    protected String contextPath;
    protected String servletPath;
    protected Database db;
    protected Page vpage;
    protected ViewUser vuser;
    private ImageCoordinate imgCoord;
    
    public ExternalPlotManager( Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    public void setup(Database db, Page vpage, ViewUser vuser)
    {
        this.db = db;
        this.vpage = vpage;
        this.vuser = vuser;
    }

    public void setParammap(Map<String, String[]> parammap)
    {
        this.paramMap = parammap;
    }

    public void setContextPath(String contextPath)
    {
        this.contextPath = contextPath;
    }

    public void setServletPath(String servletPath)
    {
        this.servletPath = servletPath;
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
}
