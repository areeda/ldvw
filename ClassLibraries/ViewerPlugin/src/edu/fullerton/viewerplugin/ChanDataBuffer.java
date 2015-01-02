/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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
package edu.fullerton.viewerplugin;

import edu.fullerton.ldvjutils.TimeInterval;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.BaseChanSingle;
import edu.fullerton.ldvjutils.Butterworth;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.*;
import edu.fullerton.ndsproxyclient.NDSBufferStatus;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * provides access to channel data, handling server queries and data caching
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class ChanDataBuffer
{

    private DataTable dataTable;
    private BaseChanSelection baseChanInfo;
    private ChanInfo chanInfo;

    private TimeInterval timeInterval;
    private int dataLength;
    private float[] data;
    private String lastError = "";
    private ViewUser vuser;
    private final Database db;
    
    private final boolean useCache = false;
    private BaseChanSingle singleChan;
    private int requestSize;


    /**
     * Default constructor we need the database
     * @param db ligodv database object used for channel info
     */
    private ChanDataBuffer(Database db)
    {
        this.db = db;
        dataTable = null;
        chanInfo = null;
        singleChan = null;
    }

    /**
     * Transfer data from an appropriate server for the time interval specified
     *
     * @return true if data successfully transfered
     */
    private boolean getDataFromServer() throws LdvTableException, WebUtilException, SQLException
    {
        if (singleChan == null || timeInterval == null)
        {
            throw new LdvTableException("getDataFromServer called without channel or time specified");
        }
        long strtMs = System.currentTimeMillis();
        boolean ret = false;

        long strt = timeInterval.getStartGps();
        long stop = timeInterval.getStopGps();
        requestSize = (int) (stop - strt);
        List<ChanInfo> ciList = getSortedChanInfo();
        
        for(int cidx=0; cidx < ciList.size() && ! ret; cidx++)
        {
            ChanInfo ci = ciList.get(cidx);
            NDSProxyClient client = null;
            try
            {
                client = new NDSProxyClient(ci.getServer());

                if (!client.connect(100000))
                {
                    ret = false;
                    lastError = client.getLastError();
                }
                else
                {
                    // special handling of minute data
                    String cType = singleChan.getType();
                    float fs = ci.getRate();
                    if (fs < 1)
                    {
                        // A requirement of the NDS2 protocol is that samples must start on
                        // an appropriate boundary.  For sample rates > 1 sec it's second boundaries
                        // for less than 1 sec it's minute or 4 sec...

                        int perSec = Math.round(1/fs);  // that defines the boundary we need
                        strt = (strt+perSec-1) / perSec * perSec;     // round to next boundary nb: int op
                        stop = stop / perSec * perSec;  // make that one the previous boundary
                        if (strt >= stop)               // just in case they are asking for less than 1 minute
                        {
                            stop = strt + perSec;
                        }
                        requestSize = (int) (stop - strt);
                        dataLength = requestSize / perSec;
                        timeInterval.setStartGps(strt);
                        timeInterval.setStopGps(stop);
                    }
                    else
                    {
                        dataLength = Math.round(requestSize * ci.getRate());
                    }

                    if (cType.equalsIgnoreCase("online"))
                    {
                        requestSize = (int) (stop - strt);
                        strt = 0;        // needed for online data request
                        stop = requestSize;
                        dataLength = Math.round(requestSize * chanInfo.getRate());
                    }
                    int stride = requestSize;
                    while (stride * ci.getBytesPerSample() * ci.getRate() > 262144
                            && !cType.equalsIgnoreCase("online"))
                    {
                        stride /= 2;
                    }
                    stride = stride < 1 ? 1 : stride;
                    if (client.requestData(ci.getChanName(), ci.getcType(), strt, stop, stride))
                    {
                        NDSBufferStatus nbs = client.getBufferStatus();
                        checkBufStatus(ci, nbs);
                        data = new float[dataLength];
                        for (int i = 0; i < dataLength; i++)
                        {
                            data[i] = client.getNextDouble().floatValue();
                        }
                        ret = true;
                        chanInfo = ci;
                    }
                    else
                    {
                        ret = false;
                        lastError = client.getLastError();
                    }
                    client.bye();
                }
            }
            catch (NDSException ex)
            {
                ret = false;
                lastError = "Data transfer problem: " + ex.getLocalizedMessage();
                dataLength = 0;

                if (client != null)
                {
                    lastError +=  " - " + client.getLastError();
                    try
                    {
                        client.bye();
                    }
                    catch (NDSException ex1)
                    {
                        // we tried anyway
                    }
                }
            }
            if (ret && chanInfo.getcType().equalsIgnoreCase("online") )
            {
                long rStrt = timeInterval.getStartGps();
                long rStop = timeInterval.getStopGps();
                long bStrt = client.getStartGPS();
                timeInterval.setStartGps(bStrt);
                timeInterval.setStopGps(bStrt + rStop - rStrt);
            }
            if (ret)
            {
                cacheData();
                // @todo we should be logging failed transfers also
                long elap = System.currentTimeMillis() - strtMs;
                long bytes = dataLength * ci.getBytesPerSample();
                if (vuser != null)
                {
                    vuser.logTransfer(bytes, elap);
                }
            }
        }
        return ret;
    }
    /**
     * Find all the single channels for base channel selection, sort them by likelihood 
     * of a successfurl transfer
     * @return list of ChanInfo objects sorted by likliehood of 
     * @throws LdvTableException
     * @throws SQLException 
     */
    public List<ChanInfo> getSortedChanInfo() throws LdvTableException, SQLException
    {
        ArrayList<ChanInfo> ret = new ArrayList<>();
        
        ChanPointerTable cpt = new ChanPointerTable(db);
        String ctype = singleChan.getType();
        String ttype = singleChan.getTrendType();
        int baseId = singleChan.getIndexID();
        List<Integer> cidxList;
        if (ttype.isEmpty())
        {
            cidxList = cpt.getChanList(baseId, ctype);
        }
        else
        {
            String[] typList = { ttype };
            cidxList = cpt.getChanList(baseId, ctype, typList);
        }
        ChannelTable chanTbl = new ChannelTable(db);
        
        for(Integer idx : cidxList)
        {
            ChanInfo ci = chanTbl.getChanInfo(idx);
            int rank = calcRank(ci);
            ci.setRank(rank);
            ret.add(ci);
        }
        Collections.sort(ret, new RankComparator());
        return ret;
    }

    /**
     * Deal with the issue of  multiple sample frequencies and data types.  We don't know which ones
     * apply to the data until we transfer it.  This routine updates fields as needed.
     * 
     * @param nbs - what the actual data looks like
     */
    private void checkBufStatus(ChanInfo chanInfo, NDSBufferStatus nbs)
    {
        if (Math.abs(chanInfo.getRate() - nbs.getFs()) > .001 || 
            ! chanInfo.getdType().contentEquals(nbs.getType()))
        {
            chanInfo.setRate(nbs.getFs());
            chanInfo.setdType(nbs.getType());
            dataLength = Math.round(requestSize * chanInfo.getRate());
        }
    }
    /**
     * Utility class to allow sorting ChanInfo objects by how good we think they'll be for
     * actually getting data
     */
    private class RankComparator implements Comparator
    {

        @Override
        public int compare(Object o1, Object o2)
        {
            ChanInfo ci1 = (ChanInfo) o1;
            ChanInfo ci2 = (ChanInfo) o2;
            return ci1.getRank().compareTo(ci2.getRank());
        }
        
    }
    /**
     * Until we have definitive data on what's available where guess which one is better than the next.
     * @param ci channel we're looking for
     * @return ranking heuristic
     */
    private int calcRank(ChanInfo ci)
    {
        int ret = 1024;  // no clue value
        long age = timeInterval.getAge();
        boolean preferSite = age < 5 * 24 * 3600;
        String bestSite;
        switch (singleChan.getIfo())
        {
            case "L1": bestSite="ligo-la.caltech.edu"; break;
            case "H1": bestSite="ligo-wa.caltech.edu"; break;
            default:   bestSite="ligo.caltech.edu"; break;
        }
        String server = ci.getServer().toLowerCase();
        if (preferSite && server.contains(bestSite))
        {
            ret /= 2;
        }
        // prefer the nds domain name over the backups
        if (server.startsWith("nds."))
        {
            ret /= 4;
        }
        return ret;
    }
    /**
     * The view user is used to identify log entries
     * 
     * @param vuser - identify the user making the request
     */
    public void setVuser(ViewUser vuser)
    {
        this.vuser = vuser;
    }
    /**
     * Length of data buffer 
     * @return number of samples in data buffer
     */
    public int getDataLength()
    {
        return dataLength;
    }

    /**
     * Get the name of the channel this buffer represents
     * 
     * @return the name
     * @throws LdvTableException
     */
    public String getChanName() throws LdvTableException
    {
        ChanInfo ci = getChanInfo();
        return ci.getChanName();
    }
    /**
     * When data has been transferred a specific channel is available.  Until then the base channel
     * information which is not server, type, or sample rate specific.
     * 
     * @return may be null if base to specific channel transfer has not been performed.
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public ChanInfo getChanInfo() throws LdvTableException
    {
        if (chanInfo == null)
        {
            try 
            {
                List<ChanInfo> ciList = getSortedChanInfo();
                if (! ciList.isEmpty())
                {
                    chanInfo = ciList.get(0);
                }
            }
            catch (SQLException ex) 
            {
                throw new LdvTableException("GetChanInfo: problem finding info on " + 
                                            toString(), ex);
            }
        }
        return chanInfo;
    }

    public void setChanInfo(ChanInfo chanInfo)
    {
        this.chanInfo = chanInfo;
    }

    public float[] getData()
    {
        return data;
    }

    public void setData(float[] data)
    {
        this.data = data;
    }

    public TimeInterval getTimeInterval()
    {
        return timeInterval;
    }

    /**
     * 
     * @param timeInterval 
     */
    public void setTimeInterval(TimeInterval timeInterval)
    {
        this.timeInterval = timeInterval;
    }

    /**
     * If the last nds operation returned an error return a hopefully reasonable translation
     * @return an error message for the user.
     */
    public String getLastError()
    {
        return lastError;
    }

    /**
     * Get the query command for requested data so end user can determine if it's our problem or nds'
     * this one works on an initialized ChanDataBuffer object
     * 
     * @return command line buffer request
     */
    public String getNDSQuery()
    {
        String ret = "";
        if (chanInfo == null)
        {
            ret = "no chanInfo available";
        }
        else
        {
            ret = ChanDataBuffer.getNDSQuery(chanInfo, timeInterval);
        }
        return ret;
    }

    /**
     * use the data specification to generate a query command for nds utilities to help determine cause of a transfer problem
     * @param ci defines the channel
     * @param ti defines the time interval
     * @return command line buffer request
     */
    public static String getNDSQuery(ChanInfo ci, TimeInterval ti)
    {
        String server = ci.getServer();
        String chname = ci.getChanName();
        String ctype = ci.getcType();
        long startGps = ti.getStartGps();
        long stopGps = ti.getStopGps();
        long duration = stopGps - startGps;
        if (ctype.toLowerCase().equalsIgnoreCase("online"))
        {
            startGps =0;
            stopGps = duration;
        }
        String nds = String.format("nds_query -n %1$s -s %2$d -e %3$d %4$s,%5$s", server, startGps, stopGps, chname, ctype);
        return nds;
    }

    /**
     * Create a list of ChanDataBuffer from the selected channels and times.  Data will be retrieved
     * from our Cache or transferred from the server specified.
     * @param baseSelections selections as base channels or specific channels 
     * @param db database object
     * @param times list of time intervals
     * @param vpage Page object to add errors if needed
     * @param vuser User object to keep stats
     * @return list of buffers if any data is transferred.
     * @throws SQLException
     * @throws WebUtilException
     * @throws LdvTableException 
     */
    public static ArrayList<ChanDataBuffer> dataBufFactory(Database db, 
                                                           Map<Integer, BaseChanSelection> baseSelections,
                                                           List<TimeInterval> times, Page vpage, 
                                                           ViewUser vuser) 
            throws SQLException, WebUtilException, LdvTableException
    {
        return ChanDataBuffer.dataBufFactory(db, baseSelections, times, vpage, vuser, false);
    }
    /**
     * Get a list of ChanDataBuffer that allows creation of empty buffers
     * @param db
     * @param baseSelections
     * @param times
     * @param vpage
     * @param vuser
     * @param noxfer - transfer not needed, usually external program does it
     * @return list of bufferspecified by the the selections
     * @throws LdvTableException 
     * @todo this routine should not need vpage or vuser that stuff can be moved to caller
     */
    public static ArrayList<ChanDataBuffer> dataBufFactory(Database db, 
                                                           Map<Integer, BaseChanSelection> baseSelections,
                                                           List<TimeInterval> times, Page vpage,
                                                           ViewUser vuser,boolean noxfer)
            throws LdvTableException, WebUtilException, SQLException
    {
        ArrayList<ChanDataBuffer> ret = new ArrayList<>();

        // Base channels can have multiple selections raw and mean, minute trend for example
        // This loop turns this into single selections, then transfers data if necessary
        // Here necessary means plot is made inside ldvw rather than by an external program
        for (TimeInterval ti : times)
        {
            for (BaseChanSelection bcs : baseSelections.values())
            {
                List<BaseChanSingle> selList = bcs.getSingleChanList();
                for(BaseChanSingle singleChan : selList)
                {
                    ChanDataBuffer dbuf = new ChanDataBuffer(db);
                    dbuf.setVuser(vuser);
                    
                    dbuf.singleChan = singleChan;
                    dbuf.timeInterval = ti;
                    dbuf.dataLength = 0;
                    dbuf.data = null;

                    if (noxfer)
                    {
                        ret.add(dbuf);
                    }                
                    else
                    {
                        if (dbuf.findData())
                        {
                            ret.add(dbuf);
                        }
                        else if (vpage != null)
                        {
                            try
                            {
                                // add error message trying to explain why we couldn't get the buffer
                                PageItemList erMsg = new PageItemList();
                                erMsg.add(singleChan.toString() + ". ");
                                String timeDescription = ti.getTimeDescription();
                                erMsg.addLine(timeDescription);
                                erMsg.add(dbuf.getLastError());
                                erMsg.addBlankLines(2);
                                erMsg.addLine("If you have the nds2 client installed, you may get more information on the problem by trying:");
                                //erMsg.addLine(ChanDataBuffer.getNDSQuery());
                                erMsg.addBlankLines(2);
                                erMsg.addStyle("color", "red");
                                erMsg.addHorizontalRule();

                                vpage.add(erMsg);
                            }
                            catch (WebUtilException ex)
                            {
                                String ermsg = "dataBufFactory: Can't add error message:" +
                                               ex.getClass().getSimpleName() + " - " +
                                               ex.getLocalizedMessage();
                            }
                        }
                        else
                        {
                            System.err.format("Error getting %s at %s: %s%n",
                                              singleChan.toString(),ti.getTimeDescription(),dbuf.getLastError());
                        }
                    }
                }
            }
        }

        return ret;
    }

    public static ArrayList<ChanDataBuffer> testDataFactory(String testName, Database db, 
                                                            Map<Integer, BaseChanSelection> baseSelections, 
                                                            List<TimeInterval> times, Page vpage, 
                                                            ViewUser vuser) 
            throws LdvTableException
   {
       throw new LdvTableException("no test data for now");
//        ArrayList<ChanDataBuffer> ret = new ArrayList<>();
//
//        ChannelTable ct;
//        ChannelIndex channelIndex;
//        try
//        {
//            ct = new ChannelTable(db);
//            channelIndex = new ChannelIndex(db);
//        }
//        catch (SQLException ex)
//        {
//            String ermsg = "testDataFactory cannot create ChannelTable: " + ex.getClass() + " - " +
//                           ex.getLocalizedMessage();
//            throw new LdvTableException(ermsg);
//        }
//
//        for (TimeInterval ti : times)
//        {
//            for (BaseChanSelection bcs : baseSelections.values())
//            {
//                Map<String[], List<ChanInfo>> selList = channelIndex.getBestChanList(bcs, ti);
//                for (Entry<String[], List<ChanInfo>> ent : selList.entrySet())
//                {
//                    List<ChanInfo> ciList = ent.getValue();
//                
//                    for (ChanInfo ci : ciList)
//                    {
//                        if (ci == null)
//                        {
//                            ChanDataBuffer dbuf = new ChanDataBuffer(db);
//                            dbuf.setVuser(vuser);
//
//    //                        dbuf.baseChanInfo = ci;
//                            dbuf.timeInterval = ti;
//                            long strt = ti.getStartGps();
//                            long stop = ti.getStopGps();
//                            int requestSize = (int) (stop - strt);
//                            int bufSize = (int) (requestSize * ci.getRate());
//                            dbuf.dataLength = bufSize;
//                            dbuf.data = new float[bufSize];
//
//                            if (testName.equalsIgnoreCase("impulse"))
//                            {
//                                ci.setChanName("Impulse");
//                                dbuf.data[0] = 0.f;
//                                for (int i = 1; i < bufSize; i++)
//                                {
//                                    dbuf.data[i] = (i == bufSize / 2) ? 10000.f : 0.f;
//                                }
//                            }
//                            else
//                            {
//                                if (testName.equalsIgnoreCase("random"))
//                                {
//                                    ci.setChanName("White noise");
//                                    Random generator = new Random(System.currentTimeMillis());
//                                    for (int i = 0; i < bufSize; i++)
//                                    {
//                                        dbuf.data[i] = (float) (100 * generator.nextFloat());
//                                    }
//                                }
//                                else
//                                {
//                                    if (testName.equalsIgnoreCase("sine1"))
//                                    {
//                                        ci.setChanName("Single sine wave");
//                                        makeSine(1, ci.getRate(), dbuf.data, bufSize);
//                                    }
//                                    else
//                                    {
//                                        if (testName.equalsIgnoreCase("sine3"))
//                                        {
//                                            ci.setChanName("3 sines at even frequency ");
//                                            makeSine(3, ci.getRate(), dbuf.data, bufSize);
//                                        }
//                                        else
//                                        {
//                                            if (testName.equalsIgnoreCase("sine4"))
//                                            {
//                                                ci.setChanName("4 sines at odd frequency ");
//                                                makeSine(4, ci.getRate(), dbuf.data, bufSize);
//                                            }
//                                        }
//                                    }
//                                }
//                            }
//                            ret.add(dbuf);
//                        }
//                    }
//                }
//            }
//        }
//        return ret;
    }

    private static void makeSine(int n, float rate, float[] buf, int bufSize)
    {
        float[] fl = new float[n];
        float f0 = (float) (2 * Math.PI / rate);
        float nyq = rate / 2;

        for (int j = 0; j < fl.length; j++)
        {
            fl[j] = (j + 1.0f) / (fl.length + 1) * nyq * f0;
        }

        for (int i = 0; i < bufSize; i++)
        {
            buf[i] = 0.f;
            for (int j = 0; j < fl.length; j++)
            {
                buf[i] += (float) Math.sin(i * fl[j]) * ((j % 2) + 1);
            }
        }

    }

    /**
     * Check our data cache to see if we already have the request, 
     * otherwise we will try to retrieve the data from
     * specified server
     *
     * @return true if we have all of the data
     * @throws LdvTableException
     * @throws WebUtilException
     */
    private boolean findData() throws LdvTableException, WebUtilException, SQLException
    {
        boolean ret=false;
        if (useCache)
        {
            //ret = getDataFromCache(ci,ti);
        }
        if (!ret)
        {
            ret = getDataFromServer();
        }
        if (!ret)
        {
            dataLength = 0;
            data = null;
        }
        return ret;
    }

    /**
     *
     */
    public void detrend()
    {
        double[] x = new double[dataLength];
        double[] y = new double[dataLength];

        for (int i = 0; i < dataLength; i++)
        {

            x[i] = i;
            y[i] = data[i];
        }
        LinearRegression lr = new LinearRegression(x, y);
        double b = lr.getIntercept();
        double m = lr.getSlope();
        double fit, t;
        for (int i = 0; i < dataLength; i++)
        {
            data[i] = (float) (data[i] - (m * i + b));
        }
    }
    /**
     * Check to see if we have the requested data or part of it in our cache if part of it is 
     * available we will fill in the gaps, or at least try
     * @param ci Channel specifier
     * @param time time interval specifier
     * @return true if we've loaded all of data
     * @throws LdvTableException 
     */
    private boolean getDataFromCache(ChanInfo ci, TimeInterval time) throws LdvTableException
    {
        boolean gotData = false;
        if (useCache)
        {
//            try
//            {
//                BaseChanSingle bcs = new BaseChanSingle(baseChanInfo, type)
//                bcs.setSingleIdx(ci.getId());
//                baseChanInfo = bcs;
//                timeInterval = time;
//                if (dataTable == null)
//                {
//                    dataTable = new DataTable(db);
//                }
//                ArrayList<TimeInterval> tiList = dataTable.findBuffer(ci, time);
//                long myStart = time.getStartGps();
//                long myStop = time.getStopGps();
//                if (!tiList.isEmpty())
//                {
//                    // first see if the exact buffer we want is there waiting for us
//                    for(TimeInterval ti : tiList)
//                    {
//                        long tiStart = ti.getStartGps();
//                        long tiStop = ti.getStopGps();
//                        if (myStart == tiStart && myStop == tiStop)
//                        {
//                            data = dataTable.getData(ti);
//                            dataLength = data.length;
//                            gotData = true;
//                            dataTable.updateLastAccess(ti.getCacheId());
//                        } 
//                    }
//                    if (!gotData)
//                    {
//                        int outIdx = 0;
//                        long outTime = myStart;
//                        dataLength = Math.round((myStop-myStart) * ci.getRate());
//                        data = new float[dataLength];
//                        
//
//                        for(TimeInterval ti : tiList)
//                        {
//                            long tStart = ti.getStartGps();
//                            long tStop = ti.getStopGps();
//                            if (outTime < tStart)
//                            {
//                                // we need to transfer some from the server
//                                TimeInterval tti = new TimeInterval(outTime, tStart);
//                                int tcnt = Math.round((tStart-outTime) * ci.getRate());
//                                ChanDataBuffer tbuf = new ChanDataBuffer(db);
//                                //tbuf.getDataFromServer(ci, tti);
//                                System.arraycopy(tbuf.getData(), 0, data, outIdx, tcnt);
//                                outIdx += tcnt;
//                                outTime += tStart-outTime;
//                                
//                            }
//                            else if (tStart <= outTime && tStop > outTime)
//                            {
//                                // we have at least some overlap copy the part we need
//                                long sstrt = tStart;
//                                if (tStart < outTime)
//                                {
//                                    sstrt = outTime - tStart;
//                                }
//                                long sstop = Math.min(tStop,myStop);
//                                int tcnt = Math.round((sstop-sstrt)*ci.getRate());
//                                float[] tbuf=dataTable.getData(ti);
//                                int srcPos = Math.round((sstrt-tStart)*ci.getRate());
//                                System.arraycopy(tbuf, srcPos, data, outIdx, tcnt);
//                                dataTable.updateLastAccess(ti.getCacheId());
//                                outIdx += tcnt;
//                                outTime += sstop-sstrt;
//                            }
//                            
//                        }
//                        if (outTime < myStop)
//                        {
//                            // we need to transfer some from the server to fill end of buffer
//                            TimeInterval tti = new TimeInterval(outTime, myStop);
//                            int tcnt = Math.round((myStop - outTime) * ci.getRate());
//                            ChanDataBuffer tbuf = new ChanDataBuffer(db);
//                            //tbuf.getDataFromServer(ci, tti);
//                            
//                            System.arraycopy(tbuf.getData(), 0, data, outIdx, tcnt);
//                            outIdx += tcnt;
//                            outTime += myStop-outTime;
//                            
//                        }
//                        gotData = true;
//                    }
//                }
//            }
//            catch (Exception ex)
//            {
//                throw new LdvTableException("Saving data to cache: " + ex.getClass().getSimpleName() 
//                                            + ": " +ex.getLocalizedMessage());
//            }
        }
        return gotData;
    }

    /**
     * Save this data buffer in local cache for fast retrieval
     * @throws LdvTableException
     * @throws SQLException 
     */
    private void cacheData() throws LdvTableException
    {
        if (useCache)
        {
            if (dataLength > 0 && dataLength == data.length)
            {
                try
                {
                    if (dataTable == null)
                    {
                        dataTable = new DataTable(db);
                    }
//                    dataTable.cache(baseChanInfo, timeInterval, data);
                }
                catch (Exception ex)
                {
                    throw new LdvTableException("Saving data to cache:" + ex.getLocalizedMessage());
                }
                
            }
            else
            {
                throw new LdvTableException("Data inconsistnecy: " + lastError);
            }
        }
    }

    /**
     * Apply spatial convolution kernel to contents
     * 
     * @param fkernel convolution kernel
     * @see Butterworth
     */
    public void filter(double[] fkernel)
    {
        data = Butterworth.convolve(data, fkernel);
    }

    
    @Override
    public String toString()
    {
        String cName;
        cName = singleChan.toString();
        Long startgps = timeInterval.getStartGps();
        Long stopgps = timeInterval.getStopGps();
        
        Long duration = stopgps-startgps;
        String utc = TimeAndDate.gpsAsUtcString(startgps);
        String ret = String.format("%1$s,start: %2$s (%3$d) len: %4$s",
                                   cName,utc,startgps,TimeAndDate.hrTime(duration));
        return ret;
    }
}
