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
package spectrumapp;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvjutils.TimeInterval;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import edu.fullerton.viewerplugin.SpectrumCalc;
import edu.fullerton.viewerplugin.SpectrumPlot;
import edu.fullerton.viewerplugin.WindowGen;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * A command line driven app to allow ldvw spectrum displays to be run as part of live plots
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SpectrumApp
{
    private static long startMs;
    // database info

    private Database db;
    private ChannelTable chanTbl;
    private ChanInfo chanInfo;
    // program spec
    private final String version = "0.0.0";
    private final String programName = "SpectrumApp.jar";
    private final int debugLevel = 1;

    // command line parameters
    private boolean logFreq;
    private boolean logPower;
    private boolean dodetrend;

    private SpectrumCalc.Scaling scaling;
    private WindowGen.Window window;

    private Integer outX;
    private Integer outY;
    private Integer startGPS = 0;
    private Integer duration;
    private Integer xTicks;
    private Integer yTicks;
    
    private Float secPerFFT;
    private Float overlap;
    private Float fmin;
    private Float fmax;

    private String ofileName;
    
    // input specification
    private String[] channelNames;
    private String server;
    private String cType;               // chan type eg raw, rds, ...

    private SpectrumPlot sp;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int stat;
        startMs = System.currentTimeMillis();
        try
        {
            SpectrumApp me = new SpectrumApp();

            // decode command line and set parameters
            boolean doit = me.processArgs(args);

            // generate image
            if (doit)
            {
                stat = me.doPlot();
            }
            else
            {
                stat = 10;
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(SpectrumApp.class.getName()).log(Level.SEVERE, null, ex);
            stat = 11;
        }
        // timing info
        double elapsedSec = (System.currentTimeMillis() - startMs) / 1000.;
        
        System.out.format("Run time: %1$.2f sec%n",elapsedSec);
        System.exit(stat);

    }
    /**
     * Process the command line arguments and set fields
     *
     * @param args the main passed in arguments
     * @return true if we continue false means exit (didn't want to do it here)
     */
    private boolean processArgs(String[] args)
    {
        boolean ret;
        SpectrumCommandLine cmd = new SpectrumCommandLine();
        if (cmd.parseCommand(args, programName, version))
        {
            channelNames = cmd.getChannelNames();
            server = cmd.getServer();
            cType = cmd.getcType();

            duration = cmd.getDuration() < 1 ? 20 : cmd.getDuration();

            startGPS = cmd.getStartGPS();
            secPerFFT = cmd.getSecPerFFT() <= 0 ? 1 : cmd.getSecPerFFT();
            
            Float ft = cmd.getFmax();
            fmax = ft == null || ft < 0 ? 0.f : ft;
            
            ft = cmd.getFmin();
            fmin = ft == null || ft < 0 ? 0.f : ft;
            
            logFreq = cmd.isLogFreq();
            logPower = cmd.isLogPower();
            scaling = cmd.getScaling();
            dodetrend = cmd.isDetrend();
            window = cmd.getWindow();

            ofileName = cmd.getOfileName().isEmpty() ? "/tmp/test.png" : cmd.getOfileName();
            outX = Math.max(1024, cmd.getOutX());
            outY = Math.max(768, cmd.getOutY());

            ft = cmd.getOverlap();
            if (ft == null || ft < 0)
            {
                overlap = -1.f;
            }
            else
            {
                overlap = ft;
            }

            xTicks = cmd.getxTicks() < 3 ? 7 : cmd.getxTicks();
            yTicks = cmd.getyTicks() < 3 ? 3 : cmd.getyTicks();

            // set up the spectrum calculator
            sp = new SpectrumPlot();
            sp.setDispFormat("Stacked");
            sp.setDoDetrend(false);     // we do that on transfer, if needed
            sp.setLogXaxis(logFreq);
            sp.setLogYaxis(logPower);
            sp.setOverlap(overlap);
            sp.setScaling(scaling);
            sp.setSecperfft(secPerFFT);
            sp.setWindow(window);
            ret = true;
        }
        else
        {
            ret = false;
        }

        return ret;
    }

    private int doPlot() throws SQLException, WebUtilException, LdvTableException, ViewConfigException
    {
        int ret;
        HashSet<Integer> selections = getSelections();
        ArrayList<TimeInterval> times = getTimes();
        ArrayList<ChanDataBuffer> data = getData(selections, times);
        
        if (data.isEmpty())
        {
            System.err.println("No data to plot.");
            ret=3;
        }
        else
        {
            sp.setFmin(fmin);
            sp.setFmax(fmax);
            sp.makePlotFile(data, false, ofileName);
            ret = 0;
        }
        return ret;
    }

    private HashSet<Integer> getSelections() throws SQLException, LdvTableException, ViewConfigException
    {
        HashSet<Integer> ret = new HashSet<>();
        long strt = System.currentTimeMillis();
        getDbTables();
        
        {
            int n;
            if (channelNames == null || channelNames.length == 0)
            {
                throw new IllegalArgumentException("No Channel specified");
            }
            if ((server == null || server.isEmpty()) && (cType == null || cType.isEmpty()))
            {
                for(String channelName : channelNames)
                {
                    n = chanTbl.getBestMatch(channelName);
                    ret.add(n);
                }
            }
            else
            {
                if (cType == null || cType.isEmpty())
                {
                    cType = "raw";
                }
                for(String channelName : channelNames)
                {
                    TreeSet<ChanInfo> chSet = chanTbl.getAsSet(server, channelName, cType, 10);
                    if (chSet.size() > 1)
                    {
                        System.err.println("Warning: more than one channel matches: " + channelName);
                    }
                    
                    for(ChanInfo ci : chSet)
                    {
                        Integer id=ci.getId();
                        ret.add(id);
                    }
                }
            }
            if (ret.isEmpty())
            {
                for(String channelName : channelNames)
                {
                    System.err.println("Channel requested was not found: " + channelName);
                }
            }
        }
        return ret;

    }

    private ArrayList<TimeInterval> getTimes()
    {
        
        TimeInterval ti = new TimeInterval(startGPS,startGPS+duration);
        ArrayList<TimeInterval> ret = new ArrayList<>();
        ret.add(ti);
        return ret;
    }

    private ArrayList<ChanDataBuffer> getData(HashSet<Integer> selections, ArrayList<TimeInterval> times) throws WebUtilException
    {
        
        ArrayList<ChanDataBuffer> bufList;
        try
        {
            bufList = ChanDataBuffer.dataBufFactory(db, selections, times, null, null, false);
            // is there any preprocessing to be done?
            for (ChanDataBuffer dbuf : bufList)
            {
                if (dodetrend)
                {
                    dbuf.detrend();
                }
            }
        }
        catch (Exception ex)
        {
            throw new WebUtilException(ex);
        }

        return bufList;

    }
    /**
     * Connect to the database and create table objects we need
     */
    private void getDbTables() throws LdvTableException, SQLException, ViewConfigException
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
}
