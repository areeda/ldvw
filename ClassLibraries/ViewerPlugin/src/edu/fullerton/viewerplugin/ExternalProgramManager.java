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
import edu.fullerton.ldvtables.ImageTable;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;

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
    
    /**
     * Use our server's keytab to get a new or refresh our Kerberos TGT
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public static void getTGT() throws WebUtilException
    {
        File keyTab = new File("/usr/local/ldvw/keytab/ldvw.keytab");
        if (keyTab.exists())
        {
            try
            {
                String cmd = String.format("kinit ligodv-test-areeda@LIGO.ORG -k -t %1$s", keyTab.getCanonicalPath());
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
        catch (IOException e)
        {
            throw new WebUtilException("Error executing command [" + command + "] - " + e.getLocalizedMessage());
        }
        catch (InterruptedException e)
        {
            throw new WebUtilException("Command interrupted: " + e.getLocalizedMessage());
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
    public PageItemList makeDescription(PlotProduct product, ArrayList<ChanDataBuffer> bufList) throws WebUtilException
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
}
