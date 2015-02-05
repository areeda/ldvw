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
package viewerplugin;

import edu.fullerton.jspWebUtils.WebUtilException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class GDSFilter
{
    private String prog = "/usr/local/ldvw/bin/ldvw-filter";
    private int debugLevel=5;
    
    public void apply(float[] data, float fs, String filtType, float cutoff, int order) throws WebUtilException
    {
        double[] ddata = new double[data.length];
        for(int i=0; i< data.length;i++)
        {
            ddata[i] = data[i];
        }
        apply(ddata, fs, filtType, cutoff, order);
        for (int i = 0; i < data.length; i++)
        {
            data[i] = (float) ddata[i];
        }
    }
    public void apply(double[] data, float fs, String filtType, float cutoff, int order) throws WebUtilException
    {

        StringBuilder strData = new StringBuilder();
        for(int i=0; i< data.length; i++)
        {
            strData.append(String.format("%1$.8f\n", data[i]));
        }
        if (debugLevel > 4)
        {
            try
            {
                FileWriter fw = new FileWriter("/tmp/filtInput.csv");
                fw.append(strData.toString());
                fw.close();
            }
            catch (IOException ex)
            {
                Logger.getLogger(GDSFilter.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        String cmd = String.format("%1$s --filt %2$s --cutoff %3$f --order %4$d --fs %5$f", 
                                   prog, filtType, cutoff, order, fs);
        ExternalProgramManager epm= new ExternalProgramManager();
        if (epm.runExternalProgram(cmd, strData.toString()))
        {
            String out = epm.getStdout();
            InputStream is = new ByteArrayInputStream(out.getBytes());

            // read it with BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            int i=0;
            try
            {
                Pattern numPat = Pattern.compile("[\\d\\.\\+\\-\\e]+");
                Matcher numMat;
                
                while ((line = br.readLine()) != null)
                {
                    try
                    {
                        numMat = numPat.matcher(line);
                        if (numMat.find())
                        {
                            
                            data[i] = Float.parseFloat(numMat.group());
                        }
                    }
                    catch (NumberFormatException numberFormatException)
                    {
                        throw new WebUtilException("Unable to convert data from filter: [" + line + "]");
                    }
                    i++;
                }
            }
            catch (IOException ex)
            {
                throw new WebUtilException("Error calling fitler program: " + ex.getLocalizedMessage());
            }
            if (i != data.length)
            {
                throw new WebUtilException("Got wrong number of samples back from filter program");
            }
        }
        else
        {
            String stderr = epm.getStderr();
            int status = epm.getStatus();
            String erMsg = String.format("Error running filter program (%1$s).  Status %2$d", prog,status);
            throw new WebUtilException(erMsg + stderr);
        }
    }
    
}
