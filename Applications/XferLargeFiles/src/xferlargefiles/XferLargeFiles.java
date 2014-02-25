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
package xferlargefiles;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.Progress;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class XferLargeFiles
{
    private String version = "0.0.0";
    private String programName = "XferLargeFiles";
    
    // command line arguments
    private boolean showProgressBar;
    private String ofileName;
    
    // input specification
    private String channelName;
    private String server;
    private String cType;               // chan type eg raw, rds, ...
    private Integer startGPS = 0;
    private Integer duration;
    // from channel database
    private double sampleRate;
    private int bytesPerSample;
    
    // database info
    private Database db;
    private ChannelTable chanTbl;
    private ChanInfo chanInfo;

    private NDSProxyClient ndsClient;
    private String ndsErmsg = "";

    private Progress progressBar;
    private long startMs;
    private String xferErrMsg;
    // return status
    private String status;      // "Success" or an error message
    
    // output
    private File out = null;
    private DataOutputStream output = null;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int stat;
        XferLargeFiles me=null;
        try
        {
            me = new XferLargeFiles();

            // decode command line and set parameters
            boolean doit = me.processArgs(args);

            // generate image
            if (doit)
            {
                stat = me.doXfer();
            }
            else
            {
                stat = 10;
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(XferLargeFiles.class.getName()).log(Level.SEVERE, null, ex);
            stat = 11;
            if (me != null && me.ndsClient != null)
            {
                try
                {
                    me.ndsClient.bye();
                }
                catch (NDSException ex1)
                {
                    // we can ignore that one.
                }
            }
        }
        System.exit(stat);

    }
    
    

    private boolean processArgs(String[] args)
    {
        boolean ret;
        XferCommandLine cmd = new XferCommandLine();
        if (cmd.parseCommand(args, programName, version))
        {
            channelName = cmd.getChannelName();
            server = cmd.getServer();
            cType = cmd.getcType();

            duration = cmd.getDuration() < 1 ? 20 : cmd.getDuration();

            startGPS = cmd.getStartGPS();
            showProgressBar = cmd.isShowProgressBar();
            ofileName = cmd.getOfileName();
            ret = true;
        }
        else
        {
            ret = false;
        }

        return ret;

    }

    private int doXfer() throws LdvTableException, SQLException
    {
        int ret = 0;
        startMs = System.currentTimeMillis();
        try
        {
            
            initProgress();

            setProgress("Getting Channel info.");

            getDbTables();

            setProgress("Getting Channel info.");
            if (!getChanInfo())
            {
                System.exit(1);
            }
            System.out.format("Chan: %1$s, sample rate: %2$,.3f, bytes per sample: %3$d, strt: %4$d"
                                + " dur: %5$d\n",
                              channelName, sampleRate, bytesPerSample, startGPS, duration);

            setProgress("Starting transfer.");
            int stride2 = duration;
            int stride = duration / 100;
            int maxStride = (int) (1000000 / sampleRate);
            int minStride = 30;
            
            stride = Math.max(stride, minStride);
            stride = Math.min(stride, duration);
            stride = Math.min(stride, maxStride);
            
            boolean wantsToCancel = false;
            for (int curT = 0; curT < duration && !wantsToCancel; curT += stride2)
            {
                setProgress(String.format("Processing %1$,4d of %2$,4d seconds of data", curT, duration));
                setProgress(curT, duration);
                int curgps = startGPS + curT;
                int curDur = stride;
                if (curDur + curgps > startGPS + duration)
                {
                    curDur = startGPS + duration - curgps;
                }
                try
                {
                    ndsClient = new NDSProxyClient(server);
                    ndsClient.connect(200000);

                    boolean reqStat = ndsClient.requestData(channelName, chanInfo.getcType(), curgps, curgps + duration, stride);
                    if (reqStat)
                    {
                        int dt = (int) (ndsClient.getStartGPS() - startGPS);
                        setProgress(String.format("Processing %1$,4d of %2$,4d seconds of data", dt, duration));
                        setProgress(dt, duration);
                        int startSample = (int) (dt * sampleRate);
                        int nsample = (int) (duration * sampleRate);
                        addBuf(startSample, nsample);

                        
                        wantsToCancel = checkCancel();
                    }
                    else
                    {
                        ndsErmsg=ndsClient.getLastError();
                        if (!checkNoData(curgps, curDur, ndsErmsg))
                        {
                            wantsToCancel = true;
                            System.err.format("Transfer error: %s\n", ndsErmsg);
                            ret = 2;
                        }
                    }
                    ndsClient.bye();
                    ndsClient = null;
                    // verify we transferred all of it
                    long expectedBytes = Math.round(sampleRate * duration * 8);
                    long bytesWritten = 0;
                    if (output != null)
                    {
                        bytesWritten = output.size();
                        output.flush();
                        output.close();
                    }
                    if (bytesWritten != expectedBytes)
                    {
                        String ermsg = String.format("Output file is not the correct size.  "
                                    + "Expected: %1$d, wrote: %2$d\n", expectedBytes, bytesWritten);
                        ermsg += ndsErmsg;
                        //out.delete();
                        throw new LdvTableException(ermsg);
                    }
                    float elapsed = (System.currentTimeMillis() - startMs)/1000.f;
                    float xferRate = bytesWritten/elapsed;
                    System.out.format("%1$s - bytes transfered %2$d. time: %3$.1f rate: %4$.1f KBps\n", 
                                      ofileName, bytesWritten, elapsed, xferRate/1000.f);
                }
                catch (Exception ex)
                {
                    xferErrMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                    wantsToCancel = true;
                    throw new LdvTableException(xferErrMsg);
//                    if (!checkNoData(curgps, curDur, xferErrMsg))
//                    {
//                        wantsToCancel = true;
//                        System.err.format("Transfer error: %s\n", xferErrMsg);
//                        ret = 2;
//                    }
                }
            }
        }
        catch (Exception ex)
        {
            status = "Error: " + ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            System.err.println(ex.toString());
            if (ndsClient != null)
            {
                try
                {
                    ndsClient.bye();
                    ndsClient = null;
                }
                catch (NDSException ex1)
                {
                    // we can ignore that one.
                }
            }
            ret = 5;
        }
        closeProgress();
        return ret;

    }
    //===================================|
    // progress bar control              |
    //===================================|

    public void setProgressDialog(Progress pb)
    {
        progressBar = pb;
    }

    /**
     * set up the progress bar if so requested
     */
    private void initProgress()
    {

        if (showProgressBar && progressBar == null)
        {   // there's an issue if called from matlab of getting the progress frame
            // in matlab's event loop so we can't just create a new one here
            progressBar = new Progress();
        }
        if (progressBar != null)
        {
            progressBar.setChanName(channelName);
            progressBar.setWorkingOn("Initializing");
            progressBar.setEstTime("Time remaining: unknown");
            progressBar.setProgress(-1);
            progressBar.setPosition();
        }
    }

    private void setProgress(int dt, int duration)
    {
        if (progressBar != null)
        {
            String cur = String.format("%1$d of %2$d seconds of data", dt, duration);
            int pct = Math.round(dt * 100.f / duration);
            double elapsed = (System.currentTimeMillis() - startMs) / 1000.;
            double remaining;
            String etl;
            if (dt > 0)
            {
                remaining = elapsed * duration / dt - elapsed;
                etl = String.format("Elapsed: %4.0f, remaining: %2$4.0f seconds", elapsed, remaining);
            }
            else
            {
                etl = String.format("Elapsed: %4.0f, remaining: unknown", elapsed);
            }
            progressBar.setEstTime(etl);
            progressBar.setProgress(pct);
        }
    }

    /**
     * Set the progress "working on" string and update times
     *
     * @param what new content of working on
     */
    private void setProgress(String what)
    {
        if (progressBar != null)
        {
            double elapsed = (System.currentTimeMillis() - startMs) / 1000.;
            String etl = String.format("Elapsed: %4.0f", elapsed);
            progressBar.setEstTime(etl);
            progressBar.setWorkingOn(what);
            progressBar.setProgress(-1);
        }
    }

    private void closeProgress()
    {
        if (progressBar != null)
        {
            progressBar.done();
        }
    }

    private boolean checkCancel()
    {
        boolean ret = false;
        if (progressBar != null)
        {
            ret = progressBar.wantsCancel();
        }
        return ret;
    }
    /**
     * Connect to the database and create table objects we need
     */
    private void getDbTables() throws LdvTableException, SQLException
    {
        if (db == null)
        {
            ViewerConfig vc = new ViewerConfig();
            db = vc.getDb();
            if (db == null)
            {
                throw new LdvTableException("Can't connect to LigoDV-web database");
            }
        }
        if (chanTbl == null)
        {
            chanTbl = new ChannelTable(db);
        }
    }
    /**
     * Using previously set up object members verify the channel and get needed info, complication
     * is when the channel is only partially specified
     *
     * @return true if we got what we needed, else return after we've printed errors.
     */
    private boolean getChanInfo() throws SQLException
    {
        boolean ret;
        long strt = System.currentTimeMillis();
        {
            int n;
            if (channelName == null || channelName.isEmpty())
            {
                throw new IllegalArgumentException("No Channel specified");
            }
            if ((server == null || server.isEmpty()) && (cType == null || cType.isEmpty()))
            {
                n = chanTbl.getBestMatch(channelName);
                chanInfo = chanTbl.getChanInfo(n);
                server = chanInfo.getServer();
            }
            else
            {
                if (cType == null || cType.isEmpty())
                {
                    cType = "raw";
                }
                TreeSet<ChanInfo> chSet = chanTbl.getAsSet(server, channelName, cType, 10);
                if (chSet.size() > 1)
                {
                    System.err.println("Warning: more than one channel matches: " + channelName);
                }
                else if (chSet.size() == 1)
                {
                    chanInfo = chSet.first();
                }
            }
            if (chanInfo == null)
            {
                System.err.format("Channel requested is not in our database: %1$s at: %2$s %n", 
                                  channelName, server);
                ret = false;
            }
            else
            {
                sampleRate = chanInfo.getRate();
                String dtyp = chanInfo.getdType();

                if (dtyp.equalsIgnoreCase("INT-16"))
                {
                    bytesPerSample = 2;
                }
                else if (dtyp.equalsIgnoreCase("INT-32"))
                {
                    bytesPerSample = 4;
                }
                else if (dtyp.equalsIgnoreCase("INT-64"))
                {
                    bytesPerSample = 8;
                }
                else if (dtyp.equalsIgnoreCase("FLT-32"))
                {
                    bytesPerSample = 4;
                }
                else if (dtyp.equalsIgnoreCase("FLT-64"))
                {
                    bytesPerSample = 8;
                }
                else if (dtyp.equalsIgnoreCase("CPX-64"))
                {
                    bytesPerSample = 8;
                }
                ret = true;
            }
//            float dur = (System.currentTimeMillis() - strt) / 1000.f;
//            System.out.format("Get channel info took %1$.1f sec.\n", dur);
        }
        return ret;
    }
    private boolean checkNoData(int curgps, int curDur, String msg)
    {
        boolean ret = false;
        if (msg.toLowerCase().contains("requested data were not")
            || msg.toLowerCase().contains("no such channel")
            || msg.toLowerCase().contains("read timed out")
            || msg.toLowerCase().contains("unknown error"))
        {
            int strt = (curgps - startGPS);
            int strtSample = (int) (strt * sampleRate);
            int stopSample = (int) ((strt + curDur) * sampleRate - 1);
            noData(strtSample, stopSample);
            ret = true;
        }
        return ret;
    }
    private void noData(int strtSampleNum, int stopSampleNum)
    {

    }

    private void addBuf(int startSample, int nsample) throws LdvTableException
    {
        try
        {
            if (output == null)
            {

                    // first time set up writer
                    out = new File(ofileName);
                    if (out.exists())
                    {
                        throw new LdvTableException("Output file [" + ofileName + "] already exists");
                    }
                    output = new DataOutputStream(new FileOutputStream(out));
                
            }
            for (int i=0;i<nsample;i++)
            {
                Double x=ndsClient.getNextDouble();
                output.writeDouble(x);
            }
        }
        catch (Exception ex)
        {
            String ermsg = ex.getClass().getSimpleName() + " - " + ex.getLocalizedMessage();
            throw new LdvTableException(ermsg);
        }
    }
    
}
