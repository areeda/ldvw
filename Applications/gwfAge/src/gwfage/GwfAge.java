
/*
 * Copyright (C) 2016 Joseph Areeda <joseph.areeda@ligo.org>
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
package gwfage;

import static edu.fullerton.ldvjutils.TimeAndDate.gps2date;
import static edu.fullerton.ldvjutils.TimeAndDate.gps2utc;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scan frame files looking at the time specified in the file name and the 
 * timestamp to determine the latency.
 * @author Joseph Areeda <joseph.areeda@ligo.org>
 */
public class GwfAge
{

    /**
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        GwfAge me = new GwfAge();
        String dir = args[0];
        try
        {
            me.scanDir(dir);
            me.report();
        }
        catch (IOException ex)
        {
            Logger.getLogger(GwfAge.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private ArrayList<Long> ageList;
    private int dirCnt;
    private Pattern pat;

    public void scanDir(String dir) throws IOException
    {
        ageList = new  ArrayList<>(2000);
        dirCnt=0;
        String fpat = ".+-(\\d+)-(\\d+).gwf";
        pat = Pattern.compile(fpat);
        Path path = Paths.get(dir);
        Files.walk(path).forEach(pth -> procFile(pth));
    }

    private void procFile(Path pth)
    {
        File file = pth.toFile();
        if (file.isDirectory())
        {
            dirCnt++;
        }
        else
        {
            String name = pth.getFileName().toString();
            Matcher m = pat.matcher(name);
            if (m.find())
            {
                int strt = Integer.parseInt(m.group(1));
                int dur = Integer.parseInt(m.group(2));
                int end =  strt + dur;
                long utcEnd = gps2utc(end);
                
                Date lastmod = new Date(file.lastModified());
                Date frameEnd = gps2date(end);
                long dage = (lastmod.getTime() - frameEnd.getTime()) / 1000;
                
                long age = (lastmod.getTime() +500) / 1000 - utcEnd;
                ageList.add(age);
                
            }
            
        }
        
    }

    private void report()
    {
        int n = ageList.size();
        System.out.format("files: %1$d%n", n);
        if (n > 3)
        {
            Collections.sort(ageList);
            long min, max, mean, mode;
            min = ageList.get(0);
            max = ageList.get(n-1);
            mode = ageList.get(n/2);
            
            int hsize = (int) Math.sqrt(n);
            int[] hist = new int[hsize];
            Arrays.fill(hist, 0);
            double fact = (max - min) / hsize;
            long tot = 0;
 
            int[] divs = { 0, 300, 1800, 3600, 3600*2, 3600*4, 3600*8, 3600*24};
            int[] cts = new int[divs.length];
            Arrays.fill(cts, 0);
            for(Long age : ageList)
            {
                tot += age;
                int idx = (int) ((age - min) / fact);
                idx = Math.min(idx, hsize-1);
                hist[idx]++;
                
                for (int i = divs.length - 1; i >= 0; i--)
                {
                    if (age >= divs[i])
                    {
                        cts[i]++;
                        break;
                    }
                }

            }
            System.out.format("min: %1$d%n", min);
            System.out.format("mode: %1$d%n", mode);
            System.out.format("mean: %1$d%n", tot/n);
            System.out.format("max: %1$d%n%n", max);
            
            for (int i = 0; i < divs.length - 1; i++)
            {
                System.out.format("\"%1$d-%2$d\", %3$d%n", divs[i], divs[i + 1] - 1, cts[i]);
            }

            System.out.println("");
            
            for(int i=0; i <hsize; i++)
            {
                long agec = (long) (min + i * fact);
                System.out.format("%1$d %2$d%n", agec, hist[i]);
            }
        }    
    }
    
}
