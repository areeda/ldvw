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
package edu.fullerton.jspWebUtils;

/**
 *
 * @author Joseph Areeda <joe@areeda.com>
 */
public class WebUtilException extends Exception
{

    public WebUtilException(String msg)
    {
        super(msg);
    }
    
    public WebUtilException(Exception ex)
    {
        super(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
    }
    
    public WebUtilException(String message, Throwable cause)
    {
        super(message + ": " + cause.getClass().getSimpleName() + ": "
                + cause.getLocalizedMessage(), cause);
    }
}
