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

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ndstables.NdsChanAvailTable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import ndsJUtils.NDSChannelAvailability;

/**
 * Maintain a set of unique availability object with a persistent id value
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class AvailManager 
{
    private int accession;
    private final Database db;
    private final Map<NDSChannelAvailability, Integer> allAvail;
    
    public AvailManager(Database db)
    {
        accession = 0;
        this.db = db;
        allAvail = new HashMap<>();
    }

    public void loadDb() throws LdvTableException
    {
        try
        {
            NdsChanAvailTable ndscat = new NdsChanAvailTable(db);
            ndscat.streamAll();
            NDSChannelAvailability nca;
            while ((nca = ndscat.streamNext()) != null)
            {
                int idx = nca.getIdx();
                nca.setFromDb(true);
                allAvail.put(nca, idx);
            }
            ndscat.streamClose();
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Loading full Channel Availability Table", ex);
        }
    }

    int store(NDSChannelAvailability ca) throws LdvTableException
    {
        int ret = 0;
        if (allAvail.containsKey(ca))
        {
            ret = allAvail.get(ca);
        }
        else
        {
            int t = ca.getIdx();
            if (t > accession)
            {
                accession = t;
                ret = t;
            }
            else if (t == 0)
            {
                accession++;
                ca.setIdx(accession);
                allAvail.put(ca, accession);
            }
            else if (allAvail.containsValue(t))
            {
                ret = t;
            }
            else
            {
                throw new LdvTableException(("Duplicate availability index"));
            }
        }
        return ret;
    }
    public void updateDb() throws LdvTableException
    {
        try
        {
            NdsChanAvailTable ndscat = new NdsChanAvailTable(db);
            for(Entry<NDSChannelAvailability, Integer> ent : allAvail.entrySet())
            {
                NDSChannelAvailability nca = ent.getKey();
                if (!nca.isFromDb())
                {
                    ndscat.insertUpdate(nca);
                }
            }
            
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Saving channel availablility.", ex);
        }

    }
}
