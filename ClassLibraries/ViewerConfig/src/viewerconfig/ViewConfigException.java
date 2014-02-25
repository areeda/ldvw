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
package viewerconfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ViewConfigException extends Exception
{
    private String causeType = null;
    private Throwable cause;

    public ViewConfigException()
    {
        super();
    }

    /**
     * Construct a network exception with messages and causes from another exception
     *
     * @param ex the exception specified as th cause
     */
    public ViewConfigException(Exception ex)
    {
        super(ex);
        causeType = ex.getClass().getCanonicalName();
    }

    /**
     * construct an exception with specified detailed message
     *
     * @param message
     */
    public ViewConfigException(String message)
    {
        super(message);
    }

    /**
     * Construct an viewConfigException with the specified message and cause
     *
     * @param message
     * @param cause cause, may be null to indicate cause is unknown
     */
    public ViewConfigException(String message, Throwable cause)
    {
        super(message + ": " + cause.getClass().getSimpleName() + ": "
                + cause.getLocalizedMessage(), cause);
    }

    /**
     * Returns the cause of this exception or null if the cause is nonexistent or unknown.
     *
     * @returns the cause of this exception or null if the cause is nonexistent or unknown.
     */
    @Override
    public Throwable getCause()
    {
        return this.cause;
    }
}
