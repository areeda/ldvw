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

import edu.fullerton.ldvjutils.TimeAndDate;
import java.io.File;
import java.io.PrintStream;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ignores all files put into the specified queue.
 * Used for development and as a way to temporarily remove a plot.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class IgnoreQueue extends QueueProcessor implements Runnable
{
    
    
    public IgnoreQueue(BlockingQueue<File> inq, PrintStream log)
    {
       super(inq, log);
    }
    @Override
    public void run()
    {
        try
        {
            Pattern fnamePat = Pattern.compile("(.+)-(\\d+)-(\\d+)\\.(gwf|tmp)");
            Matcher fnameMat;
            while(!abort)
            {
                File infile = inq.take();
                
                // low latency data is transfered as .tmp file then renamed to .gwf
                // and we do not get a watcher signal on the rename so we have to check it ourselves
                fnameMat = fnamePat.matcher(infile.getName());
                
                int cnt = 0;
                final int maxCnt = 50;
                final int sleepInterval = 100;
                long age=0;
                
                if (fnameMat.find())
                {
                    if (fnameMat.group(4).equals("tmp"))
                    {
                        String dir = infile.getParent();
                        String nam = fnameMat.group(1).substring(1);
                        String newName = dir + "/" + nam + "-" + fnameMat.group(2) + "-" +
                                         fnameMat.group(3) + ".gwf";
                        File newInfile = new File(newName);
                        
                        while (cnt > maxCnt && ! newInfile.exists())
                        {
                            Thread.sleep(sleepInterval);
                            cnt++;
                        }
                        if (newInfile.exists())
                        {
                            long startGps = Long.parseLong(fnameMat.group(2));
                            long now = System.currentTimeMillis();
                            
                            age = now - TimeAndDate.gps2utc(startGps) * 1000;

                            infile = newInfile;
                        }
                    }
                    else
                    {
                        long startGps = Long.parseLong(fnameMat.group(2));
                        long now = System.currentTimeMillis();

                        age = now - TimeAndDate.gps2utc(startGps) * 1000;
                    }
                }
                String msg = "Ignore - " + infile.getAbsolutePath();
                if (cnt > 0)
                {
                    msg += String.format(", wait: %1$.2f", (cnt * sleepInterval)/1000f);
                }
                if (age > 0)
                {
                    msg += String.format(", age(s): %1$.1f", (age/1000.f));
                }
                logit( msg);
            }
        }
        catch (InterruptedException ex)
        {
            Logger.getLogger(IgnoreQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
