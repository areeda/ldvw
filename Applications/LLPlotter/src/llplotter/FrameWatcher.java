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
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class FrameWatcher implements Runnable
{
    public volatile boolean abort;
    private final Path sourceDir;
    private final String frameType;
    private final PrintStream log;
    private final List<BlockingQueue<File>> outQueues;
    private final SimpleDateFormat sdf;
    
    public FrameWatcher(String sourceDir, String frameType, PrintStream log, 
                        List<BlockingQueue<File>> outQueues)
    {
        this.sourceDir = FileSystems.getDefault().getPath(sourceDir);
        this.frameType = frameType;
        this.log = log;
        this.outQueues = outQueues;
        abort = false;
        
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        GregorianCalendar now = new GregorianCalendar(utctz);
        sdf.setTimeZone(utctz);
    }

    @Override
    public void run()
    {
        try
        {
            Pattern fnamePat;
            Matcher fnameMat;
            
            fnamePat = Pattern.compile("(.+)-(\\d+)-(\\d+)\\.(gwf|tmp)");
            WatchService watchService;
            watchService = sourceDir.getFileSystem().newWatchService();
            sourceDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            String smsg = String.format("Start watching %1$s for %2$s frames.", sourceDir, frameType);
            logit(smsg);
            
            while (!abort)
            {
                WatchKey wkey = watchService.take();
                for (final WatchEvent<?> event : wkey.pollEvents())
                {
                    Path context = (Path) event.context();
                    String fname = context.getFileName().toString();
                    
                    WatchEvent.Kind<?> kind = event.kind();
                    String kindName = "unknown";
                    if (kind.equals(StandardWatchEventKinds.ENTRY_CREATE))
                    {
                        kindName = "Created.";
                    }
                    else if (kind.equals(StandardWatchEventKinds.ENTRY_DELETE))
                    {
                        kindName = "Deleted.";
                    }
                    else if (kind.equals(StandardWatchEventKinds.ENTRY_MODIFY))
                    {
                        kindName = "Modified.";
                        
                        fnameMat = fnamePat.matcher(fname);
                        if (fnameMat.find())
                        {
                            long startGps = Long.parseLong(fnameMat.group(2));
                            long now = System.currentTimeMillis();
                            long age = now - TimeAndDate.gps2utc(startGps) * 1000;
                            logit(String.format("Queued: %1$s age: %2$.2f", fname, age/1000.));

                            File file = new File(sourceDir + "/" + fname);
                            for(BlockingQueue<File> q : outQueues)
                            {
                                q.add(file);
                            }
                        }
                    }
                    logit(String.format("%1$s - %2$s", fname, kindName));
                }
                if (!wkey.reset())
                {
                    logit(sourceDir.toString() + " is no longer valid.");
                    wkey.cancel();
                    watchService.close();
                    break;
                }
            }
        }
        catch (InterruptedException | IOException ex)
        {
            logError(ex);
        }

    }

    private void logit(String msg)
    {
        log.format("%1$s: %2$s%n", sdf.format(new Date()), msg);
    }

    private void logError(Exception ex)
    {
        String ermsg = ex.getClass().getSimpleName() + " - " + ex.getLocalizedMessage();
        logit(ermsg);
    }

}
