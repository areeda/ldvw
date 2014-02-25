/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
package odcplot;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.Progress;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ndsproxyclient.NDSException;
import edu.fullerton.ndsproxyclient.NDSProxyClient;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.input.SwappedDataInputStream;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * ODC - Online Detector Characterization channels are bit masks representing the status of systems 
 * or subsystems descirbed in https://dcc.ligo.org/cgi-bin/private/DocDB/ShowDocument?docid=93660
 * 
 * This LigoDV Web product tries to make a pretty picture from an arbitrary length of ODC data
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class OdcPlot
{
    // program spec
    private final String version = "0.0.5";
    private final String programName = "OdcPlot.jar";
    private NDSProxyClient ndsClient;
    
    // database info
    private Database db;
    private ChannelTable chanTbl;
    private ChanInfo chanInfo;
    
    // data spec
    private String server;
    
    private String infilename;
    private SwappedDataInputStream inStream;

   
    private String channelName;
    private double sampleRate;

    private int startGPS;
    private int duration;
    private boolean useTestData;

    // output image specs
    
    private int outX=1024, outY=550;        // final image size
    private int titleHeight;
    private int dimX, dimY;             // size of plot area
    private int xTicks = 6;
    private boolean showProgressBar=false;
    private Progress progressBar;
    private final byte NODATA=8;        // table value for missing data
    private final int minStride = 30;
    private final int nColors = 32;     // how many colors are we using to code state
    private int bytesPerSample;
    private int lblHeight;              // height of a character in axis label font
    private int lblFontSize  = 9;
    private int titleFontSize  = 11;
    private boolean addLegend = true;
    private boolean addGPS = true;
    private final         Color[] keyColors =
        {
            Color.BLACK,    // 0 = unset (shouldn't happen)
            Color.RED,      // 1 = bad
            Color.GREEN,    // 2 = good
            Color.YELLOW,   // 3 = bad + good
            Color.CYAN,     // 4 = Parity but unset (shouldn't happen)
            Color.MAGENTA,  // 5 = Parity error + bad
            Color.BLUE,     // 6 = Parity error + good
            Color.ORANGE,   // 7 = Parity error + bad + good
            Color.BLACK     // 8 = No NDS data
        };

    // odc spec
    private int nbits;         // number of bits to display
    private final int maxBits=32;       // number of bits in input data
    private double sample2colFact;      // factor to convert sample number to column number
    private int colPerSample;           // if we're zooming excessively we may do more than one pixel per sample
    private final byte[] bits;
    private final int[] bitMask;
                            
    private byte[][] data;
    private String[] bitNames;
    private boolean checkParity;        // some channels have a parity bit others do not
    
    // output
    String ofileName;
    // timing
    private static long startMs;
    
    // return status
    private String status;      // "Success" or an error message
    //---------------------------------------------------
    /**
     * This allows the jar file to be called from a command line
     * but the Class can be used as part of another program
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        OdcPlot me = new OdcPlot();
        
        // decode command line and set parameters
        boolean doit = me.processArgs(args);
        // generate image
        if (doit)
        {
            try
            {
                me.doPlot();
            }
            catch (WebUtilException | ViewConfigException ex)
            {
                System.err.format("Error: %1$s %2$s\n", ex.getClass().getSimpleName(), 
                                                        ex.getLocalizedMessage());
            }
             
        }
    }
    public OdcPlot()
    {
        this.bits = new byte[maxBits];
        bitMask = new int[Integer.SIZE];
        int v=1;
        for(int i=0; i< bitMask.length; i++)
        {
            bitMask[i] = v;
            v = v << 1;
        }
    }
    /**
     * Create specified plots
     */
    public void doPlot() throws WebUtilException, ViewConfigException
    {
        try
        {
            startMs = System.currentTimeMillis();
            initProgress();
            setProgress("Getting Channel info.");
            getDbTables();
            
            setProgress("Getting Channel info.");
            if (!getChanInfo())
            {
                System.exit(1);
            }
            setProgress("Starting transfer.");
            getBitNames(channelName);
            int nSamples=0;
            if (!infilename.isEmpty())
            {
                nSamples = setupFileReads();
            }
            
            initImage();
            
            long singleBufMs = 0;
            long multipleBufMs = 0;
            long nBytes = 0;
            long strtMs = System.currentTimeMillis();

            bufn = 0;
            boolean wantsToCancel = false;
            boolean ret;

            if (useTestData)
            {
                int stopSample = (int) (duration * sampleRate);
                noData(data, 0, stopSample, (byte)3);
            }
            else if (!infilename.isEmpty())
            {
                try
                {
                    addBuf(data, 0, nSamples);
                }
                catch (NDSException ex)
                {
                    throw new WebUtilException("Reading from file", ex);
                }
            }
            else
            {
                try
                {
                    ndsClient = new NDSProxyClient(server);
                    ndsClient.connect();

                    int fullStride = duration;
                    nBytes = (long) (chanInfo.getRate() * chanInfo.getBytesPerSample() * duration);
                    
                    while (fullStride * chanInfo.getRate() * chanInfo.getBytesPerSample() > 512*1024)
                    {
                        fullStride /= 2;
                    }
                    fullStride = Math.max(fullStride, minStride);

                    ret = ndsClient.requestData(channelName, chanInfo.getcType(), startGPS, startGPS+duration, fullStride);
                    if (ret)
                    {
                        int nsample = (int) (duration * sampleRate);
                        addBuf(data, 0, nsample);
                    }
                    singleBufMs = ndsClient.getTotalTimeMs();
                    ndsClient.bye();
                    ndsClient = null;
                }
                catch (NDSException | IOException ex)
                {
                    System.err.format("Error trying to transfer data all at once: %1$s - %2$s%n", 
                                      ex.getClass().getSimpleName(), ex.getLocalizedMessage());
                    ret = false;
                    try
                    {
                        ndsClient.bye();
                    }
                    catch (NDSException ex1)
                    {
                        // we can ignore that one
                    }
                    ndsClient = null;
                }
                // we tried getting all data at once (the fast way) if that failed try harder
                if (!ret)
                {
                    int stride = duration / 100;
                    stride = Math.max(stride, minStride);
                    stride = Math.min(stride, duration);
                    for (int curT = 0;curT < duration && ! wantsToCancel; curT += stride)
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
                            if (ndsClient == null)
                            {
                                ndsClient = new NDSProxyClient(server);
                                ndsClient.connect();
                            }
                            ret = ndsClient.requestData(channelName, chanInfo.getcType(), curgps, curgps+curDur, curDur);
                            if (ret)
                            {
                                int t=curgps;
                                while(t<curgps+curDur && ! wantsToCancel)
                                {
                                    int dt = (int) (ndsClient.getStartGPS()-startGPS);
                                    setProgress(String.format("Processing %1$,4d of %2$,4d seconds of data", dt, duration));
                                    setProgress(dt, duration);
                                    int startSample = (int)(dt*sampleRate);
                                    int nsample = (int) (curDur * sampleRate);
                                    addBuf(data,startSample,nsample);
                                    int buflsec =  curDur;
                                    t = (int) (ndsClient.getStartGPS() + buflsec);
                                    bufn++;
                                    wantsToCancel = checkCancel();
                                }
                            }
                            else
                            {
                                String msg = ndsClient.getLastError();
                                if (!checkNoData(curgps,curDur,msg))
                                {
                                    wantsToCancel = true;
                                    System.err.format("Transfer error: %s\n", msg);
                                }
                            }
                            multipleBufMs = ndsClient.getTotalTimeMs();
                            ndsClient.disconnect();
                            ndsClient.bye();
                            
                            ndsClient=null;
                        }
                        catch(Exception ex)
                        {
                            String msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                            if (!checkNoData(curgps,curDur, msg))
                            {
                                wantsToCancel = true;
                                System.err.format("Transfer error: %s\n", msg);
                            }
                            if (ndsClient != null)
                            {
                                try
                                {
                                    ndsClient.disconnect();
                                    ndsClient.bye();
                                    ndsClient = null;
                                }
                                catch (Exception ex2)
                                {
                                    // we ignore errors in the error handlers, 
                                    // because I don't know what else to do
                                }
                            }
                        }
                    }
                }
            }
            if (wantsToCancel)
            {
                status = "Canceled by user";
            }
            else
            {
                makeImage();
                status = "Success";
            }
            // timing info
            double elapsedSec = (System.currentTimeMillis() - startMs) / 1000.;
            long bytes = (long) (duration * sampleRate * Float.SIZE/8);
            double rateKBps = bytes/elapsedSec/1000;
            System.out.format("Run time: %1$.2fs, total bytes xfer: %2$,d, process rate %3$.1f KBps%n",
                                        elapsedSec,bytes,rateKBps);
            System.out.format("Single transfer time: %1$.1fs, multi: %2$.1fs rate %3$.0fKb/s%n",
                              singleBufMs/1000., multipleBufMs/1000.,
                              bytes/(double)(singleBufMs + multipleBufMs));
            System.out.format("#imgPos ODC (x0,y0, w,h, strt, dur): %1$d, %2$d, %3$d, %4$d, %5$d, %6$d,%n",
                              imgX0, imgY0, dimX, dimY, startGPS, duration);
        }
        catch(LdvTableException | SQLException | IOException ex)
        {
            status = "Error: " + ex.getLocalizedMessage();
            System.err.println(ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        closeProgress();
    }
   
    /**
     * Using previously set up object members verify the channel and get needed info
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
            if (server == null || server.isEmpty())
            {
                n = chanTbl.getBestMatch(channelName);
                chanInfo = chanTbl.getChanInfo(n);
            }
            else
            {
                TreeSet<ChanInfo> chSet = chanTbl.getAsSet(server, channelName, "raw", 10);
                if (chSet.size() > 1)
                {
                    System.err.println("Warning: more than one raw channel matches: " + channelName);
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
            String sRateStr = sampleRate < 1 ? String.format("%1$.3f", sampleRate) : 
                    String.format("%1$.0f", sampleRate);
            System.out.format("\nChan: %1$s, sample rate: %2$s, bytes per sample: %3$d\n",
                              channelName, sRateStr, bytesPerSample);
            float dur = (System.currentTimeMillis() - strt) / 1000.f;
            System.out.format("Get channel info took %1$.1f sec.\n", dur);
        }
        return ret;
    }

    private int bufn = 0;
    /**
     * Given a buffer of real data add it to our data table
     * @param data - the data we generate the image from
     * @param buffer - nds2 buffer object
     * @param strtSampleNum - starting position in samples
     */
    private void addBuf(byte[][] data, int strtSampleNum, int len) throws NDSException, IOException
    {
        
        bufn++;
//        System.out.println(String.format("%1$d, strt: %2$d, end: %3$d sample: %4$d",
//                                         bufn, firstCol, lastCol, strtSampleNum));
        
        for(int ip=0;ip<len;ip++)
        {
            int col = (int) Math.round((strtSampleNum+ip) * sample2colFact);
            col = col >= dimX ? dimX-1 : col;
            long d;
            if (inStream != null)
            {
               Float t = inStream.readFloat();
               Float v = inStream.readFloat();
               d = (long) Math.round(v);
            }
            else
            {
                d = ndsClient.getNextLong();
            }
            getBits(d);
            for(int dups=0;dups<colPerSample;dups++)
            {
                for(int b=0;b<nbits;b++)
                {
                    data[col + dups][b] |= bits[b];
                }
            }
        }
    }

    private void noData(byte[][] data, int strtSampleNum, int stopSampleNum,byte val)
    {
        long len = stopSampleNum - strtSampleNum;
        bufn++; 
        int strtCol = (int) Math.round(strtSampleNum*sample2colFact);
        int stopCol = (int) Math.round(stopSampleNum*sample2colFact);
        stopCol = stopCol >= dimX ? dimX-1 : stopCol;
        
        for(int col=strtCol; col <= stopCol; col++)
        {
            for(int b=0;b<nbits;b++)
            {
                data[col][b] = val;
            }
        }
    }
    /**
     * Separate the bits in a sample into the field bits
     * 3 bits of the byte are used
     * 0 - false
     * 1 - true
     * 2 - parity error
     * @param d input sample
     * @return true if odd parity
     */
    private boolean getBits(long d)
    {
        boolean ret = true;
        int sum=0;
        int it;
        for(int i=0;i<bitMask.length;i++)
        {
            it = (d & bitMask[i]) != 0 ? 1 : 0;
            sum += it;
            bits[i] = (byte) (it == 0 ? 1 : 2);
        }
        if (sum % 2 == 0 && checkParity)
        {
            ret = false;
            for (int i = 0; i < 24; i++)
            {
                
                bits[i] |= 4;
            }
        }
        return ret;
    }
    // Image generation from data array
    private WritableRaster rast;        // image pixels of the colored plot section
    private BufferedImage img;          // output image
    private Graphics2D grph;            // Graphics context of image
    private Font lblFont;               // the font we're using to make labels
    private Font titleFont;             // guess what we're going to use that one for
    private int imgX0,imgY0;            // origin of the plot section
    FontMetrics labelMetrics;           // used for positioning and sizing labels
    
    
    /**
     * Generate and write the output image
     * @throws IOException some problem with the file writing
     */
    private void makeImage() throws IOException
    {
        int bheight = dimY/nbits;   // # of pixels high for each bit
        byte[] bitColor = new byte[bheight];
        for(int b = 0; b<nbits; b++)
        {
            String bName = b<bitNames.length ? bitNames[b] : "";
            if (! bName.isEmpty())
            {
                int sy = bheight * b;
                for(int x=0;x<dimX;x++)
                {
                    byte bval = data[x][b];
                    for(int i=0;i<bheight;i++)
                    {
                        bitColor[i] = bval;
                    }
                    rast.setDataElements(x + imgX0, sy + imgY0, 1, bheight, bitColor);
                }
            }
        }
        finalizeImage();
    }
    /**
     * Generate a Index Color Model with our color coding and establish text for color legend (if enabled)
     * 
     * @return 
     */
    private IndexColorModel getColorModel()
    {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        int rd,gr,bl;
        
       
        for (int i = 0; i < 256; i++)
        {
            r[i] = g[i] = b[i] = 0;

            if (i < keyColors.length)
            {
                rd = keyColors[i].getRed();
                gr = keyColors[i].getGreen();
                bl = keyColors[i].getBlue();
            }
            else
            {
                int intensity = Math.round((i-8)/255.f * 255);
                rd = gr = bl = intensity;
            }

            // deal with signed bytes and unsigned 8-bit index colors
            r[i] |= rd & 0xff;
            g[i] |= gr & 0xff;
            b[i] |= bl & 0xff;
        }
        IndexColorModel ret = new IndexColorModel(8, 256, r, g, b);
        return ret;
    }

    private void initImage()
    {
        img = new BufferedImage(outX, outY, BufferedImage.TYPE_BYTE_INDEXED,getColorModel());
        grph = img.createGraphics();
        grph.setBackground(Color.white);
        grph.clearRect(0, 0, outX, outY);
        if (outX < 600)
        {
            lblFontSize = 9;
            titleFontSize = 11;
            addLegend = false;
            addGPS = false;
        }
        else if (outX < 800)
        {
            lblFontSize = 12;
            titleFontSize = 14;
            addLegend = true;
            addGPS = true;
        }
        else if (outX < 1025)
        {
            lblFontSize = 14;
            titleFontSize = 16;
            addLegend = true;
            addGPS = true;
        }
        else
        {
            lblFontSize = 24;
            titleFontSize = 32;
            addLegend = true;
            addGPS = true;
        }        
        
        
        String title;
        String utcDate = TimeAndDate.gpsAsUtcString(startGPS);
        String hrTime = TimeAndDate.hrTime(duration);
        String cname;
        if (useTestData)
        {
            cname = String.format("Testing at %1$.f Hz", sampleRate);
        }
        else
        {
            cname = channelName;
        }
        title = String.format("%1$s %2$s UTC (%3$,d) t=%4$s", cname, utcDate, startGPS, hrTime);

        lblFont = new Font("Liberation Sans", Font.PLAIN, lblFontSize);
        titleFont = new Font("Liberation Serif", Font.BOLD, titleFontSize);
        FontMetrics titleMetrics = grph.getFontMetrics(titleFont);
        labelMetrics = grph.getFontMetrics(lblFont);
        
        titleHeight = titleMetrics.getHeight();
        int titleWidth = titleMetrics.stringWidth(title);
        
        while (titleWidth > outX && lblFontSize > 7)
        {
            titleFontSize --;
            lblFontSize--;
            lblFont = new Font("Dialog", Font.PLAIN, lblFontSize);
            titleFont = new Font("Serif", Font.BOLD, titleFontSize);
            titleMetrics = grph.getFontMetrics(titleFont);
            labelMetrics = grph.getFontMetrics(lblFont);
            
            titleHeight = titleMetrics.getHeight();
            titleWidth = titleMetrics.stringWidth(title);
            
        }

        imgY0 = titleHeight + titleFontSize/2;
        
        lblHeight = labelMetrics.getHeight();
        int lblWidth = Integer.MIN_VALUE;
        for(String lbl : bitNames)
        {
            int lw = labelMetrics.stringWidth(lbl);
            lblWidth = Math.max(lblWidth, lw);
        }
        imgX0 = lblWidth + 10;
        dimX = outX - imgX0 - 5;
        int bLblLines = addLegend ? 4 : 3;
        bLblLines = addGPS ? bLblLines + 1 : bLblLines;
        
        dimY = outY - imgY0 - lblHeight * bLblLines;
        
        int nsamples = (int) (duration * sampleRate);
        colPerSample = 1;
        
        if (nsamples < dimX)
        {
            // we have more pixels available than we have samples so making it pretty takes some work
            colPerSample = dimX / nsamples;
            dimX=colPerSample * nsamples;
        }
        sample2colFact = (double) (dimX - 1) / (double) nsamples;
        // create and clear the data buffer
        data = new byte[dimX][maxBits];
        for (int x = 0; x < dimX; x++)
        {
            for (int y = 0; y < maxBits; y++)
            {
                data[x][y] = 0;
            }
        }
        int tx;
        if (titleWidth < dimX)
        {
            tx = (dimX - titleWidth) / 2 + imgX0;
        }
        else if (titleWidth < outX)
        {
            tx = (outX - titleWidth) / 2;
        }
        else
        {
            tx = 0;
        }
        tx = tx < 0? 0 : tx;
        tx = tx + titleWidth > outX ? 0 : tx;
        
        int ty = 5 + titleMetrics.getAscent();
        grph.setPaint(Color.BLACK);
        grph.setFont(titleFont);
        grph.drawString(title, tx, ty);
        rast = img.getRaster();
    }
    /**
     * Fill out the result image with the plot, bit names, time axis and legend
     * @throws IOException - must be a problem writing out the result file
     */
    private void finalizeImage() throws IOException
    {
        img.setData(rast);  // copy the image raster
        addColorLegend();
        
        // add the bit names
        int bheight = dimY/nbits;   // # of pixels high for each bit
        int lasc = labelMetrics.getAscent();
        int imgXmax = imgX0+dimX;
        for(int b=0;b<bitNames.length;b++)
        {
            String bname = bitNames[b];
            bname = bname.isEmpty() ? "Unused" : bname;
            int bw = labelMetrics.stringWidth(bname);
            int bx = imgX0 - 2 - bw;
            int by = bheight * b + (lasc + bheight)/2 + imgY0;
            grph.drawString(bname, bx, by);
            int y=bheight*b + imgY0;
            grph.drawLine(1, y, imgXmax, y);
        }
        int y = bitNames.length * bheight + imgY0;
        grph.drawLine(1, y, imgXmax, y);
        grph.drawLine(1, imgY0, 1, y);
        
        // time axis labels, ticks & grid
        int tw = dimX/xTicks;
        float dt = duration/((float)xTicks);
        String fmt = "%1$.0f";
        if (duration < xTicks)
        {
            fmt = "%1$.2f";
        }
        else if (duration < xTicks * 2)
        {
            fmt = "%1$.1f";
        }
        int yp = y + 5 + lasc;
        int lstGpsEnd = 0;
        
        for(int t = 0;t<=xTicks;t++)
        {
            int x = t * tw + imgX0;
            long tsec = Math.round(t * dt);
            String xLbl;
            if (duration > 5)
            {
                xLbl = TimeAndDate.hrTime(tsec);
            }
            else
            {
                xLbl = String.format("%1$.2f", t*dt);
            }
            int xw = labelMetrics.stringWidth(xLbl);
            int xp =  x-xw/2;
            if (t == xTicks)
            {   // make sure the last tick is at the edge (round off errors)
                // and the label ends there since we can't center it
                x=imgXmax;
                xp = x - xw;
            }
            grph.drawLine(x, imgY0, x, y+3);
            grph.drawString(xLbl, xp, yp);
            
            // add gps time on every other tick
            
            
            if (t % 2 == 0 && addGPS)
            {
                String gpsLbl = String.format("%1$,d", startGPS + tsec);
                int gpsLblLen = labelMetrics.stringWidth(gpsLbl);
                if (t == xTicks)
                {
                    xp = x - gpsLblLen;
                }
                else
                {
                    xp = x - gpsLblLen / 2;
                }
                if (xp > lstGpsEnd + 5)
                {
                    grph.drawString(gpsLbl,xp, yp + lblHeight + 3);
                    lstGpsEnd = xp + gpsLblLen;
                }
            }
        }
        // aux info
        yp += lblHeight * 7 / 3;
        int imgCenter = (imgXmax - imgX0)/2;
        int nsamples = (int) (duration*sampleRate);
        float spp = ((float)duration)/dimX;
        String auxInfo = String.format("Fs=%,.0fHz, n=%,d, ", 
                                       sampleRate, nsamples);
        int lspp = (int) Math.round(Math.log10(spp));
        int nd = lspp < 0 ? (-lspp + 1) : lspp < 1 ? 2 : 1;
        String sfmt = String.format("pixel=%%.%df sec",nd);
        auxInfo += String.format(sfmt,spp);
        
        int xw = labelMetrics.stringWidth(auxInfo);
        int xp = imgCenter - xw/2 + imgX0;
        
        grph.drawString(auxInfo, xp, yp);
        
        // draw some edge markers
        grph.drawLine(0, 0, 0, 5);
        grph.drawLine(0, 0, 5, 0);
        
        grph.drawLine(outX-1, outY-1, outX-1, outY-6);
        grph.drawLine(outX-1, outY-1, outX-6, outY-1);
        
        // write the file
        File outputfile = new File(ofileName);
        ImageIO.write(img, "png", outputfile);
        System.out.println("Wrote output to: " + ofileName);
    }
    private void addColorLegend()
    {
        String[] clrLeg =
        {
            "", "Bad", "Good", "Bad & good", "", "Parity & bad", "Parity & good",
            "Parity & good & bad", "NDS data missing"
        };
        String[] clrLegShort =
        {
            "", "Bad", "Good", "Good+bad", "", "Parity Err", "PE-2",
                "PE-3", "No NDS data"
        };
        String[] colorLegend = clrLeg;
        
        // Add a legend
        int lyp = outY - lblHeight / 2;
        int byp = (int) Math.round(lyp - lblHeight * .75);
        int boxWidth = lblHeight;
        int lxps = 5;
        int lxp = lxps;
        int lxpe = lxp;
        int[] legTxtPos = new int[colorLegend.length];
        int loffset = labelMetrics.stringWidth(" ");
        int roffset = labelMetrics.stringWidth("M");
        boolean condensed = false;
        
        if (addLegend)
        {
            lxp = lxps;
            lxpe = lxp;
            for (int idx = 0; idx < colorLegend.length; idx++)
            {
                legTxtPos[idx] = 0;
                String colorLbl = colorLegend[idx];
                if (!colorLbl.isEmpty() && (checkParity || !colorLbl.toLowerCase().contains("parity") ))
                {                    
                    int lblLen = labelMetrics.stringWidth("- " + colorLbl);
                    legTxtPos[idx] = lxp;
                    lxp += boxWidth + lblLen + loffset + roffset * 3;
                    lxpe = lxp;
                }
            }
        }

        // center the color legend over whole image
        if (lxpe - lxps <= outX)
        {
            int cx = (outX - lxps - lxpe) / 2;
            for(int i=0;i<legTxtPos.length;i++)
            {
                if (legTxtPos[i] > 0)
                {
                    legTxtPos[i] += cx;
                }
            }
        }
        else
        {
            // see if we can fit a "condensed" label
            colorLegend = clrLegShort;
            lxp = lxps;
            lxpe = lxp;
            boxWidth = lblHeight/2;
            for (int idx = 0; idx < colorLegend.length; idx++)
            {
                legTxtPos[idx] = 0;
                String colorLbl = colorLegend[idx];
                if (!colorLbl.isEmpty())
                {
                    int lblLen = labelMetrics.stringWidth("- " + colorLbl);
                    legTxtPos[idx] = lxp;
                    lxp += boxWidth + lblLen + loffset + roffset * 2;
                    lxpe = lxp;
                }
            }
            if (lxpe - lxps <= outX)
            {
                int cx = (outX - lxps - lxpe) / 2;
                for (int i = 0; i < legTxtPos.length; i++)
                {
                    if (legTxtPos[i] > 0)
                    {
                        legTxtPos[i] += cx;
                    }
                }
                condensed = true;
            }
            else
            {
                addLegend = false;
            }
        }
        grph.setFont(lblFont);
        

        if (addLegend)
        {
            // Add the text to the legend
            for (int idx = 0; idx < colorLegend.length; idx++)
            {
                String colorLbl = colorLegend[idx];
                if (!colorLbl.isEmpty())
                {
                    lxp = legTxtPos[idx];
                    grph.setColor(keyColors[idx]);
                    if (condensed)
                    {
                        grph.fillOval(lxp, byp, lblHeight/2, lblHeight);
                        
                        grph.setColor(Color.BLACK);
                        grph.drawOval(lxp, byp, lblHeight/2, lblHeight);
                    }
                    else
                    {
                        grph.fillRoundRect(lxp, byp, lblHeight, lblHeight, lblHeight / 3, lblHeight / 3);

                        grph.setColor(Color.BLACK);
                        grph.drawRoundRect(lxp, byp, lblHeight, lblHeight, lblHeight / 3, lblHeight / 3);
                    }
                    grph.drawString("- " + colorLbl, lxp + lblHeight + loffset, lyp);
                }
            }
        }
    }

    /**
     * Process the command line arguments and set fields 
     * @param args the main passed in arguments
     * @return true if we continue false means exit (didn't want to do it here)
     */
    private boolean processArgs(String[] args)
    {
        boolean ret = true;
        
        Options options = new Options();
        Option helpOpt = new Option("help", "print this message");
        Option verOpt = new Option("version", "print the version information and exit");
        Option prgOpt = new Option("progress", "show graphical progress bar");
        Option nodOpt = new Option("nodata", "don't get data, show lables only");
        
        Option outOpt = OptionBuilder.withArgName("out").hasArg().withDescription("output (png) base of filename").create("outfile");
        Option geomOpt = OptionBuilder.withArgName("geometry").hasArg().withDescription("image size <X>x<Y>").create("geom");
        Option chanOpt = OptionBuilder.withArgName("channel").hasArg().withDescription("channel <name,type>").create("chan");
        Option srvOpt = OptionBuilder.withArgName("server").hasArg().withDescription("server <URL>").create("server");
        Option filOpt = OptionBuilder.withArgName("file").hasArg().withDescription("filename").create("file");
        Option strtOpt = OptionBuilder.withArgName("start").hasArg().withDescription("GPS start time").create("start");
        Option durOpt = OptionBuilder.withArgName("duration").hasArg().withDescription("duration (seconds)").create("dur");
        Option xtkOpt = OptionBuilder.withArgName("xticks").hasArg().withDescription("tick marks/grid lines on time axis").create("xticks");
        
        options.addOption(helpOpt);
        options.addOption(verOpt);
        options.addOption(outOpt);
        options.addOption(geomOpt);
        options.addOption(chanOpt);
        options.addOption(strtOpt);
        options.addOption(durOpt);
        options.addOption(prgOpt);
        options.addOption(srvOpt);
        options.addOption(filOpt);
        options.addOption(xtkOpt);
        options.addOption(nodOpt);

        CommandLineParser parser = new GnuParser();
        CommandLine line;
        boolean wantHelp = false;
        try
        {
            // parse the command line arguments
            line = parser.parse(options, args);
        }
        catch (ParseException exp)
        {
            // oops, something went wrong
            System.err.println("Command parsing failed.  Reason: " + exp.getMessage());
            wantHelp = true;
            line = null;
        }
        String ermsg = "";
        
        if (line != null)
        {
            if (line.hasOption("version"))
            {
                System.out.println(programName + " - version " + version);
                ret = false;
            }
            
            showProgressBar = line.hasOption("progress");
            useTestData = line.hasOption("nodata");
            
            if (line.hasOption("outfile"))
            {
                ofileName = line.getOptionValue("outfile");
            }
            String val;
            String intpat = "^\\d+$";
            Matcher m;
            
            if (line.hasOption("geom"))
            {
                val = line.getOptionValue("geom");
                Pattern gp = Pattern.compile("(\\d+)x(\\d+)");
                m = gp.matcher(val);
                if (m.find())
                {
                    String sx = m.group(1);
                    String sy = m.group(2);
                    if (sx.matches(intpat) && sy.matches(intpat))
                    {
                        outX = Integer.parseInt(sx);
                        outY = Integer.parseInt(sy);
                    }
                    else
                    {
                        ermsg += "can't parse geometry value (" + val + ")";
                        wantHelp = true;
                        ret = false;
                    }
                }
                else
                {
                    ermsg += "can't parse geometry value (" + val + ")";
                    wantHelp = true;
                    ret = false;
                }
            }
            
            if (line.hasOption("chan"))
            {
                channelName = line.getOptionValue("chan");
            }
            server="";
            if (line.hasOption("server"))
            {
                server = line.getOptionValue("server");
            }
            infilename="";
            if (line.hasOption("file"))
            {
                infilename = line.getOptionValue("file");
            }
            if (line.hasOption("start"))
            {
                val = line.getOptionValue("start");
                if (val.matches(intpat))
                {
                    startGPS = Integer.parseInt(val);
                }
                else
                {
                    ermsg += "can't parse start value (" + val + ")";
                    wantHelp = true;
                    ret = false;
                }
            }
            if (line.hasOption("dur"))
            {
                val = line.getOptionValue("dur");
                if (val.matches(intpat))
                {
                    duration = Integer.parseInt(val);
                }
                else
                {
                    ermsg += "can't parse duration value (" + val + ")";
                    wantHelp = true;
                    ret = false;
                }
            }
            if (line.hasOption("xticks"))
            {
                val = line.getOptionValue("xticks");
                if (val.matches(intpat))
                {
                    xTicks = Integer.parseInt(val);
                }
                else
                {
                    ermsg += "can't parse xticks value (" + val + ")";
                    wantHelp = true;
                    ret = false;
                }
            }
        }
        if (!ermsg.isEmpty())
        {
            System.out.println("Command error:\n" + ermsg);
        }
        if (wantHelp || line.hasOption("help"))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName, options);
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
            String title = String.format("%s, at %s", channelName, TimeAndDate.gpsAsUtcString(startGPS));
            progressBar.setChanName(title);
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
            String cur = String.format("%1$d of %2$d seconds of data", dt,duration);
            int pct = Math.round(dt*100.f/duration);
            double elapsed = (System.currentTimeMillis()-startMs)/1000.;
            double remaining;
            String etl;
            if (dt > 0)
            {
                remaining = elapsed * duration/dt - elapsed;
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
    private void setProgress(String what)
    {
        if (progressBar != null)
        {
            double elapsed = (System.currentTimeMillis()-startMs)/1000.;
            String etl = String.format("Elapsed: %4.0f",elapsed);
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
    
    private void getBitNames(String chan)
    {
        
        String[] pslOdcBitNames =
        {
            "Summary",
            "ISS loopstate",
            "Photodiode A saturation",
            "Photodiode B saturation",
            "Transfer function 1 injection",
            "Transfer function 2 injection",
            "Transfer function 1B test",
            "RES_MON test",
            "OSC locked state",
            "MIXMOD test",
            "TPD test",
            "FSS Oscillation Test",
            "",
            "FSS loopstate",
            "FSS resonnance test",
            "RFPD test"
        };

        String[] lldDQBitnames =
        {
            "Science mode",
            "ITF fully locked",
            "h(t) reconstruction ok",
            "",
            "CAT1: data passes CAT1 online check ",
            "CAT1 Status known",
            "CBC injection off",
            "CBC Injection known state",
            "Burst injection off",
            "Burst Injection known state",
            "CW injection off ",
            "CW Injection known state",
            "Stochastic injection off",
            "Stochastic Injection known state",
            "Data passes MBTA CAT2 online",
            "MBTA CAT2 known state"            
        };
        String[] sus1Names =    // for Channels matching SUS-QUAD*, SUS-ITMX*, SUS-ETMX*
        {
            "Summary",
            "Master Switch ON/OFF Status",
            "DACKILL Status",
            "M0 WatchDog Status",
            "R0 WatchDog Status",
            "L1 (UIM)WatchDog Status",
            "L2 (PUM)WatchDog Status",
            "L3 (TST)WatchDog Status",
            "M0 DAMP State",
            "R0 DAMP State",
            "L1 DAMP State",
            "L2 DAMP State",
            "LOCK State"
        };
        String[] sus2Names =    // for Channels matching SUS-MC*, SUS-PR*, SUS-SR* and SUS-BS*
        {
            "Summary",
            "Master Switch ON/OFF Status",
            "DACKILL Status",
            "M1 WatchDog Status",
            "M2 WatchDog Status",
            "M3 WatchDog Status",
            "M1 DAMP State",
            "LOCK State"
        };
        String[] isiNames =     // for the ISI models
        {
            "Summary",
            "ST1 DAMP",
            "ST1 Isolation",
            "ST2 DAMP",
            "ST2 Isolation",
            "Master Switch",
            "ST1 WatchDog",
            "ST2 WatchDog"
        };
        
        String[] imcNames =     // for imc models
        {
            "Summary Bit",
            "Control System Req On",
            "DOF1 PIT OK",
            "DOF2 PIT OK",
            "DOF3 PIT OK",
            "DOF4 PIT OK",
            "DOF1 YAW OK",
            "DOF2 YAW OK",
            "DOF3 YAW OK",
            "DOF4 YAW OK",
            "MC2 Transmitted High",
            "MC2 Trans High (Relative)",
            "IM4 Trans High (Relative)",
            "IM4 Transmitted High"
        };
        
        String cn = chan.toUpperCase();
        
        if (cn.contains("PSL-ODC_CHANNEL_OUTPUT") || cn.contains("PSL-ODC_CHANNEL_OUT_DQ"))
        {
            checkParity = true;
            bitNames = pslOdcBitNames;
        }
        else if (cn.contains("LLD-DQ_VECTOR"))
        {
            checkParity = false;
            bitNames = lldDQBitnames;
        }
        else if (
                   cn.contains("SUS-QUAD") || cn.contains("SUS-ITMX") || cn.contains("SUS-ETMX")
                || cn.contains("SUS-ITMY") || cn.contains("SUS-ETMY") || cn.contains("SUS-ITMY") 
                || cn.contains("HPI-ETMY")
                )
        {
            checkParity = true;
            bitNames = sus1Names;
        }
        else if (cn.matches(".*SUS-MC.*") || cn.matches(".*SUS-PR.*")  
                 || cn.matches(".*SUS-SR.*")  || cn.matches(".*SUS-BS.*"))
        {
            checkParity = true;
            bitNames = sus2Names;
        }
        else if (cn.matches(".*ISI-.*ODC_CHANNEL_OUT_DQ"))
        {
            checkParity = true;
            bitNames = isiNames;
        }
        else if (cn.matches(".*IMC-ODC_CHANNEL_OUT.*"))
        {
            checkParity = true;
            bitNames = imcNames;
        }
        else
        {
            checkParity = false;
            bitNames = new String[maxBits];
            for(int i=0;i<nbits;i++)
            {
                bitNames[i] = String.format("Bit %1$02d",i);
            }
        }
        nbits = bitNames.length;
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

    /**
     * Given the segment we tried to read and the error message decide if it's a gap or more serious problem
     * @param curgps - segment start
     * @param curDur - segment duration
     * @param msg - text from the exception
     * @return true if it's a gap false if it's serious ie. should we keep reading
     */
    private boolean checkNoData(int curgps, int curDur, String msg)
    {
        boolean ret = false;
        if (msg.toLowerCase().contains("requested data were not")
            || msg.toLowerCase().contains("no such channel")
            || msg.toLowerCase().contains("read timed out")
            || msg.toLowerCase().contains("unknown error")
                )
        {
            int strt = (curgps - startGPS);
            int strtSample = (int) (strt * sampleRate);
            int stopSample = (int) ((strt + curDur) * sampleRate - 1);
            noData(data, strtSample, stopSample, NODATA);
            ret = true;
        }
        return ret;
    }

    /**
     * For binary input files from our frame reader set up the input streadm
     * @return number of entries to read
     * @throws WebUtilException 
     */
    private int setupFileReads() throws WebUtilException
    {
        File inFile = new File(infilename);
        int siz = (int) (inFile.length()/(Float.SIZE/8)/2);     // convert bytes to # entries (time, val)
        if (!inFile.canRead())
        {
            throw new WebUtilException("Can't open " + infilename + " for reading");
        }
        try
        {
            inStream = new SwappedDataInputStream ( new FileInputStream(inFile));
            float minTime=Float.MAX_VALUE;
            float maxTime = - Float.MAX_VALUE;
            
            for(int i = 0 ; i< siz; i++)
            {
                Float t = inStream.readFloat();
                Float d = inStream.readFloat();
                minTime = Math.min(minTime, t);
                maxTime = Math.max(maxTime, t);
            }
            startGPS = (int) (minTime * 24 * 3600);
            duration = (int) ((maxTime - minTime) * 24 * 3600);
            inStream.close();
            inStream = new SwappedDataInputStream ( new FileInputStream(inFile));
        }
        catch (IOException ex)
        {
            throw new WebUtilException("Can't open " + infilename + " for reading");
        }
        
        return siz;
    }

}
