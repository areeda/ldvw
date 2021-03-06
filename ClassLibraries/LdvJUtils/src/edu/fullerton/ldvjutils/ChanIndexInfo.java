/*
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
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
package edu.fullerton.ldvjutils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Represents a row in the ChannelIndex table so this is closely tied
 * @see ChannelIndex
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanIndexInfo
{
    protected int indexID;
    protected String name;        // base name of channel
    private int nameHash;
    protected String ifo;
    protected String subsys;
    private float minRawRate;
    private float maxRawRate;
    private float minRdsRate;
    private float maxRdsRate;
    private boolean hasRaw;
    private boolean hasRds;
    private boolean hasOnline;
    private boolean hasMtrends;
    private boolean hasStrends;
    private boolean hasTstPnt;
    private boolean hasStatic;
    private String cisAvail;
    protected int nServers;
    protected ArrayList<Integer> chanIDs;
    
    private final static String fldnames = "( name, nameHash, ifo, subsys, "
                                        + "minRawRate, maxRawRate, minRdsRate, maxRdsRate, "
                                        + "hasRaw, hasMtrends, hasStrends, hasTstpnt, "
                                        + "hasStatic, hasOnline, hasRds, "
                                        + "cisAvail, nServers"
                                        + ")";
    private boolean inited;
    
    public ChanIndexInfo()
    {
        chanIDs = new ArrayList<>();
        inited = false;
    }

    /**
     * Copy fields usually from a derived object
     * @param cii other object
     */
    protected void init(ChanIndexInfo cii)
    {
        indexID = cii.indexID;
        name = cii.name;
        nameHash = cii.nameHash;
        ifo = cii.ifo;
        subsys = cii.subsys;
        minRawRate = cii.minRawRate;
        maxRawRate = cii.maxRawRate;
        minRdsRate = cii.minRdsRate;
        maxRdsRate = cii.maxRdsRate;
        hasMtrends = cii.hasMtrends;
        hasStrends = cii.hasStrends;
        hasOnline = cii.hasOnline;
        hasRaw = cii.hasRaw;
        hasRds = cii.hasRds;
        hasStatic = cii.hasStatic;
        hasTstPnt = cii.hasTstPnt;
        nServers = cii.nServers;
        cisAvail = cii.cisAvail;
        chanIDs = new ArrayList<>(cii.chanIDs);
        inited = cii.inited;
    }
    /**
     * Short summary of object with name and types
     * @return
     */
    @Override
    public String toString()
    {
        String ret = String.format("%1$,d. %2$s ",indexID, name);
        String types = "(";
        if (hasRaw) types += "Raw, ";
        if (hasRds) types += "Rds, ";
        if (hasMtrends) types += "M-trend, ";
        if (hasStrends) types += "S-trend, ";
        types += String.format("srv: %1$d.)",nServers);
        return ret + types;
    }
    public String getSqlFieldNames()
    {
        return fldnames;
    }
    
    public String getSqlFieldValues()
    {
        String ret = String.format("( '%1$s', %2$d, '%3$s', '%4$s', "
                + "%5$.5f, %6$.5f, %7$.5f, %8$.5f, "
                + "'%9$1s', '%10$1s', '%11$1s', '%12$1s', "
                + "'%13$1s', '%14$1s', '%15$1s', "
                + "'%16$s', '%17$d' )", 
                name, nameHash, ifo, subsys, 
                minRawRate, maxRawRate, minRdsRate, maxRdsRate,
                hasRaw?"T":"F", hasMtrends?"T":"F", hasStrends?"T":"F", hasTstPnt?"T":"F", 
                hasStatic?"T":"F", hasOnline?"T":"F", hasRds?"T":"F",
                cisAvail, nServers
                );
        return ret;
    }
   
    /**
     * Initialize the object from a database row.
     * Note if the ResultSet contains more than one row only the current row is used
     * 
     * @param rs a row in the ChannelIndex table
     * @throws java.sql.SQLException
     */
    public void fill(ResultSet rs) throws SQLException
    {
        indexID = rs.getInt("indexID");
        name = rs.getString("name");
        nameHash = rs.getInt("nameHash");
        ifo = rs.getString("ifo");
        subsys = rs.getString("subsys");
        minRawRate = rs.getFloat("minRawRate");
        maxRawRate = rs.getFloat("maxRawRate");
        minRdsRate = rs.getFloat("minRdsRate");
        maxRdsRate = rs.getFloat("maxRdsRate");
        hasRaw = rs.getString("hasRaw").equalsIgnoreCase("t");
        hasRds = rs.getString("hasRds").equalsIgnoreCase("t");
        hasOnline = rs.getString("hasOnline").equalsIgnoreCase("t");
        hasMtrends = rs.getString("hasMtrends").equalsIgnoreCase("t");
        hasStrends = rs.getString("hasStrends").equalsIgnoreCase("t");
        hasStatic = rs.getString("hasStatic").equalsIgnoreCase("t");
        hasTstPnt = rs.getString("hasTstPnt").equalsIgnoreCase("t");
        cisAvail = rs.getString("cisAvail");
        nServers = rs.getInt("nServers");
        inited = true;
    }
    public void fill (ChanIndexInfo cii)
    {
        indexID = cii.indexID;
        name = cii.name;
        nameHash = cii.nameHash;
        ifo = cii.ifo;
        subsys=cii.subsys;
        minRawRate = cii.minRawRate;
        maxRawRate = cii.maxRawRate;
        minRdsRate = cii.minRdsRate;
        maxRdsRate = cii.maxRdsRate;
        hasRaw = cii.hasRaw;
        hasRds = cii.hasRds;
        hasOnline = cii.hasOnline;
        hasMtrends = cii.hasMtrends;
        hasStrends = cii.hasStrends;
        hasStatic = cii.hasStatic;
        hasTstPnt = cii.hasTstPnt;
        cisAvail = cii.cisAvail;
        nServers = cii.nServers;
        inited = true;
    }
    public ArrayList<String> getTypeList()
    {
        ArrayList<String> ret = new ArrayList<>();
        if (hasRaw) ret.add("Raw");
        if (hasRds) ret.add("RDS");
        if (hasOnline) ret.add("Online");
        if (hasMtrends) ret.add("Minute-trend");
        if (hasStrends) ret.add("Second-trend");
        if (hasStatic) ret.add("Static");
        if (hasTstPnt) ret.add("TestPoint");
        ret.trimToSize();
        return ret;
    }
    //-------Getters/Setters-----------
    public int getIndexID()
    {
        return indexID;
    }

    public boolean isInited()
    {
        return inited;
    }

    public void setIndexID(int indexID)
    {
        this.indexID = indexID;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
        nameHash = name.hashCode();
    }

    public int getNameHash()
    {
        return nameHash;
    }

    public String getIfo()
    {
        if (ifo == null)
        {
            ifo = "";
        }
        return ifo;
    }

    public void setIfo(String ifo)
    {
        this.ifo = ifo;
    }

    public void setSubsys(String subsys)
    {
        this.subsys = subsys;
    }

    public String getSubsys()
    {
        return subsys;
    }

    
    public boolean hasRaw()
    {
        return hasRaw;
    }

    public void setHasRaw(boolean hasRaw)
    {
        this.hasRaw = hasRaw;
    }

    public boolean hasRds()
    {
        return hasRds;
    }

    public void setHasRds(boolean hasRds)
    {
        this.hasRds = hasRds;
    }

    public boolean hasMtrends()
    {
        return hasMtrends;
    }

    public void setHasMtrends(boolean hasMtrends)
    {
        this.hasMtrends = hasMtrends;
    }

    public boolean hasStrends()
    {
        return hasStrends;
    }

    public void setHasStrends(boolean hasStrends)
    {
        this.hasStrends = hasStrends;
    }

    public boolean hasTestpoint()
    {
        return hasTstPnt;
    }

    public void setHasTestpoint(boolean hasTestpoint)
    {
        this.hasTstPnt = hasTestpoint;
    }

    public boolean hasStatic()
    {
        return hasStatic;
    }

    public void setHasStatic(boolean hasStatic)
    {
        this.hasStatic = hasStatic;
    }

    public int getnServers()
    {
        return nServers;
    }

    public void setnServers(int nServers)
    {
        this.nServers = nServers;
    }

    public ArrayList<Integer> getChanIDs()
    {
        return chanIDs;
    }

    public void setChanIDs(ArrayList<Integer> chanIDs)
    {
        this.chanIDs = chanIDs;
    }

    public String getCisAvail()
    {
        if (cisAvail == null)
        {
            cisAvail = "";
        }
        return cisAvail;
    }

    public void setCisAvail(String cisAvail)
    {
        this.cisAvail = cisAvail;
    }

    public float getMinRawRate()
    {
        return minRawRate;
    }

    public void setMinRawRate(float minRawRate)
    {
        this.minRawRate = minRawRate;
    }

    public float getMaxRawRate()
    {
        return maxRawRate;
    }

    public void setMaxRawRate(float maxRawRate)
    {
        this.maxRawRate = maxRawRate;
    }

    public float getMinRdsRate()
    {
        return minRdsRate;
    }

    public void setMinRdsRate(float minRdsRate)
    {
        this.minRdsRate = minRdsRate;
    }

    public float getMaxRdsRate()
    {
        return maxRdsRate;
    }

    public void setMaxRdsRate(float maxRdsRate)
    {
        this.maxRdsRate = maxRdsRate;
    }

    public boolean hasOnline()
    {
        return hasOnline;
    }

    public void setHasOnline(boolean hasOnline)
    {
        this.hasOnline = hasOnline;
    }

    public boolean hasTstPnt()
    {
        return hasTstPnt;
    }

    public void setHasTstPnt(boolean hasTstpnt)
    {
        this.hasTstPnt = hasTstpnt;
    }

    public String getCSV()
    {
        StringBuilder ret = new StringBuilder();
        ret.append(name).append(", ");
        ret.append(ifo).append(", ");
        ret.append(subsys).append(", ");
        ret.append(getRateStr(minRawRate)).append(", ");
        ret.append(getRateStr(maxRawRate)).append(", ");
        ret.append(getRateStr(minRdsRate)).append(", ");
        ret.append(getRateStr(maxRdsRate)).append(", ");
        ret.append(getBoolStr(hasRaw)).append(", ");
        ret.append(getBoolStr(hasRds)).append(", ");
        ret.append(getBoolStr(hasOnline)).append(", ");
        ret.append(getBoolStr(hasMtrends)).append(", ");
        ret.append(getBoolStr(hasStrends)).append(", ");
        ret.append(getBoolStr(hasTstPnt)).append(", ");
        ret.append(getBoolStr(hasStatic)).append(", ");
        String cisAvailability = "no";
        if (cisAvail.equalsIgnoreCase("a"))
        {
            cisAvailability = "auto";
        }
        else if (cisAvail.equalsIgnoreCase("d"))
        {
            cisAvailability = "desc";
        }
        ret.append(cisAvailability).append(", ");
        ret.append(Integer.toString(nServers));
        
        return ret.toString();
    }
    private String getRateStr(float v)
    {
        String ret;
        if (v >= 1)
        {
            ret = String.format("%.0f",v);
        }
        else
        {
            ret = String.format("%.4f",v);
        }
        return ret;
    }
    private String getBoolStr(boolean b)
    {
        return b ? "T" : "F";
    }
    public static String getCSVHeader()
    {
        String hdr = "name, ifo, subsys, minRawRate, maxRawRate, minRdsRate, maxRdsRate,"
                + "hasRaw, hasRds, hasOnline, hasMtrends, hasStrends, hasTstPnt, hasStatic,"
                + "cisAvail, nServers";
        return hdr;
    }  
}
