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
package excesspower;

import edu.fullerton.ldvjutils.LdvTableException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import viewerconfig.ViewConfigException;

/**
 * Manage ExcessPower plot generation
 * 
 * Most of the heavy lifting is done by external programs
 * This application provides an easy interface meant to
 * be run as a Condor job or from the command line.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ExcessPower
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        int ret;
        ExcessPower me = new ExcessPower();
        ret = me.doAll(args);
        System.exit(ret);
    }
    private long startTime;
    private long transferStart;
    private long plotStart;
    private long emailStart;
    private StringWriter errStringWriter;
    private PrintWriter errPrintWriter;
    private PrintWriter outPrintWriter;
    private StringWriter outStringWriter;

    private int doAll(String[] args)
    {
        int ret;
        startTime = System.currentTimeMillis();
        // initialize the rest in case an error keeps them from running;
        transferStart = startTime;
        plotStart = startTime;
        emailStart = startTime;

        errStringWriter = new StringWriter();
        errPrintWriter = new PrintWriter(errStringWriter);


        outStringWriter = new StringWriter();
        outPrintWriter = new PrintWriter(outStringWriter);

        ret = doit(args);
        System.out.println(outStringWriter.toString());
        System.err.println(errStringWriter.toString());
        return ret;

    }

    private int doit(String[] args)
    {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
