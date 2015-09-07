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

/**
 * A separate task to transfer a
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class GetChanListTask implements Runnable
{
    private final String server;
    private final String cType;
    private final String outFile;

    GetChanListTask(String server, String cType, String outFile)
    {
        this.server = server;
        this.cType = cType;
        this.outFile = outFile;
    }

    @Override
    public void run()
    {
        File out = new File(outFile);
    }

}
