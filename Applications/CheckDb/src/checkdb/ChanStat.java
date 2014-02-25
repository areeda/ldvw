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
package checkdb;

import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.ChanPointer;
import java.util.ArrayList;
import java.util.HashSet;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanStat
{
    private static final String[]trendTypes = { "min", "mean", "max", "n", "rms"};
    
    private final HashSet<String> mtrends;
    private final HashSet<String> strends;
    private final HashSet<String> types;
    private final HashSet<Float> rawRates;
    private final HashSet<Float> rdsRates;
    private final ArrayList<ChanPointer> chanPtrs;
    private String cisAvail;
    private float maxRdsRate, minRdsRate;
    private float maxRawRate, minRawRate;
    
    private String errors = "";
    private final boolean didTrendCheck=false;
    
    ChanStat ()
    {
        mtrends = new HashSet<>(5);
        strends = new HashSet<>(5);
        types = new HashSet<>(6);
        rawRates = new HashSet<>(5);
        rdsRates = new HashSet<>(5);
        chanPtrs = new ArrayList<>();
        cisAvail = "";
        maxRdsRate = maxRawRate = Float.MIN_VALUE;
        minRdsRate = minRawRate = Float.MAX_VALUE;
    }
    
    void add(ChanInfo ci)
    {
        String cType = ci.getcType();
        ChanPointer cptr = new ChanPointer();
        cptr.setMyID(ci.getId());
        cptr.setcType(cType);
        
        String cis = ci.getCisAvail();
        if (cis.equalsIgnoreCase("d"))
        {
            cisAvail = "d";
        }
        else if (cis.equalsIgnoreCase("a"))
        {
            cisAvail = "a";
        }
        if (cType.toLowerCase().contains("trend"))
        {
            String name = ci.getChanName();
            int dotPos= name.lastIndexOf(".");
            String trndType=name.substring(dotPos+1);
            cptr.setTrendType(trndType);
            
            switch(trndType)
            {
                case "mean":
                case "min":
                case "max":
                case "n":
                case "rms":
                    HashSet<String> trndList=null;
                    switch(cType)
                    {
                        case "minute-trend":
                            trndList=mtrends;
                            break;
                        case "second-trend":
                            trndList=strends;
                            break;
                        default:
                            errors += String.format("    Unknown trend type: %1$s%n", cType);
                    }
                    if (trndList != null)
                    {
                        if (trndList.equals(trndType))
                        {
                            errors += String.format("  Duplicate %1$s entry for %2$s", cType, trndType);
                        }
                        else
                        {
                            trndList.add(trndType);
                        }
                    }
                    break;
                    
                default:
                    errors += String.format("    Unknown trend: %1$s%n",trndType);
            }
            types.add(cType);
        }
        else if (cType.equalsIgnoreCase("raw") || cType.equalsIgnoreCase("rds"))
        {
            
            String name = ci.getChanName();
            for (String trendType : trendTypes)
            {
                if (name.toLowerCase().endsWith("." + trendType))
                {
                    errors += String.format("    %1$s channel has a trend like name [%2$s] %n",
                                            cType,name);
                }
            }
            Float rate=ci.getRate();
            
            if (cType.equalsIgnoreCase("raw"))
            {
                if (rawRates.contains(rate))
                {
                    errors += String.format("    Duplicate rate of raw channel: %1$.2f%n",rate);
                }
                else
                {
                    rawRates.add(rate);
                    maxRawRate = Math.max(maxRawRate, rate);
                    minRawRate = Math.min(minRawRate, rate);
                }
            }
            else if (cType.equalsIgnoreCase("rds"))
            {
                if (rdsRates.contains(rate))
                {
                    errors += String.format("    Duplicate rate of rds channel: %1$.2f%n", rate);
                }
                else
                {
                    rdsRates.add(rate);
                    maxRdsRate = Math.max(maxRdsRate, rate);
                    minRdsRate = Math.min(minRdsRate,rate);
                }
            }
            types.add(cType);
            
        }
        else
        {
            if (types.contains(cType))
            {
                errors += String.format("    Duplicate channel type: %1$s%n",cType);
            }
            types.add(cType);
        }
        chanPtrs.add(cptr);
    }
    public String getError()
    {
        if (!didTrendCheck)
        {
            if(mtrends.size() > 0 && mtrends.size() != trendTypes.length)
            {
                addMissingTrends(mtrends);
            }
            if (strends.size() > 0 && strends.size() != trendTypes.length)
            {
                addMissingTrends(strends);
            }
        }
        return errors;
    }

    private void addMissingTrends(HashSet<String> trends)
    {
        for(String ttype : trendTypes)
        {
            if (!trends.contains(ttype))
            {
                errors += String.format("    Missing trend type: %1$s%n", ttype);
            }
        }
    }

    boolean hasMultipleRawRates()
    {
        return rawRates.size() > 1;
    }
    boolean hasMultipleRdsRates()
    {
        return rdsRates.size() > 1;
    }

    int getRawRateCount()
    {
        return rawRates.size();
    }
    int getRdsRateCount()
    {
        return rdsRates.size();
    }
    String getRdsRateList()
    {
        String ret = "";
        for (Float r : rdsRates)
        {
            if (ret.length() > 0)
            {
                ret += ", ";
            }
            if (r > 1)
            {
                ret += String.format("%1$.0f", r);
            }
            else
            {
                ret += String.format("%1$.3f", r);
            }
        }
        return ret;
    }

    String getRawRateList()
    {
        String ret = "";
        for (Float r : rawRates)
        {
            if (ret.length() > 0)
            {
                ret += ", ";
            }
            if (r > 1)
            {
                ret += String.format("%1$.0f", r);
            }
            else
            {
                ret += String.format("%1$.3f", r);
            }
        }
        return ret;
    }

    public String getCisAvail()
    {
        return cisAvail;
    }

    public void setCisAvail(String cisAvail)
    {
        this.cisAvail = cisAvail;
    }

    public float getMaxRdsRate()
    {
        return maxRdsRate;
    }

    public float getMinRdsRate()
    {
        return minRdsRate;
    }

    public float getMaxRawRate()
    {
        return maxRawRate;
    }

    public float getMinRawRate()
    {
        return minRawRate;
    }
    
    public boolean hasRaw()
    {
        return types.contains("raw");
    }
    public boolean hasOnline()
    {
        return types.contains("online");
    }
    public boolean hasRds()
    {
        return types.contains("RDS");
    }
    public boolean hasMtrends()
    {
        return types.contains("minute-trend");
    }
    public boolean hasStrend()
    {
        return types.contains("second-trend");
    }
    public boolean hasStatic()
    {
        return types.contains("static");
    }

    ArrayList<ChanPointer> getChanPointerList()
    {
        return chanPtrs;
    }

    void setIndexID(int indexID)
    {
        for(ChanPointer cptr : chanPtrs)
        {
            cptr.setIndexID(indexID);
        }
    }
}
