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

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 * A popup progress bar.
 * 
 * This class must run in the event loop.  Access functions set fields which are then
 * used to set Swing widgets from inside the event loop.
 * 
 * BEWARE of calling any of the underlying JFrame or component methods from outside the event loop
 * they are not thread-safe.  If that doesn't make sense don't do call anything here, use the wrapper.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class ProgressFrame extends javax.swing.JFrame implements ActionListener
{
    private boolean hasCanceled = false;
    private Timer updTimer;
    private int speed = 250;    // how often to update the dialog in ms
    private int delay = 1000;   // how soon to do the first update
    
    //-----Access functions for widgets set these, then we
    //     update the GUI on the timer event
    private volatile boolean doSetPosition;
    private volatile boolean doSetProgress;
    private volatile int progValue=-1;
    private volatile boolean doSetTitleLabel;
    private volatile String titleLabel="";
    private volatile String estTimeVal="";
    private volatile boolean doEstTime;
    private volatile String workingOnVal="";
    private volatile boolean doWorkingOn;
    private volatile String chanNameVal="";
    private volatile boolean doChanName;
    private volatile String windowTitleVal = "Progress";
    private volatile boolean doWindowTitle;
    
    // These variables are for automatic update of estimated time label in the form
    // Working on x of y.  Est time remaining: mm:ss
    private long startTimeMs;
    private double endVal;
    private double curVal;
    /**
     * Creates new form  for the progress dialog an starts the update timer
     * setting all children to update to their default values
     */
    public ProgressFrame()
    {
        initComponents();
        hasCanceled = false;    // they haven't canceled us yet
        
        // update everything as soon as we're ready
        doSetPosition = true;
        doSetProgress=true;
        doSetTitleLabel=true;
        doEstTime=true;
        doWorkingOn=true;
        doChanName=true;
        doWindowTitle=true;
        
        // start the timer for regular updates
        updTimer = new Timer(speed,this);
        updTimer.setInitialDelay(delay);
        updTimer.start();
        setVisible(true);
    }
    

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT
     * modify this code. The content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        progBar = new javax.swing.JProgressBar();
        titleLbl = new javax.swing.JLabel();
        chanNameLbl = new javax.swing.JLabel();
        workingOnLbl = new javax.swing.JLabel();
        estTimeLeftLbl = new javax.swing.JLabel();
        cancelBtn = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        progBar.setStringPainted(true);

        titleLbl.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLbl.setText("Processing ODC Plot");
        titleLbl.setMaximumSize(new java.awt.Dimension(200, 14));
        titleLbl.setPreferredSize(new java.awt.Dimension(200, 14));

        chanNameLbl.setText(" ");

        workingOnLbl.setText(" ");

        estTimeLeftLbl.setText(" ");

        cancelBtn.setText("Cancel");
        cancelBtn.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cancelBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(titleLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 399, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progBar, javax.swing.GroupLayout.DEFAULT_SIZE, 502, Short.MAX_VALUE)
                    .addComponent(cancelBtn)
                    .addComponent(estTimeLeftLbl)
                    .addComponent(workingOnLbl)
                    .addComponent(chanNameLbl))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {chanNameLbl, estTimeLeftLbl, titleLbl, workingOnLbl});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(titleLbl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(chanNameLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(workingOnLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(estTimeLeftLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addComponent(progBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(cancelBtn)
                .addGap(28, 28, 28))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void cancelBtnActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelBtnActionPerformed
    {//GEN-HEADEREND:event_cancelBtnActionPerformed
        hasCanceled = true;
    }//GEN-LAST:event_cancelBtnActionPerformed

    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelBtn;
    private javax.swing.JLabel chanNameLbl;
    private javax.swing.JLabel estTimeLeftLbl;
    private javax.swing.JProgressBar progBar;
    private javax.swing.JLabel titleLbl;
    private javax.swing.JLabel workingOnLbl;
    // End of variables declaration//GEN-END:variables

    //---------------------------------------------------------------
    // Public access functions, these are the ones users should use |
    //---------------------------------------------------------------
    public void setChanName(String val)
    {
        chanNameVal = val;
        doChanName = true;
    }
    public void setWindowTitle(String val)
    {
        windowTitleVal = val;
    }
    public void setWorkingOn(String val)
    {
        workingOnVal = val;
        doWorkingOn = true;
    }
    public void setEstTimeLeft(String val)
    {
        estTimeVal = val;
        doEstTime=true;
    }
    public void setTitleLbl(String val)
    {
        titleLabel = val;
        doSetTitleLabel = true;
    }
    public void setProgress(int val)
    {
        progValue = val;
        doSetProgress = true;
    }
    public void setPosition()
    {
        doSetPosition = true;
    }
   
    public void startTiming(long total)
    {
        startTiming((double) total);
    }
    /**
     * If you want us to calculate remaining time and % progress, call this method on start
     * then call updateDoneSoFar.
     * @param total used to calculate % complete
     */
    public void startTiming(double total)
    {
        startTimeMs = System.currentTimeMillis();
        setProgress(0);     // progress bar to determinate
        endVal= total;
    }
    public void bumpCurTally(long add)
    {
        bumpCurTally((double) add);
    }
    public void bumpCurTally(double add)
    {
        curVal += add;
        updateDoneSoFar(curVal);
    }
    public void updateDoneSoFar(long cur)
    {
        updateDoneSoFar((double)cur);
    }
    public void updateDoneSoFar(double cur)
    {
        double elapsed = (System.currentTimeMillis() - startTimeMs) / 1000.f;
        double estLeft;
        int pct=0;
        String estLeftStr = String.format("Working on %1$.0f of %2$.0f",cur,endVal);
        if (endVal > 0 && cur > 0)
        {
            pct = (int) Math.round(cur * 100./ endVal);
            estLeft = (endVal / cur - 1) * elapsed ;
            int hr=(int) (estLeft/3600);
            int min = (int) ((estLeft - hr*3600) / 60);
            int sec = (int) Math.round(estLeft) % 60;
            String left = hr > 0 ? Integer.toString(hr) + ":" : "";
            if (min > 0 || hr > 0)
            {
                left += String.format("%1$02d:", min);
            }
            left += String.format("%1$02d", sec);
            estLeftStr += ".  Est time remaining: " + left;
        }
        setEstTimeLeft(estLeftStr);
        setProgress(pct);
    }
    /**
     * poll if cancel button was pressed
     * @return true if cancel button has been pressed
     */
    public boolean wantsCancel()
    {
        return hasCanceled;
    }
    /**
     * I hate the default position on multiple monitors so this makes Joe happy
     */
    private void mySetPosition()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension sd = tk.getScreenSize();
        int myHeight = getHeight();
        int scrnYpos = (int) (sd.getHeight() / 2 - myHeight);
        setLocation(200, scrnYpos);
    }
    private void mySetProgress()
    {
        if (progValue < 0)
        {
            progBar.setIndeterminate(true);
        }
        else
        {
            progBar.setIndeterminate(false);
            progBar.setValue(progValue);
        }
    }
    /**
     * What to do when the timer goes off.  The purpose of this is to get all Swing component changes
     * done in the Event Dispatch Thread
     * @param ae timer event (any others are ignored if we even get them)
     */
    @Override
    public void actionPerformed(ActionEvent ae)
    {
        if (isVisible())
        {
            if (doSetPosition)
            {
                mySetPosition();
                doSetPosition = false;
            }
            if (doSetProgress)
            {
                mySetProgress();
                doSetProgress=false;
            }
            if (doSetTitleLabel)
            {
                titleLbl.setText(titleLabel);
                doSetTitleLabel = false;
            }
            if (doEstTime)
            {
                estTimeLeftLbl.setText(estTimeVal);
                doEstTime = false;
            }
            if (doWindowTitle)
            {
                setTitle(windowTitleVal);
            }
            if (doWorkingOn)
            {
                workingOnLbl.setText(workingOnVal);
                doWorkingOn = false;
            }
            if (doChanName)
            {
                chanNameLbl.setText(chanNameVal);
                doChanName = false;
            }
        }
    }

    /**
     * when user wants us to go away and allow the app to exit, they call this method
     */
    public void done()
    {
        updTimer.stop();
        dispose();
    }
}
