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

import edu.fullerton.viewerplugin.ChanDataBuffer;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvtables.ViewUser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public interface PlotProduct
{
    /**
     * Create one or more plots from data provided
     * 
     * Note: internal products should implement this interface while products that use external 
     * programs should subclass the ExternalPlotManager.
     * 
     * @param dbuf data buffers with full descriptors
     * @param compact flag that image is small so minimize text added
     * @return list of image IDs of product images saved to the database
     * @throws WebUtilException 
     */
    ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) 
            throws WebUtilException;
    
    /**
     * flag to say whether we can accept all data sets at one time or one for each call default is
     * one per call, override in the plugin if you can take multiples
     *
     * @return true if you want a bunch of datasets for each plot
     */
    boolean isStackable();
    
    /**
     * Flag to say whether this product needs 2 datasets for each calculation such as coherence
     * 
     * @return true if pairs of datasets are needed for each result.
     */
    boolean isPaired();
    /**
     * If the parameterMap contains our enable key, we're selected.
     * @return true if this plot is selected
     */
    boolean isSelected();
    
    /**
     * The enable key is an HTML parameter name for the check box that selects this plot
     * @return - the parameter name
     */
    String getEnableKey();
    
    /**
     * Determines whether the PluginManager creates an image description for each image in the list
     * or if the product itself creates the description
     * 
     * @return  true if the PluginManager should do it, false if the product does it
     */
    boolean needsImageDescriptor();
    
    /**
     * Set the dimensions of the resulting image
     * @param width in pixels
     * @param height in pixels
     */
    void setSize(int width, int height);
    
    // For generalized selection
    /**
     * Get the external name of this product for selection and results page
     * 
     * @return descriptive name
     */
    String getProductName();
    
    /**
     * The namespace is the prefix for all form parameters for this product
     * @return the prefix string
     */
    String getNameSpace();
    /**
     * Return html object that displays all user settable parameters
     * @return the html object
     */
    PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException;

    /**
     * Does this product want the PlotManager to get data, or does it do it itself
     * @return true if PlotManager is to get the data
     */
    boolean needsDataXfer();
    
    /**
     * Stacked/single selector
     * @param dispFormat contains "Stacked" or "Single"
     */
    public void setDispFormat(String dispFormat);
    
    /**
     * set the common objects
     * @param db - our database
     * @param vpage - the output page
     * @param vuser - user who currently owns the session
     */
    public void setup(Database db, Page vpage, ViewUser vuser);
    
    /**
     * Pass all parameters from the request
     * @param parameterMap - all parameters
     */
    public void setParameters(Map<String, String[]> parameterMap);
    
    /**
     * Some products run in the background and do not return a list of images to display.
     * 
     * @return true if caller should expect a non-empty list of images.
     */
    public boolean hasImages();
    
    /**
     * Some products such as Coherence need at least 2 channels and the UI lets them select a
     * reference channel
     * @param baseChans list of selected channels
     */
    public void setChanList(List<BaseChanSelection> baseChans);
}
