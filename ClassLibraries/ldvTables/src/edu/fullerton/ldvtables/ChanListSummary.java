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
package edu.fullerton.ldvtables;

import com.areeda.jaDatabaseSupport.Utils;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A data class to represent a partial channel list
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ChanListSummary
{
    private String server;
    private String cType;
    private int count;
    private String md5;
    private String crc;
    private File cListFile;
    private String dir="/tmp";
    private boolean needsUpd;
    
    /**
     * Minimal constructor
     * @param server
     * @param cType
     * @param count 
     */
    public ChanListSummary(String server, String cType, int count)
    {
        this.server = server;
        this.cType = cType;
        this.count = count;
        setDir();
    }

    public final void setDir()
    {
        String condorTmp = System.getenv("_CONDOR_SCRATCH_DIR");
         dir = "/tmp";
        if ( condorTmp != null && !condorTmp.isEmpty())
        {
            File tmp = new File(condorTmp);
            if (tmp.exists() && tmp.isDirectory() && tmp.canWrite())
            {
                dir = tmp.getAbsolutePath();
                if (!dir.endsWith("/"))
                {
                    dir += "/";
                }
            }
        }
    }
    /**
     * set the output directory for saving the list
     * 
     * @param dir path to a directory
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public void setDir(String dir) throws WebUtilException
    {
        File dirf = new File(dir);
        if (!dirf.exists())
        {
            throw new WebUtilException("ChanListSummary: setDir: directory does not exist");
        }
        if (!dirf.isDirectory())
        {
            throw new WebUtilException("ChanListSummary: setDir: but path is not a directory");
        }
        if (!dirf.canWrite())
        {
            throw new WebUtilException("ChanListSummary: setDir: Directory not writable");
        }
        this.dir = dir;
    }

    public String getServer()
    {
        return server == null ? "" : server;
    }

    public String getcType()
    {
        return cType == null ? "" : cType;
    }

    public int getCount()
    {
        return count;
    }

    public String getMd5()
    {
        return md5 == null ? "0" : md5;
    }

    public File getcListFile()
    {
        return cListFile;
    }

    public String getcListFilename() throws IOException
    {
        String ret = cListFile == null ? "<none>" : cListFile.getCanonicalPath();
        return ret;
    }

    public void setcListFile(File cListFile)
    {
        this.cListFile = cListFile;
    }
    
    public void setcListFile(String fName)
    {
        File clf = new File(fName);
        this.cListFile = clf;
    }

    public boolean isNeedsUpd()
    {
        return needsUpd;
    }

    public void setNeedsUpd(boolean needsUpd)
    {
        this.needsUpd = needsUpd;
    }
    
    public void printSummary() throws IOException
    {
        System.out.println(toString());
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return String.format("Server: %1$s, type: %2$s, count: %3$,d,  needs update: %4$b%n",
                          getServer(), getcType(), getCount(), needsUpd);
    }
    /**
     * Save an array of ChanInfo Objects save them to a text file
     * @param channelList
     * @throws IOException
     * @throws NoSuchAlgorithmException 
     */
    public void dumpFile(ArrayList<ChanInfo> channelList) throws IOException, NoSuchAlgorithmException
    {
        String fname = dir + "/" + server + "-" + cType + ".cList";
        cListFile = new File(fname);
        if (cListFile.exists())
        {
            cListFile.delete();
        }
        MessageDigest md;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(cListFile)))
        {
            md = MessageDigest.getInstance("MD5");
            for(ChanInfo ci : channelList)
            {
                String line = ci.getCSV() + "\n";
                byte[] data = line.getBytes();
                md.update(data, 0, data.length);
                bw.write(line);
            }
        }
        md5 = Utils.getMd5String(md);
    }

    /**
     * Read the text file with a list of channels and return a Map of Sets indexed on IFO
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     * @throws LdvTableException
     */
    public TreeMap<String,HashSet<ChanInfo>> getChanSets() throws FileNotFoundException, IOException, LdvTableException
    {
        TreeMap<String,HashSet<ChanInfo>> ret = new TreeMap<>();
        Pattern ifoPat = Pattern.compile("(.+):");
        Matcher ifoMat;
        String ifo;
        HashSet<ChanInfo> chnSet;
        
        if (cListFile != null && cListFile.exists() && cListFile.canRead())
        {
            BufferedReader br = new BufferedReader(new FileReader(cListFile));
            String line;
            while ((line = br.readLine())!= null)
            {
                ChanInfo ci = new ChanInfo();
                ci.fillCSV(line);
                ci.setServer(server);
                String cname = ci.getChanName();
                ifoMat = ifoPat.matcher(cname);
                if (ifoMat.find())
                {
                    ifo = ifoMat.group(1);
                    chnSet = ret.get(ifo);
                    if (chnSet == null)
                    {
                        chnSet = new HashSet<ChanInfo>();
                    }
                    chnSet.add(ci);
                    ret.put(ifo, chnSet);
                }
            }
        }
        
        return ret;
    }

    public String getCrc()
    {
        return crc;
    }

    public void setCrc(String crc)
    {
        this.crc = crc;
    }

   
}
