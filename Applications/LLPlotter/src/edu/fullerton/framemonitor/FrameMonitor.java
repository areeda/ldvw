/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
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
package edu.fullerton.framemonitor;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class FrameMonitor
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        FrameMonitor me = new FrameMonitor();
        me.startTasks();
    }
//===== non-static methods
    private final List<PlotQueDefn> watchQueDefn;
    private final List<PlotQueDefn> listenQueDefn;
    
    private final List<Thread> watchers;
    private final List<Thread> processors;
    private final int pollingInterval = 1000;
    private final FileAlterationMonitor monitor;
    
    public FrameMonitor()
    {
        // init the predefined queues and plots
        watchQueDefn = new ArrayList<>();
        
        watchQueDefn.add(new PlotQueDefn("H1_darm", "H-H1_llhoft", "/dev/shm/llhoft/H1"));
        watchQueDefn.add(new PlotQueDefn("L1_darm", "L-L1_llhoft", "/dev/shm/llhoft/L1"));
        watchQueDefn.add(new PlotQueDefn("V1_darm", "V-V1_llhoft", "/dev/shm/llhoft/V1"));
        watchQueDefn.add(new PlotQueDefn("H1_detchar", "H-H1_lldetchar", "/dev/shm/lldetchar/H1"));
        
        listenQueDefn = new ArrayList<>();
        
        listenQueDefn.add(new PlotQueDefn("L1_raw",     "L-L1_R",               "/archive/frames/A6/L0/L1/L-L1_R-10947"));
        listenQueDefn.add(new PlotQueDefn("H1_raw",     "H-H1_R",               "/archive/frames/A6/L0/H1/H-H1_R-10947"));
        listenQueDefn.add(new PlotQueDefn("H1_sensmon", "SenseMonitor_CAL_H1",  "/archive/frames/dmt/H1/trends/SenseMonitor_CAL_H1/H-M-1094"));
        listenQueDefn.add(new PlotQueDefn("L1_sensmon", "SenseMonitor_CAL_L1",  "/archive/frames/dmt/L1/trends/SenseMonitor_CAL_L1/L-M-1094/"));
        
        
        watchers = new ArrayList<>();
        processors = new ArrayList<>();
        
        monitor = new FileAlterationMonitor(pollingInterval);
    }
    public void startTasks()
    {
        PrintStream log = System.out;
        
        // Built in watchers are very efficient but only work for native file systems 
        // But they do not work on nfs volumes
        for(PlotQueDefn pqd : watchQueDefn)
        {
            File dir = new File(pqd.getDirName());
            if (!dir.exists())
            {
                System.err.println("Directory: " + pqd.getDirName() + " does not exist.");
            }
            else
            {
                // create a list of queues that will process the files found
                BlockingQueue<File> outQue = new ArrayBlockingQueue<>(128);
                List<BlockingQueue<File>> fileQueue = new ArrayList<>();
                fileQueue.add(outQue);

                // create the processes that will consume the queues
                QueueProcessor pq = new IgnoreQueue(outQue, log);
                Thread pqt = new Thread(pq);
                pqt.setName(pqd.getQueName() + " - consumer");
                pqt.start();
                processors.add(pqt);


                FrameWatcher w = new FrameWatcher(pqd.getDirName(), pqd.getFrameType(), log, fileQueue);
                Thread wt = new Thread(w);
                wt.setName(pqd.getQueName() + " - watcher");
                wt.start();
                watchers.add(wt);
            }
        }
        // The Apache commons listeners poll so they also work on nfs volumes
        for(PlotQueDefn pqd : listenQueDefn)
        {
            File dir = new File(pqd.getDirName());
            if (!dir.exists())
            {
                System.err.println("Directory: " + pqd.getDirName() + " does not exist.");
            }
            else
            {
                // create a list of queues that will process the files found
                BlockingQueue<File> outQue = new ArrayBlockingQueue<>(128);
                List<BlockingQueue<File>> fileQueue = new ArrayList<>();
                fileQueue.add(outQue);

                // create the processes that will consume the queues
                QueueProcessor pq = new IgnoreQueue(outQue, log);
                Thread pqt = new Thread(pq);
                pqt.setName(pqd.getQueName() + " - consumer");
                pqt.start();
                processors.add(pqt);

                FileAlterationObserver observer = new FileAlterationObserver(pqd.getDirName());
                ChangeListener cl = new ChangeListener(log, outQue);
                observer.addListener(cl);
                monitor.addObserver(observer);
            }
        }
        try
        {
            monitor.start();
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable()
            {

                @Override
                public void run()
                {
                    try
                    {
                        System.out.println("Stopping monitor.");
                        monitor.stop();
                    }
                    catch (Exception ignored)
                    {
                    }
                }
            }));
        }
        catch (Exception ex)
        {
            Logger.getLogger(FrameMonitor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
}
