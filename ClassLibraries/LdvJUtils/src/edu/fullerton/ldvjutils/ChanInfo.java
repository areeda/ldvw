/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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
import java.sql.Timestamp;

/**
 * An class containing the information retrieved on a single channel from NDS2 client or from the database.
 * 
 * <p>This is a multipurpose object in the sense it is designed to be used with only the NDS2 client or
 * in combination with a database (currently mySql only but sqlite is planned) as a cache.</p>
 * 
 * 
 * <p>All fields are private so please use Getter/Setter methods for access</p>
 * 
 * 
 * <p>The NDS2 server provides:</p>
 * 
 * 
 * <code>Channel name</code>: a unique string identifying the data source.  At this time there is an on-going project
 *      to develop a Channel Information system see <a href="https://wiki.ligo.org/DetChar/ChannelInfoSystemDesign"> CIS Wiki </a> to
 *      help us figure out what the names mean.<br/>
 * 
 * 
 * <code>Sample Frequency</code>: in Hertz values range from 0.016 for minute trends to 32,768 samples/sec<br/>
 * <code>Test-point</code>:  a flag indicating channel data is diagnostic in nature<br/>
 * <code>Channel Type</code>: the type of data stored at the server, current valid list of types is:<br/>
 *      unknown, raw, online, rds, second-trend, minute-trend, test-point or static
 *      where unknown specifies any of the others in a channel list request.<br/>
 * <code>Bytes per sample</code>: In the data format transferred from the server<br/>
 * <code>Data type</code>:  current list of valid type are: INT-16, INT-32, INT-64, FLT-32, FLT-64, or CPX-64<br/>
 * <code>Calibration Factors</code>: Convert raw counts to meaningful units.<br/>
 *     They consist of Slope, Gain, Offset and units.  We are trying to find the formula for conversion
 *     (note we've only seen identity transforms and null units so far)<b<br/>r/>
 * 
 * <p>This Class is also able to work with a database record defined as part of the LigoDV-lite project
 * but it is not a requirement.  If you are using it with the NDSProxyClient these fields will all be empty</p>
 * 
 * <code>id</code>:  The autoincrement primary key.  It is efficient but should persist longer than a session since
 *      a rebuild of the database is not guaranteed to keep the value intact.<br/>
 * <code>available</code>:   Set to false when the channel list from a server is missing a channel that was there once.<br/>
 * <code>updating</code>:    Used to determine if any channels were deleted.<br/>
 * <code>firstSeen</code>:   First time we pulled a channel list from this server and saw this channel<br/>
 * <code>lastMod</code>:     Updated if an existing channel has a change in sample frequency, data format, or calibration factors<br/>
 * 
 * Notes: <br/>
 *  - A channel source is uniquely identified by server, channel name and channel type.<br/>
 *  - A channel/channel type may be available from multiple servers and should return the same data for a specified time<br/>
 * 
 * @author joe areeda
 */
public class ChanInfo implements Comparable
{
    // L1:SUS-SOS_TIMING_ERROR,16.00,0,raw,4,FLT-32,1.000,1.000,0.000,

    private Integer id;             // database only: primary integer key for temporary joining or reference oly
    private String chanName;        // LIGO name of the channel
    private String server;          // Server from which we obtained this record some channel/types are available
                                    // on multiple servers
    private boolean available;      // Database flag so we keep track of deleted channels
    private boolean updating;       // Database flag so we can know which records in the database are no longer available
    private Float rate;             // Sample frequency in Hz
    private Integer tstPnt;
    private String cType;
    private Integer bytesPerSample;
    private String dType;
    private Float gain;
    private Float offset;
    private Float slope;
    private String units;
    private Timestamp firstSeen;
    private Timestamp lastMod;
    private String cisAvail;
    private Integer availId;

    // Field names for bulk insert statement
    private static final String fldNames = "(name, server, sampleRate, tstPnt, cType, bytesPerSam, dType, gain, offset, slope, "
            + "units, available, updating, firstSeen, lastMod)";
    
    /**
     * Construct a ChanInfo object with all values cleared
     */
    public ChanInfo()
    {
        id = 0;
        chanName = "";
        rate = 0.f;
        tstPnt = 0;
        cType = "";
        bytesPerSample = 0;
        dType = "";
        gain = 0.f;
        offset = 0.f;
        slope = 0.f;
        units = "";
        firstSeen = null;
        lastMod = null;
        cisAvail = "";
        availId = 0;
    }

    /**
     * Send the current values to stdout as CSV
     * see the Apache opencsv package
     */
    public void print()
    {
        String line = getCSV();
       
        System.out.println(line);
    }
    /**
     * Format the NDS2 part of the object as a comma separated value string
     * @return single line of a CSV file
     */
    public String getCSV()
    {
        String line = "";
        line += String.format("%1$s,", chanName);
        line += String.format("%1$.5f,", rate);
        line += String.format("%1$d,", tstPnt);
        line += String.format("%1$s,", cType);
        line += String.format("%1$d,", bytesPerSample);
        line += String.format("%1$s,", dType);
        line += String.format("%1$.4f,", gain);
        line += String.format("%1$.4f,", offset);
        line += String.format("%1$.4f,", slope);
        line += String.format("%1$s", units);
        return line;
    }

    
    /**
     * Bytes per sample describes the raw data returned from the server
     * Currently the only values in the database are 2, 4 and 8
     * 
     * @return number of bytes in a sample transferred from the server
     */
    public int getBytesPerSample()
    {
        return bytesPerSample;
    }

    /**
     * Setting individual values is of use only when channel data comes from a different source.
     * 
     * 
     * @param bytesPerSample 
     */
    public void setBytesPerSample(int bytesPerSample)
    {
        this.bytesPerSample = bytesPerSample;
    }

    /**
     * Get the Channel type eg raw, online, minute-trend
     * see NDS package for the authoritative list of channel types
     * 
     * @return name of the type
     */
    public String getcType()
    {
        return cType;
    }

    /**
     * Set the Channel type.  Note we do not check if it's valid
     * Setting individual values is of use only when channel data comes from a different source.
     * see NDSClient.requestData for a list of known channel types
     * @param cType value to set
     */
    public void setcType(String cType)
    {
        this.cType = cType;
    }

    /**
     * Return the channel name from this record.  
     * @return unique channel name
     */
    public String getChanName()
    {
        return chanName;
    }

    /**
     * Setting individual values is of use only when channel data comes from a different source.
     * @param chanName 
     */
    public void setChanName(String chanName)
    {
        this.chanName = chanName;
    }

    /**
     * Get the raw data type of transfer from server
     * Possible values are INT-16, INT-32, INT-64, FLT-32, FLT-64, or CPX-64
     * 
     * @return data type code
     */
    public String getdType()
    {
        return dType;
    }

    /**
     * Set the data type
     * Setting individual values is of use only when channel data comes from a different source.
     * If this is set to an invalid or improper value nothing good will happen
     * @param dType the data type
     */
    public void setdType(String dType)
    {
        this.dType = dType;
    }

    /**
     * Gain, Slope, and Offset are calibration values
     * @return gain value as a float
     */
    public Float getGain()
    {
        return gain;
    }

    /**
     * Set gain for the calibration
     * @param gain 
     */
    public void setGain(Float gain)
    {
        this.gain = gain;
    }

    /**
     * Gain, Slope and Offset are calibration values
     * @return offset value as a float
     */
    public Float getOffset()
    {
        return offset;
    }

    /**
     * Gain, Slope and Offset are calibration values
     * @param offset 
     */
    public void setOffset(Float offset)
    {
        this.offset = offset;
    }

    /**
     * 
     * @return Sample frequency in Hz
     */
    public Float getRate()
    {
        return rate;
    }

    public void setRate(Float rate)
    {
        if (rate > 0.016 && rate <= 0.0171)
        {
            rate = 1/60.f;
        }
        this.rate = rate;
    }

    /**
     * Gain, Slope and Offset are calibration values
     * @return slope value as a float
     */
    public Float getSlope()
    {
        return slope;
    }

    public void setSlope(Float slope)
    {
        this.slope = slope;
    }

    /**
     * If data has (non identity) calibration factors these are the name of the units counts are
     * converted into.
     * 
     * @return name of the calibrated unit
     */
    public String getUnits()
    {
        return units;
    }

    /**
     * If you set the calibration factors you should set the name of the new units
     * 
     * @param units 
     */
    public void setUnits(String units)
    {
        this.units = units;
    }

    /**
     * Available is a database flag that signifies we saw this channel last time we pulled a full 
     * channel list.  A false value indicates this channel existed at one time but is not currently 
     * available on that server
     * 
     * @param available 
     */
    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    /**
     * length of a raw data sample from the server.  Note that access functions for data do not necessarily
     * return data in the same format.
     * 
     * @param bytesPerSample 
     */
    public void setBytesPerSample(Integer bytesPerSample)
    {
        this.bytesPerSample = bytesPerSample;
    }

    /**
     * A database field that indicates when we first pulled a channel list from the particular server
     * with this channel.
     * 
     * @param firstSeen 
     */
    public void setFirstSeen(Timestamp firstSeen)
    {
        this.firstSeen = (Timestamp) firstSeen.clone();
    }

    /**
     * A database field that should be considered temporary.  It is the autoincrement primary key and
     * will only potentially change if the table is rebuilt.  It is efficient for a session but should not
     * be saved because it is almost impossible to get these things exact when the db crashes.
     * 
     * @param id 
     */
    public void setId(Integer id)
    {
        this.id = id;
    }

    /**
     * A database field that is updated when a channel that we have seen before has had a change in
     * calibration factors, sample frequency, data format.
     * @param lastMod 
     */
    public void setLastMod(Timestamp lastMod)
    {
        this.lastMod = (Timestamp) lastMod.clone();
    }

    /**
     * The server this channel information came from.  Channels can be available on multiple servers.
     * 
     * @param server 
     */
    public void setServer(String server)
    {
        this.server = server;
    }

    public void setTstPnt(Integer tstPnt)
    {
        this.tstPnt = tstPnt;
    }


    public boolean isAvailable()
    {
        return available;
    }


    public Timestamp getFirstSeen()
    {
        return (Timestamp) firstSeen.clone();
    }

    public Integer getId()
    {
        return id;
    }

    public Timestamp getLastMod()
    {
        return (Timestamp) lastMod.clone();
    }

    public String getServer()
    {
        return server;
    }

    public Integer getTstPnt()
    {
        return tstPnt;
    }

    public boolean isUpdating()
    {
        return updating;
    }
    /**
     * Given the ResultSet from a database query fill in the object
     * @param rs datbase query result set
     * @throws SQLException suggests getting a problem column
     */
    public void fill(ResultSet rs) throws SQLException
    {
        id = rs.getInt("myId");
        chanName = rs.getString("name");
        server=rs.getString("server");
        available=rs.getBoolean("available");
        updating=rs.getBoolean("updating");
        setRate(rs.getFloat("sampleRate"));
        tstPnt = rs.getInt("tstPnt");
        cType = rs.getString("cType");
        bytesPerSample = rs.getInt("bytesPerSam");
        dType = rs.getString("dType");
        gain = rs.getFloat("gain");
        offset = rs.getFloat("offset");
        slope = rs.getFloat("slope");
        units = rs.getString("units");
        firstSeen = rs.getTimestamp("firstSeen");
        lastMod = rs.getTimestamp("lastMod");
        cisAvail = rs.getString("cisAvail");
        cisAvail = cisAvail == null ? " " : cisAvail;
        availId = rs.getInt("availId");
        availId = availId == null ? 0 : availId;
    }
    /**
     * Given a CSV line from the proxy server fill in object fields
     * 
     * @param nextLine - line formatted like the server would
     * @throws LdvTableException parsing problem like invalid number format
     */
    public void fill(String[] nextLine) throws LdvTableException
    {
        if (nextLine.length > 8)
        {
            id = 0;
            chanName = nextLine[0];
            chanName = removeNonPrinatbles(chanName);
            setRate(cvtFloat(nextLine[1]));
            tstPnt = cvtInt(nextLine[2]);
            cType = nextLine[3];
            bytesPerSample = cvtInt(nextLine[4]);
            dType = nextLine[5];
            gain = cvtFloat(nextLine[6]);
            offset = cvtFloat(nextLine[7]);
            slope = cvtFloat(nextLine[8]);
            if (nextLine.length > 9)
            {
                units = nextLine[9];
            }
            else
            {
                units = "";
            }
            firstSeen = null;
            lastMod = null;
            if (cType.equalsIgnoreCase("minute-trend"))
            {
                // value that comes to us does not have necessary accuracy
                rate = 1.f/60;
            }
        }
        else
        {
            throw new LdvTableException("invalid attempt to create a ChanInfo object from array.");
        }
    }
    /**
     * Fill the object with a line from NDS2 proxy server or our own CSV output
     * @param line 
     */
    public void fillCSV(String line) throws LdvTableException
    {
        String[] flds = line.trim().split("\\s*,\\s*");
        fill(flds);
    }

    /**
     * Is the database record referring to the same data.  Some of the stuff in the db is there
     * to keep track of when changes occur
     * 
     * @param ci the other object
     * @return true if they refer to the same data
     */
    public boolean isSame(ChanInfo ci)
    {
        boolean same = true;
        same = same && (id.equals(ci.id) || id.equals(0) || ci.id.equals(0));
        same = same && chanName.equals(ci.chanName);
        same = same && server.equals(ci.server);
        same = same && almostEquals(rate, ci.rate);
        same = same && tstPnt.equals(ci.tstPnt);
        same = same && cType.equals(ci.cType);
        same = same && bytesPerSample.equals(ci.bytesPerSample);
        same = same && dType.equals(ci.dType);
        same = same && almostEquals(gain,ci.gain);
        same = same && almostEquals(offset, ci.offset);
        same = same && almostEquals(slope, ci.slope);
        same = same && units.equals(ci.units);
        
        return same;
    }
    @Override
    public boolean equals (Object o)
    {
        
        boolean same = true;
        if (o  instanceof ChanInfo)
        {
            ChanInfo ci = (ChanInfo) o;
            same = same && chanName.equals(ci.chanName);
            same = same && server.equals(ci.server);
            same = same && cType.equals(ci.cType);
            same = same && rate.equals(ci.rate);
            same = same && dType.equals(ci.dType);
        }
        else
        {
            same = false;
        }
        return same;
    }
    @Override
    public int hashCode()
    {
        int ret=0;
        ret += ret *17 + chanName.hashCode();
        ret += ret * 31 + server.hashCode();
        ret += ret * 13 + cType.hashCode();
        
        return ret;
    }
    public String addSQLFields(ChanInfo ci)
    {
        String line = "";
        line += String.format("name='%1$s', ", chanName);
        line += String.format("server='%1$s', ", server);
        line += String.format("sampleRate='%1$.5f', ", rate);
        line += String.format("tstPnt=%1$d, ", tstPnt);
        line += String.format("cType='%1$s', ", cType);
        line += String.format("bytesPerSam=%1$d, ", bytesPerSample);
        line += String.format("dType='%1$s', ", dType);
        line += String.format("gain=%1$.4f, ", gain);
        line += String.format("offset=%1$.4f, ", offset);
        line += String.format("slope=%1$.4f, ", slope);
        line += String.format("units='%1$s', ", units); 
        
        line += String.format("available=%1$s, ", available ? "TRUE" : "FALSE");
        line += String.format("updating=%1$s, ", updating ? "TRUE" : "FALSE");
        line += String.format("firstSeen='%1$s', ", firstSeen.toString());
        line += String.format("lastMod='%1$s', ", lastMod.toString());
        line += String.format("cisAvail='%1$s', ", cisAvail);
        line += String.format("availId='%1$d', ", availId);
        return line;
    }
    public static String getSqlFieldNames()
    {
        return fldNames;
    }
    public String getSqlFieldValues()
    {
        String availStr = available ? "TRUE" : "FALSE";
        String updtStr = updating ? "TRUE" : "FALSE";
        String ret = String.format("('%1$s', '%2$s', %3$.3f, %4$d, '%5$s', %6$d, '%7$s', %8$.4f, %9$.4f, %10$.4f,"
                + " '%11$s', %12$s, %13$s, now(),now())", 
                chanName, server, rate, tstPnt, cType, bytesPerSample, dType, gain, offset, slope, 
                units, availStr, updtStr
                );
        return ret;
    }

    /**
     * get generic info on this channel
     * @return name, chan type, data type, sample rate and server
     */
    @Override
    public String toString()
    {
        String ret = String.format("%1$s,%2$s %3$s at ", chanName,cType,dType);
        if (rate > 0.999)
        {
            ret += String.format("%1$.0f Hz", rate);
        }
        else
        {
            ret += String.format("%1$.3f Hz", rate);
        }
        ret += " from " + server;
        return ret;
    }
    private boolean almostEquals(Float f1, Float f2)
    {
        double epsilon = Math.pow(2., -16);
        double delta = f1 -f2;
        boolean ret = Math.abs(delta) < epsilon;
        return ret;
    }

    /**
     * Compare two Channel info objects based on:
     * name, server, chan type, rate, and data type (in that order)
     * @param o the other
     * @return -1 if this < other 0 if equal 1 if this > other
     */
    @Override
    public int compareTo(Object o)
    {
        int ret;
        ChanInfo ci;
        if (o instanceof ChanInfo)
        {
            int t;
            ci = (ChanInfo) o;
            ret = this.chanName.compareTo(ci.chanName);
            if (ret == 0 && server != null && ci.server != null)
            {
                ret = server.compareTo(ci.server);
            }
            else if (ret == 0 && cType != null && ci.cType != null)
            {
                ret = cType.compareTo(ci.cType);
            }
            else if (ret == 0)
            {
                ret = rate.compareTo(ci.rate);
            }
            else if (ret == 0 && dType != null && ci.dType != null)
            {
                ret = dType.compareTo(ci.dType);
            }
        }
        else
        {
            ret = -1;
        }
        if (ret < 0)
        {
            ret = -1;
        }
        if (ret > 0)
        {
            ret = 1;
        }
        return ret;
    }

    public void setUpdating(boolean updating)
    {
        this.updating = updating;
    }

    
    //========================= PRIVATE METHODS =======================
    private Float cvtFloat(String string) throws LdvTableException
    {
        Float ret;
        try
        {
            ret = Float.parseFloat(string);
        }
        catch (Exception ex)
        {
            String ourMessage = String.format("%s - [%s]",ex.getLocalizedMessage(),string);
            throw new LdvTableException(ourMessage);
        }
        return ret;
    }

    private Integer cvtInt(String string) throws LdvTableException
    {
        Integer ret;
        try
        {
            ret = Integer.parseInt(string);
        }
        catch (Exception ex)
        {
            throw new LdvTableException(ex);
        }
        return ret;
    }

    public String removeNonPrinatbles(String chanName)
    {
        char[] chars = chanName.toCharArray();
        boolean hasBad = false;
        for(char c : chars)
        {
            hasBad |= c < 32 | c >= 127;
        }
        String ret;
        if (hasBad)
        {
            ret="";
            for(char c :chars)
            {
                if (c >= 32 && c < 127 )
                {
                    ret += c;
                }
            }
            System.err.println("Unprintable character found in channel: " + ret);
        }
        else
        {
            ret = chanName;
        }
        return ret;
    }

    public String getCisAvail()
    {
        return cisAvail;
    }

    public Integer getAvailId()
    {
        return availId;
    }

    public void setAvailId(Integer availId)
    {
        this.availId = availId;
    }
    
    public String getBaseName()
    {
        String basename = chanName;
        if (getcType().toLowerCase().contains("trend"))
        {
            int dotPos = basename.lastIndexOf(".");
            basename = basename.substring(0, dotPos);
        }
        return basename;
    }
}
        