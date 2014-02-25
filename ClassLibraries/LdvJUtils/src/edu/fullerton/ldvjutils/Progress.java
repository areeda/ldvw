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
package edu.fullerton.ldvjutils;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * A public wrapper for our ProgressFrame class
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Progress
{
    private ProgressFrame progressFrame;
    
    public Progress()
    {
        //Schedule a job for the event-dispatching thread:
        //creating and showing our Frame.
        ProgressJob pj = new ProgressJob();
        try
        {
            SwingUtilities.invokeAndWait(pj);
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(Progress.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (InvocationTargetException ex)
        {
            Logger.getLogger(Progress.class.getName()).log(Level.SEVERE, null, ex);
        }
        progressFrame = pj.getProgressFrame();
        if (progressFrame == null)
        {
            System.err.println("joe still has a bug");
        }
    }
    /**
     * When using the startTiming method, this adds an increment to the current running total
     * 
     * @param add - increment in the same units as total set in startTiming
     * @see #startTiming
     */
    public void bumpCurTally(long add)
    {
        progressFrame.bumpCurTally(add);
    }
    /**
     * When using the startTiming method, this adds an increment to the current running total
     *
     * @param add - increment in the same units as total set in startTiming
     * @see #startTiming
     */
    public void bumpCurTally(double add)
    {
        progressFrame.bumpCurTally(add);
    }

    /**
     * When the process is finished call this method to close the window and release all resources
     */
    public void done()
    {
        progressFrame.done();
    }
    
    /**
     * Set line 1 of descriptive text
     * @param val - text for line 1
     */
    public void setChanName(String val)
    {
        progressFrame.setChanName(val);
    }
    
    /**
     * Set line 3 of descriptive text
     * @param val - text for line 3
     */
    public void setEstTime(String val)
    {
        progressFrame.setEstTimeLeft(val);
    }
    
    /**
     * Set/reset dialog position to Joe's preferred spot
     */
    public void setPosition()
    {
        progressFrame.setPosition();
    }
    
    /**
     * Set the progress bar to a specific percent complete
     * @param val - percent complete 0-100, or < 0 sets it to indeterminate mode
     */
    public void setProgress(int val)
    {
        progressFrame.setProgress(val);
    }
    /**
     * Set the window title
     * @param val - text of the new title
     */
    public void setTitle(String val)
    {
        progressFrame.setWindowTitle(val);
    }
    /**
     * Set the top, centered label inside the dialog
     * @param val - text of the label
     */
    public void setTitleLbl(String val)
    {
        progressFrame.setTitleLbl(val);
    }
    
    /**
     * Set line 2 of the descriptive text
     * 
     * @param val - text for line 2
     */
    public void setWorkingOn(String val)
    {
        progressFrame.setWorkingOn(val);
    }
    
    /**
     * If you want us to keep track of progress and estimate time to completion use this and 
     * the bumpCurTally method.  This starts an internal time and sets internal counter to zero.
     * The units are arbitrary, and used to compute percent complete and remaining time
     * 
     * @param total - value in arbitrary units that represent 100% complete
     * @see #bumpCurTally
     * @see #updateDoneSoFar(long) 
     */
    public void startTiming(double total)
    {
        progressFrame.startTiming(total);
    }
    
    /**
     * If you want us to keep track of progress and estimate time to completion use this and the
     * bumpCurTally method. This starts an internal time and sets internal counter to zero. The
     * units are arbitrary, and used to compute percent complete and remaining time
     *
     * @param total - value in arbitrary units that represent 100% complete
     * @see #bumpCurTally
     * @see #updateDoneSoFar(long)
     */
    public void startTiming(long total)
    {
        progressFrame.startTiming(total);
    }
    /**
     * Set the current progress to an absolute value in arbitrary units
     * 
     * @param cur - current progress in units used by startTiming
     * @see #startTiming
     * @see #bumpCurTally
     */
    public void updateDoneSoFar(double cur)
    {
        progressFrame.updateDoneSoFar(cur);
    }
    /**
     * Set the current progress to an absolute value in arbitrary units
     *
     * @param cur - current progress in units used by startTiming
     * @see #startTiming
     * @see #bumpCurTally
     */
    public void updateDoneSoFar(long cur)
    {
        progressFrame.updateDoneSoFar(cur);
    }
    /**
     * Poll to see if the cancel button was pressed
     * 
     * @return true if cancel button was pressed
     */
    public boolean wantsCancel()
    {
        return progressFrame.wantsCancel();
    }
}
