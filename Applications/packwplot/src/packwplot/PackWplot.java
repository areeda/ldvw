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
package packwplot;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Run after wplot collects images of the same type and uses ImageMajick to pack them into one png
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PackWplot
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        try
        {
            PackWplot me = new PackWplot();
            me.processArgs(args);
            me.processDir();
            me.packImages();
        }
        catch (IOException ex)
        {
            Logger.getLogger(PackWplot.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private File outDir;        /// Directory with wplot images
    private final TreeSet<WplotImageFile> imageFiles = new TreeSet<>();
    private final int verbose=3;
    private String outPath;

    /**
     * We take the same arguments as dmt_wplot. Find output directory.
     * @param args - command line arguments
     */
    private void processArgs(String[] args) throws IOException
    {
        List<String> dmtCmd = new ArrayList<>();
        dmtCmd.add("/usr/bin/dmt_wplot");
        for(String arg: args)
        {
            dmtCmd.add(arg);
            if (arg.startsWith("outDir"))
            {
                String[] dirArg = arg.split("=");
                if (dirArg.length > 1)
                {
                    String dirName = dirArg[1];
                    File dir = new File(dirName);
                    if (!dir.canRead())
                    {
                        System.err.format("Can't access %1$s%n", dir.getAbsolutePath());
                        System.exit(1);
                    }
                    outDir = dir;
                    outPath = dir.getCanonicalPath();
                }
            }
        }
        if (outDir == null)
        {
            outDir = new File(".");
            outPath = outDir.getCanonicalPath();
            dmtCmd.add("--outDir='" + outPath + "'");
        }
        if (!runExternalProgram(dmtCmd))
        {
            throw new AssertionError("dmt_wplot failed");
        }
    }

    /**
     * Scan the directory and sort image files for packing
     */
    private void processDir()
    {
        File[] files = outDir.listFiles();
        for (File file: files)
        {
            if (WplotImageFile.isMine(file))
            {
                WplotImageFile wif = new WplotImageFile();
                wif.init(file);
                imageFiles.add(wif);
            }
        }
        if (verbose > 2)
        {
            System.out.format("%d file(s) found%n",imageFiles.size());
        }
    }

    private void packImages() throws IOException
    {
        WplotImageFile last = null;
        List<WplotImageFile> flist = new ArrayList<>();     // these belong in one Montage
        for(WplotImageFile imageFile: imageFiles)
        {
            if (last == null)
            {
                last = imageFile;
            }
            else if (!last.isSameSet(imageFile))
            {
                pack(flist);
                last = imageFile;
                flist.clear();
            }
            flist.add(imageFile);
        }
        if (!flist.isEmpty())
        {
            pack(flist);
        }
    }

    private void pack(List<WplotImageFile> flist) throws IOException
    {
        String program = "/usr/bin/montage";
        String geometry;
        if (flist != null && !flist.isEmpty())
        {
            int size = flist.size();
            if (size > 1)
            {
                if (size < 4)
                {
                    geometry = String.format("+%1$d+1", size);
                }
                else
                {
                    int rows = (int)(Math.round(Math.sqrt(size)));
                    int cols = (size+rows-1)/rows;
                    geometry = String.format("+%1$d+%2$d",cols,rows);
                }
                List<String> cmd = new ArrayList<>();
                cmd.add(program);
                cmd.add("-geometry");
                cmd.add(geometry);
                for(WplotImageFile imageFile: flist)
                {
                    cmd.add(imageFile.file.getCanonicalPath());
                }
                String outfile = outPath + "/" + flist.get(0).getBase() + "-montage.png";
                cmd.add(outfile);
                runExternalProgram(cmd);
                rmRawFiles(flist);
            }
        }
    }
    private boolean runExternalProgram(List<String> cmd)
    {
        int status;
        try
        {
            Process p;
            String[] a = new String[0];
            p = Runtime.getRuntime().exec(cmd.toArray(a));

            InputStream stderrStrm = p.getErrorStream();    // attached to stderr
            // nb: the reason in and out are reversed is that I'm talking about the external program
            // and java is talking about this program.
            InputStream stdoutStrm = p.getInputStream();   // attached to stdout


            BufferedReader outRdr = new BufferedReader(new InputStreamReader(stdoutStrm));
            String line;
            StringBuilder outTxt = new StringBuilder();
            while ((line = outRdr.readLine()) != null)
            {
                outTxt.append(line).append("<br>\n");
            }

            StringBuilder errTxt = new StringBuilder();
            BufferedReader errRdr = new BufferedReader(new InputStreamReader(stderrStrm));
            while ((line = errRdr.readLine()) != null)
            {
                errTxt.append(line).append("<br>\n");
            }

            status = p.waitFor();
            if (status != 0 )
            {
                System.err.append(errTxt.toString());
            }
        }
        catch (InterruptedException|IOException ex)
        {
            Logger.getLogger(PackWplot.class.getName()).log(Level.SEVERE, null, ex);
            status = 1;
        }
        return status == 0;
    }

    private void rmRawFiles(List<WplotImageFile> flist)
    {
        for(WplotImageFile imageFile: flist)
        {
            File file = imageFile.file;
            file.delete();
        }
    }
}
