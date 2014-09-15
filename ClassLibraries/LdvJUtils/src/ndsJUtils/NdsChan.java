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

package ndsJUtils;

import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.Server;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a base channel collected from the NDS channel list files
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class NdsChan 
{
    private int indexID;
    private String name;        // base name of channel
    private int nameHash;
    private String ifo;
    private String subsys;
    private Float minRaw;
    private Float maxRaw;
    private Float minRds;
    private Float maxRds;
    
    private int cTypes;
    private String cisAvail;
    private int servers;
    private final Set<Integer> chanAvail;
    
    private boolean modified;
    private boolean inited;
    private String access;
    
    private final int ctRds;
    private final int ctRaw;

    public NdsChan()
    {
        modified = inited = false;
        servers = 0;
        chanAvail = new HashSet<>();
        name = ifo = subsys = cisAvail = "";
        nameHash = 0;
        minRaw = 0f;
        maxRaw = 0f;
        minRds = 0f;
        maxRds = 0f;
        cTypes = 0;
        ctRds = NDSChanType.name2int("rds");
        ctRaw = NDSChanType.name2int("raw");
    }
    public void merge(NdsChan it) throws LdvTableException
    {
        setName(it.getName());  // this will verify names are compatible
        
        // merge sets
        if ((cTypes | it.cTypes) != cTypes)
        {
            modified = true;
            cTypes |= it.cTypes;
        }
        if ((servers | it.servers) != servers)
        {
            modified = true;
            servers |= servers;
        }
        if (minRaw > it.minRaw || minRaw == 0)
        {
            modified = true;
            minRaw = it.minRaw;
        }
        if (maxRaw < it.maxRaw)
        {
            modified = true;
            maxRaw = it.maxRaw;
        }
        if (minRds > it.minRds || minRaw == 0)
        {
            modified = true;
            minRds = it.minRds;
        }
        if (maxRds < it.maxRds)
        {
            modified = true;
            maxRds = it.maxRds;
        }
        
        if (!chanAvail.containsAll(it.chanAvail))
        {
            modified = true;
            chanAvail.addAll(it.chanAvail);
        }
               
    }
    public void init(NdsServer server, String chname, String ctype, String access, String dtype, 
                    float fs, int avail) throws LdvTableException
    {
        setName(chname);

        int ct = NDSChanType.name2int(ctype);
        

        if (ct == ctRaw)
        {
            if (fs < minRaw || minRaw == 0f)
            {
                minRaw = fs;
                modified = true;
            }
            if (fs > maxRaw)
            {
                maxRaw = fs;
                modified = true;
            }
        }
        if (ct == ctRds)
        {
            if (fs < minRds || minRds == 0f)
            {
                modified = true;
                minRds = fs;
            }
            if (fs > maxRds)
            {
                modified = true;
                maxRds =fs;
            }
        }
        if ((cTypes & ct) == 0)
        {
            cTypes |= ct;
            modified = true;
        }
        if ((servers | server.getBitMask()) != servers)
        {
            servers |= server.getBitMask();
            modified = true;
        }
        this.access = access;
        if (!this.chanAvail.contains(avail))
        {
            modified = true;
            this.chanAvail.add(avail);
        }
        inited = true;
    }
    public void add(Collection<Integer> nca)
    {
        chanAvail.addAll(nca);
    }
    
    private void setName(String chname) throws LdvTableException
    {
        String newName = chname;
        if (chname.contains("."))
        {
            String[] nameParts = chname.split("\\.");
            newName = nameParts[0].trim();
        }
        if (name.isEmpty())
        {
            name = newName;
            nameHash = name.hashCode();
            Pattern ifoSubsysPat = Pattern.compile("(^.+):((.+?)[_-])?");
            Matcher ifoSubsys = ifoSubsysPat.matcher(name);
            if (ifoSubsys.find())
            {
                ifo = ifoSubsys.group(1);
                subsys = ifoSubsys.group(3);
                subsys = subsys == null ? "" : subsys;
            }
            else
            {
                ifo = subsys = "";
            }
            modified = true;
        }
        else if ( !name.contentEquals(newName))
        {
            throw new LdvTableException("NdsChan: Attempt to merge specs with different names.");
        }
    }

    private boolean mergeAvailability(ArrayList<NDSChannelAvailability> avails)
    {
        boolean ret = false;
        for(NDSChannelAvailability a : avails)
        {
            if (!chanAvail.contains(a.getIdx()))
            {
                chanAvail.add(a.getIdx());
                ret = true;
            }
        }
        return ret;
    }

    public int getIndexID()
    {
        return indexID;
    }

    public void setIndexID(int indexID)
    {
        this.indexID = indexID;
    }

    public int getNameHash()
    {
        return nameHash;
    }

    public void setNameHash(int nameHash)
    {
        this.nameHash = nameHash;
    }

    public String getIfo()
    {
        return ifo;
    }

    public void setIfo(String ifo)
    {
        this.ifo = ifo;
    }

    public String getSubsys()
    {
        return subsys;
    }

    public void setSubsys(String subsys)
    {
        this.subsys = subsys;
    }


    public String getCisAvail()
    {
        return cisAvail;
    }

    public void setCisAvail(String cisAvail)
    {
        this.cisAvail = cisAvail;
    }

    public boolean isModified()
    {
        return modified;
    }

    public void setModified(boolean modified)
    {
        this.modified = modified;
    }

    public String getAccess()
    {
        return access;
    }

    public void setAccess(String access)
    {
        this.access = access;
    }

    public String getName()
    {
        return name;
    }


    public String getMinRawRateStr()
    {
        float it = getMinRawRate();
        String ret;
        if (it < 1)
        {
            ret = String.format("%.5f", it);
        }
        else
        {
            ret = String.format("%.0f", it);
        }
        return ret;
    }

    public float getMinRawRate()
    {
        return minRaw;
    }
    public String getMaxRawRateStr()
    {
        float it = getMaxRawRate();
        String ret;
        if (it < 1)
        {
            ret = String.format("%.5f", it);
        }
        else
        {
            ret = String.format("%.0f", it);
        }
        return ret;
    }

    public float getMaxRawRate()
    {
        return maxRaw;
    }
    public String getMinRdsRateStr()
    {
        float it = getMinRdsRate();
        String ret;
        if (it < 1)
        {
            ret = String.format("%.5f", it);
        }
        else
        {
            ret = String.format("%.0f", it);
        }
        return ret;
    }

    public float getMinRdsRate()
    {
        return minRds;
    }
    public String getMaxRdsRateStr()
    {
        float it = getMaxRdsRate();
        String ret;
        if (it < 1)
        {
            ret = String.format("%.5f", it);
        }
        else
        {
            ret = String.format("%.0f", it);
        }
        return ret;
    }

    public float getMaxRdsRate()
    {
        return maxRds;
    }

    public String getcTypeStr()
    {
        return Integer.toString(cTypes);
    }
    public int getcTypes()
    {
        return cTypes;
    }

    public void setcTypes(int cTypes)
    {
        this.cTypes = cTypes;
    }

    public boolean isInited()
    {
        return inited;
    }

    public Set<Integer> getAvail()
    {
        return chanAvail;
    }

    public Float getMinRaw()
    {
        return minRaw;
    }

    public void setMinRaw(Float minRaw)
    {
        this.minRaw = minRaw;
    }

    public Float getMaxRaw()
    {
        return maxRaw;
    }

    public void setMaxRaw(Float maxRaw)
    {
        this.maxRaw = maxRaw;
    }

    public Float getMinRds()
    {
        return minRds;
    }

    public void setMinRds(Float minRds)
    {
        this.minRds = minRds;
    }

    public Float getMaxRds()
    {
        return maxRds;
    }

    public void setMaxRds(Float maxRds)
    {
        this.maxRds = maxRds;
    }

    public int getServers()
    {
        return servers;
    }

    public void setServers(int servers)
    {
        this.servers = servers;
    }

    public void fill(ResultSet rs) throws LdvTableException
    {
        modified = false;
        try
        {
            indexID = rs.getInt("indexID");
            name = rs.getString("name");
            ifo = rs.getString("ifo");
            subsys = rs.getString("subsys");
            servers = rs.getInt("servers");
            cisAvail = rs.getString("cisAvail");
            nameHash = rs.getInt("nameHash");
            cTypes = rs.getInt("cTypeMask");
            minRaw = rs.getFloat("minRawRate");
            maxRaw = rs.getFloat("maxRawRate");
            minRds = rs.getFloat("minRdsRate");
            maxRds = rs.getFloat("maxRdsRate");
            inited = true;
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("Filling NdsChan object from db", ex);
        }
        
    }

}
