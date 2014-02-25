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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class Spectrum
{
    private double startTime;
    private double[] spectrum;
    private double[] scaledSpectrum;
    private double sampleRate;
    private double fmax;            // frequency max
    private double fmin;            // frequency min
    private double binWidth;
    private double smin, smax;      // sample min, max
    private boolean isNull;
    private double sum;

    public Spectrum()
    {
        isNull = true;
    }
    
    void init(double t, double[] result, double sampleRate, double fmax, double fmin, double binWidth)
    {
        startTime = t;
        this.sampleRate = sampleRate;
        this.binWidth = binWidth;
        double nyquist = sampleRate/2;
        this.fmin = fmin < binWidth ? binWidth : fmin;
        this.fmax = fmax == 0 || fmax > nyquist ? nyquist : fmax;
        int s = (int) (Math.round(this.fmin/binWidth) - 1);
        int e = (int) (Math.round(this.fmax/binWidth));
        int l = e-s;
        spectrum = new double[l];
        smin = Double.MAX_VALUE;
        smax = Double.MIN_VALUE;
        sum = 0.;
        double tmp;
        // copy just the frequencies we want and calc min/max
        for(int idx = 0; idx < l; idx ++)
        {
            tmp = result[idx + s];
            sum += tmp;
            spectrum[idx] = tmp;
            smin = Math.min(tmp, smin);
            smax = Math.max(tmp,smax);
        }
        isNull = false;
    }
    
    public boolean isNull()
    {
        return this.isNull;
    }

    public double getMin()
    {
        return smin;
    }
    
    public double getMax()
    {
        return smax;
    }

    /**
     * Convert the spectrum to an unsigned byte array scaled to our min/max
     * @param bmax max pixel value in byte array
     */
    void normalize(int bmax,boolean logIntensity) throws IOException
    {
        normalize(bmax,smin,smax,logIntensity);
    }

    /**
     * Convert the spectrum to an unsigned byte array scaled to [probably] global min/max
     * @param bmax max pixel value in byte array
     * @param gmin min input value to use
     * @param gmax max input value to use
     */
    void normalize(double outMax, double gmin, double gmax, boolean logIntensity) throws IOException
    {
        double inrange = gmax-gmin;
        scaledSpectrum = new double[spectrum.length];
        
        if (!isNull && inrange > 0 && gmin <= smax && gmax >= smin && smax-smin > 0)
        {
            double inrangeLog = Math.log10(gmax) - Math.log10(gmin);
            double scaleFactor = logIntensity ? outMax/inrangeLog : outMax / inrange;
            double logMin = Math.log10(gmin);
            for (int idx = 0; idx < spectrum.length; idx++)
            {
                double val;
                if (logIntensity)
                {
                    double delta = Math.log10(spectrum[idx]) - logMin;
                    val = delta * scaleFactor;
                }
                else
                {
                    val = (spectrum[idx]-gmin) * scaleFactor;
                }
                val = Math.max(val, 0);
                val = Math.min(val, outMax);
                scaledSpectrum[idx] = val;
            }
        }
        else
        {
            for(int idx=0;idx<spectrum.length;idx++)
            {
                scaledSpectrum[idx] = 0;
            }
        }
    }

    /**
     * get number of frequency bins saved in this spectrum
     * @return number of bins
     */
    public int size()
    {
        return spectrum.length;
    }

    public double getFmin()
    {
        return fmin;
    }

    public double getFmax()
    {
        return fmax;
    }
    
    public double getBinWidth()
    {
        return binWidth;
    }

    /**
     * Get the interpolated value of the desired frequency
     * @param f desired frequency in Hz
     * @return the interpolated value
     */
    public double getScaledVal(double f)
    {
        double ret = 0;
        int fn;
        if (f >= fmin - 0.1 && f <= fmax + 0.1)
        {
            double dfn = (f-fmin)/(fmax-fmin) * (scaledSpectrum.length-2)+1;

            fn = (int) Math.round(dfn);
            fn = Math.max(0, fn);
            fn = Math.min(scaledSpectrum.length-1, fn);
            ret = scaledSpectrum[fn];
        }
        else
        {
            throw new IllegalArgumentException(String.format("Spectrum.getScaledValue: Invalid frequency %1$.2f", f));
        }
        return ret;
    }
    
    /**
     * Get the interpolated value of the desired frequency
     *
     * @param freq desired frequency in Hz
     * @return the interpolated value
     */
    
    public double getVal(double freq)
    {
        double ret = 0;
        int fn;
        // the 0.1's are because of float round off errors
        if (freq >= fmin - 0.1 && freq <= fmax+.1)
        {
            double dfn = (freq - fmin) / (fmax - fmin) * (spectrum.length - 2) + 1;

            fn = (int) dfn;
            fn = Math.max(0, fn);
            fn = Math.min(spectrum.length - 1, fn);
            double delta = dfn - fn;
            
            if (fn >= spectrum.length - 2 || delta <= 0 || delta >= 1)
            {
                ret = spectrum[fn];
            }
            else
            {
                 ret = spectrum[fn] + delta * (spectrum[fn+1] - spectrum[fn]);
            }
        }
        else
        {
            throw new IllegalArgumentException(String.format("Spectrum.getScaledValue: Invalid frequency %1$.2f", freq));
        }
        return ret;
    }
    
    public void dumpAscii(String fname) throws IOException
    {
        String filename = fname;
        if (fname == null || fname.isEmpty())
        {
            filename = "/tmp/spectrum.csv";
        }
        BufferedWriter bwr = new BufferedWriter(new FileWriter(filename));
        for(int f= 0; f<spectrum.length;f++)
        {
            if (scaledSpectrum == null)
            {
                bwr.append(String.format("%1$.5f, %2$.10g\n", (f + 1) * binWidth, spectrum[f]));
            }
            else
            {
                bwr.append(String.format("%1$.5f, %2$.10g, %3$.5f\n", (f+1)*binWidth, spectrum[f], scaledSpectrum[f]));
            }
        }
        bwr.close();
    }

    double getTime()
    {
        return startTime;
    }

    double[] getFvals()
    {
        return spectrum;
    }
    
    void setFvals(double[] fvals)
    {
        int l = fvals.length;
        if (l != spectrum.length)
        {
            throw new IllegalArgumentException("Spectrum.setFvals cannot change the length, use init");
        }
        spectrum = fvals;
        smin = Double.MAX_VALUE;
        smax = Double.MIN_VALUE;
        double tmp;
        boolean empty = true;
        for (int idx = 0; idx < fvals.length; idx++)
        {
            tmp = spectrum[idx];
            if (tmp > 0)
            {
                smin = Math.min(tmp, smin);
                smax = Math.max(tmp, smax);
                empty=false;
            }
        }
        if (empty)
        {
            smin=smax=0;
        }
    }

    public double getStartTime()
    {
        return startTime;
    }

    public double getSampleRate()
    {
        return sampleRate;
    }

    public double getSum()
    {
        return sum;
    }

    /**
     * Get value of the spectrum by index (not frequency)
     * @param idx 0 <=index value < number Frequncies  stored
     * @return value at that index
     */
    double getVal(int idx)
    {
        double ret;
        if (idx >= spectrum.length - 1)
        {
            ret = spectrum[spectrum.length -1];
        }
        else
        {
            ret = spectrum[idx];
        }
        return ret;
    }

    void setVal(int t, double d)
    {
        spectrum[t] = d;
    }

    /**
     * filtering can generate negative and zero values which frustrate log scaling.
     * So here we find the minimum non-zero value and set everything below that to it
     */
    void clipNegZero()
    {
        double tmin = Double.MAX_VALUE;
        double x;
        for(int idx=0;idx<spectrum.length;idx++)
        {
            x = spectrum[idx];
            if (x > 0)
            {
                tmin = Math.min(tmin, x);
            }
        }
        for (int idx = 0; idx < spectrum.length; idx++)
        {
            x = spectrum[idx];
            if (x <= 0)
            {
               spectrum[idx] = tmin;
            }
        }
    }
    
}
