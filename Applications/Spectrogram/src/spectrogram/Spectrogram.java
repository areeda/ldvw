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
package spectrogram;

import au.com.bytecode.opencsv.CSVReader;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.Progress;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ndsproxyclient.NDSBufferStatus;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import edu.fullerton.viewerplugin.GDSFilter;
import edu.fullerton.viewerplugin.SpectrumCalc;
import edu.fullerton.viewerplugin.WindowGen.Window;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.io.input.SwappedDataInputStream;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Spectrogram
{
    // database info
    private Database db;
    private ChannelTable chanTbl;
    private ChanInfo chanInfo;
    
    // program spec
    private final String version = "0.0.3";
    private final String programName = "Spectrogram.jar";
    private final int debugLevel = 2;
    
    // data spec
    private NDSProxyClient ndsClient;
    private String server;
    private String channelName;
    private String cType;
    
    // these are used if we're reading in data from a file
    private double sampleRate=0.;
    private int startGPS;
    private int duration;
    private boolean useAltData;
    
    private String testDataFilename;
    private File testDataFile;
    private File rawDataFile;

    // output image specs
    private int outX = 1024, outY = 550;        // final image size
    private int titleHeight;
    private int dimX, dimY;             // size of plot area
    private int xTicks = 6;
    private float up,lo;
    private boolean smooth;
    
    private int em;
    private int lblHeight;              // height of a character in axis label font
    
    // progress bar
    private boolean showProgressBar = false;
    private Progress progressBar;
    
    private final int minStride = 30;
    private final int targetFftCount = 200;
    private int bytesPerSample;
    
    private String color;
    
    // output
    String ofileName = "spectrogramTest.png";
    // timing
    private static long startMs;
    // return status
    private String status;      // "Success" or an error message
    //---------------------------------------------------
    private int colPerSample;

    private boolean norm;
    private boolean logFreq;
    private boolean logIntensity;
    private boolean interp;
    
    private int yTicks;
    private Rectangle cmRect;
    private Rectangle pltRect;
    private IndexColorModel colorModel;
    
    // processing options
    private SpectraCache spectraCache;
    private double fmin = 0, fmax = 0;         // frequency limits
    private int nfft;
    private int flen;
    private int overlapSamples;
    private SpectrumCalc spectrumCalculator;
    private boolean dodetrend;
    private Window window;
    private float secPerFFT;
    private float overlap;
    private SpectrumCalc.Scaling scaling;

    // prefilter specification
    private String filtType;
    private float cutoff;
    private int order;
    private String xferErrMsg;
    private GDSFilter gdsfilt;
    private double start;
    private double binWidth;
    private String rawDataFilename;
    private SwappedDataInputStream inStream;
    private SpectrogramCommandLine cmd;

    public Spectrogram() throws SQLException
    {
        
    }
    /**
     * This allows the jar file to be called from a command line but the Class can be used as part
     * of another program
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) throws WebUtilException, NDSException
    {
        int stat;
        try
        {
            Spectrogram me = new Spectrogram();

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
        catch (SQLException | ViewConfigException ex)
        {
            Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
            stat = 11;
        }
        System.exit(stat);
    }

    /**
     * The controller of the process loop, init -> get data -> process -> make image
     * @return 
     * @throws viewerconfig.ViewConfigException 
     * @throws edu.fullerton.jspWebUtils.WebUtilException 
     * @throws edu.fullerton.ndsproxyclient.NDSException 
     */
    public int doPlot() throws ViewConfigException, WebUtilException, NDSException
    {
        int ret = 0;
        long xfernt=0;
        long procnt=0;
        
        try
        {
            startMs = System.currentTimeMillis();
            initProgress();
            initChanInfo();
            initImage();
            initCache();
            if (debugLevel > 1)
            {
                System.out.format("\nChan: %1$s, sample rate: ",
                                  channelName);
                if (sampleRate >= 1)
                {
                    System.out.format("%1$.0f", sampleRate);
                }
                else
                {
                    System.out.format("%1$.3f", sampleRate);
                }
                System.out.format(", bytes/sample: %1$1d%n", bytesPerSample);
                System.out.format("Sec/fft: %1$.2f, overlap: %2$.2f%n", secPerFFT, overlap);
            }

            int stride = duration / 100;
            int maxStride = (int) (1000000/sampleRate);
            stride = Math.max(stride, minStride);
            stride = Math.min(stride, duration);
            stride = Math.min(stride, maxStride);

            bufn = 0;
            boolean wantsToCancel = false;

            if (useAltData)
            {
                int stopSample = (int) (duration * sampleRate);
                
                if (!testDataFilename.isEmpty())
                {
                    readAddTestData(testDataFile);
                }
                else if (!rawDataFilename.isEmpty())
                {
                    readAddRawData();
                }
                else
                {
                    noData(0, stopSample);
                }
            }
            else
            {
                int stride2 = duration;
                
                for (int curT = 0; curT < duration && !wantsToCancel; curT += stride2)
                {
                    long xStrt=System.nanoTime();
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
                        ndsClient.connect();

                        boolean reqStat = ndsClient.requestData(channelName, chanInfo.getcType(), 
                                                                curgps, curgps + duration, stride);
                        if (reqStat)
                        {   
                            // for the first buffer we get channel information from NDS2 and 
                            // adjust accordingly
                            int dt = (int) (ndsClient.getStartGPS() - startGPS);
                            setProgress(String.format("Processing %1$,4d of %2$,4d seconds of data", 
                                                      dt, duration));
                            setProgress(dt, duration);
                            NDSBufferStatus bufferStatus = ndsClient.getBufferStatus();
                            if (sampleRate != bufferStatus.getFs())
                            {
                                sampleRate = bufferStatus.getFs();
                                initImage();
                                initCache();
                            }
                            if (fmax == 0)
                            {
                                fmax = sampleRate / 2;
                            }

                            int startSample = (int) (dt * sampleRate);
                            int nsample = (int) (duration * sampleRate);
                            long pStrt=System.nanoTime();
                            xfernt += pStrt - xStrt;
                            
                            addBuf(startSample, nsample);
                            procnt += System.nanoTime() - pStrt;
                            
                            bufn++;
                            wantsToCancel = checkCancel();
                        }
                        else
                        {
                            String msg = ndsClient.getLastError();
                            wantsToCancel = true;
                            System.err.format("Transfer error: %s\n", msg);
                            ret = 2;
                        }
                        ndsClient.bye();
                        ndsClient = null;
                    }
                    catch (Exception ex)
                    {
                        xferErrMsg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                        if (!checkNoData(curgps, curDur, xferErrMsg))
                        {
                            wantsToCancel = true;
                            System.err.format("Transfer error: %s\n", xferErrMsg);
                            ret  = 2;
                        }
                        try
                        {
                            //ndsClient.disconnect();
                            ndsClient.bye();
                            ndsClient = null;
                        }
                        catch (Exception ex2)
                        {
                            // Ignore error in error handler
                        }
                    }
                }
            }
            if (wantsToCancel)
            {
                if (spectraCache.size() > nfft/4)
                {
                    status = "Shortened";
                }
                else
                {
                    status = "Canceled";
                }
                status += " by user or because of error";
                if (xferErrMsg == null || xferErrMsg.isEmpty())
                {
                    xferErrMsg = status;
                }
                else
                {
                    xferErrMsg += " - " + status;
                }
                    
            }
            else
            {
                status = "Success";
            }
            long mkimgnt=0;
            if (spectraCache.size() > nfft/4)
            {
                long mkimgStrt = System.nanoTime();
                makeImage();
                mkimgnt=System.nanoTime() - mkimgStrt;
            }
            else if (spectraCache.size() == 0)
            {
                System.err.println("Unable to transfer data.  Last error received: " + xferErrMsg);
                ret = 3;
            }
            else
            {
                System.err.println("Some data transfered but not enough spectra to make an image (" + 
                                   Integer.toString(spectraCache.size()) + ")");
                System.err.println("Last error received: " + xferErrMsg);
                ret = 4;
            }
            // timing info
            double elapsedSec = (System.currentTimeMillis() - startMs) / 1000.;
            long bytes = (long) (duration * sampleRate * Float.SIZE / 8);
            double rateKBps = bytes / elapsedSec / 1000;
            System.out.format("Run time: %1$.2fs, total bytes xfer: %2$,d, process rate %3$.1f KBps%n",
                                             elapsedSec, bytes, rateKBps);
            double xfersec = xfernt / 1.0e9;
            double procsec = procnt / 1.0e9;
            double mkimgsec = mkimgnt / 1.0e9;
            double ovhdsec = elapsedSec - xfersec - procsec - mkimgsec;
            System.out.format("Transfer: %1$.2f, process: %2$.2f, image: %3$.2f, overhead: %4$.2f %n", 
                              xfersec, procsec,mkimgsec, ovhdsec);
        }
        catch (LdvTableException | SQLException | IOException | IllegalArgumentException ex)
        {
            status = "Error: " + ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            System.err.println(ex.toString());
            ret = 5;
        }
        closeProgress();
        return ret;
    }
    private void initChanInfo() throws LdvTableException, SQLException, ViewConfigException, WebUtilException
    {
        if (!useAltData)
        {
            setProgress("Getting Channel info.");

            getDbTables();

            setProgress("Getting Channel info.");
            if (!getChanInfo())
            {
                System.exit(1);
            }
        }
        else
        {
            // test data can either be no data or input from a csv file
            testDataFile = null;
            rawDataFile = null;
            if (rawDataFilename.isEmpty() && testDataFilename.isEmpty())
            {
                channelName = "No data (labels only)";
            }
            else if (!testDataFilename.isEmpty())
            {
                testDataFile = new File(testDataFilename);
                channelName = testDataFile.getName();
                bytesPerSample = Float.SIZE/8;
            }
            else if (!rawDataFilename.isEmpty())
            {
                Pattern fnPat = Pattern.compile("(.*/)?(.+)-(\\d+)-(\\d+).dat");
                Matcher fnMat = fnPat.matcher(rawDataFilename);
                if (!fnMat.find())
                {
                    throw new WebUtilException("Raw data file not named corrrectly.");
                }
                channelName = fnMat.group(2);
                startGPS = Integer.parseInt(fnMat.group(3));
                duration = Integer.parseInt(fnMat.group(4));
                bytesPerSample = Float.SIZE/8;
            }
            
            
        }
        setProgress("Starting transfer.");
    }
    /**
     * Using previously set up object members verify the channel and get needed info, complication is
     * when the channel is only partially specified
     *
     * @return true if we got what we needed, else return after we've printed errors.
     */
    private boolean getChanInfo() throws SQLException
    {
        boolean ret=false;
        long strt = System.currentTimeMillis();
        {
            int n;
            if (channelName == null || channelName.isEmpty())
            {
                throw new IllegalArgumentException("No Channel specified");
            }
            if ( (server == null || server.isEmpty()) && (cType == null || cType.isEmpty()) )
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
                    chanInfo = null;
                    if (sampleRate > 0)
                    {
                        for(ChanInfo ci : chSet)
                        {
                            if (Math.abs(ci.getRate()-sampleRate) < .0001)
                            {
                                chanInfo = ci;
                                break;
                            }
                        }
                    }
                    if (chanInfo == null)
                    {
                        System.err.print("Warning: more than one channel matches: " + channelName);
                        System.err.println(" and no applicable rate specified.");
                    }
                }
                else if (chSet.size() == 1)
                {
                    chanInfo = chSet.first();
                }
            }
            if (chanInfo == null)
            {
                System.err.println("Channel requested was not found: " + channelName);
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
                
                float dur = (System.currentTimeMillis() - strt) / 1000.f;
                System.out.format("Get channel info took %1$.1f sec.\n", dur);
            }
        }
        return ret;
    }

    private int bufn = 0;
    private double[] rawData;
    /**
     * Given a NDS connection ready to transfer time series data add it to our freq domain cache
     * 
     * @param strtSampleNum - starting position in samples
     * @param len - number of samples in buffer
     */
    private void addBuf(int strtSampleNum, int len) throws NDSException, WebUtilException
    {
        int cnt = 0;
        start = strtSampleNum/sampleRate;
        binWidth = 1. / secPerFFT;
        gdsfilt = new GDSFilter();

        while (cnt < len)
        {
            bufn++;
            boolean gotBuf = false;
            int idx;
            if (rawData == null)
            {
                // first buffer fill the whole thing
                rawData = new double[flen];
                for(idx = 0;idx < flen && idx < len; idx++)
                {
                    rawData[idx] = ndsClient.getNextDouble();
                }
                cnt += flen;
                gotBuf = idx == (len - 1);
            }
            else
            {
                // subsequent buffers we first deal with overlap
                int offset = flen - overlapSamples;
                for(idx = 0; idx < overlapSamples;idx++)
                {
                    rawData[idx] = rawData[idx+offset];
                }
                
                for(idx = overlapSamples; idx < flen && cnt < len; idx++, cnt++)
                {
                    rawData[idx] = ndsClient.getNextDouble();
                }
                
                gotBuf = idx == (len - 1);
            }
            filtAndSave();
        }
        
    }
    /**
     * Given a buffer of real data add it to our freq domain cache
     * almost a duplicate of NDS2 version for efficiency
     *
     * @param rawBuf - array of time domain samples
     * @param strtSampleNum - starting position in samples
     * @param len - number of samples in buffer (it doesn't need to be full)
     */
    private void addBuf(double[] rawBuf, int strtSampleNum, int len) throws NDSException, WebUtilException
    {
        int cnt = 0;
        start = strtSampleNum / sampleRate;
        binWidth = 1. / secPerFFT;
        rawData = new double[flen];

        while (cnt < len)
        {
            bufn++;
            // copy data for next fft and add it to cache
            if (cnt + flen < len)
            {
                System.arraycopy(rawBuf, cnt, rawData, 0, flen);
                filtAndSave();
            }
            cnt += flen - overlapSamples;
        }

    }

    private void filtAndSave() throws WebUtilException
    {
        writeRawBuffer(bufn, rawData);
        double[] temp = new double[rawData.length];
        System.arraycopy(rawData, 0, temp, 0, rawData.length);
        if (!filtType.isEmpty())
        {
            gdsfilt.apply(temp, (float) sampleRate, filtType, cutoff, order);
        }
        double[] result = spectrumCalculator.doCalc(temp, sampleRate);
        spectraCache.add(start, result, sampleRate, fmax, fmin, binWidth);
        start += flen - overlapSamples;

    }

    
    private void noData(int strtSampleNum, int stopSampleNum)
    {
        long len = stopSampleNum - strtSampleNum;
        bufn++;
       
    }
    // Image generation from data array
    private WritableRaster rast;        // image pixels of the colored plot section
    private BufferedImage img;          // output image
    private Graphics2D grph;            // Graphics context of image
    private Font lblFont;               // the font we're using to make labels
    private Font titleFont;             // guess what we're going to use that one for
    private int imgX0, imgY0;            // origin of the plot section
    FontMetrics labelMetrics;           // used for positioning and sizing labels

    /**
     * Generate and write the output image
     *
     * @throws IOException some problem with the file writing
     */
    private void makeImage() throws IOException
    {
        Rectangle imgR = new Rectangle(imgX0, imgY0, dimX, dimY);
        SpectraCache.Normalization normalization = norm ? SpectraCache.Normalization.DIVBYMEAN : SpectraCache.Normalization.ALL;
        spectraCache.makeImage(rast,imgR,normalization,logIntensity,logFreq);
        finalizeImage();
    }
    
    /**
     * Define the size and position of different parts of image and add global labels
     */
    private void initImage()
    {
        
        colorModel = IndexColorTables.getColorTable(color);
        
        // use anti-aliasing font representation
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // img contains the output image with all the labels
        img = new BufferedImage(outX, outY, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        grph = img.createGraphics();
        grph.setBackground(Color.white);
        grph.clearRect(0, 0, outX, outY);
        

        String fontName = getAGoodFont();
        lblFont = new Font(fontName, Font.PLAIN, 16);
        titleFont = new Font(fontName, Font.BOLD, 18);
        FontMetrics titleMetrics = grph.getFontMetrics(titleFont);
        labelMetrics = grph.getFontMetrics(lblFont);
        em = labelMetrics.stringWidth("M");
        
        String title;
        String utcDate;
        
        String hrTime = TimeAndDate.hrTime(duration);
        String cname;
        if (useAltData)
        {
            cname = "Test Data";
            if (testDataFilename != null && !testDataFilename.isEmpty())
            {
                File f = new File(testDataFilename);
                cname = f.getName();
            }
            if (rawDataFilename != null && !rawDataFilename.isEmpty())
            {
                File f = new File(rawDataFilename);
                cname = f.getName();
            }
            if (channelName != null && !channelName.isEmpty())
            {
                cname = channelName;
            }
            if (startGPS == 0)
            {
                Date now = new Date();
                startGPS = (int) TimeAndDate.utc2gps(now.getTime()/1000);
            }
        }
        else
        {
            cname = channelName;
        }
        utcDate = TimeAndDate.gpsAsUtcString(startGPS);
        title = String.format("%1$s %2$s - %3$,d (%4$s)", cname, utcDate, startGPS, hrTime);

        titleHeight = titleMetrics.getHeight();
        int titleWidth = titleMetrics.stringWidth(title);

        lblHeight = labelMetrics.getHeight();
        int lblWidth = labelMetrics.stringWidth("99,999");
        
        int tbMargin = 10;
        int lrMargin = 10;

        // plot title
        int tx = outX/2 - titleWidth/2;
        int ty = (int) Math.round(lblHeight * 1.5);
        grph.setPaint(Color.BLACK);
        grph.setFont(titleFont);
        grph.drawString(title, tx, ty);
        
        // calc and draw color map
        int cmWidth = 48;
        int cmLblWidth = labelMetrics.stringWidth("0.000000") + em;
        int cmLeft = outX - lrMargin * 2 - titleHeight - cmLblWidth - cmWidth;
        imgY0 = titleHeight + tbMargin * 2;
        dimY = outY - imgY0 - lblHeight * 5 - tbMargin;
        cmRect = new Rectangle(cmLeft, imgY0, cmWidth, dimY);
        grph.drawRect(cmLeft, imgY0, cmWidth, dimY);
        
        // calc and draw image rectangle
        imgX0 = lrMargin * 2 + titleHeight + lblWidth;
        dimX = cmLeft - imgX0 - lrMargin;
        pltRect = new Rectangle(imgX0, imgY0, dimX, dimY);
        grph.drawRect(imgX0, imgY0, dimX, dimY);

        
        // y-axis label
        String leftAxisLabel = "Frequency (Hz)";
        
        grph.setColor(Color.BLACK);
        tx = lblHeight + lrMargin;
        titleWidth = titleMetrics.stringWidth(leftAxisLabel);
        ty = outY/2 + titleWidth/2;
        drawRotatedText(tx,ty,-90,leftAxisLabel);

        // color map label
        String rightAxisLabel = scaling.toString();
        if (norm)
        {
            rightAxisLabel += " Normalized.";
        }
        tx = outX - lblHeight - lrMargin;
        titleWidth = titleMetrics.stringWidth(rightAxisLabel);
        ty = outY/2 + titleWidth/2;
        drawRotatedText(tx,ty, -90,rightAxisLabel);
        
        int nsamples = (int) (duration * sampleRate);
        colPerSample = 1;

        if (nsamples < dimX)
        {
            // we have more pixels available than we have samples so making it pretty takes some work
            colPerSample = dimX / nsamples;
            dimX = colPerSample * nsamples;
        }
        
        rast = img.getRaster();
        fillColorMap();
    }

    /**
     * The spectrum cache holds all spectra until we're ready to clip, normalize and interpolate them into an image
     */
    private void initCache()
    {
        
        
        flen = (int) (secPerFFT * sampleRate);
        
        if (overlap < 0)
        {
            // set overlap to make prettier pictures
            float t =  (duration / secPerFFT);
            if (t < targetFftCount)
            {
                overlap = 1-t/targetFftCount;
            }
            else
            {
                overlap = 0.5f;
            }
        }
        nfft = (int) Math.round(duration / secPerFFT / (1-overlap));
        overlapSamples = (int) (flen * overlap);
        if (overlapSamples >= flen)
        {
            overlapSamples = flen - 1;
        }
        if (overlapSamples <0)
        {
            overlapSamples = 0;
        }
        if (overlapSamples > 0)
        {
            nfft -= 1;
        }
        overlap = (float) overlapSamples / flen;
        
        spectraCache = new SpectraCache(nfft);
        spectraCache.setDebugLevel(debugLevel);
        spectraCache.setSmooth(smooth);
        spectraCache.setInterp(interp);
        spectraCache.setUp(up);
        spectraCache.setLo(lo);
        
    }
    /**
     * Put the created raster into the image, label the axis and generally make it look pretty and write it out.
     * 
     * @throws IOException probably image write failed
     */
    private void finalizeImage() throws IOException
    {
        img.setData(rast);
        grph.setFont(lblFont);
        // add the axis labels
        int lasc = labelMetrics.getAscent();
        int imgXmax = imgX0 + dimX;
        
        // label the X-axis of plot
        if (xTicks == 0)
        {
            // @todo make an intelligent choice
            xTicks = 11;
        }
        double tw = (double)dimX / (xTicks-1);
        double dt = secPerFFT * (1-overlap) * spectraCache.size() / ((float) xTicks-1);
        String fmt = "%1$.0f";
        if (duration < xTicks)
        {
            fmt = "%1$.2f";
        }
        else if (duration < xTicks * 2)
        {
            fmt = "%1$.1f";
        }
        int y = (int) (pltRect.getY() + pltRect.getHeight() + 5);
        int yp = y + 5 + lasc;
        int gpsyp = (int) (yp + lblHeight * 1.2);
        int lastGpsPos = 0; // too see if it fits nicely
        for (int t = 0; t < xTicks; t++)
        {
            int x = (int) (t * tw + imgX0);
            long tsec = Math.round(t * dt);
            String xLbl;
            if (duration > 1000)
            {
                xLbl = TimeAndDate.hrTime(tsec,true);
            }
            else
            {
                xLbl = String.format(fmt, t * dt);
            }
            int xw = labelMetrics.stringWidth(xLbl);
            int xp = x - xw / 2;
            if (t == xTicks)
            {   // make sure the last tick is at the edge (round off errors)
                // and the label ends there since we can't center it
                x = imgXmax;
                xp = x - xw;
            }
            
            grph.drawLine(x, imgY0, x, y);
            grph.drawString(xLbl, xp, yp);
            
            // add the gps time for this tick if it fits
            String gpsStr = String.format("%1$,d", (int)(startGPS + t*dt));
            int gpsWdt = labelMetrics.stringWidth(gpsStr);
            int gpsPos = x - gpsWdt/2;
            
            if (gpsPos > lastGpsPos + em*2)
            {
                grph.drawString(gpsStr, gpsPos, gpsyp);
                lastGpsPos = gpsPos + gpsWdt;
            }
        }
        // label the Y-axis
        if (yTicks == 0)
        {
            // @todo make a more intelligent choice
            yTicks = 11;
        }
        int xps = imgX0 - 3;
        int xpe = imgX0 + dimX;
        float bw = 1.f / secPerFFT;
        
        // get the actual frequency range
        fmin = spectraCache.getSmin();
        fmax = spectraCache.getSmax();
        
        if (logFreq)
        {
            double mxexp = Math.log10(fmax-fmin);
            double mnexp;
            if (fmin > 0)
            {
                mnexp = Math.log10(fmin);
            }
            else
            {
                fmin = bw;
                mnexp = Math.log10(bw);
            }
            
            double val =0;
            double lastLabel = 0;
            for (int ex = (int)Math.floor(mnexp); val < fmax; ex++)
            {
                double exp;
                if (mnexp < mxexp)
                {
                    exp = Math.pow(10,ex);
                }
                else
                {
                    exp = Math.pow(10,ex-1);
                }
                for(int s = 1; s < 10; s++)
                {
                    val = s * exp;
                    if (mnexp == mxexp)
                    {
                        val += Math.pow(10,mnexp);
                    }
                    if (val >= fmin && val <= fmax)
                    {
                        drawGrid(mxexp,val);
                        if (s == 1 || s == 2 || s ==5)
                        {
                            labelYTick(mxexp,val);
                            lastLabel = val;
                        }
                    }
                }
           }
            // mark the max value if it's not too close to the last label
            if ((fmax - lastLabel)/fmax > .15)
            {
                labelYTick(mxexp,(int)fmax);
            }
        }
        else
        {
            double yfact = (fmax-fmin)/yTicks;
            double ypf = ((double)dimY)/yTicks;
            
            for(int yt=0; yt <= yTicks; yt ++)
            {
                double yv = yt * yfact + fmin;
                int ytp = (int) (dimY - Math.round(yt * ypf) + imgY0);
                grph.drawLine(xps, ytp, xpe, ytp);
                String fstr = String.format("%1$,.0f", yv);
                int fstrLen = labelMetrics.stringWidth(fstr);
                int fsp = xps - fstrLen -2;
                grph.drawString(fstr, fsp, ytp+lblHeight/2);
            }
        }
        
        // add the aux info line at the bottom
        int imgCenter = (imgXmax - imgX0) / 2;
        String auxInfo = getAuxInfo();
        
        int xw = labelMetrics.stringWidth(auxInfo);
        int xp = imgCenter - xw / 2 + imgX0;
        int auxyp = (int) (outY - lblHeight * 1.5);
        
        grph.drawString(auxInfo, xp, auxyp + lblHeight + 3);
        
        // label the color scale
        labelColorMapTics();
        
        // write the file
        File outputfile = new File(ofileName);
        ImageIO.write(img, "png", outputfile);
        System.out.println("Wrote: " + ofileName);
    }
    /**
     * draw the grid lines for a log axis
     * @param mxexp - log10 of max value
     * @param val - this value
     */
    private void drawGrid(double mxexp, double val)
    {
        Color oldcolor = grph.getColor();
        Color transparentWhite = new Color(1, 1, 1, 0.5f);
        grph.setColor(transparentWhite);
        int ypos = getYpos(mxexp,val);
        int xpe = imgX0 + dimX;
        
        grph.drawLine(imgX0-3, ypos, xpe, ypos);
        grph.setColor(oldcolor);
    }
    private int getYpos(double mxexp, double val)
    {
        int ypos;
        
        double logMin = fmin == 0 ? 1 : Math.log10(fmin);
        
        double yfrac = (Math.log10(val) - logMin)/(Math.log10(fmax) - logMin);
        ypos = dimY - (int) (yfrac * dimY) + imgY0;
        return ypos;
    }
    /**
     * Add a tick to the y axis
     * @param mxexp
     * @param val 
     */
    private void labelYTick(double mxexp, double val)
    {
        int ypos = getYpos(mxexp,val);
        int xps = imgX0 - 5;
        int xpe = imgX0 + dimX;
        
        grph.drawLine(xps, ypos, xpe, ypos);
        String fmt = "%1$,.0f";
        if (val == 0)
        {
            fmt = "%1$.1f";
        }
        else if (val < 1)
        {
            int exp = - (int)Math.floor(Math.log10(val));
            fmt = "%1$." + Integer.toString(exp) + "f";
        }
        String fstr = String.format(fmt, val);
        int fstrLen = labelMetrics.stringWidth(fstr);
        int fsp = xps - fstrLen - 2;
        grph.drawString(fstr, fsp, ypos + lblHeight / 2);
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
        cmd = new SpectrogramCommandLine();
        if (cmd.parseCommand(args, programName, version))
        {
            channelName = cmd.getChannelName();
            server = cmd.getServer();
            cType = cmd.getcType();
            sampleRate = cmd.getSampleRate();
            
            duration = cmd.getDuration() < 1 ? 20 : cmd.getDuration();
            
            startGPS = cmd.getStartGPS();
            
            useAltData = cmd.isUseTestData();
            testDataFilename = cmd.getTestDataFile();
            rawDataFilename = cmd.getRawDataFile();
            showProgressBar = cmd.isShowProgressBar();
            smooth = cmd.isSmooth();
            interp = cmd.isInterp();
            
            secPerFFT = cmd.getSecPerFFT() <= 0 ? 1 : cmd.getSecPerFFT();
            
            Float ft = cmd.getFmax();
            fmax = ft == null || ft < 0 ? 0. : ft;
            
            ft = cmd.getFmin();
            fmin = ft == null || ft < 0 ? 0. : ft;
            
            logFreq = cmd.isLogFreq();
            logIntensity = cmd.isLogIntensity();
            norm = cmd.isNorm();
            scaling = cmd.getScaling();
            dodetrend = cmd.isDetrend();
            window = cmd.getWindow();
            filtType = cmd.getFiltType();
            cutoff = cmd.getCutoff();
            order = cmd.getOrder();
            
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
            up = cmd.getUp();
            if (up <= 0 || up > 1)
            {
                up = 1;
            }
            lo = cmd.getLo();
            if (lo < 0 || lo >=1)
            {
                lo = 0;
            }
            color = cmd.getColor();
            if (color.isEmpty())
            {
                color = "jet";
            }
            spectrumCalculator = new SpectrumCalc();
            spectrumCalculator.setDoDetrend(dodetrend);
            spectrumCalculator.setScaling(scaling);
            spectrumCalculator.setWindow(window);
            
            
            ret = true;
        }
        else
        {
            ret = false;
        }
        
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

    //---------------------------------------------|
    //  Access functions so others can control us  |
    //---------------------------------------------|
    public void setStartGPS(int startGPS)
    {
        this.startGPS = startGPS;
    }

    public void setDuration(int duration)
    {
        this.duration = duration;
    }

    public void setOfileName(String ofileName)
    {
        this.ofileName = ofileName;
    }

    public void setShowProgressBar(boolean showProgressBar)
    {
        this.showProgressBar = showProgressBar;
    }

    //-------------------------------
    public String getStatus()
    {
        return status;
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

    private void drawRotatedText(int tx, int ty, double theta, String text)
    {
        
        AffineTransform fontAT = new AffineTransform();
        fontAT.setToIdentity();
        
        fontAT.rotate(Math.toRadians(theta));
        
        
        Font curFont = grph.getFont();
        Font rotFont = curFont.deriveFont(fontAT);
        
        
        grph.setFont(rotFont);
        grph.drawString(text, tx, ty);
        
        grph.setFont(curFont);
    }

    /**
     * draw the color map in predefined rectangle
     */
    private void fillColorMap()
    {
        int modelSize = colorModel.getMapSize();
        int mapWidth = (int) cmRect.getWidth();
        int mapHeight = (int) cmRect.getHeight();
        
        // draw the color bar
        double fact = (modelSize - 6) / ((double)mapHeight) ;
        byte[] colorval = new byte[mapWidth];
        
        for (int y=0; y < mapHeight-1; y++)
        {
            int my = (int) (cmRect.getY() + mapHeight - y - 1);
            int colorIdx = (int) (y * fact);
            colorIdx = colorIdx >= modelSize ? modelSize -1 : colorIdx;
            for (int b=0;b<mapWidth;b++)
            {
                 colorval[b] = (byte) (colorIdx & 255);
            }
            rast.setDataElements((int)(cmRect.getX()+1), my, mapWidth-1, 1, colorval);
        }
        
    }

    /**
     * label the color map appropriately
     */
    private void labelColorMapTics()
    {
        int mapWidth = (int) cmRect.getWidth();
        int mapHeight = (int) cmRect.getHeight();
        
        double imin = spectraCache.getiMin();
        double imax = spectraCache.getiMax();

        // add tick marks and labels to the color bar
        int y0 = (int) cmRect.getY();
        int x0 = (int) (cmRect.getX() + mapWidth - 4);
        grph.setFont(lblFont);
        grph.setColor(Color.BLACK);
        for (int p = 0; p <= 100; p++)
        {
            if ((p % 10 == 0 && p != 90) || (logIntensity && p < 10 && p % 2 == 0 ))
            {
                int p1 = p;
                double p2;      // actual value in the image
                p2 = p/100. * (imax-imin) + imin;
                if (logIntensity)
                {
                    p1 = p == 0 ? 0 : (int) (Math.log10((double) p) * 50);
                    p2 = Math.pow(10, p2);
                }
                int y = mapHeight - Math.round(p1 * mapHeight / 100.f) + y0;
                grph.drawLine(x0, y, x0 + 6, y);
                String lbl;
                int exp = (int) Math.log10(p2);
                if (Math.abs(exp) < 4)
                {
                    lbl = String.format("%1$4f", p2);
                }
                else
                {
                    double p3 = p2 / Math.pow(10, exp) * 10;
                    lbl = String.format("%1$.2fe%2$d",p3,exp);
                }
                
                grph.drawString(lbl, x0 + 8, y + lblHeight / 3);
            }
        }
    }
    

    /**
     * Generate a line of text that describes what we did
     * @return description string
     */
    private String getAuxInfo()
    {
        String auxInfo;
        int nsamples = (int) (duration * sampleRate);
        
        float bw = 1.f / secPerFFT;

        if (sampleRate >= 1)
        {
            auxInfo = String.format("Fs=%1$,.0fHz",sampleRate);
        }
        else
        {
            auxInfo = String.format("Fs=%1$,.3fHz",sampleRate);
        }
        
        auxInfo += String.format(", sec/fft = %1$.2f, overlap = %2$.2f, fft length=%3$,d", 
                                 secPerFFT, overlap, (int) (secPerFFT * sampleRate));
        
        auxInfo += String.format(", #-FFT = %1$d", spectraCache.size());
        
        if (bw >= 1)
        {
            auxInfo += String.format(", bw = %1$.0f", bw);
        }
        else
        {
            auxInfo += String.format(", bw = %1$.2f", bw);
        }
        auxInfo += String.format(", in samples = %1$,.0fK", nsamples/1000.);
        if (up < .999)
        {
            auxInfo += String.format(", up = %1$.2f", up);
        }
        if (lo > .001)
        {
            auxInfo += String.format(", low = %1$.2f", lo);
        }
        return auxInfo;
    }
    /**
     * Use a local file as test data
     * written during the LVC meeting in the Bethesda Hyatt where Internet service sucked big time
     * @param testData the csv file to read
     */
    private void readAddTestData(File testData)
    {
        try
        {
            setProgress("Read and add test data.");
            ArrayList<Double> data = new ArrayList<>();
            // read the file
            CSVReader csvReader = new CSVReader(new InputStreamReader(new FileInputStream(testData)));

            String[] rowAsTokens;
            String val;
            Double v;
            int nvals = 0;
            String fltPat = "[+\\-]?(([1-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?";
            while ((rowAsTokens = csvReader.readNext()) != null)
            {
                val = rowAsTokens.length == 2 ? rowAsTokens[1] : rowAsTokens[0];
                val = val.trim();
                if (val.matches(fltPat))
                {
                    v = Double.parseDouble(val);
                    data.add(v);
                    nvals++;
                }
            }

            if (data.size() < 16)
            {
                noData(0, (int) (16*sampleRate));
            }
            else
            {
                if (debugLevel > 4)
                {
                    double[] dbgRaw = new double[data.size()];
                    for(int idx=0;idx<data.size();idx++)
                    {
                        dbgRaw[idx] = data.get(idx);
                    }
                    writeRawBuffer(-1, dbgRaw);
                }
                int startPos = 0;
                duration = (int) (data.size()/sampleRate);
                double[] fftdata = new double[flen];
                binWidth = 1.0 / secPerFFT;
                while(startPos + flen <= data.size())
                {
                    for(int idx = 0; idx < flen; idx++ )
                    {
                        fftdata[idx] = data.get(idx+startPos);
                    }
                   
                    double[] temp = new double[fftdata.length];
                    System.arraycopy(fftdata, 0, temp, 0, fftdata.length);
                    writeRawBuffer(startPos, temp);
                    double[] result = spectrumCalculator.doCalc(temp, sampleRate);
                    spectraCache.add(startPos, result, sampleRate, fmax, fmin, binWidth);
                    startPos += flen - overlapSamples;
                }
            }
            setProgress("All spectra calculated");
        }
        catch (IOException | NumberFormatException ex)
        {
            Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void readAddRawData() throws WebUtilException, NDSException
    {
        if (rawDataFilename != null && !rawDataFilename.isEmpty())
        {
            rawDataFile = new File(rawDataFilename);
        }
        if (rawDataFile == null || !rawDataFile.canRead())
        {
            throw new WebUtilException("Request for raw data but file cannot be read, or not set.");
        }
        long nSamples = rawDataFile.length() / (Float.SIZE/8)/2;
        try
        {
            
            inStream = new SwappedDataInputStream(new FileInputStream(rawDataFile));
            int startPos = 0;
            int blen = (int) Math.min(nSamples,1024*1024);
            double[] rawDataBuffer = new double[blen];
            
            for(long n = 0; n<nSamples; n+= blen)
            {
                int dlen=blen;
                if (n+blen > nSamples)
                {
                    dlen = (int) (nSamples - n);
                }
                for(int i=0; i< dlen; i++)
                {
                    Float t = inStream.readFloat();
                    Float d = inStream.readFloat();
                    rawDataBuffer[i] = d;
                }
                addBuf(rawDataBuffer,(int) n, dlen);
            }
        }
        catch (IOException  ex)
        {
            throw new WebUtilException("Error reading raw data file", ex);
        }
        
    }

    /**
     * Select a font that a) looks good and b) is available on this system (Macs were getting me)
     * @return name of the font
     */
    private String getAGoodFont()
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        String[] fontNames = ge.getAvailableFontFamilyNames();
        String ret = "";
        for(int idx=0;idx<fontNames.length && ret.isEmpty();idx++)
        {
            String font=fontNames[idx];
            if (font.equalsIgnoreCase("DejaVu Sans"))
            {
                ret = fontNames[idx];
            }
        }
        for (int idx = 0; idx < fontNames.length && ret.isEmpty(); idx++)
        {
            String font = fontNames[idx];
            if (font.toLowerCase().contains("century schoolbook l"))
            {
                ret = fontNames[idx];
            }
        }
        if (ret.isEmpty())
        {
            ret = "Serif";
        }
        return ret;
    }

    /**
     * If the appropriate debug level is set write the raw data used for ffts
     * @param bufn - identifying buffer number -1 means all
     * @param rawData array to be written
     */
    private void writeRawBuffer(int bufn, double[] rawData)
    {
        if (debugLevel > 2)
        {
            BufferedWriter bw = null;
            try
            {
                String fname;
                if (bufn == -1)
                {
                    fname = "/tmp/raw-all.csv";
                }
                else
                {
                    fname = String.format("/tmp/raw-%1$03d.csv", bufn);
                }
                bw = new BufferedWriter(new FileWriter(fname));
                for (int idx = 0; idx < rawData.length; idx++)
                {
                    String l = String.format("%1$.10f\n", rawData[idx]);
                    bw.write(l);
                }
            }
            catch (IOException ex)
            {
                Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
            }
            finally
            {
                try
                {
                    bw.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(Spectrogram.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }
    /**
     * For binary input files from our frame reader set up the input stream
     *
     * @return number of entries to read
     * @throws WebUtilException
     */
    private long setupFileReads(String infilename) throws WebUtilException
    {
        setProgress("Scan input file for min/max GPS times.");
        File inFile = new File(infilename);
        long siz = inFile.length() / (Float.SIZE / 8) / 2;     // convert bytes to # entries (time, val)
        if (!inFile.canRead())
        {
            throw new WebUtilException("Can't open " + infilename + " for reading");
        }
        try
        {
            inStream = new SwappedDataInputStream(new FileInputStream(inFile));
            float minTime = Float.MAX_VALUE;
            float maxTime = -Float.MAX_VALUE;

            setProgress("Searhing for min/max time in input file.");
            int opct = 0;
            for (int i = 0; i < siz; i++)
            {
                int pct = (int) (100 * i / siz);
                if (pct > opct)
                {
                    setProgress(pct, 100);
                    opct = pct;
                }
                Float t = inStream.readFloat();
                Float d = inStream.readFloat();
                minTime = Math.min(minTime, t);
                maxTime = Math.max(maxTime, t);
            }
            startGPS = (int) (minTime);
            duration = (int) (maxTime - minTime);
            inStream.close();
            inStream = new SwappedDataInputStream(new FileInputStream(inFile));
        }
        catch (IOException ex)
        {
            throw new WebUtilException("Can't open " + infilename + " for reading");
        }

        return siz;
    }

}
