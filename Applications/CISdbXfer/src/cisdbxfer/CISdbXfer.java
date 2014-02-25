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
package cisdbxfer;

import au.com.bytecode.opencsv.CSVReader;
import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvtables.ChannelTable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeSet;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class CISdbXfer
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length < 1)
        {
            System.err.println("usage: java -Xmx1g -jar CISdbXfer <infile>");
            System.exit(2);
        }
        String infile = args[0];
        try
        {
            CISdbXfer me = new CISdbXfer();
            me.update(infile);
        }
        catch (Exception ex)
        {
            System.err.println("Processing error: " + ex.getClass().getSimpleName() 
                               + " - " + ex.getLocalizedMessage());
        }
    }
    private ChannelTable chnTbl;
    private Database db=null;
    private int verbose=5;

    public CISdbXfer() throws SQLException, ViewConfigException
    {
        if (db != null)
        {
            db.close();
        }
        ViewerConfig vc = new ViewerConfig();
        db = vc.getDb();
        if (verbose > 1)
        {
            System.out.print("Connected to: ");
            System.out.println(vc.getLog());
        }
        chnTbl = new ChannelTable(db);

    }

    private void update(String infile) throws FileNotFoundException, IOException, SQLException
    {
        int lineCount = 0;
        int updCount = 0;
        
        FileReader fr = new FileReader(infile);
        CSVReader reader = new CSVReader(fr);
        chnTbl.clearCisAvail();
        
        String[] nextLine;
        int siz=0;
        
        TreeSet<String> availChan = new TreeSet<String>();
        TreeSet<String> descChan = new TreeSet<String>();
        
        while ((nextLine = reader.readNext()) != null)
        {
            lineCount++;
            
            if (nextLine.length > 1 )
            {
                descChan.add(nextLine[0].trim());
            }
            else
            {
                availChan.add(nextLine[0].trim());
            }
        }
        availChan.removeAll(descChan);
        if(verbose > 1)
        {
            System.out.println("Input file read, updating channels in CIS");
        }
        doDbUpdate(availChan, descChan);
        
        
        
    }
    
    private void doDbUpdate(TreeSet<String> availChan,TreeSet<String> descChan ) throws SQLException
    {
        TreeSet<Integer> availNums = new TreeSet<Integer>();
        TreeSet<Integer> descNums = new TreeSet<Integer>();
        int count=0;
        
        if (verbose > 1)
        {
            System.out.format("Searching our database%n", descNums.size());
        }
        ResultSet allChans = chnTbl.getAllNameId();
        
        while(allChans.next())
        {
            Integer myId = allChans.getInt("myId");
            String name = allChans.getString("name");
            String basename = name;
            if (basename.contains("."))
            {
                basename = basename.substring(0,basename.indexOf("."));
            }
            if (descChan.contains(basename))
            {
                descNums.add(myId);
            }
            else if (availChan.contains(basename))
            {
                availNums.add(myId);
            }
        }
        allChans.close();
        
        if (verbose > 1)
        {
            System.out.format("updating %1$,7d channels with descriptions%n", descNums.size());
        }
        if (descNums.size() > 0)
        {
            chnTbl.setCisAvail(descNums,"D");
        }
        
        if (verbose > 1)
        {
            System.out.format("updating %1$,7d channels without descriptions%n", availNums.size());
        }
        if (descNums.size() > 0)
        {
            chnTbl.setCisAvail(availNums, "A");
        }
    }
}
