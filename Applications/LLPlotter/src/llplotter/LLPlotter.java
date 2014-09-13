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
package llplotter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class LLPlotter
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        LLPlotter me = new LLPlotter();
        me.startTasks();
    }
//===== non-static methods
    private List<PlotQueDefn> queDefn;
    private List<Thread> watchers;
    private List<Thread> processors;
    public LLPlotter()
    {
        // init the predefined queues and plots
        queDefn = new ArrayList<>();
        
        queDefn.add(new PlotQueDefn("H1_darm", "H-H1_llhoft", "/dev/shm/llhoft/H1"));
        queDefn.add(new PlotQueDefn("L1_darm", "L-L1_llhoft", "/dev/shm/llhoft/L1"));
        queDefn.add(new PlotQueDefn("V1_darm", "V-V1_llhoft", "/dev/shm/llhoft/V1"));
        queDefn.add(new PlotQueDefn("L1_raw",  "L-L1_R",      "/archive/frames/A6/L0/L1/L-L1_R-10946"));
        queDefn.add(new PlotQueDefn("H1_raw",  "H-H1_R",      "/archive/frames/A6/L0/H1/H-H1_R-10946"));
        
        watchers = new ArrayList<>();
        processors = new ArrayList<>();
    }
    public void startTasks()
    {
        PrintStream log = System.out;
        
        for(PlotQueDefn pqd : queDefn)
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
    
    
}
