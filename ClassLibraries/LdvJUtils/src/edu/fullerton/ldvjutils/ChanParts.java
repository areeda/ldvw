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
 * A class representing the different fields in the channel tables for searching/selecting
 * 
 * @author Joseph Areeda <joe@areeda.com>
 */
public class ChanParts
{
    private static final String[] ifo =
    {
        "G1", "H0", "H1", "H2", 
        "L0", "L1",
        "V1", "C0",
        "HVE-EX", "HVE-EY", "HVE-LX","HVE-LY", "HVE-MR", "HVE-MX", "HVE-MY", 
        "LVE-EX", "LVE-EY", "LVE-LX", "LVE-LY", "LVE-MR"
    };
    private static final String[] subSystems =
    {
        "ASC", "BSC", "CDS", "DAQ", "DBB", "DMT", "FEC", "FMC", "FSS", "HPI", "GDS", "IOO", "IMC", 
        "IOP", "ISI", "ISS", "LSC", "LLD", "OAF", "OMC", "PEM", "PMC", "PSL", "RFM", "SEI", 
        "SUS", "TCS", "TST"       
    };
    private static final String[] sampleRates =
    {
        "0.0167", "0.25", "1", "16", "32", "64", "128", "256", "512", "1024",
        "2048", "4096", "8192", "16384", "32768", "65536"
    };
    private static final String[] sampleRateCmp = { ">=", "=", ">", "<=", "<", "~=" };
    
    private static final String[] servers =
    {
        "nds.ligo.caltech.edu", 
        "nds.ligo-wa.caltech.edu",        
        "nds.ligo-la.caltech.edu",
        "nds40.ligo.caltech.edu"
    };

    private static final String[] chanTypes =
    {
        "Online", "Raw", "RDS", "Second-trend", "Minute-trend", "Test-point", "Static"
    };
    private static final String[] dataTypes =
    {
        "INT-16", "INT-32 ", "FLT-32", "FLT-64", "CPX-64"
    };

    public static String[] getChanTypes()
    {
        return chanTypes.clone();
    }
    
    
    public static String[] getIFOList()
    {
        return ifo.clone();
    }

    public static String[] getSubSystems()
    {
        return subSystems.clone();
    }

    public static String[] getSampleRates()
    {
        return sampleRates.clone();
    }

    public static String[] getServers()
    {
        return servers.clone();
    }

    public static String[] getSampleRateCmp()
    {
        return sampleRateCmp.clone();
    }

    public static String[] getDataTypes()
    {
        return dataTypes.clone();
    }
    
    
}
