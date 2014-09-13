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
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.concurrent.BlockingQueue;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public abstract class QueueProcessor implements Runnable
{
    protected BlockingQueue<File> inq;
    protected PrintStream log;
    private final SimpleDateFormat sdf;
    
    public volatile boolean abort;

    private QueueProcessor()
    {
        sdf = null;
    }
    public QueueProcessor(BlockingQueue<File> inq, PrintStream log)
    {
        this.inq = inq;
        abort = false;
        this.log = log;
        
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone utctz = new SimpleTimeZone(0, "UTC");
        GregorianCalendar now = new GregorianCalendar(utctz);
        sdf.setTimeZone(utctz);

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
}
