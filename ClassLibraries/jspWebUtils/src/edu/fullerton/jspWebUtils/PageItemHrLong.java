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
package edu.fullerton.jspWebUtils;

/**
 * Convert a number to a human readable form eg 1234567 = 1.23M 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageItemHrLong extends PageItem
{
    private long val=0;
    private int sigDig=3;
    private String unit="";
    
    public PageItemHrLong(long val, int sigDig, String unit)
    {
        this.val = val;
        this.sigDig = sigDig;
        this.unit = unit;
    }

    @Override
    public String getHtml() throws WebUtilException
    {
        long[] breaks = {1000000000000L, 1000000000000L,  1000000000L, 1000000L,    1000L,  1L};
        String[] sym =  {"P",           "T",            "G",        "M",        "k",        ""};
        String out="";
        
        int ddig= sigDig;
        double x=val;
        String q = "";
        
        for(int i = 0; i< breaks.length; i++)
        {
            if (Math.abs(val) >= breaks[i])
            {
                x = val / (double) breaks[i];
                int idig = (int) Math.log10(Math.abs(x));
                ddig = idig > sigDig ? 0 : sigDig - idig;
                q = sym[i];
                break;
            }
        }
        String fmt = "%1$." + Integer.toString(ddig) + "f" + q + unit;
        out = String.format(fmt,x);
        return out;
    }
    
}
