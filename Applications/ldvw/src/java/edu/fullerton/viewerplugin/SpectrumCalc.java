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
package edu.fullerton.viewerplugin;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.fullerton.viewerplugin.WindowGen.Window;

/**
 * Second generation Fourier spectrum calculator
 * 
 * The big difference is this class has no display code only compute methods.
 * It incorporates windows, detrending, scaling that have been validated against DMT and LigoDV.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class SpectrumCalc
{
    private int debugLevel=1;
    
    
    public enum Scaling
    {
        
        AS
        {
            @Override
            public String toString()
            {
                return "Amplitude (Counts)";
            }
        },
        ASD
        {
            @Override
            public String toString()
            {
                String ret = "Amplitude spectral density (Counts / \u221AHz)";
                if (useTex)
                {
                    ret = "ASD $\\left( \\frac{\\mathrm{Counts}}{\\sqrt{\\mathrm{Hz}}}\\right)$";
                }
                return ret;
            }
        },
        PS
        {
            @Override
            public String toString()
            {
                return "Power (Counts ^2)";
            }
        },
        PSD
        {
            @Override
            public String toString()
            {
                String ret = "Power spectral density (Counts ^2 / \u221AHz)";
                if (useTex)
                {
                    ret = "PSD $\\left( \\frac{\\mathrm{Counts}^2}{\\sqrt{\\mathrm{Hz}}}\\right)$";
                }
                return ret;
            }
        };
        private static boolean useTex = false;
        void setTex(boolean b)
        {
            useTex = b;
        }
    };
    
    private WindowGen.Window window = WindowGen.Window.HAMMING;
    private Scaling scaling = Scaling.ASD;
    private boolean doDetrend = true;
    

    /**
     * default constructor sets default parmeters to the way we like to do the calculation
     * Scaling: ASD
     * Window: Hamming
     * Detrend: true
     * Fmin-Fmax = full range
     */
    public SpectrumCalc()
    {
        scaling = Scaling.ASD;
        window = WindowGen.Window.HAMMING;
        doDetrend = true;
    }
    
    
    
    public double[] doCalc(double[] data, double fs)
    {
        // Data integrity checks
        if (data == null || data.length < 8)
        {
            throw new IllegalArgumentException("Data for fft is null or length < 8");
        }
        

        int flen = data.length;
        
        

        if (debugLevel > 2)
        {
            showStats("Raw", data,fs);
        }
        
        // allocate buffers
        
        DoubleFFT_1D fft = new DoubleFFT_1D(flen);
        double[] fftd = new double[flen * 2];

        double[] result = new double[flen / 2 + 1];
        for (int i = 0; i < flen / 2 + 1; i++)
        {
            result[i] = 0;
        }

        double[] win = WindowGen.getWindow(window, flen);
        
        if (debugLevel > 2)
        {
            //showStats("Win", win, fs);
        }
        int n = 0;      // number of ffts calculated
        double[] dtemp = new double[flen];
        System.arraycopy(data, 0, dtemp, 0, flen);
        if (doDetrend)
        {
            LinearRegression.detrend(dtemp);     // subtract a linear fit to the data
        }

        // apply window function
        for (int i2 = 0; i2 < flen; i2++)
        {
            dtemp[i2] *= win[i2];
        }
        for (int i2 = 0; i2 < flen; i2++)
        {
            fftd[i2 * 2] = dtemp[i2];
            fftd[i2 * 2 + 1] = 0.;
        }
        fft.complexForward(fftd);
        n++;
        double r;
        double im;
        for (int i3 = 0; i3 < flen / 2; i3++)
        {
            r = fftd[i3 * 2];
            im = fftd[i3 * 2 + 1];

            result[i3] += (r * r + im * im);
        }

        // calculate scale factor
        double winsum = 0., winsumsq = 0;
        for (int idx = 0; idx < flen; idx++)
        {
            winsum += win[idx];
            winsumsq += win[idx] * win[idx];
        }
        
        double scale = 1.;
        switch (scaling)
        {
            case AS:
                scale = Math.sqrt(2.) / winsum;
                break;
            case ASD:
                scale = Math.sqrt(2. / (winsumsq * fs));
                break;
            case PS:
                scale = 2 / (winsum * winsum);
                break;
            case PSD:
                scale = 2 / (winsumsq * fs);
                break;
        }
        
        // scale the results appropriately
        for(int idx =0; idx < result.length; idx++)
        {
            switch(scaling)
            {
                case AS:
                case ASD:
                    result[idx] = Math.sqrt(result[idx]/n) * scale;
                    break;
                    
                case PS:
                case PSD:
                    result[idx] *= scale/n;
                    break;
            }
        }
        return result;
    }

    public void setWindow(Window window)
    {
        this.window = window;
    }

    public void setScaling(Scaling scaling)
    {
        this.scaling = scaling;
    }

    public void setDoDetrend(boolean doDetrend)
    {
        this.doDetrend = doDetrend;
    }

    private void showStats(String what, double[] data, double fs)
    {
        int n = data.length;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0;
        double mean;
        double var=0;
        double sd;
        
        for(int idx = 0; idx < n; idx ++)
        {
            double it = data[idx];
            min = Math.min(min, it);
            max = Math.max(max, it);
            sum += it;
        }
        mean = sum/n;
        for(int idx = 0; idx < n; idx ++)
        {
            double it = data[idx];
            var += (it - mean) * (it -mean);
        }
        sd = Math.sqrt(var/n);
        System.out.format("Min: %1$.05f, Max: %2$.05f, Mean: %3$05f, SD: %4$.05f, - %5$s\n", 
                          min, max, mean, sd, what);
    }
}
