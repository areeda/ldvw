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

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class represent the output images of dmt_wplot
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class WplotImageFile implements Comparable
{
    private static final Pattern filePat = Pattern.compile("^([\\d.]+)_(.*)_([\\d.]+)_(.*).png$");
    public Double time;
    public String chan;
    public Double length;
    public String type;
    public File file;
    
    static boolean isMine(File file)
    {
        boolean ret;
        Matcher m = filePat.matcher(file.getName());
        ret = m.find();
        return ret;
    }

    /**
     * Breakdown the file name into fields for sorting
     * @param file 
     */
    void init(File file)
    {
        Matcher m = filePat.matcher(file.getName());
        if (m.find())
        {
            this.file = file;
            time = Double.parseDouble(m.group(1));
            chan = m.group(2);
            length = Double.parseDouble(m.group(3));
            type = m.group(4);
        }
    }
    @Override
    public String toString()
    {
        String description;
        description = String.format("%1$s - %2$.3f - %3$.2f, %4$s", chan, time, length, type);
        return description;
    }

    @Override
    public int compareTo(Object o)
    {
        int ret;
        WplotImageFile other = (WplotImageFile)o;
        ret = time.compareTo(other.time);
        if (ret == 0)
        {
            ret = type.compareTo(other.type);
        }
        if (ret == 0)
        {
            ret = chan.compareTo(other.chan);
        }
        if (ret == 0)
        {
            ret = length.compareTo(other.length);
        }
        return ret;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final WplotImageFile other = (WplotImageFile) obj;
        if (!Objects.equals(this.time, other.time))
        {
            return false;
        }
        if (!Objects.equals(this.chan, other.chan))
        {
            return false;
        }
        if (!Objects.equals(this.length, other.length))
        {
            return false;
        }
        if (!Objects.equals(this.type, other.type))
        {
            return false;
        }
        return true;
    }
    @Override
    public int hashCode()
    {
        int ret;
        ret = chan.hashCode();
        ret *= 13 + type.hashCode();
        ret *= 7 + time.hashCode();
        ret *= 3 + length.hashCode();
        return ret;
    }

    boolean isSameSet(WplotImageFile imageFile)
    {
        boolean ret = chan.equals(imageFile.chan)
                      && Math.abs(time - imageFile.time) < 1e-4
                      && type.equals(imageFile.type);
        return ret;
    }

    String getBase()
    {
        String base = String.format("%1$.3f_%2$s_%3$s",time, chan, type);
        return base;
    }
}
