/*
 * Copyright (C) 2014 Joseph Areeda <joseph.areeda at ligo.org>
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

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;
import java.util.List;

/**
 * Represents a single channel, time series source, whereas a base channel
 * can represent many.  This channel may be available on multiple servers
 * but we assume they are identical.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 * @version 1
 */
public class BaseChanSingle extends BaseChanSelection
{
    private String[] type;
    
    public BaseChanSingle(BaseChanSelection bcs, String[] type)
    {
        super.init(bcs);
        this.type = new String[2];
        System.arraycopy(type, 0, this.type, 0, type.length);
    }
    
    public String toString()
    {
        String ret = String.format("%1$s, %2$s", name,type[0]);
        if (type[0].contains("trend"))
        {
            ret += ", " + type[1];
        }
        return ret;
    }
    
    public String getType()
    {
        return type[0];
    }
    
    public String getTrendType()
    {
        return type[1];
    }
}
