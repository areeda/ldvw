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

package edu.fullerton.ldvjutils;

import java.util.ArrayList;
import java.util.List;

/**
 * When a base channel is selected we need additional info to actually use it.
 * This class extends the base channel to hold what is selected and how to get it.
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class BaseChanSelection extends ChanIndexInfo
{
    private int baseChanIdx;
    protected int singleIdx;
    private int minuteTrends;
    private int secondTrends;
    private boolean raw;
    private boolean online;
    private boolean staticType;
    private boolean testpoint;
    private boolean rds;
    // note the index of trendNames is significant during execution
    protected final String[] trendNames = { "min", "mean", "max", "rms", "n"};
    private final String[] trendSelChoices = // NB: item 0 is used by clear all, item 1 by select all
                            { "none", "min, mean, max", "mean", "min", "max", "min, max", "rms", "n" };

    
    public BaseChanSelection()
    {
        baseChanIdx = minuteTrends = secondTrends = singleIdx = 0;
        raw = online = staticType = testpoint = rds = false;
    }
    
    public void init(BaseChanSelection bcs)
    {
        baseChanIdx = bcs.baseChanIdx;
        singleIdx = bcs.singleIdx;
        minuteTrends = bcs.minuteTrends;
        secondTrends = bcs.secondTrends;
        raw = bcs.raw;
        online = bcs.online;
        staticType = bcs.staticType;
        testpoint = bcs.testpoint;
        rds = bcs.rds;
        super.init(bcs);
    }
    @Override
    public void init(ChanIndexInfo cii)
    {
        super.init(cii);
    }
    
    public void setBaseChanIdx(int chanIdx)
    {
        this.baseChanIdx = chanIdx;
        indexID = chanIdx;
    }

    public void setSingleIdx(int singleIdx)
    {
        this.singleIdx = singleIdx;
    }

    /**
     * Get array of trend choices for a drop down menu
     * @return subset of all combinations
     */
    public String[] getTrendSelChoices()
    {
        return trendSelChoices;
    }
    /**
     * Based on what has been selected get an entry in the drop down menu of available types
     * @param trendType - a string that contains minute or second
     * @return one of the entries returned by getTrendSelChoices
     * @throws LdvTableException if an unknown trend type is specified or the selection value we have does not correspond to a menu choice
     */
    public String getTrendSelectString(String trendType) throws LdvTableException
    {
        int val;
        if (trendType.toLowerCase().contains("minute"))
        {
            val = minuteTrends;
        }
        else if (trendType.toLowerCase().contains("second"))
        {
            val = secondTrends;
        }
        else
        {
            throw new LdvTableException("getTrendSelectString: Unknown trend type - " + trendType);
        }
        String ret;
        switch(val)
        {
            case 0:
                ret = "none";
                break;
            case 1:
                ret ="min";
                break;
            case 2:
                ret = "mean";
                break;
            case 4:
                ret = "max"; 
                break;
            case 8:
                ret = "rms";
                break;
            case 16:
                ret = "n";
                break;
            case 7:
                ret = "min, mean, max";
                break;
            case 5:
                ret = "min, max";
                break;
            default:
                String ermsg = String.format("getTrendSelectString: Selections do not correspond "
                        + "to a menu item %1$d", val);
                throw new LdvTableException(ermsg);
        }
        return ret;
    }
    public boolean isRaw()
    {
        return raw;
    }

    public void setRaw(boolean raw)
    {
        this.raw = raw;
    }

    public boolean isOnline()
    {
        return online;
    }

    public void setOnline(boolean online)
    {
        this.online = online;
    }

    public boolean isStaticType()
    {
        return staticType;
    }

    public void setStaticType(boolean staticType)
    {
        this.staticType = staticType;
    }

    public boolean isTestpoint()
    {
        return testpoint;
    }

    public void setTestpoint(boolean testpoint)
    {
        this.testpoint = testpoint;
    }

    public boolean isRds()
    {
        return rds;
    }

    public void setRds(boolean rds)
    {
        this.rds = rds;
    }

    public void setSingleSel(String ctype) throws LdvTableException
    {
        switch(ctype.toLowerCase())
        {
            case "raw":
                raw = true;
                break;
            case "online":
                raw = true;
                break;
            case "static":
                staticType = true;
                break;
            case "testpoint":
                testpoint = true;
                break;
            case "rds":
                rds = true;
                break;
            default:
                throw new LdvTableException("Base channel selection, unknown channel type " + ctype);
        }
    }
    /**
     * to provide backwards compatibility with single channel selections add the current channel
     * to the selections of this base channel.  
     * Note base channel must be initialized first.
     * 
     * @param ci the information object on the one channel.
     */
    public void addSingleChan(ChanInfo ci) throws LdvTableException
    {
        String baseName = ci.getBaseName();
        if (! baseName.contentEquals(name))
        {
            String ermsg = String.format("Attempt to select channel (%1$s) from wrong base channel (%2$s)", 
                                         ci.getChanName(), name);
            throw new LdvTableException(ermsg);
        }
        String ctype = ci.getcType();
        String cname = ci.getChanName();
        if (ctype.toLowerCase().contains("trend"))
        {
            String trendType = "";
            for(String ttype : trendNames)
            {
                if (cname.endsWith(ttype))
                {
                    trendType = ttype;
                    setTrend(ctype, ttype);
                    break;
                }
                if (trendType.isEmpty())
                {
                    String ermsg = String.format("BaseChanSelection: Unknown trend channel: %1$s", cname);
                    throw new LdvTableException(ermsg);
                }
            }
        }
        else
        {
            setSingleSel(ctype);
        }
    }
    public void setTrend(String trendType, String trendVal) throws LdvTableException
    {
        int trends = 0;
        String tval = trendVal.toLowerCase();
        String[] desiredTrends = tval.split(",");
        for(int i=0; i < trendNames.length; i++)
        {
            for(String t : desiredTrends)
            {
                if (t.toLowerCase().trim().equalsIgnoreCase(trendNames[i]))
                {
                    trends |= (1 << i);
                }
            }
        }
        switch(trendType)
        {
            case "minute":
                minuteTrends |= trends;
                break;
            case "second":
                secondTrends |= trends;
                break;
            default:
                throw new LdvTableException("Base channel selection: unknown trend type: " + trendType);
        }
    }
    public boolean hasSingle()
    {
        return raw | rds;
    }
    public boolean hasTrends()
    {
        return secondTrends != 0 | minuteTrends != 0;
    }

    public boolean isSelected(String type) throws LdvTableException
    {
        boolean ret;
        String lctype = type.trim().toLowerCase();
        switch(lctype)
        {
            case "raw":
                ret = raw;
                break;
            case "rds":
                ret = rds;
                break;
            case "online":
                ret = online;
                break;
            case "statictype":
                ret = staticType;
                break;
            case "testpoint":
                ret = testpoint;
                break;
            default:
                throw new LdvTableException("BaseChannel - isSelected called with unknown type: " + type);
        }
        return ret;
    }

    public int getNsel()
    {
        int ret = 0;
        ret += raw ? 1 : 0;
        ret += rds ? 1 : 0;
        ret += online ? 1 : 0;
        ret += staticType ? 1 : 0;
        ret += testpoint ? 1 : 0;
        ret += countTrends(minuteTrends);
        ret += countTrends(secondTrends);
        ret += singleIdx > 0 ? 1 : 0;
        return ret;
    }
    public int countTrends(int trnd)
    {
        int ret = 0;
        for (int i = 0; i < trendNames.length; i++)
        {
            ret += (trnd & (1 << i)) != 0 ? 1 : 0;
        }
        return ret;
    }
    /**
     * get a list of specific selections for this base channel
     * 
     * @return  2d array [0] = type, [1] specific trend (min, max ...) if type is min or sec trend,
     *          may bem an empty list but will not be null
     */
    public List<String[]> getSelections()
    {
        ArrayList<String[]> ret = new ArrayList<>();
        if (raw)
        {
            String[] it = {"raw", ""};
            ret.add(it);
        }
        if (online)
        {
            String[] it =
            {
                "online", ""
            };
            ret.add(it);
        }
        if (staticType)
        {
            String[] it =
            {
                "static", ""
            };
            ret.add(it);
        }
        if (testpoint)
        {
            String[] it =
            {
                "testpoint", ""
            };
            ret.add(it);

        }
        if (rds)
        {
            String[] it =
            {
                "RDS", ""
            };
            ret.add(it);
        }
        chkTrend(ret,"minute-trend", minuteTrends);
        chkTrend(ret, "second-trend", secondTrends);
        
        return ret;
    }
    /**
     * Add any selected trends to the list
     * @param ret - the list
     * @param typ - name of the trends
     * @param trendSelect - bit mask of selected trends
     */
    private void chkTrend(ArrayList<String[]> ret, String typ, int trendSelect)
    {
        if ( trendSelect != 0)
        {
            for (int b = 0; b < trendNames.length; b++)
            {
                int mask = 1 << b;
                if ((trendSelect & mask) != 0)
                {
                    String[] it =
                    {
                        typ, trendNames[b]
                    };
                    ret.add(it);
                }
            }
        }
    }
    /**
     * Return a list of specific channel info objects one for each type selected 
     * these will represent one time series
     *
     * @param bcs - base channel with selected types flagged
     * @return List of single selections for each type selected
     */
    public List<BaseChanSingle> getSingleChanList() throws LdvTableException
    {
        List<BaseChanSingle> ret = new ArrayList<>();
        List<String[]> selectedTypes = getSelections();
        
        for (String[] type : selectedTypes)
        {
            BaseChanSingle single = new BaseChanSingle(this, type);
            
            ret.add(single);
        }

        return ret;
    }


}
