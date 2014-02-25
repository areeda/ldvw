/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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

package edu.fullerton.ndsproxyclient;

/**
 * Error during a network transaction
 * 
 * @author joe areeda
 */
public class NDSException extends Exception
{
    public NDSException() 
    {
        super();
    }

    /**
     * Construct a network exception with messages and causes from another exception
     * 
     * @param ex the exception specified as th cause
     */
    public NDSException(Exception ex)
    {
        super(ex);
    }
    
    /**
     * construct an exception with specified detailed message
     * @param message 
     */
    public NDSException(String message)
    {
        super(message);
    }
    
    /**
     * Construct an NDSException with the specified message and cause
     * 
     * @param message
     * @param cause cause, may be null to indicate cause is unknown
     */
    public NDSException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
