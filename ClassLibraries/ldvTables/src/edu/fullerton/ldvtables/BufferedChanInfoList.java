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
package edu.fullerton.ldvtables;

import edu.fullerton.ldvjutils.ChanInfo;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A class to aid in working with large numbers of channels for updating
 * or classifying the channel tables.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class BufferedChanInfoList
{
    private int bufsize = 1000;   /// how many channels to get at one time
    private int curOffset;              /// offset of next query
    private final int nchan;                  /// how many matching records in the db
    private ResultSet rs;               /// the results of the query
    private boolean done;               /// once we've hit the end don't try again
    
    private final String clistServer, clistChanType;
    private final ChannelTable ct;

    public BufferedChanInfoList(ChannelTable ct, String server, String channelType)
    {
        this.ct = ct;
        
        curOffset = 0;
        nchan = ct.getCount(server, channelType);
        clistServer = server;
        clistChanType = channelType;
        rs = null;
        done = false;
    }

    /**
     * get the next channel matching the criteria set up by constructor
     * @return ChanInfo object identifying the channel or null if no more
     * @throws java.sql.SQLException
     */
    public ChanInfo getNextChan() throws SQLException
    {
        ChanInfo ret = null;
        
        if (!done)
        {
            if (rs == null)
            {
                rs = ct.findChannelByServerType(clistServer, clistChanType,curOffset,bufsize);
                curOffset += bufsize;
            }
            // This is not an else clause.  
            // First time through we read and here we test if that was successful
            if (rs != null)
            {
                if (rs.next())
                {   // if we have any left in this batch
                    ret = new ChanInfo();
                    ret.fill(rs);
                }
                else if (curOffset < nchan)
                {   // more left get another batch
                    rs = ct.findChannelByServerType(clistServer, clistChanType, curOffset, bufsize);
                    curOffset += bufsize;
                    if (rs.next())
                    {   // if we have any left in this batch
                        ret = new ChanInfo();
                        ret.fill(rs);
                    }
                    else
                    {   // now we're done
                        rs.close();
                        done = true;
                    }
                }
                else
                {   // there's two of these, it depends on whether nchan is a multiple of bufsize
                    // which one gets executed.
                    rs.close();
                    done = true;
                }
            }
            else
            {
                done = true;
            }
        }        
        return ret;
        
    }

    public void setBufsize(int bufsize)
    {
        this.bufsize = bufsize;
    }
    
}
