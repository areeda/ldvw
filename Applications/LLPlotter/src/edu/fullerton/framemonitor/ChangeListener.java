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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChangeListener  implements FileAlterationListener
{
    private FileAlterationObserver observer;
    private final SimpleDateFormat sdf;
    private final PrintStream log;
    private final BlockingQueue<File> outQue;
    private final Pattern fnamePat;
    private Matcher fnameMat;


    public ChangeListener(PrintStream log, BlockingQueue<File> outQue)
    {
        this.log = log;
        this.outQue = outQue;
        
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        GregorianCalendar now = new GregorianCalendar(utctz);
        sdf.setTimeZone(utctz);
        fnamePat = Pattern.compile("(.+)-(\\d+)-(\\d+)\\..*(gwf|tmp)");
    }
    
    protected void logit(String msg)
    {
        log.format("%1$s: %2$s%n", sdf.format(new Date()), msg);
    }

    protected void logError(Exception ex)
    {
        String ermsg = ex.getClass().getSimpleName() + " - " + ex.getLocalizedMessage();
        logit(ermsg);
    }
    @Override
    public void onStart(FileAlterationObserver observer)
    {
    }

    @Override
    public void onDirectoryCreate(File directory)
    {
        logit("Directory created: " + directory.getAbsolutePath());
    }

    @Override
    public void onDirectoryChange(File directory)
    {
        logit("Directory changed:" + directory.getAbsolutePath());
    }

    @Override
    public void onDirectoryDelete(File directory)
    {
        logit("Directory deleted: " + directory.getAbsolutePath());
    }

    @Override
    public void onFileCreate(File file)
    {
        fnameMat = fnamePat.matcher(file.getName());
        if (fnameMat.find())
        {
            if (fnameMat.group(4).equals("gwf"))
            {
                //logit("File created: " + file.getAbsolutePath());
                outQue.add(file);
            }
        }
    }

    @Override
    public void onFileChange(File file)
    {
        //logit("File changed: " + file.getAbsolutePath());
    }

    @Override
    public void onFileDelete(File file)
    {
        //logit("File deleted: " + file.getAbsolutePath());
    }

    @Override
    public void onStop(FileAlterationObserver observer)
    {
    }
}
