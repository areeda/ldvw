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

package checkchansourcedata;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ChannelTable;
import edu.fullerton.ndsproxyclient.ChanSourceData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * A utility to check for problems with Chan Source data
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CheckChanSourceData
{
   
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        CheckChanSourceData me = new CheckChanSourceData();
        try
        {
            if (me.parseCommand(args, me.getClass().getSimpleName(), "0.0.1"))
            {
                me.go();
            }
        }
        catch (Exception ex)
        {
            Logger.getLogger(CheckChanSourceData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private final HashSet<Integer> chans;
    private Database db;
    private final String configFile;
    private final int verbose = 3;
    private String server;
    private String chantype;
    private ChannelTable chanTbl;
    private int minId;
    private int maxId;
    private int nId;
    private ArrayList<Integer> idList;
    private Random generator;
    
    CheckChanSourceData()
    {
        this.idList = null;
        this.configFile = "";
        this.db = null;
        chans = new HashSet<>();
    }
    
    private void go() throws SQLException, ClassNotFoundException, ViewConfigException, LdvTableException
    {
        setup();
        chanTbl = new ChannelTable(db);
        initRandChan();
        
        while (chans.size() < 10000)
        {
            if (chans.size() % 100 == 0)
            {
                System.out.print(".");
                System.out.flush();
            }
            long now = TimeAndDate.nowAsGPS();
            ChanInfo chanInfo = getRandomChannel();

            ChanSourceData csd = new ChanSourceData();
            csd.pullData(chanInfo);
            long minGps = csd.getServerMin();
            long maxGps = csd.getServerMax();
            int nIntervals = csd.getnRawIntervals();
            boolean bad = (minGps < 814000000 || maxGps > now);
            if (bad)
            {
                System.out.format(
                    "%n%1$-59s %2$-24s %3$10d - %4$10d %5$4d (%6$5d)%n", 
                    chanInfo.getChanName(), chanInfo.getServer(),
                    minGps,maxGps,nIntervals, chans.size());
            }

        }
    }
    

    /**
     * Open a connection to the database and create the channel table objects we need
     *
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    private void setup() throws SQLException, ClassNotFoundException, ViewConfigException
    {
        if (db != null)
        {
            db.close();
        }
        ViewerConfig vc;
        vc = new ViewerConfig();
        if (!configFile.isEmpty())
        {
            vc.setConfigFileName(configFile);
        }
        db = vc.getDb();
        if (verbose > 1)
        {
            System.out.print("Connected to: ");
            System.out.println(vc.getLog());
        }

    }
    boolean parseCommand(String[] args, String programName, String version)
    {
        boolean ret = true;

        Options options = new Options();

        options.addOption(new Option("help", "print this message"));
        options.addOption(new Option("version", "print the version information and exit"));

        options.addOption(OptionBuilder.withArgName("server").hasArg().withDescription("limit to server only").create("server"));
        options.addOption(OptionBuilder.withArgName("chantype").hasArg().withDescription("limit to this channel type only").create("chantype"));
        
        CommandLineParser parser = new GnuParser();

        boolean wantHelp = false;
        CommandLine line;
        try
        {
            // parse the command line arguments
            line = parser.parse(options, args);
        }
        catch (ParseException exp)
        {
            // oops, something went wrong
            System.err.println("Command parsing failed.  Reason: " + exp.getMessage());
            wantHelp = true;
            line = null;
        }
        if (line != null)
        {
            if (line.hasOption("version"))
            {
                System.out.println(programName + " - version " + version);
                ret = false;
            }

            wantHelp = line.hasOption("help");
            if (line.hasOption("server"))
            {
                server = line.getOptionValue("server");
            }
            else
            {
                server = "";
            }
            if (line.hasOption("chantype"))
            {
                chantype = line.getOptionValue("chantype");
            }
            else
            {
                chantype = "";
            }
        }
        if (wantHelp)
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(programName, options);
            ret = false;
        }

        return ret;
    }

    private void initRandChan() throws SQLException, LdvTableException
    {
        minId = chanTbl.getMinChanId();
        maxId = chanTbl.getMaxChanId();
        nId = maxId - minId;
        if (!server.isEmpty() || !chantype.isEmpty())
        {
            idList = chanTbl.getIdList(server, chantype);
            if (idList.isEmpty())
            {
                throw new LdvTableException("No channels matched spec");
            }
            minId = 0;
            maxId = idList.size() -1;
            nId = idList.size();
            System.out.format("%1$,d match server='%2$s', chantype='%3$s'%n", nId,server, chantype);
        }
        generator = new Random();
    }
    
    private ChanInfo getRandomChannel() throws LdvTableException
    {
        int n = -1;
        int cnt = 50;
        ChanInfo chanInfo = null;
        
        for (int i = 0; i<cnt && n<0; i++)
        {
            n = generator.nextInt(nId) + minId;
            if (  idList != null)
            {
                n = idList.get(n);
            }

            if (!chans.contains(n))
            {
                chans.add(n);
                chanInfo = chanTbl.getChanInfo(n);
            }
            else
            {
                n = -1;
            }
        }
        if (chanInfo == null)
        {
            throw new LdvTableException("No more channels.");
        }
        return chanInfo;
    }
}
