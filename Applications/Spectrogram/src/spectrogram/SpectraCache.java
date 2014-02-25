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

import edu.fullerton.ldvjutils.Butterworth;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class SpectraCache
{
    private Spectrum[] spectra;
    private int next = 0;
    private double gmin;
    private double gmax;
    private double binWidth;

    private int debugLevel = 1;
    private double iMin;
    private double iMax;
    private final int maxColor=250;
    private boolean smooth = false;
    private double lo;
    private double up;

    public enum Interpolation
    {

        NONE, LINEAR
    };
    private Interpolation interpolation = Interpolation.NONE;
    
    public enum Normalization  { ALL, EACH, DIVBYMEAN, EDGE };
    
    public SpectraCache(int nfft)
    {
        spectra = new Spectrum[nfft];
    }

    void add(double t, double[] result, double sampleRate, double fmax, double fmin, double binWidth)
    {
        if (next < spectra.length)
        {
            Spectrum spectrum = new Spectrum();
            spectrum.init(t,result,sampleRate,fmax,fmin,binWidth);

            spectra[next] = spectrum;
            next++;
            if (debugLevel > 3)
            {
                if (next % 10 == 0)
                {
                    System.out.print(next);
                }
                else if (next % 5 == 0)
                {
                    System.out.print("5");
                }
                else
                {
                    System.out.print(".");
                }
                System.out.flush();
            }
        }
    }
    
    public int size()
    {
        return next;
    }
    public void normalize(Normalization norm, boolean logIntensity) throws IOException
    {
        // Find the global min, max values
        gmin=Double.MAX_VALUE;
        gmax = Double.MIN_VALUE;
        BufferedWriter bw=null;
        if (debugLevel>2)
        {
            bw = new BufferedWriter(new FileWriter("/tmp/MinMaxSum.csv"));
        }
        for(int idx =0; idx < next; idx++)
        {
            double tmin = spectra[idx].getMin();
            double tmax = spectra[idx].getMax();
            double sum = spectra[idx].getSum();
            double t = spectra[idx].getStartTime();
            
            if (bw != null)
            {
                String l = String.format("%1$.3f, %2$.10f, %3$.10f, %4$,.5f\n", t, tmin,tmax,sum);
                bw.write(l);
            }
            
            gmin = Math.min(gmin, tmin);
            gmax = Math.max(gmax, tmax);
        }
        if (bw != null)
        {
            bw.close();
        }
        switch(norm)
        {
            case ALL:
            default:
                double range = gmax-gmin;
                for (int idx = 0; idx < next; idx++)
                {
                    spectra[idx].normalize(250,gmin,gmax,logIntensity);
                }
                break;
                
            case DIVBYMEAN:
                divByMean();
                normalize(Normalization.ALL, logIntensity);  // then normalize the remaining
                
            case EACH:
                for (int idx = 0; idx < next; idx++)
                {
                    spectra[idx].normalize(250, logIntensity);
                }
                break;
                
            case EDGE:
                throw new UnsupportedOperationException("Diff normalization not yet implemented");

        }
    }
    
    /**
     * Get the number of frequencies in each spectrum, and check that they are all the same
     * @return number of frequency bins saved or -1 if no non null spectra are saved
     * @throws IllegalStateException if cache contains spectra of differing lengths
     */
    public int getNFreq()
    {
        int n = -1;
        for (int idx = 0; idx < next; idx++)
        {
            if (!spectra[idx].isNull())
            {
                int ns = spectra[idx].size();
                if (n < 0)
                {
                    n = ns;
                }
                else if (ns != n)
                {
                    throw new IllegalStateException("Spectrum cache contains different length spectra. BUG");
                }
            }
        }
        return n;
    }
    
    public double getSmin()
    {
        Double ret = null;
        
        for (int idx = 0; idx < next; idx++)
        {
            if (!spectra[idx].isNull())
            {
                double min = spectra[idx].getFmin();
                if (ret == null)
                {
                    ret = min;
                }
                else if (min != ret)
                {
                    throw new IllegalStateException("Spectrum cache contains different minimum frequencies. BUG");
                }
            }
        }
        if (ret == null)
        {
            ret = 0.;
        }
        return ret;
    }

    /**
     * Scan the spetra and return the maximum frequency confirming all are identical
     * 
     * @return maximum frequency value
     */
    public double getSmax()
    {
        Double ret = null;

        for (int idx = 0; idx < next; idx++)
        {
            if (!spectra[idx].isNull())
            {
                double min = spectra[idx].getFmax();
                if (ret == null)
                {
                    ret = min;
                }
                else if (min != ret)
                {
                    throw new IllegalStateException("Spectrum cache contains different maximum frequencies. BUG");
                }
            }
        }
        if (ret == null)
        {
            ret = 0.;
        }
        return ret;
    }

    double getBinWidth()
    {
         Double ret = null;

        for (int idx = 0; idx < next; idx++)
        {
            if (!spectra[idx].isNull())
            {
                double bw = spectra[idx].getBinWidth();
                if (ret == null)
                {
                    ret = bw;
                }
                else if (bw != ret)
                {
                    throw new IllegalStateException("Spectrum cache contains different bandwidths. BUG");
                }
            }
        }
        if (ret == null)
        {
            ret = 0.;
        }
        return ret;
        
    }
    
    double getTmax()
    {
        Double ret = null;

        for (int idx = 0; idx < next; idx++)
        {
            if (!spectra[idx].isNull())
            {
                double t = spectra[idx].getTime();
                if (ret == null)
                {
                    ret = t;
                }
                else if (t > ret)
                {
                    ret = t;
                }
            }
        }
        if (ret == null)
        {
            ret = 0.;
        }
        return ret;

    }

    /**
     * Calculate the index value for the requested pixel
     * @param specNum somewhere in the list of spectra
     * @param freq somewhere in the frequency list (by number not Hz)
     * @return pixel value
     */
    double getScaledVal(double specNum, double freq)
    {
        double ret = 0;
        
        switch(interpolation)
        {
            case NONE:
            {
                int t;
                int f;
                t = (int) (specNum);
                f = (int) (freq);
                if (t >= next || f > getSmax() || t < 0 || f < getSmin())
                {
                    throw new IllegalArgumentException("getScaledVal: Frequency or time out of bounds for get value");
                }
                if (!spectra[t].isNull())
                {
                    ret = spectra[t].getScaledVal(f);
                }
                else
                {
                    throw new IllegalArgumentException("getScaledVal: request spectra has no values.");
                }
            }
                break;
                
            case LINEAR:
            default:
            {
                int t = (int) specNum;
                double dt = specNum - t;
                
                if (t >= next || t < 0 )
                {
                    throw new IllegalArgumentException("Interpolation: Time out of bounds for get value");
                }
                if (!spectra[t].isNull())
                {
                    int t1 = t + 1;
                    t1 = t1 >= next ? next - 1 : t1;
                    
                    double x0 = spectra[t].getScaledVal(freq);
                    double x1 = spectra[t1].getScaledVal(freq);

                    double z  = x0 + dt * (x1-x0);
                    
                    ret = z;
                }
            }
                break;
        }
        
        return ret;
    }

    /**
     * Calculate the possibly interpolated value for the requested pixel
     *
     * @param specNum somewhere in the list of spectra
     * @param freq somewhere in the frequency list (by number not Hz)
     * @return pixel value
     */
    double getVal(double specNum, double freq)
    {
        double ret = 0;
        boolean bad = false;
        
        switch (interpolation)
        {
            case NONE:
            {
                int t;
                
                t = (int) (specNum);
                
                if (t >= next || freq > getSmax() +0.01 || t < 0 || freq < getSmin() - 0.01)
                {
                    throw new IllegalArgumentException("getScaledVal: Frequency or time out of bounds for get value");
                }
                if (!spectra[t].isNull())
                {
                    ret = spectra[t].getVal(freq);
                    if (ret < 0)
                    {
                        bad = true;
                    }
                }
                else
                {
                    throw new IllegalArgumentException("getScaledVal: request spectra has no values.");
                }
            }
            break;

            case LINEAR:
            default:
            {
                int t = (int) specNum;
                double dt = specNum - t;
                

                if (t >= next || t < 0)
                {
                    throw new IllegalArgumentException("Interpolation: Time out of bounds for get value");
                }
                if (!spectra[t].isNull())
                {
                    int t1 = t + 1;
                    t1 = t1 >= next ? next - 1 : t1;

                    double x0 = spectra[t].getVal(freq);
                    double x1 = spectra[t1].getVal(freq);

                    double z = x0 + dt * (x1 - x0);
                    if (z < 0)
                    {
                        bad=true;
                    }

                    ret = z;
                }
            }
            break;
        }

        return ret;
    }
    /**
     * Take the cached spectra and interpolated/decimate them into the appropriate section of
     * the output image
     * 
     * @param rast the raster of the whole results image including labels and colortables that we dont are about
     * @param imgR the area we put out data into
     * @param norm how do we normalize the different spectra into one map of power to color
     * @param logIntensity the color map is logarithmic
     * @param logFreq the frequency axis is logarithmic
     * @throws IOException 
     */
        void makeImage(WritableRaster rast, Rectangle imgR, Normalization norm, boolean logIntensity, 
                   boolean logFreq) throws IOException
    {
        int imgX0 = (int) imgR.getX();
        int imgY0 = (int) imgR.getY();
        int dimX = (int) imgR.getWidth();
        int dimY = (int) imgR.getHeight();
        
        int nSpec = size();
        int nFreq = getNFreq();

        
        
        // here normalize means scale the amplitude/power to a common value
        normalize(norm, logIntensity);
        
        if (smooth)
        {
            smoothFreq(dimY);
            smoothTime(dimX);
            clipNegZero();
        }
        // for debuggery write out everything
        if (debugLevel > 4)
        {
            for (int idx = 0; idx < spectra.length; idx++)
            {
                String filename = String.format("/tmp/spectrum-%1$03d.csv", idx+1);
                if (spectra[idx] != null)
                {
                    spectra[idx].dumpAscii(filename);
                }
            }
        }
        double[][] dImg = new double[dimX][dimY];
        
        
        double fvals[] = new double[dimY];  // holds the frequency value at each Y- pixel
        double tvals[] = new double[dimX];  // holds the time value at each X - pixel
        if (logFreq)
        {
            double fsmin = getSmin();
            double fsmax = getSmax();
            if (fsmin <= 0 || fsmax <= 0 || fsmin == fsmax)
            {
                throw new IllegalStateException("Can't take logs of this frequency range.");
            }
            double lfmin = Math.log10(fsmin);
            double lfmax = Math.log10(fsmax);
            double fsrange = lfmax - lfmin;

            for (int y = 0; y < dimY; y++)
            {
                double freq;
                
                double yfrac = (double)(dimY-y-1)/(dimY-1);      // 0 to 1
                double lf = fsrange * yfrac + lfmin;
                
                freq = Math.pow(10, lf); // in the range of frequencies

                fvals[y] = freq;
            }
        }
        else
        {
            double fsmin = getSmin();
            double fsmax = getSmax();
            double fsrange = fsmax-fsmin;
            double fscale = fsrange/(dimY-1);
            for (int f = 0; f < dimY; f++)
            {
                fvals[dimY - f - 1] = f * fscale + fsmin;
            }
        }
        
        // get the interpolated values for translating x pixel # to time (spectrum number)
        for (int t = 0; t < dimX; t++)
        {
            tvals[t] = (double) t * (next) / dimX;      // factor to convert x coord to spectrum number
        }

        if (debugLevel > 2)
        {
            dumpVals(tvals, fvals); // debugging
        }
        // Make a double array of pixels
        double tval;
        for (int t = 0; t < dimX; t++)
        {
            tval = tvals[t];      // factor to convert x coord to spectrum number
            for (int f = 0; f < dimY; f++)
            {
                double xx;
                if (norm == Normalization.EACH)
                {
                    xx = getScaledVal(tval, fvals[f]);
                }
                else
                {
                    xx = getVal(tval, fvals[f]);
                }
                dImg[t][f] = xx;
            }
        }
        // now convert the double precision pixels to bytes
        getIminMax(dImg,logIntensity);
        double delta = iMax-iMin;
        double scale = delta > 0 ? maxColor/delta : 0;
        for(int x=0;x<dimX;x++)
        {
            for(int y=0;y<dimY;y++)
            {
                double it = dImg[x][y];
                int pVal = (int) Math.round((it-iMin) * scale);
                pVal = Math.max(0, pVal);
                pVal = Math.min(maxColor,pVal);
                byte rVal = (byte)(pVal & 255);
                rast.setSample(x+imgX0, y+imgY0, 0, rVal);
            }
        }
    }
    /**
     * debugging routine to validate conversion factors
     *
     * @param fvals
     */
    private void dumpVals(double[] tvals, double[] fvals) throws IOException
    {
        String filename = "/tmp/fvals.csv";

        BufferedWriter bwr = new BufferedWriter(new FileWriter(filename));
        for (int f = 0; f < Math.max(tvals.length, fvals.length); f++)
        {
            if (f < tvals.length)
            {
                bwr.append(String.format("%1$9.5f,", tvals[f]));
            }
            else
            {
                bwr.append("         ,");
            }
            if (f < fvals.length)
            {
                bwr.append(String.format("%1$.5f\n", fvals[f]));
            }
            else
            {
                bwr.append("\n");
            }
        }
        bwr.close();
    }
    
    private void getIminMax(double[][] dImg, boolean logIntensity) throws IOException
    {
        int dimX = dImg.length;
        int dimY = dImg[0].length;
        
        iMin = Double.MAX_VALUE;
        iMax = Double.MIN_VALUE;
        double badMin = Double.MAX_VALUE;
        int badCnt=0;
        int goodCnt = 0;
        for (int x = 0; x < dimX; x++)
        {
            for (int y = 0; y < dimY; y++)
            {
                double it = dImg[x][y];
                if (logIntensity)
                {
                    if (it > 0)
                    {
                        it = Math.log10(it);
                        dImg[x][y] = it;
                        iMin = Math.min(iMin, it);
                        iMax = Math.max(iMax, it);
                        goodCnt++;
                    }
                    else
                    {
                        badMin = Math.min(badMin,it);
                        badCnt++;
                        dImg[x][y] = iMin;
                        if (debugLevel > 4)
                        {
                            System.err.format("Bad val (%1$4d,%2$4d) %3$.3f\n", x,y,it);
                        }
                    }
                }
                else
                {
                    iMin = Math.min(iMin, it);
                    iMax = Math.max(iMax, it);

                }
            }
        }
        if (debugLevel > 1 && badCnt > 0)
        {
            System.out.format("Bad values: %,d, good values: %,d\n",badCnt, goodCnt);
        }
        int histSize = 1000;
        int hist[] = new int[histSize];
        double range = iMax - iMin;
        if (range > 0)
        {
            double binSize = range/histSize;
            for(int h=0;h<histSize;h++)
            {
                hist[h] = 0;
            }
            int bin;
            int n=dimX*dimY;
            for (int x = 0; x < dimX; x++)
            {
                for (int y = 0; y < dimY; y++)
                {
                    double it = dImg[x][y];
                    bin = (int) Math.round((it - iMin)/binSize);
                    bin = Math.min(bin, histSize-1);
                    if (bin < 0)
                    {
                        bin=0;      // I needed a breakpoint
                    }
                    hist[bin]++;
                }
            }
            if (debugLevel > 2)
            {
                dumpHist(hist,iMin,binSize);
            }
            int pMin=0;
            int pMax=histSize-1;
            int t=0;
            double pcntlMin = lo;
            double pcntlMax = up;
            for(int h=0;h<histSize;h++)
            {
                t+=hist[h];
                if (t < n * pcntlMin)
                {
                    pMin = h;
                }
                if (t < n * pcntlMax)
                {
                    pMax=h;
                }
            }
            double tMin = iMin;
            iMin = pMin * binSize + tMin;
            iMax = pMax * binSize + tMin;
        }
        if (debugLevel > 3)
        {
            String minmax = String.format("Final image min max = (%1$.5f, %2$.5f)\n", iMin,iMax);
            System.out.println(minmax);
        }
    }

    void dumpHist(int[] hist,double min, double binSize) throws IOException
    {
        
        String filename = "/tmp/hist.csv";

        BufferedWriter bwr = new BufferedWriter(new FileWriter(filename));
        for (int f = 0; f < hist.length; f++)
        {
            double val = min + binSize * f;
            bwr.append(String.format("%1$.5f, %2$d\n", val,hist[f]));
        }
        bwr.close();

    }

    private void divByMean()
    {
        int nFreq = getNFreq();
        double[] mean = new double[nFreq];
        for(int f=0;f<nFreq;f++)
        {
            mean[f] = 0;
        }
        
        // calculate the mean of each frequency value (power or amplitude)
        for(int t=0;t<next;t++)
        {
            double[] fVals = spectra[t].getFvals();
            for (int f = 0; f < nFreq; f++)
            {
                mean[f] += fVals[f]/nFreq;
            }
        }
        // 
        for (int t = 0; t < next; t++)
        {
            double[] fVals = spectra[t].getFvals();
            for (int f = 0; f < nFreq; f++)
            {
                if (mean[f] > 0)
                {
                    fVals[f] /= mean[f];
                }
            }
            spectra[t].setFvals(fVals);
        }
    }
    /**
     * Smooth the calculated spectra across time not frequencies
     * 
     * @param dimX output length used to guess optimal cut off frequency
     */
    private void smoothTime(int dimX) throws IOException
    {
        int n = next;
        float cutoff = (float) (dimX / 50.0 / n);  // 2 of that is because nyquist is 0.5
        cutoff = (float) Math.min(0.3, cutoff);     // well we should do some smoothing
        double[] kernel;
        int kSize = Math.min(61,n/2);
        if (kSize > 2)
        {
            kernel = Butterworth.getKernel(false, kSize, cutoff, 4);
            double[] temp = new double[n];
            int nf = getNFreq();

            for(int f = 0; f<nf; f++)
            {
                for(int t = 0; t< n; t++)
                {
                    double x = spectra[t].getVal(f);
                    temp[t] = x;
                }
                double[] temp2 = Butterworth.convolve(temp, kernel);
                String saveFilename = String.format("/tmp/smoothTime-%04d.csv",f);
                BufferedWriter saveSmooth = null;
                if ((nf < 20 || f % 10 == 0) && debugLevel > 2)
                {
                    saveSmooth = new BufferedWriter(new FileWriter(saveFilename));
                }
                for(int t = 0; t < n; t++)
                {
                    if (saveSmooth != null)
                    {
                        String saveStr = String.format("%1$d, %2$.10f, %3$.10f\n", 
                                                       t,temp[t],temp2[t]);
                        saveSmooth.write(saveStr);
                    }
                    spectra[t].setVal(f,temp2[t]);
                }
                if (saveSmooth != null)
                {
                    saveSmooth.close();
                }
            }
        }
    }
    /**
     * Smooth each spectrum across frequencies
     * @param dimY
     * @throws IOException 
     */
    private void smoothFreq(int dimY) throws IOException
    {
        int n = getNFreq();
        float cutoff = (float) (dimY / 4.0 / n);  // 2 is because nyquist is 0.5
        cutoff = (float) Math.min(0.3, cutoff);     // well we should do some smoothing
        double[] kernel;
        kernel = Butterworth.getKernel(false, 11, cutoff, 4);
        double[] temp = new double[n];
        int nt = next;

        for (int t = 0; t < nt; t++)
        {
            for (int f = 0; f < n; f++)
            {
                double x = spectra[t].getVal(f);
                temp[f] = x;
            }
            double[] temp2 = Butterworth.convolve(temp, kernel);
            String saveFilename = String.format("/tmp/smoothFreq-%04d.csv", t);
            BufferedWriter saveSmooth = null;
            if ((nt < 20 || t % 100 == 0) && debugLevel > 3)
            {
                saveSmooth = new BufferedWriter(new FileWriter(saveFilename));
            }
            for (int f = 0; f < n; f++)
            {
                if (saveSmooth != null)
                {
                    String saveStr = String.format("%1$.3f, %2$.10f, %3$.10f\n",
                                                   f*getBinWidth()+getSmin(), temp[f], temp2[f]);
                    saveSmooth.write(saveStr);
                }
                spectra[t].setVal(f, temp2[f]);
            }
            if (saveSmooth != null)
            {
                saveSmooth.close();
            }
        }
        
    }
    /**
     * Filtering can add negative and zero values to the spectra, fix that!
     */
    private void clipNegZero()
    {
        int nt = next;

        for (int t = 0; t < nt; t++)
        {
            spectra[t].clipNegZero();
        }
    }
    /**
     * Image minimum is available after makeImage, this value is used for color mapping, not 
     * necessarily the actual minimum value in the data
     * @return minimum pixel value
     */
    public double getiMin()
    {
        return iMin;
    }

    /**
     * Image maximum is available after makeImage this value is used for color mapping, not 
     * necessarily the actual minimum value in the data
     * @return maximum pixel value
     */
    public double getiMax()
    {
        return iMax;
    }

    public void setDebugLevel(int debugLevel)
    {
        this.debugLevel = debugLevel;
    }

    public void setSmooth(boolean smooth)
    {
        this.smooth = smooth;
    }

    void setUp(float up)
    {
        this.up = up;
    }

    void setLo(float lo)
    {
        this.lo = lo;
    }    

    void setInterp(boolean interp)
    {
        interpolation = interp ? Interpolation.LINEAR : Interpolation.NONE;
    }
}
