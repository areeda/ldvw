/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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

package edu.fullerton.ldvplugin;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItem;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.plugindefn.SpectrumDefn;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import java.util.ArrayList;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SpectrumManager extends ExternalPlotManager
{
    private final String nameSpace = "gwsp";
    
    public SpectrumManager(Database db, Page vpage, ViewUser vuser)
    {
        super(db, vpage, vuser);
    }

    @Override
    public ArrayList<Integer> makePlot(ArrayList<ChanDataBuffer> dbuf, boolean compact) throws WebUtilException
    {
        ArrayList<Integer> ret;
        SpectrumDefn spd = new SpectrumDefn();
        spd.init();
        ret = makePcPlot(spd, dbuf, compact);
        return ret;
    }

    @Override
    public boolean isStackable()
    {
        return true;
    }

    @Override
    public boolean isPaired()
    {
        return false;
    }

    @Override
    public String getProductName()
    {
        return "Spectrum (GWpy)";
    }

    @Override
    public PageItem getSelector(String enableKey, int nSel, String[] multDisp) throws WebUtilException
    {
        SpectrumDefn spd = new SpectrumDefn();
        spd.init();
        this.enableKey = enableKey;
        spd.setFormParameters(paramMap);
        PageItemList ret = spd.getSelector(enableKey, nSel);
        return ret;
    }

    @Override
    public String getNameSpace()
    {
        return nameSpace;
    }

}
