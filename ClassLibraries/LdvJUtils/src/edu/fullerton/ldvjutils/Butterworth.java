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
package edu.fullerton.ldvjutils;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * Define Butterworth filters in time and frequency domain
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Butterworth
{
    
    private double[] a;
    private double[] b;
    /**
     * create a frequency domain butterworth windows, mirrored for use with fft
     *
     * @param highPass true if high pass false if low pass
     * @param len length of vector returned (includes mirrored values)
     * @param cutoff cutoff frequency in samples/pixel (you convert from Hz)
     * @param order butterworth exponent = 2 * order
     * @return window for cutoff with gain of 1
     */
    public static double[] getWindow(boolean highPass, int len, float cutoff, int order)
    {
        double[] win = new double[len];
        int l2 = len/2;
        for (int i = 0; i <= l2; i++)
        {
            double f;
            double gain;
            f  = i / ((double) l2);
            
            gain = (double) (1 / (1 + Math.pow(f / cutoff, (2 * order))));
            if (highPass)
            {
                gain = 1 - gain;
            }
            
            win[i] = gain;
            if (i > 0)
            {
                win[len - i] = gain;
            }
        }
        return win;
    }
    /**
     * Create a spatial domain convolution kernel for the filter specified
     * @see Butterworth#getWindow(boolean, int, double, int) 
     * @param highPass true=high pass false=low pass
     * @param len length of the kernel must be odd
     * @param cutoff filter cutoff in cycles/pixels (0 < cutoff <0.5)
     * @param order butterworth order
     * @return convolution kernel suitable for our convolve function 
     * @see Butterworth#convolve(float[], float[]) 
     */
     public static double[] getKernel(boolean highPass, int len, float cutoff, int order)
    {
        int wlen = 64;
        while (wlen < len * 4)
        {
            wlen *= 2;
        }
        double[] win = getWindow(highPass, wlen, cutoff, order);
//        for (int i = 0; i < wlen; i++)
//        {
//            if (i % 2 == 1)
//            {
//                win[i] = 0;
//            }
//        }
        DoubleFFT_1D fftd = new DoubleFFT_1D(wlen);
        fftd.realInverse(win, false);
        int l1 = len % 2 == 1 ? len : len + 1;
        double[] kernel = new double[l1];
        int l2 = l1 / 2;
        double gain = 0;
        for (int i = 0; i <= l2; i++)
        {
            double k = win[i * 2];
            kernel[l2 + i] = k;
            kernel[l2 - i] = k;
            gain += i == 0 ? k : k * 2;
        }
        if (Math.abs(gain) > 0  && (Math.abs(gain) <0.95 || Math.abs(gain) > 0.95))
        {
            for (int i = 0; i < l1; i++)
            {
                kernel[i] /= Math.abs(gain);
            }
        }
        return kernel;
    }

    /**
     * Perform a time domain convolution
     * @param timeDomain input vector
     * @param kernel convolution kernel
     * @return filtered input vector
     */
    public static float[] convolve(float[] timeDomain, double[] kernel)
    {
        int n = timeDomain.length;
        int kn = kernel.length;
        int kn2 = (kn + 1)/ 2;
        float[] ret = new float[n];

        for (int i = 0; i < n; i++)
        {
            double it = 0;
            for (int k = 0; k < kn; k++)
            {
                int i2 = i + k - kn2;
                if (i2 < 0)
                {
                    i2 = -i2;
                }
                else if (i2 > n - 1)
                {
                    i2 = 2 * n - i2 - 1;
                }
                it += timeDomain[i2] * kernel[k];
            }
             ret[i] = (float) it;
        }
        return ret;
    }

    /**
     * Perform a time domain convolution
     *
     * @param timeDomain input vector
     * @param kernel convolution kernel
     * @return filtered input vector
     */
    public static double[] convolve(double[] timeDomain, double[] kernel)
    {
        int n = timeDomain.length;
        int kn = kernel.length;
        int kn2 = (kn + 1) / 2;
        double[] ret = new double[n];

        for (int i = 0; i < n; i++)
        {
            double it = 0;
            for (int k = 0; k < kn; k++)
            {
                int i2 = i + k - kn2;
                if (i2 < 0)
                {
                    i2 = -i2;
                }
                if (i2 > n - 1)
                {
                    i2 = 2 * n - i2 - 1;
                }
                it += timeDomain[i2] * kernel[k];
            }
            ret[i] = it;
        }
        return ret;
    }
    /**
     * calculates the frequency response of the kernel
     * @param kernel spatial domain kernel
     * @param len length of the returned vector
     * @return 
     */
    public static float[] transferFunction(double[] kernel, int len)
    {
        
        float[] in = new float[len*2];
        for(int i=0;i<len*2;i++)
        {
            in[i] = 0;
        }
        int i1 = (len -kernel.length)/2;
        for(int i=i1;i<i1+kernel.length;i++)
        {
            in[i*2] = (float) kernel[i-i1];
        }
        FloatFFT_1D fftd = new FloatFFT_1D(len*2);
        fftd.realForward(in);
        
        float[] tf = new float[len];
        for(int i=0;i<len;i++)
        {
            tf[i] = (float) Math.sqrt(in[i*2] * in[i*2] + in[i*2+1] * in[i*2+1]);
        }
        return tf;
    }
    /**
     * 
     * @param fc
     * @param fs
     * @param a
     * @param b 
     */
    public void iirHP(float fc, float fs)
    {
        double alpha, beta, gamma;
        
        gamma = Math.tan(Math.PI * fc/fs);
        alpha = -2 * (Math.cos(Math.PI * 5 / 8) + Math.cos(Math.PI * 7 / 8));
        beta = 2 * (1 + 2 * Math.cos(Math.PI * 5 /8) * Math.cos(Math.PI * 7 / 8));
        
        a= new double[4];
        b = new double[5];
        
        double g4 = Math.pow(gamma, 4);
        double g3 = Math.pow(gamma, 3);
        double g2 = Math.pow(gamma, 2);
        
        // Denominator coefficients
        double num;
        double den = g4 + alpha * g3 + beta * g2 + alpha * gamma + 1;
        
        num = 2 * ( 2 * g4 + alpha * g3 - alpha * gamma - 2 );
        a[0] = num/den;
        
        num = 2 * (3 * g4 - beta * g2  + 3);
        
        a[1] = num / den;
        
        num = 2 * ( 2 * g4 - alpha * g3 +  alpha * gamma - 2);
        a[2] = num / den;
        
        num = g4 - alpha * g3 + beta * g2 - alpha * gamma + 1;
        
        a[3] = num / den;
         
        // numberator coefficients
        b[0] = 1 / den;
       
        b[1] = -4 / den;
        
        b[2] = 6 / den;
        
        b[3] = -4 / den;
        
        b[4] = 1 / den;
    }

    public double[] getA()
    {
        return a;
    }

    public double[] getB()
    {
        return b;
    }
    
}
