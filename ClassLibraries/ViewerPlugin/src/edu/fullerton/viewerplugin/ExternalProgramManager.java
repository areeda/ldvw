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
package edu.fullerton.viewerplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageTable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * General routines for calling command line functions
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ExternalProgramManager 
{
    protected int status;                       // status of last command
    protected StringBuilder outTxt, errTxt;     // stdout and stderr from last command
    protected ImageTable imgTbl = null;
    protected ArrayList<String> env;
    protected Path tmpPath = null;
    protected ArrayList<File> tmpFileDirs=null; // temporary files and directories we have created
    
    /**
     * Use our server's keytab to get a new or refresh our Kerberos TGT
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public static void getTGT() throws WebUtilException
    {
        ExternalProgramManager.getTGT("/usr/local/ldvw/keytab/ldvw.keytab", "ligodv-test-areeda@LIGO.ORG");
    }
    public static void getTGT(String filename, String username) throws WebUtilException
    {
        File keyTab = new File(filename);
        if (keyTab.exists())
        {
            try
            {
                String cmd = String.format("kinit " + username +" -k -t %1$s", keyTab.getCanonicalPath());
                Process p = Runtime.getRuntime().exec(cmd);
                p.waitFor();
            }
            catch (InterruptedException ex)
            {
                throw new WebUtilException("Interrupt received: " + ex.getLocalizedMessage());
            }
            catch (IOException ex)
            {
                throw new WebUtilException("Problem getting Kerberos ticket: " + ex.getLocalizedMessage());
            }

        }
    }

    public ExternalProgramManager()
    {
        this.env = null;
    }
    /**
     * Run an external (command line) program with nothing sent to stdin
     * @see ExternalProgramManager#getStderr() for output sent to stderr
     * @see ExternalProgramManager#getStdout() for output sent to stdout
     * @param command the command to execute (assume no environment)
     * @return status true if status code is 0
     * @throws WebUtilException any errors produced by trying to run that command except return codes 
     */
    public boolean runExternalProgram(String command) throws WebUtilException
    {
        return runExternalProgram(command,"");
    }
    public boolean runExternalProgram(ArrayList<String> command) throws WebUtilException
    {
        return runExternalProgram(command,"");
    }
    
    public boolean runExternalProgram(String command, String sendData) throws WebUtilException
    {
        boolean ret;
        try
        {
            ExternalProgramManager.getTGT();   // we need a Kerberos ticket for this

            Process p;
            if (env == null || env.isEmpty())
            {
                p = Runtime.getRuntime().exec(command);
            }
            else
            {
                String[] a = new String[0];
                p = Runtime.getRuntime().exec(command, env.toArray(a));
            }
            ret = getProcessOutput(p,sendData);
            
        }
        catch (IOException e)
        {
            throw new WebUtilException("Error executing command [" + command + "] - " + e.getLocalizedMessage());
        }
        return ret;
    }

    /**
     * Run an external program and wait for completion
     * @param command - command and arguments as a list (no quoting needed)
     * @param sendData - String to pass on stdin
     * @return true if programs return status == 0
     * @throws WebUtilException 
     * @see #getStatus() - for actual return status value
     * @see #getStderr()  - text program sent to stderr
     * @see #getStdout()  - text program sent to stdout
     */
    public boolean runExternalProgram(List<String> command, String sendData) throws WebUtilException
    {
        boolean ret;
        try
        {
            ExternalProgramManager.getTGT();   // we need a Kerberos ticket for this

            Process p;
            if (env == null || env.isEmpty())
            {
                String[] a = new String[0];
                p = Runtime.getRuntime().exec(command.toArray(a));
            }
            else
            {
                String[] a = new String[0];
                p = Runtime.getRuntime().exec(command.toArray(a), env.toArray(a));
            }
            ret = getProcessOutput(p,sendData);
            
        }
        catch (IOException e)
        {
            throw new WebUtilException("Error executing command [" + command + "] - " + e.getLocalizedMessage());
        }
        return ret;
    }

    public String getStdout()
    {
        return outTxt.toString();
    }
    
    public String getStderr()
    {
        return errTxt.toString();
    }
    public int getStatus()
    {
        return status;
    }
    public PageItemList makeDescription(PlotProduct product, ArrayList<ChanDataBuffer> bufList) throws WebUtilException, LdvTableException
    {
        PageItemList intro = new PageItemList();
        PageItemString pname = new PageItemString(product.getProductName());
        pname.addStyle("font-weight", "bold");
        intro.add(pname);
        intro.addBlankLines(1);
        for (ChanDataBuffer buf : bufList)
        {
            intro.add(buf.getChanInfo().toString() + ". ");
            String timeDescription = buf.getTimeInterval().getTimeDescription();
            intro.add(timeDescription);
            intro.addBlankLines(1);
        }
        
        return intro;
    }
    /**
     * The image table is the database section which holds results.
     * 
     * @param db mysql database object
     * @return image table object
     * @throws SQLException 
     */
    public ImageTable getImageTable(Database db) throws SQLException
    {
        if (imgTbl == null)
        {
            imgTbl = new ImageTable(db);
        }
        return imgTbl;
    }
    public void addEnv(String[] e)
    {
        if (env == null )
        {
            env = new ArrayList<>();
        }
        for (String es : e)
        {
            env.add(es);
        }
    }
    /**
     * Help manage temporary files/directories
     * If you use these calls and call removeTemps we will keep track and delete all files 
     * and directories
     * @param prefix
     * @return a new empty temporary directory
     * @throws IOException 
     * @see #getTempFile(java.lang.String, java.lang.String)  
     * @see #removeTemps() 
     */
    public File getTempDir(String prefix) throws IOException
    {
        File tempDir = Files.createTempDirectory("ldvw_"+prefix).toFile();
        addTmpFile(tempDir);
        if (tmpPath == null)
        {
            tmpPath = tempDir.toPath();
        }
        return tempDir;
    }
    public File getTempFile(String prefix, String ext) throws IOException
    {
        File tmpFile;
        if (tmpPath == null)
        {
            tmpFile = Files.createTempFile(prefix, ext).toFile();
        }
        else
        {
            tmpFile = Files.createTempFile(tmpPath, prefix, ext).toFile();
        }
        addTmpFile(tmpFile);
        return tmpFile;
    }
    /**
     * remove all the files and directories we created
     */
    public void removeTemps()
    {
        if (tmpFileDirs != null)
        {
            for (File f : tmpFileDirs)
            {
                rm(f);
            }
        }
    }
    private void addTmpFile(File tempDir)
    {
        if (tmpFileDirs == null)
        {
            tmpFileDirs = new ArrayList<>();
        }
        tmpFileDirs.add(tempDir);
    }

    /**
     * remove a file or directory (and all files and directories in that directory)
     * 
     * @param f what you want rubbed out
     */
    private void rm(File f)
    {
        if (f.exists())
        {
            if (f.isDirectory())
            {
                File[] listFiles = f.listFiles();
                for (File sf : listFiles)
                {
                    rm(sf);
                }
                f.delete();
            }
            else
            {
                f.delete();
            }
        }
    }
    public File writeTempCSV(String prefix, double[][] data) throws IOException
    {
        File tmpFile = getTempFile(prefix, ".csv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile)))
        {
            for (double[] r : data)
            {
                bw.append(String.format("%.6f, %.20g%n", r[0],r[1]));
            }
        }
        
        return tmpFile;
    }
    /**
     * Add an external image file to the image table in the ligodv database
     * @param outFile - output of some product (png, pdf, jpg, gif)
     * @param db - ligodv database
     * @param userCn - user's common name to file this under
     * @return image ID of the newly added image
     * @throws WebUtilException 
     */
    public int addImg2Db(File outFile, Database db, String userCn) throws WebUtilException
    {
        int imgId = 0;      // return code 0 means no image saved
        try
        {
            ImageTable itbl = getImageTable(db);
            
            // read in the output image
            long len = outFile.length();
            if (len > 1000)
            {
                FileInputStream fis = null;
                try 
                {
                    byte[] imgBytes = new byte[(int) len];
                    fis = new FileInputStream(outFile);
                    int rlen = fis.read(imgBytes);
                    ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);
                    imgId = itbl.addImg(userCn, bis, "image/png");
                }
                catch (NoSuchAlgorithmException | IOException  ex) 
                {
                    throw new WebUtilException("Saving image to database", ex);
                }
                finally 
                {
                    try 
                    {
                        if (fis != null)
                        {
                            fis.close();
                        }
                    }
                    catch (IOException ex) 
                    {
                        throw new WebUtilException("Saving image to database", ex);
                    }
                }
            }
            else
            {
                throw new WebUtilException("Adding image to database: image is not valid.");
            }
        }
        catch (SQLException ex)
        {
            throw new WebUtilException("Saving image to database", ex);
        }
        return imgId;
    }

    private boolean getProcessOutput(Process p, String sendData) throws WebUtilException
    {
        boolean ret = false;
        try
        {
            InputStream stderrStrm = p.getErrorStream();    // attached to stderr
            // nb: the reason in and out are reversed is that I'm talking about the external program
            // and java is talking about this program.
            InputStream stdoutStrm = p.getInputStream();   // attached to stdout

            if (sendData != null && !sendData.isEmpty())
            {
                try (OutputStream stdinStrm = p.getOutputStream())
                {
                    stdinStrm.write(sendData.getBytes());
                    stdinStrm.flush();
                }
            }
            BufferedReader outRdr = new BufferedReader(new InputStreamReader(stdoutStrm));
            String line;
            outTxt = new StringBuilder();
            while ((line = outRdr.readLine()) != null)
            {
                outTxt.append(line).append("<br>\n");
            }

            errTxt = new StringBuilder();
            BufferedReader errRdr = new BufferedReader(new InputStreamReader(stderrStrm));
            while ((line = errRdr.readLine()) != null)
            {
                errTxt.append(line).append("<br>\n");
            }

            status = p.waitFor();
            ret = (status == 0);
        }
        catch (InterruptedException e)
        {
            throw new WebUtilException("Command interrupted: " + e.getLocalizedMessage());
        }
        catch (IOException ex)
        {
            throw new WebUtilException("Running external program:", ex);
        }
        return ret;
    }
}
