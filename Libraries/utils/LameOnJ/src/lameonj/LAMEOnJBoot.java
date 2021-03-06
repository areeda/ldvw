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

package lameonj;

import lameonj.impl.LAMEOnJBootImpl;

/**
 * <code>LAMEOnJBoot</code> is the root factory of LAMEOnJ. 
 *
 */
public abstract class LAMEOnJBoot
{
    protected static final LAMEOnJBootImpl SINGLETON = new LAMEOnJBootImpl();
    
    /**
     * Creates a new instance of LAMEOnJ
     */
    protected LAMEOnJBoot()
    {
    }
    
    /**
     * Returns the singleton object of {@link LAMEOnJ}.
     *
     * @return the singleton object.
     */
    public static LAMEOnJ get()
    {
        return SINGLETON.getLAMEOnJ();
    }

}
