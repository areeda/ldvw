/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
package odcplot;

import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.Progress;
import viewerconfig.ViewConfigException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class MatlabGlue
{
    private static Progress pb = null;
    
    public MatlabGlue()
    {
        
    }
    public static void setProgress(Progress pbar)
    {
        pb = pbar;
    }
    public static String createImageFile(Integer startTime, Integer duration, String outFile) throws ViewConfigException
    {
        String ret;

        OdcPlot op = new OdcPlot();
        op.setStartGPS(startTime);
        op.setDuration(duration);
        op.setOfileName(outFile);
        if (pb != null)
        {
            op.setProgressDialog(pb);
            op.setShowProgressBar(true);
        }
        else
        {
            op.setShowProgressBar(false);
        }
        try
        {
            op.doPlot();
            ret = op.getStatus();
        }
        catch (WebUtilException ex)
        {
            ret = "Error: " + ex.getLocalizedMessage();
        }
        
        
        return ret;
    }
}
