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
package com.areeda.jaDatabaseSupport;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Blob;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * general, small functions that don't really fit into any other class.
 *
 * @author joe areeda
 */
public class Utils
{

    /**
     * Parse a tab seprated single line into the individual fields
     *
     * @param in - tab separated single line
     * @return String array of the fields in input string
     */
    public static String[] parseTSV(String in)
    {
        char sep = '\t';
        String[] ret = null;
        String t;
        ArrayList<String> flds = new ArrayList<String>();
        int pos = 0, epos;
        boolean notDone = true;
        int i;
        String s = in;                 // temporary

        for (i = 0; notDone; i++)
        {
            epos = s.indexOf(sep, pos);
            if (epos == pos)
            {
                t = "";
            }
            else
            {
                if (epos > 0)
                {
                    t = s.substring(pos, epos);
                }
                else
                {
                    t = s.substring(pos);
                    notDone = false;
                }
            }
            t = t.trim();
            if (t.length() > 0)
            {
                char c0 = t.charAt(0), c1 = t.charAt(t.length() - 1);
                if ((c0 == '"' && c1 == '"') || (c0 == '\'' && c1 == '\''))
                {
                    if (t.length() > 1)
                    {
                        t = t.substring(1, t.length() - 1);
                    }
                    else
                    {
                        t = "";
                    }
                }
            }
            pos = epos + 1;
            
            flds.add(t);
        }
        if (flds.size() > 0)
        {
            ret = new String[flds.size()];
            ret = flds.toArray(ret);
        }

        return ret;
    }

    public static String sqlQuote(String in)
    {
        int s = 0;
        int l = in.length();
        StringBuilder sb = new StringBuilder(l);

        String metaChar = "\'\\";

        if (in.startsWith("'") && in.endsWith("'"))
        {
            s = 1;
            l -= 1;
        }
        sb.append('\'');
        for (int i = s; i < l; i++)
        {
            char c = in.charAt(i);

            if (metaChar.indexOf(c) >= 0)
            {
                sb.append('\\');
            }
            sb.append(c);
        }
        sb.append('\'');
        return sb.toString();
    }

    /**
     * convert a Unix timestamp to date time string
     *
     * @param t Unix timestamp (milliseconds since 1/1/1970 eg System.currentTimeMillis();
     * @return time formatted as yyyy-mm-dd hh:mm:ss
     */
    public static String timestamp2DateTime(long t)
    {
        String ret;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        ret = sdf.format(t);
        return ret;

    }

    public static String getHostName()
    {
        String hostname = "unknown";
        try
        {
            InetAddress addr = InetAddress.getLocalHost();

            // Get hostname
            hostname = addr.getHostName();
        }
        catch (UnknownHostException e)
        {
        }
        return hostname;
    }

    public static String md5sum(InputStream in) throws IOException, NoSuchAlgorithmException
    {
        in.reset();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] dataBytes = new byte[1024];

        int nread;
        while ((nread = in.read(dataBytes)) != -1)
        {
            md.update(dataBytes, 0, nread);
        }
        return getMd5String(md);
    }

    public static String md5sum(byte[] data) throws NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data, 0, data.length);
        return getMd5String(md);
    }

    public static String md5sum(float[] data) 
    {
        String ret="error";
        try
        {
            byte[] b = float2ByteArray(data);
            ret = md5sum(b);
        }
        catch (Exception ex)
        {
            String ermsg = "Calculating md5sum of data: " + ex.getClass().getSimpleName() + " - " 
                + ex.getLocalizedMessage();
        }
        return ret;
    }

    public static String getMd5String(MessageDigest md)
    {
        byte[] mdbytes = md.digest();

        //convert the byte to hex format method 1
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mdbytes.length; i++)
        {
            sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * 
     * @param data
     * @return 
     */
    public static byte[] float2ByteArray(float[] data) throws IOException
    {
        byte[] ret;
        int len = data.length;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(len*Float.SIZE/8);
        DataOutputStream dos = new DataOutputStream(bos);
        for (int in = 0; in < len; in++)
        {
            dos.writeFloat(data[in]);
        }
        ret = bos.toByteArray();

        return ret;
    }
    /**
     * Convert a blob that we wrote a float array back into a usable array
     * @param blob from the database
     * @return
     * @throws SQLException 
     */
    public static float[] blob2FloatArray(Blob blob) 
    {
        float[] ret = null;
        try
        {
            int blen = (int) blob.length();
            int nbytes = Float.SIZE/8;
            int olen = (int) (blen/nbytes);
            ret = new float[olen];
            DataInputStream dis = new DataInputStream(blob.getBinaryStream());
        
            for(int o=0;o<olen;o++)
            {
                ret[o] = dis.readFloat();
            }
        }
        catch (Exception ex)
        {
            ret = null;
        }
        return ret;
    }
}
