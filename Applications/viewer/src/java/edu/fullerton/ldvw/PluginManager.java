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
package edu.fullerton.ldvw;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.*;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.ImageCoordinate;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import edu.fullerton.ldvplugin.CoherenceManager;
import edu.fullerton.ldvplugin.LivePlotManager;
import edu.fullerton.viewerplugin.GUISupport;
import edu.fullerton.ldvplugin.OdcPlotManager;
import edu.fullerton.viewerplugin.PlotProduct;
import edu.fullerton.ldvplugin.SpectrogramManager;
import edu.fullerton.viewerplugin.SpectrumPlot;
import edu.fullerton.viewerplugin.TsPlot;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ldvtables.ImageCoordinateTbl;
import edu.fullerton.ldvtables.ImageGroupTable;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.TimeInterval;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.GDSFilter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletResponse;

/**
 * manages the products we supply. Creates forms for the client to select products and set options.
 * Interprets returned form data. Calls the products whether internal classes or external plug ins.
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class PluginManager extends GUISupport
{

    private int width = 640, height = 480;      // default image dimensions
    private boolean compact = false;            // small enough format to shorten labels?
   
    private final ImageTable imgTbl;
    
    // auto refresh related
    private final String arKey = "autoRefreshCount";
    // preprocessing options, done as part of getData, before anybody sees it.
    
    private boolean doDetrend;      // detrend raw data
    private final boolean enablePrefilter = false;    // Should UI have filter option?
    private boolean doPrefilter;    // filter the input data
    private String filtType;        // high or low pass
    private float cutoff;           // frequency cutoff in cycles/sample
    private int order;              // butterworth order
    private double[] filtXferFn;    // frequency domain transfer function if we're going to undo the effects
    
    private final boolean allowTestData = false;  // Should UI have test data options?
    private final ArrayList<Integer> imageIDs;    // list of images we produced
   
    private HttpServletResponse response;
    
    public PluginManager(Database db, Page vpage, ViewUser vuser,
            Map<String, String[]> pMap) 
            throws SQLException
    {
        super(db, vpage, vuser);
        paramMap = pMap;
        imgTbl = new ImageTable(db);
        imageIDs = new ArrayList<>();
    }

    public void setResponse(HttpServletResponse response)
    {
        this.response = response;
    }
    
    /**
     * Return selection forms for all known plot types
     * 
     * @param nSel - some options depend on how many series are selected
     * @return - the thing to add to the page
     * @throws WebUtilException probably a bug in specifying html objects
     */
    PageItemList getSelector(ArrayList<ChanInfo> selectedCInfo, Integer stepNo) throws WebUtilException
    {
        int nSel = selectedCInfo.size();
        
        String[] multDisp =
        {
            "Stacked", "single"
        };
        
        String[] plotSize =
        {
            "Small (320x240)", "Medium (640x480)", "Large (1280x960)", "Wide but short (1280x300)",
            "Larger (1920x1080)", "Huge (2200x1400)"
        };

        ArrayList<PageFormSelect.Option> testDataOptions = new ArrayList<>();
       
        testDataOptions.add(new PageFormSelect.Option("Use real data", "none", true));
        testDataOptions.add(new PageFormSelect.Option("Impulse", "impulse", false));
        testDataOptions.add(new PageFormSelect.Option("Single sine f=Fs/4", "sine1", false));
        testDataOptions.add(new PageFormSelect.Option("3 sine (natural freq)", "sine3", false));
        testDataOptions.add(new PageFormSelect.Option("4 sine (odd freq)", "sine4", false));
        testDataOptions.add(new PageFormSelect.Option("Random", "random", false));

        String[] filterType = 
        {
            "None", "High Pass", "Low Pass"
        };
        PageItemList ret = new PageItemList();
        
        // prefiltering
        if (enablePrefilter)
        {
            addProcStep(ret, String.format("%1$d. Select optional prefiltering, if desired.",stepNo));
            stepNo++;
            PageTable prefiltTbl = new PageTable();
            
            PageTableRow prefilt;
            
            prefilt = new PageTableRow();
            prefilt.add("Prefilter:");
            prefilt.add(new PageFormSelect("prefilt", filterType));
            prefilt.setClassAll("noborder");
            prefiltTbl.addRow(prefilt);

            prefilt = new PageTableRow();
            prefilt.add("Cutoff (Hz):");
            prefilt.add(new PageFormText("Cutoff", "",6));
            prefilt.setClassAll("noborder");
            prefiltTbl.addRow(prefilt);

            prefilt = new PageTableRow();
            prefilt.add("Order:");
            prefilt.add(new PageFormText("Order", "4",4));
            prefilt.setClassAll("noborder");
            prefiltTbl.addRow(prefilt);
            
            ret.add(prefiltTbl);
        }

        if (allowTestData)
        {
            // substitute test data
            PageItemList testData = new PageItemList();
            testData.add("Substitute test data: ");
            testData.add(new PageFormSelect("testData",testDataOptions));
            
        }

        //========== Individual Plot Products================
        addProcStep(ret, String.format("%1$d. Select one or more plot types",stepNo));
        stepNo++;
        
        PageItemList pfDiv = new PageItemList();
        pfDiv.setId("accordion");
        
        // Add Time series
        TsPlot tsp = new TsPlot();
        PageItemList tspPil = getSelectorContent(tsp,"doTimeSeries",nSel,multDisp);
        tspPil.setUseDiv(false);
        pfDiv.add(tspPil);
        
        // add Spectrum
        SpectrumPlot sp = new SpectrumPlot();
        PageItemList spPil = getSelectorContent(sp, "doSpectrum", nSel, multDisp);
        spPil.setUseDiv(false);
        pfDiv.add(spPil);
        
        // add Spectrogram
        SpectrogramManager spgm = new SpectrogramManager( db, vpage, vuser);
        PageItemList spgPil = getSelectorContent(spgm,"doSpectrogram",nSel, multDisp);
        spgPil.setUseDiv(false);
        pfDiv.add(spgPil);
        
        // add Coherence
        CoherenceManager chm = new CoherenceManager(db,vpage,vuser);
        chm.setChanList(selectedCInfo);
        PageItemList chmPil = getSelectorContent(chm, "doCoherence", nSel, multDisp);
        chmPil.setUseDiv(false);
        pfDiv.add(chmPil);
        
        //========= put new products above this line=========
        
        // add the products and set them up as a closed accordion
        ret.add(pfDiv);
        String accScript = "jQuery( \"#accordion\").accordion({ collapsible: true,active: false, heightStyle: \"content\" });";
        vpage.addReadyJS(accScript);

        // Allow them to set the size of the plot
        addProcStep(ret, String.format("%1$d. Select plot size",stepNo));
        stepNo++;
        
        PageTable dispOptions = new PageTable();
        PageItemList pltSize = new PageItemList();
        pltSize.add("Plot size: ");
        PageFormSelect psizSel = new PageFormSelect("plotSize", plotSize);
        psizSel.setSelected(plotSize[1]);
        pltSize.add(psizSel);
        PageTableRow pltSizeRow = new PageTableRow();
        pltSizeRow.setClassName("noborder");
        pltSizeRow.add(pltSize);
        dispOptions.addRow(pltSizeRow);
        ret.add(dispOptions);

        return ret;
    }

    /**
     * Process the form that specifies which products at which times to create
     *
     * @return true if the caller is to send html, false if we sent the response (like an image or
     * movie)
     * @throws WebUtilException
     */
    public boolean doPlots() throws WebUtilException
    {
        boolean ret = true;
        boolean downloadRequested=false;

        try
        {
            
            HashSet<Integer> selections = getSelections();
            boolean noxfer = false;     // if nobody needs data we won't transfer it
            
            //===========what do they want to do?  ie. which products============
            String[] allProducts =
            {
                "doTimeSeries", "doSpectrum", "doSpectrogram", "doCoherence"
            };
            ArrayList<PlotProduct> selectedProducts = new ArrayList< >();

            Integer nSel = selections.size();
            Integer nProducts = 0;

            if (paramMap.containsKey("download"))
            {   // if downloading they don't get anything else
                nProducts = 1;
                downloadRequested = true;
            }
            else
            {
                boolean needsData = false;
                for (String s : allProducts)
                {
                    if (paramMap.containsKey(s))
                    {
                        nProducts++;
                        PlotProduct pp = getProduct(s);
                        selectedProducts.add(pp);
                        needsData |= pp.needsDataXfer();
                    }
                }
                noxfer = !needsData;
            }
            // ===========time intervals==============
            TimeAndPlotSelector tps = new TimeAndPlotSelector(db, vpage, vuser);
            tps.setContextPath(contextPath);
            tps.setParamMap(paramMap);

            ArrayList<TimeInterval> times = tps.getTimesFromForm();
            boolean gotProblem = false;
            if (nSel == 0)
            {
                vpage.add("No Channels were selected.");
                vpage.addBlankLines(1);
                gotProblem = true;
            }
            if (nProducts == 0)
            {
                vpage.add("No graphs were selected.");
                vpage.addBlankLines(1);
                gotProblem = true;
            }
            if (times.isEmpty())
            {
                vpage.add("No times were selected, or error parsing date/time.");
                vpage.addBlankLines(1);
                gotProblem = true;
            }
            setPlotSize(paramMap);
            String groupBy = "time";
            if (paramMap.containsKey("plotGroup"))
            {
                groupBy = paramMap.get("plotGroup")[0];
            }

            // ============preprocessing options===========
            doDetrend = paramMap.containsKey("doDetrend");
            prefilterSetup();

            //==========do what they ask for, or at least what we think they asked for=========
            if (!gotProblem)
            {
                if (downloadRequested)
                {
                    String[] dwnFmt = paramMap.get("dwnFmt");
                    String format = dwnFmt[0];   // one of these days there will be more options
                    ret = doDownload(selections,times,format);
                }
                else
                {
                    ret = callPlugins(selections, times, groupBy, selectedProducts, noxfer);
                    if (imageIDs.size()  >  0)
                    {
                        ImageGroupTable igt = new ImageGroupTable(db);
                        igt.deleteGroup(vuser.getCn(), "Last result");
                        for(Integer img : imageIDs)
                        {
                            igt.addToGroup(vuser.getCn(), "Last result", img);
                        }
                    }
                    int rptCnt=checkRefresh(selections, times);
                    if (rptCnt > 0)
                    {
                        String url = getMyUrl(rptCnt) + "&noHeader=1";
                        vpage.setAutoRefresh(1, url);
                    }
                }
            }
        }
        catch(WebUtilException | SQLException | IOException | NoSuchAlgorithmException | LdvTableException ex)
        {
            throw new WebUtilException("Creating plot:" + ex.getClass().getSimpleName() + ex.getLocalizedMessage());
        }
        return ret;
    }

    private void setPlotSize(Map<String, String[]> parameterMap)
    {
        String[] plotSizes = parameterMap.get("plotSize");


        if (plotSizes != null)
        {
            String it = plotSizes[0];
            int idx = it.indexOf(" ");
            if (idx > 0)
                {
                String psiz = it.substring(0, idx);

                if (psiz.equalsIgnoreCase("small"))
                {
                    width = 320;
                    height = 240;
                    compact = true;
                }
                else if (psiz.equalsIgnoreCase("large"))
                {
                    width = 1280;
                    height = 960;
                }
                else if (psiz.equalsIgnoreCase("wide"))
                {
                    width = 1280;
                    height = 300;
                }
                else if (psiz.equalsIgnoreCase("larger"))
                {
                    width = 1920;
                    height = 1080;
                }
                else if (psiz.equalsIgnoreCase("huge"))
                {
                    width = 2200;
                    height = 1400;
                }

            }
            else
            {
                Pattern psizPat = Pattern.compile("(\\d+)x(\\d+)");
                Matcher pmat = psizPat.matcher(it);
                if(pmat.find())
                {
                    width = Integer.parseInt(pmat.group(1));
                    height = Integer.parseInt(pmat.group(2));
                    
                    width = width < 128 ? 128 : width;
                    height = height < 96 ? 96 : height;
                }
            }
        }
    }

    /**
     * products and datasets have been defined, so make the graphs and send them to their browser
     *
     * @param selections set of channel #'s
     * @param times List of start time, duration
     * @param groupBy How we want stacked plots
     * @param selectedProducts List of what products we want
     * @return
     * @throws SQLException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws WebUtilException
     * @throws LdvTableException
     */
    private boolean callPlugins(HashSet<Integer> selections, ArrayList<TimeInterval> times, 
                                String groupBy, ArrayList<PlotProduct> selectedProducts,
                                boolean noxfer) 
            throws SQLException, IOException, NoSuchAlgorithmException, WebUtilException, LdvTableException
    {
        boolean ret = true;     // flag: if true viewer should send html
        int bufCount = 0;       // count of buffers we actually were able to obtain
        
        int nBuf = selections.size() * times.size(); // # buffers we want to obtain
        boolean gotStackable = false;
        
        for(PlotProduct product : selectedProducts)
        {
            gotStackable |= product.isStackable();
        }
        if (!gotStackable || groupBy.equalsIgnoreCase("all"))
        {
            ArrayList<ChanDataBuffer> data = getData(selections,times, noxfer);
            bufCount += data.size();
            if (data.size() > 0)
            {
                doAllPlots(selectedProducts,data);
            }
        }
        else if (groupBy.equalsIgnoreCase("time"))
        {
            ArrayList<TimeInterval> aTime = new ArrayList<>();
            
            for(TimeInterval ti :times)
            {
                aTime.clear();
                aTime.add(ti);
                ArrayList<ChanDataBuffer> data = getData(selections, aTime, noxfer);
                bufCount += data.size();
                if (data.size() > 0)
                {
                    doAllPlots(selectedProducts, data);
                }
            }
        }
        else if (groupBy.equalsIgnoreCase("channel"))
        {
            // Here we still want to group all trends on a single channel together
            ChannelTable chnTbl = new ChannelTable(db);
            
            TreeMap<String,HashSet<Integer>> byNames = new TreeMap<>();
            for(Integer sel : selections)
            {
                ChanInfo ci = chnTbl.getChanInfo(sel);
                if(ci.getcType().toLowerCase().contains("trend"))
                {
                    String name = ci.getChanName();
                    int dotPos = name.indexOf(".");
                    if (dotPos > 0)
                    {
                        String basename = name.substring(0, dotPos);
                        HashSet<Integer> clist = byNames.get(basename);
                        if (clist==null)
                        {
                            clist = new HashSet<>();
                        }
                        clist.add(sel);
                        byNames.put(basename, clist);
                    }
                    
                }
                else
                {
                    HashSet<Integer> clist = new HashSet<>();
                    clist.add(ci.getId());
                    byNames.put(ci.getChanName(), clist);
                }
            }
            
            // go through the group by trend or other channel
            for( Entry<String,HashSet<Integer>> ent : byNames.entrySet() )
            {
                ArrayList<ChanDataBuffer> data = getData(ent.getValue(), times, noxfer);
                bufCount += data.size();
                if (data.size() > 0)
                {
                    doAllPlots(selectedProducts, data);
                }
            }
        }
        else if (groupBy.equalsIgnoreCase("none"))
        {
            ArrayList<TimeInterval> aTime = new ArrayList<>();
            
            for(TimeInterval ti :times)
            {
                aTime.clear();
                aTime.add(ti);
                HashSet<Integer> aChan = new HashSet<>();
                for (Integer sel : selections)
                {
                    aChan.clear();
                    aChan.add(sel);
                    ArrayList<ChanDataBuffer> data = getData(aChan, aTime, noxfer);
                    bufCount += data.size();
                    if (data.size() > 0)
                    {
                        doAllPlots(selectedProducts, data);
                    }
                }
            }
        }
        else
        {
            PageItemString erMsg = new PageItemString("Sorry.  Unknown group by specified.");
            erMsg.addStyle("color", "red");
            erMsg.setAlign(PageItem.Alignment.CENTER);
            vpage.add(erMsg);
            vpage.addBlankLines(2);
        }
        if (bufCount == 0)
        {
            PageItemString erMsg = new PageItemString("Sorry.  No data is available to plot.");
            erMsg.addStyle("color", "red");
            erMsg.setAlign(PageItem.Alignment.CENTER);
            vpage.add(erMsg);
            vpage.addBlankLines(2);
        }
        return ret;
    }

    /**
     * Determine if this product is selected and if so extract its parameters
     *
     * @param p product name
     * @return a product object
     * @throws WebUtilException
     */
    private PlotProduct getProduct(String p) throws WebUtilException
    {
        PlotProduct ret;
        String dispFormat;
        String[] dispFormats;
        switch (p)
        {
            case "doTimeSeries":
                ret = new TsPlot();
                dispFormats = paramMap.get("dispFormat");
                break;
            case "doSpectrum":
                ret = new SpectrumPlot();
                dispFormats = paramMap.get("spdispFormat");
                break;
            case "doSpectrogram":
                ret = new SpectrogramManager(db, vpage, vuser);
                ret.setParameters(paramMap);
                dispFormats = paramMap.get("spgdispFormat");
                break;
            case "doCoherence":
                ret = new CoherenceManager(db, vpage, vuser);
                ret.setParameters(paramMap);
                dispFormats = paramMap.get("cohdispFormat");
                break;
            default:
                throw new WebUtilException("Unknown display product requested: " + p);
        }
        ret.setup(db, vpage, vuser);
        
        dispFormat = dispFormats == null ? "" : dispFormats.length > 0 ? dispFormats[0] : "";
        ret.setDispFormat(dispFormat);

        return ret;
    }

    private PageItemList addIntro(PlotProduct product, ArrayList<ChanDataBuffer> bufList) throws WebUtilException
    {
        PageItemList intro = new PageItemList();
        PageItemString pname = new PageItemString(product.getProductName());
        pname.addStyle("font-weight", "bold");
        intro.add(pname);
        intro.addBlankLines(1);
        for (ChanDataBuffer buf : bufList)
        {
            intro.add(buf.getChanInfo().toString() + ". ");
            String timeDescription = buf.getTimeInterval().getTimeDescription();
            intro.add(timeDescription);
            intro.addBlankLines(1);
        }
        
        return intro;
    }

    /**
     * User requested data be downloaded to their computer, not plotted by us
     * 
     * @param selections channels selected
     * @param times time interval requested
     * @param format format of the download "ligodv" weird matlab code, "csv" plain csv file
     * @return true if we did it successfully
     * @throws WebUtilException 
     */
    private boolean doDownload(Set<Integer> selections, List<TimeInterval> times, String format) 
            throws WebUtilException
    {
        boolean ret = true;
        int n = selections.size() * times.size();
        
        ArrayList<ChanDataBuffer> bufList = getData(selections,times, false);
        
        try
        {
            if (response == null)
            {
                throw new WebUtilException("Response not set for data download: Joe's bug");
            }
            DataExporter mdd = new DataExporter(response, vpage);
            if (bufList.isEmpty())
            {
                vpage.addLine("No data to export.");
                ret = true;
            }
            else if (format.toLowerCase().contains("ligodv"))
            {
                ret = mdd.sendMfile(bufList);
            }
            else if (format.toLowerCase().contains("csv"))
            {
                if (n > 1)
                {
                    vpage.addLine("Only one file may be exported as CSV");
                    ret = true;
                }
                else
                {
                    ret = mdd.sendCSVfile(bufList);
                }
            }
            else if (format.toLowerCase().contains("ligolw"))
            {
                ret = mdd.sendXMLfile(bufList);
            }
            else if (format.toLowerCase().contains("wav"))
            {
                if (n > 1)
                {
                    vpage.addLine("Only one file may be exported as CSV");
                    ret = true;
                }
                else
                {
                    ret = mdd.sendWAVfile(bufList);
                }
            }
            else
            {
                vpage.addLine("Unknown download format: " +format);
                ret = true;
            }
        }
        catch (IOException ex)
        {
            throw new WebUtilException(ex);
        }
        return ret;
    }
    
    private ArrayList<ChanDataBuffer> getData(Set<Integer> selections, 
                                              List<TimeInterval> times, boolean noxfer) 
            throws WebUtilException
    {
        ArrayList<ChanDataBuffer> bufList;
        try
        {
            String[] testData = paramMap.get("testData");
            if (testData == null || testData[0].equalsIgnoreCase("none"))
            {
                bufList = ChanDataBuffer.dataBufFactory(db, selections, times, vpage, vuser, noxfer);
            }
            else
            {
                bufList = ChanDataBuffer.testDataFactory(testData[0], db, selections, times, vpage, vuser);
            }
            // is there any preprocessing to be done?
            for (ChanDataBuffer dbuf : bufList)
            {
                if (doDetrend)
                {
                    dbuf.detrend();
                }
                if (doPrefilter)
                {
                    
                    GDSFilter filter = new GDSFilter();
                    filter.apply(dbuf.getData(),dbuf.getChanInfo().getRate(),filtType, cutoff, order);
                }
            }
        }
        catch (LdvTableException ex)
        {
            throw new WebUtilException(ex);
        }
        
        return bufList;
    }
    /**
     * Creates a plot or plots from a single buffer list.  
     * Note: The word single means a single call to product.  Some products such as coherence plots
     *       can produce multiple plots of paired channels.  Ah life used to be simple.
     * @param product - the plot product
     * @param bufList - data specifiers
     * @throws WebUtilException 
     */
    private void doSinglePlot(PlotProduct product, ArrayList<ChanDataBuffer> bufList) throws WebUtilException, LdvTableException
    {
        long plotStrt = System.currentTimeMillis();

        PageItemList desc;
        desc = addIntro(product, bufList);

        product.setSize(width, height);
        product.setParameters(paramMap);

        ArrayList<Integer> imgIds = product.makePlot(bufList, compact);
        if (imgIds.isEmpty())
        {
            vpage.add("Error generating or saving image.");
            vpage.addBlankLines(1);
        }
        else
        {
            if (product.needsImageDescriptor())
            {
                vpage.add(desc);
            }
            imageIDs.addAll(imgIds);
            for(Integer imgId : imgIds)
            {
                if (product.needsImageDescriptor())
                {
                    imgTbl.addDescription(imgId,desc.getHtml());
                }
                else
                {
                    String imgDesc = imgTbl.getDescription(imgId);
                    PageItemString descStr = new PageItemString(imgDesc, false);
                    vpage.addBlankLines(1);
                    vpage.add(descStr);
                }
                String url = String.format("/viewer/?act=getImg&imgId=%d", imgId);
                PageItemImage piImg = new PageItemImage(url, "time series plot", "");
                ImageCoordinateTbl ict = new ImageCoordinateTbl(db);
                ImageCoordinate imgCord = ict.getCoordinate(imgId);
                if (imgCord != null)
                {
                    vpage.addLoadJS(imgCord.getHeadJS());
                    String className = imgCord.getClassName();
                    String imageIdname = String.format("%1$s_%2$d", className, imgId);
                    String timeId = String.format("time_%1$d", imgId);
                    piImg.setClassName(className);
                    piImg.setId(imageIdname);
                    vpage.addBodyJS(imgCord.getInitJS(imageIdname, timeId));
                    vpage.add(piImg);
                    PageItemString timeStr = new PageItemString("Click in image for time under mouse.");
                    timeStr.setId(timeId);
                    vpage.add(timeStr);
                }
                else
                {
                    vpage.add(piImg);
                }
            }
        }
        long elap = System.currentTimeMillis() - plotStrt;
        vuser.logPlots(1, elap);
    }

    /**
     * Now comes the real work.  Take the parameters specified and data downloaded and present the plots.
     * 
     * @param selectedProducts list of product Objects
     * @param data list of data objects
     * @throws WebUtilException 
     */
    private void doAllPlots(ArrayList<PlotProduct> selectedProducts, ArrayList<ChanDataBuffer> data) throws WebUtilException, LdvTableException
    {
        ArrayList<ChanDataBuffer> plotData = new ArrayList<>();
        for(PlotProduct product : selectedProducts)
        {
            if (product.isStackable())
            {   // plot them all in one graph
                doSinglePlot(product,data);
            }
            else
            {   // plot them individually
                for(ChanDataBuffer cdb : data)
                {
                    plotData.clear();
                    plotData.add(cdb);
                    doSinglePlot(product,plotData);
                }
            }
        }
    }
    /**
     * Examine the prefilter section of the returned form data and set the options.
     * Actual filtering is done later
     * 
     * @throws WebUtilException these are conversion problems with user input
     */
    private void prefilterSetup() throws WebUtilException
    {
        String[] prefilta = paramMap.get("prefilt");
        String prefilt = "none";
        if (prefilta != null && prefilta.length > 0)
        {
            prefilt = prefilta[0];
        }
        if (prefilt.equalsIgnoreCase("none"))
        {
            doPrefilter=false;
        }
        else
        {
            doPrefilter = true;
            if (prefilt.equalsIgnoreCase("high pass"))
            {
                filtType = "high";
            }
            else if (prefilt.equalsIgnoreCase("low pass"))
            {
                filtType = "low";
            }
            else
            {
                throw new WebUtilException("Unknown filter type: " + prefilt);
            }
            String co = paramMap.get("Cutoff")[0];
            String or = paramMap.get("Order")[0];
            if(!co.matches("[\\d\\.]+"))
            {
                throw new WebUtilException(String.format("Filter cutoff value not a float [%1$s]", co));
            }
            else if (!or.matches("\\d+"))
            {
                throw new WebUtilException(String.format("Filter order is not an integer [%1$s]", or));
            }
            else
            {
                try
                {
                    order = Integer.parseInt(or);
                    cutoff = Float.parseFloat(co);
                }
                catch (NumberFormatException numberFormatException)
                {
                    String er = String.format("Problem with format of filter order [%1$s] or cutoff [%2$s]. ", 
                                              or, co);
                    er += numberFormatException.getLocalizedMessage();
                    throw new WebUtilException(er);
                }
            }
            
        }
    }
    //====================SPECIAL PLOTS==================================
    // Specialized plots are different in that they have a limited (and manageable) list of 
    // applicapble channels.
    //===================================================================

    public void specialPlotSelector(Page vpage) 
            throws WebUtilException, SQLException, LdvTableException
    {

        vpage.add(new PageItemString("Specialized plots work only for specific channels.<br><br>", false));
        PageItemList pfDiv = new PageItemList();
        pfDiv.setId("accordion");
        //=========ODC Plot
        pfDiv.add(new PageItemHeader("ODC/DQ Plots:", 3));
        PageItemList contentDiv1 = new PageItemList();
        contentDiv1.setClassName("plotSelector");
        
        OdcPlotManager odcpm = new OdcPlotManager(db, vpage, vuser);
        odcpm.setContextPath(contextPath);
        odcpm.setServletPath(servletPath);
        PageForm pf = odcpm.getSelector(vpage);
        pf.setClassName("plotSelector");
        contentDiv1.add(pf);
        pfDiv.add(contentDiv1);

        
        //========== Live Plots
        pfDiv.add(new PageItemHeader("Near Real Time Plots:", 3));
        PageItemList contentDiv2 = new PageItemList();
        contentDiv2.setClassName("plotSelector");
        PageItemList livePlotSelector = LivePlotManager.getSelector();
        livePlotSelector.setClassName("plotSelector");
        contentDiv2.add(livePlotSelector);
        pfDiv.add(contentDiv2);
        // add to page
        vpage.add(pfDiv);
        // make sure javascript we need is there
        String accScript = "jQuery( \"#accordion\").accordion({ collapsible: true, active: false });\n";
        vpage.addReadyJS(accScript);
    }
    /**
     * Generate one of our "special" (as in special Olympics) plots
     * 
     * @return true if html page is to be displayed by caller, false if we sent some other mime type
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public boolean specialPlot() throws WebUtilException
    {
        boolean ret = true;
        TimeAndPlotSelector tps = new TimeAndPlotSelector(db, vpage, vuser);
        tps.setContextPath(contextPath);
        tps.setParamMap(paramMap);

        ArrayList<TimeInterval> times = tps.getTimesFromForm();
        String ermsg = "";

        if (times.isEmpty())
        {
            ermsg += "No times were selected.<br/>";
        }
        String[] plotType = paramMap.get("odcplot");
        if (ermsg.isEmpty() && plotType != null)
        {
            OdcPlotManager odcp = new OdcPlotManager( db, vpage, vuser);
            odcp.setContextPath(contextPath);
            odcp.setServletPath(servletPath);
            odcp.setParammap(paramMap);
            String[] chanNames = paramMap.get("chanName");
            if (chanNames == null || chanNames.length == 0)
            {
                ermsg += "No channel specified.<br/>";
            }
            else
            {
                boolean doAll = false;
                TreeSet<String> chanList = new TreeSet<>();
                for(String sel : chanNames)
                {
                    chanList.add(sel);
                    if (sel.equalsIgnoreCase("All"))
                    {
                        doAll = true;
                    }
                }
                if (doAll)
                {
                    chanList = new TreeSet<>(OdcPlotManager.getOdcPlotChannelMap().keySet());
                }
                
                for(String chanName : chanList)
                {
                    String server = OdcPlotManager.getOdcPlotChannelMap().get(chanName);
                    if (server == null)
                    {
                        ermsg += "No server defined for channel: " + chanName;
                    }
                    else
                    {
                        try
                        {
                            odcp.doPlot(chanName,server,times);
                        }
                        catch (WebUtilException ex)
                        {
                            ermsg += ex.getLocalizedMessage();
                        }
                    }
                }
            }
        }
        else
        {
            ermsg += "Unknown special plot.<br/>";
        }
        if (!ermsg.isEmpty())
        {
            vpage.add(ermsg);
        }
        return ret;
    }

    /**
     * Should this page be refreshed automatically?  If so make it do that.
     * 
     * @param selections
     * @param times 
     */
    private int checkRefresh(HashSet<Integer> selections, ArrayList<TimeInterval> times) throws SQLException
    {
        int rptCnt = -1;    // assume we are not going to redo this plot on a schedule
        if (paramMap.containsKey("autoRefresh"))
        {
            boolean allOnline = true;
            String[] arCnt = paramMap.get(arKey);

            ChannelTable ct = new ChannelTable(db);
            for (Integer sel : selections)
            {
                ChanInfo ci = ct.getChanInfo(sel);
                boolean isOnline = ci.getcType().equalsIgnoreCase("online");
                allOnline &= isOnline;
            }

            if (allOnline)
            {
                String[] rcntStrs = paramMap.get("rptCnt");
                String rcntStr = rcntStrs == null || rcntStrs.length == 0 ? "" :
                        rcntStrs[0];
                int rcnt=0;
                
                if (rcntStr.matches("^\\d+$"))
                {
                    rcnt = Integer.parseInt(rcntStr);
                }
                if (arCnt != null && arCnt.length > 0)
                {
                    String a = arCnt[0];
                    if (a.trim().matches("^\\d+$"))
                    {
                        int t = Integer.parseInt(a.trim());
                        if (t > 1)
                        {
                            rptCnt = t-1;
                        }
                    }
                }
                else if (rcnt > 1)
                {
                    rptCnt = rcnt;
                }
                else
                {
                    rptCnt=30;
                }
            }
        }
        return rptCnt;
    }

    private String getMyUrl(int autoCntr)
    {
        StringBuilder url = new StringBuilder();
//        url.append(request.getScheme());
//        url.append("://");
//        url.append(request.getServerName());
//        url.append(":");
//        url.append(Integer.toString(request.getServerPort()));
        url.append(contextPath);
        url.append("/?");
        boolean needAmp = false;
        for(Entry<String,String[]> ent: paramMap.entrySet())
        {
            String name = ent.getKey();
            if (!name.equalsIgnoreCase(arKey))
            {
                String[] vals = ent.getValue();
                for(String v : vals)
                {
                    if (needAmp)
                    {
                        url.append("&");
                    }
                    else
                    {
                        needAmp = true;
                    }
                    url.append(name);
                    url.append("=");
                    url.append(v);
                }
            }
        }
        url.append(String.format("&%1$s=%2$d",arKey,autoCntr));
        
        return url.toString();
    }
    /**
     * Common action, format and add instructions for this step
     *
     * @param pf - form to add to
     * @param stepName text of the step name/instructrions
     */
    private void addProcStep(PageItemList pf, String stepName) throws WebUtilException
    {
        PageItemString procStep;
        procStep = new PageItemString(stepName);
        procStep.setClassName("processStep");
        pf.add(new PageItemBlanks());
        pf.add(procStep);
        pf.add(new PageItemBlanks());
    }

    /**
     * Create an accordion pane for this plot product
     * @param prod product object
     * @param paramName the form parameter name if they enable this plot
     * @param nSel the number of time intervals * number of channels so we know how to handle multiples
     * @param multDisp a list of the standard ways to handle multiple displays (if the product allows it)
     * @return the object to add to the page
     * @throws WebUtilException I probably have a bug in html object generation.
     */
    private PageItemList getSelectorContent(PlotProduct prod, String paramName, int nSel, String[] multDisp) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        PageItem spPI = prod.getSelector(paramName, nSel, multDisp);
        spPI.setClassName("plotSelector");

        ret.add(new PageItemHeader(prod.getProductName() + ":", 3));
        
        
        ret.add(spPI);
        
        return ret;
    }
}
