/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class LdvTableException extends Exception
{
    private String causeType = null;
    
    public LdvTableException() 
    {
        super();
    }

    /**
     * Construct a network exception with messages and causes from another exception
     * 
     * @param ex the exception specified as th cause
     */
    public LdvTableException(Exception ex)
    {
        super(ex);
        causeType = ex.getClass().getCanonicalName();
    }
    
    /**
     * construct an exception with specified detailed message
     * @param message 
     */
    public LdvTableException(String message)
    {
        super(message);
    }
    
    /**
     * Construct an LdvTableException with the specified message and cause
     * 
     * @param message
     * @param cause cause, may be null to indicate cause is unknown
     */
    public LdvTableException(String message, Throwable cause)
    {
        super(message + ": " +cause.getClass().getSimpleName() + ": "+ 
                cause.getLocalizedMessage(), cause);
    }
    
}
