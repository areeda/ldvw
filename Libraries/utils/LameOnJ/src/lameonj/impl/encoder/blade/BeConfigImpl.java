/*
 * LAMEOnJ Java based API for LAME MP3 encoder/decoder
 *
 * Copyright (c) 2006-2008 Jose Maria Arranz
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

package lameonj.impl.encoder.blade;

import com.innowhere.jnieasy.core.JNIEasy;
import lame.blade.BE_CONFIG;
import lameonj.encoder.blade.BeConfig;

public class BeConfigImpl implements BeConfig
{
    protected BE_CONFIG config;
            
    /**
     * Creates a new instance of BeConfigImpl
     */
    public BeConfigImpl()
    {
        this.config = new BE_CONFIG();
        JNIEasy.get().getNativeManager().makeNative(config);
        // Make native is highly recommended before setting any property
        // because is a union
    }
    
    public BE_CONFIG getBE_CONFIG()
    {
        return config;
    }
  
}
