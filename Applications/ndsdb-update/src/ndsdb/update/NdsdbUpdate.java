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
package ndsdb.update;

import edu.fullerton.ldvjutils.LdvTableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;

/**
 * Application to build tables from NDS2 server source files.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsdbUpdate
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        NdsdbUpdate me = new NdsdbUpdate();
        try
        {
            me.process(args);
        }
        catch (ViewConfigException| LdvTableException ex)
        {
            Logger.getLogger(NdsdbUpdate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private final int verbose = 2;
    private final boolean doFrameDump = false;
    private final boolean doChanLists = true;
    
    public NdsdbUpdate()
    {
        
    }

    private void process(String[] args) throws LdvTableException, ViewConfigException
    {
        String site=args[0];
        String dir=args[1];
        String server = args[2];
        
        if (doFrameDump)
        {
            UpdateFrameAvailability ufa = new UpdateFrameAvailability(verbose);
            ufa.updateFromCacheDump(site, dir);
        }
        
        if (doChanLists)
        {
            UpdateChannelList ucl = new UpdateChannelList();
            ucl.setVerbose(verbose);
            ucl.loadDb();
            ucl.updateFromDir(site,dir,server);
        }
    }

    
}
