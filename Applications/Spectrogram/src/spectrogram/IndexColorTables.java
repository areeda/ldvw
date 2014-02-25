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

import java.awt.Color;
import java.awt.image.IndexColorModel;

/**
 * Produce IndexColorModels for standard color tables
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class IndexColorTables
{
    public static IndexColorModel getColorTable(String name)
    {
        IndexColorModel ret = null;
        
        if (name.equalsIgnoreCase("hot"))
        {
            ret = getHot();
        }
        else if (name.equalsIgnoreCase("jet"))
        {
            ret = getJet();
        }
        else if (name.equalsIgnoreCase("bw"))
        {
            ret = getBW();
        }
        return ret;
    }
    private static int[] hot_r =
    {
        0, 2, 4, 7, 9, 12, 14, 17, 19, 22, 24, 27, 29,
        32, 34, 37, 39, 42, 44, 46, 49, 51, 54, 56, 59, 61,
        64, 66, 69, 71, 74, 76, 79, 81, 84, 87, 89, 92, 94,
        97, 99, 102, 104, 107, 109, 112, 114, 117, 119, 122, 124, 127,
        129, 131, 134, 136, 139, 141, 144, 146, 149, 151, 154, 156, 159,
        161, 164, 167, 169, 172, 174, 177, 179, 182, 184, 187, 189, 192,
        194, 197, 199, 202, 204, 207, 209, 212, 214, 216, 219, 221, 224,
        226, 229, 231, 234, 236, 239, 241, 244, 246, 249, 251, 252, 253,
        254, 254, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255
    };
    private static int[] hot_g =
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 3,
        5, 7, 9, 12, 14, 17, 19, 22, 24, 27, 29, 32, 34,
        37, 39, 42, 44, 46, 49, 51, 54, 56, 59, 61, 64, 66,
        69, 71, 74, 76, 79, 81, 84, 87, 89, 92, 94, 97, 99,
        102, 104, 107, 109, 112, 114, 117, 119, 122, 124, 127, 129, 131,
        134, 136, 139, 141, 144, 146, 149, 151, 154, 156, 159, 161, 164,
        166, 169, 171, 174, 177, 179, 182, 184, 187, 189, 192, 194, 197,
        199, 202, 204, 207, 209, 212, 214, 216, 219, 221, 224, 226, 229,
        231, 234, 236, 239, 241, 244, 246, 249, 250, 252, 253, 254, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255, 255,
        255, 255, 255, 255, 255, 255, 255, 255, 255
    };
    private static int[] hot_b =
    {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 1, 4, 7, 10, 14,
        19, 24, 29, 34, 39, 44, 49, 54, 59, 64, 69, 74, 79,
        84, 89, 94, 99, 104, 109, 114, 119, 124, 129, 134, 139, 144,
        149, 154, 159, 164, 169, 174, 179, 184, 189, 194, 199, 204, 209,
        214, 219, 224, 229, 234, 239, 244, 249, 255
    };

    private static IndexColorModel getHot()
    {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        for (int i = 0; i < 256; i++)
        {
            r[i] = g[i] = b[i] = 0;
            r[i] |= hot_r[i] & 0xff;
            g[i] |= hot_g[i] & 0xff;
            b[i] |= hot_b[i] & 0xff;
        }
        IndexColorModel ret = new IndexColorModel(8, 256, r, g, b);
        return ret;
    }

    // grabbed from the Matlab jet table and reformatted with vim as a constant array
    private static double [][] jetMat =
    {
        { 0, 0, 0.5625},
        { 0, 0, 0.6250},
        { 0, 0, 0.6875},
        { 0, 0, 0.7500},
        { 0, 0, 0.8125},
        { 0, 0, 0.8750},
        { 0, 0, 0.9375},
        { 0, 0, 1.0000},
        { 0, 0.0625, 1.0000},
        { 0, 0.1250, 1.0000},
        { 0, 0.1875, 1.0000},
        { 0, 0.2500, 1.0000},
        { 0, 0.3125, 1.0000},
        { 0, 0.3750, 1.0000},
        { 0, 0.4375, 1.0000},
        { 0, 0.5000, 1.0000},
        { 0, 0.5625, 1.0000},
        { 0, 0.6250, 1.0000},
        { 0, 0.6875, 1.0000},
        { 0, 0.7500, 1.0000},
        { 0, 0.8125, 1.0000},
        { 0, 0.8750, 1.0000},
        { 0, 0.9375, 1.0000},
        { 0, 1.0000, 1.0000},
        { 0.0625, 1.0000, 0.9375},
        { 0.1250, 1.0000, 0.8750},
        { 0.1875, 1.0000, 0.8125},
        { 0.2500, 1.0000, 0.7500},
        { 0.3125, 1.0000, 0.6875},
        { 0.3750, 1.0000, 0.6250},
        { 0.4375, 1.0000, 0.5625},
        { 0.5000, 1.0000, 0.5000},
        { 0.5625, 1.0000, 0.4375},
        { 0.6250, 1.0000, 0.3750},
        { 0.6875, 1.0000, 0.3125},
        { 0.7500, 1.0000, 0.2500},
        { 0.8125, 1.0000, 0.1875},
        { 0.8750, 1.0000, 0.1250},
        { 0.9375, 1.0000, 0.0625},
        { 1.0000, 1.0000, 0},
        { 1.0000, 0.9375, 0},
        { 1.0000, 0.8750, 0},
        { 1.0000, 0.8125, 0},
        { 1.0000, 0.7500, 0},
        { 1.0000, 0.6875, 0},
        { 1.0000, 0.6250, 0},
        { 1.0000, 0.5625, 0},
        { 1.0000, 0.5000, 0},
        { 1.0000, 0.4375, 0},
        { 1.0000, 0.3750, 0},
        { 1.0000, 0.3125, 0},
        { 1.0000, 0.2500, 0},
        { 1.0000, 0.1875, 0},
        { 1.0000, 0.1250, 0},
        { 1.0000, 0.0625, 0},
        { 1.0000, 0, 0},
        { 0.9375, 0, 0},
        { 0.8750, 0, 0},
        { 0.8125, 0, 0},
        { 0.7500, 0, 0},
        { 0.6875, 0, 0},
        { 0.6250, 0, 0},
        { 0.5625, 0, 0},
        { 0.5000, 0, 0}
    };

    private static IndexColorModel getJet()
    {
        return getMatlabTable(jetMat);
    }

    private static IndexColorModel getMatlabTable(double[][] mtbl)
    {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];
        int ncolors = 250;      // use the rest for labels
        float step = (mtbl.length - 1) / ((float) ncolors);
        
        for (int i = 0; i < 251; i++)
        {
            float c = (i*step);
            int ic = (int)c;
            float delta = c - ic;
            
            ic = Math.min(ic, mtbl.length-1);
            
            double r1 = mtbl[ic][0];
            double g1 = mtbl[ic][1];
            double b1 = mtbl[ic][2];
            
            double r2,g2,b2;
            double rv,gv,bv;
            
            if (ic < mtbl.length - 2)
            {
                r2 = mtbl[ic+1][0];
                g2 = mtbl[ic+1][1];
                b2 = mtbl[ic+1][2];
            }
            else
            {
                r2 = mtbl[ic][0];
                g2 = mtbl[ic][1];
                b2 = mtbl[ic][2];
            }
            rv = r1 + delta * (r2-r1);
            gv = g1 + delta * (g2-g1);
            bv = b1 + delta * (b2-b1);
            
            r[i] |= ((int)(rv * 255)) & 0xff;
            g[i] |= ((int)(gv * 255)) & 0xff;
            b[i] |= ((int)(bv * 255)) & 0xff;
        }
        // add some standard colors for labels
        r[251] = (byte) (Color.GREEN.getRed() & 0xff);
        g[251] = (byte) (Color.GREEN.getGreen() & 0xff);
        b[251] = (byte) (Color.GREEN.getBlue() & 0xff);
        
        r[252] = (byte) (Color.RED.getRed() & 0xff);
        g[252] = (byte) (Color.RED.getGreen() & 0xff);
        b[252] = (byte) (Color.RED.getBlue() & 0xff);
        
        r[253] = (byte) (Color.BLUE.getRed() & 0xff);
        g[253] = (byte) (Color.BLUE.getGreen() & 0xff);
        b[253] = (byte) (Color.BLUE.getBlue() & 0xff);
        r[254] = g[254] = b[254] = 0;
        r[255] = g[255] = b[255] = (byte) 0xff;
        
        IndexColorModel ret = new IndexColorModel(8, 256, r, g, b);
        
        return ret;        
    }

    private static IndexColorModel getBW()
    {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];

        for (int i=0;i<256;i++)
        {
            r[i] = g[i] = b[i] = (byte) (i & 0xff);
        }
        IndexColorModel ret = new IndexColorModel(8, 256, r, g, b);

        return ret;
    }
}
