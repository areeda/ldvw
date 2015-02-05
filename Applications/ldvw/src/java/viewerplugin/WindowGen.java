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
package viewerplugin;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class WindowGen
{

   
    public enum Window
    {

        NONE, BLACKMAN, HAMMING, HANNING, FLATTOP
    };
    
    public static double[] getWindow(Window window, int flen)
    {
        double[] win = new double[flen];
        double windowLen = flen;
        double[] a = { 0.21557895, 0.41663158, 0.277263158, 0.083578947, 0.006947368 };
        
        for(int i=0;i<windowLen;i++)
        {
            double x = i/(windowLen-1);
            double w;
            switch (window)
            {
                case HANNING:
                    w = 0.5 - 0.5*Math.cos(2*Math.PI*x);
                    break;
                case HAMMING:
                    w = 0.54 - 0.46*Math.cos(2*Math.PI*x);
                    break;
                case BLACKMAN:
                    // Force end points to zero to avoid close-to-zero negative values caused
                    // by roundoff errors.

                    w = 0.42 - 0.5*Math.cos(2*Math.PI*x) + 0.08*Math.cos(4*Math.PI*x);
                    w = i==0 ? 0. : w; 
                    break;

                case FLATTOP:
                    // Flattop window
                    // Coefficients as defined in the reference [1] (see flattopwin.m)

                    w = a[0] - a[1]*Math.cos(2*Math.PI*x) + a[2]*Math.cos(4*Math.PI*x) - a[3]*Math.cos(6*Math.PI*x) + 
                    a[4]*Math.cos(8*Math.PI*x);  
                    break;

                case NONE:
                default:
                    w = 1.0;
            }
            win[i] = w;
            //win[flen -i -1] = w;
        }
        return win;
    }
    
}
