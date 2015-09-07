/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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

package chanupdater;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Start up multiple tasks to download channel lists and write to a file
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class XferChanList 
{
    private final int NTHREADS=12;
    private final Executor exec;
    
    public XferChanList()
    {
        exec = Executors.newFixedThreadPool(NTHREADS);
    }

    /**
     * Queue a task to get a channel list into an external file
     * @param server - NDS2 server 
     * @param cType - channel type
     * @param outFile - output text file
     */
    public void getChanList(String server, String cType, String outFile)
    {
        GetChanListTask gclt = new GetChanListTask(server, cType, outFile);
        exec.execute(gclt);
    }
}
