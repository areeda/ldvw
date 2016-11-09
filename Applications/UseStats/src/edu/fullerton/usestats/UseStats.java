/*
 * Copyright (C) 2015 Joseph Areeda <joseph.areeda at ligo.org>
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

package edu.fullerton.usestats;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.UseLogInfo;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvtables.UseLog;
import edu.fullerton.ldvtables.ViewUser;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author areeda
 */
public class UseStats
{

    /**
     * Program to summarize our usage statistics by
     * time periods
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        UseStats me = new UseStats();
        me.doit();
    }
    private int verbose=6;

    private void doit()
    {
        try
        {
            ViewerConfig vc;
            vc = new ViewerConfig();

            Database db = vc.getDb();
            if (verbose > 1)
            {
                System.out.print("Connected to: ");
                System.out.println(vc.getLog());
            }
            UseLog uselog = new UseLog(db);
            uselog.streamAll();
            
            UseLogInfo uli;
            class TimeStat
            {
                public int t;
                public int count;
                public Set<String> user;
                
                public TimeStat(int t)
                {
                    this.t = t;
                    count = 0;
                    user = new HashSet<>();
                }
                public void add(String user)
                {
                    this.user.add(user);
                    count++;
                }
            }

            TimeStat[] dowTS = new TimeStat[8];
            TimeStat[] hrTS = new TimeStat[25];
            
            for(int i=0;i< dowTS.length;i++)
            {
                dowTS[i] = new TimeStat(i);
            }
            for(int i=0;i < hrTS.length; i++)
            {
                hrTS[i] = new TimeStat(i);
            }

            class DayStat
            {
                public String date;
                public int count;
                public Set<String> user;
                
                public DayStat(String date)
                {
                    this.date = date;
                    count = 0;
                    user = new HashSet<>();
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd");
            String curDate = "";
            DayStat curStat = null;
            List<DayStat> dayStats = new ArrayList<>();
            
            while ((uli = uselog.streamNext()) != null)
            {
                Calendar cal = new GregorianCalendar();
                cal.setTime(uli.getWhenSent());
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                
                dowTS[dow].add(uli.getUser());
                
                int hr = cal.get(Calendar.HOUR_OF_DAY);
                hrTS[hr].add(uli.getUser());
                
                String date = sdf.format(cal.getTime());
                
                if (curDate.isEmpty())
                {
                     curStat = new DayStat(date);
                     curStat.count++;
                     curStat.user.add(uli.getUser());
                     curDate = date;
                }
                else if (curDate.contentEquals(date))
                {
                    curStat.count++;
                    curStat.user.add(uli.getUser());
                }
                else
                {
                    dayStats.add(curStat);
                    curStat = new DayStat(date);
                    curStat.user.add(uli.getUser());
                    curStat.count++;
                    curDate = date;
                }
            }
            uselog.streamClose();
            //
            ImageTable imageTable = new ImageTable(db);
            TreeMap<String, Integer> countByUser = imageTable.getCountByUser();
            
            ViewUser viewUser = new ViewUser(db);
            Set<String> names = viewUser.getAllNames();

            int nusers = names.size();
            
            
            int[] divs = {0, 1, 25, 100, 500, 1000, 5000, 10000, 50000};
            int[] cnts = new int[divs.length];
            
            for (int i=0;i<cnts.length; i++)
            {
                cnts[i] = 0;
            }
            
            for (Entry<String,Integer> ent : countByUser.entrySet())
            {
                String name = ent.getKey();
                Integer cnt = ent.getValue();
                if (names.contains(name))
                {
                    for (int i = divs.length -1; i >= 0; i--)
                    {
                        if (cnt >= divs[i])
                        {
                            cnts[i]++;
                            break;
                        }
                    }
                }
            }
            cnts[0] = nusers - countByUser.size();
            //---print results
            System.out.println("\"Totals by day of week\"");
            System.out.println("\"day, pages, users\"");
            for (int i = 0; i < dowTS.length; i++)
            {
                System.out.printf("%1$d, %2$d, %3$d%n", i, 
                        dowTS[i].count, dowTS[i].user.size());
            }
            
            System.out.println("\n\"Totals by hour of day\"");
            System.out.println("\n\"hour,  pages, users\"");
            for (int i = 0; i < hrTS.length; i++)
            {
                System.out.printf("%1$d, %2$d, %3$d%n", i, 
                        hrTS[i].count, hrTS[i].user.size());                
            }
            
            System.out.println("\"\nTotals per day\"");
            System.out.println("\"\ndate, pages, users\"");
            for(DayStat ds :dayStats)
            {
                System.out.printf("%1$s, %2$d, %3$d%n", ds.date, ds.count, ds.user.size());
            }
            
            System.out.println("\"\nUsers by number of plots\"\n");
            System.out.println("\"nplots, nusers\"\n");
            for(int i=0; i<divs.length-1; i++)
            {
                System.out.format("\"%1$d-%2$d\", %3$d%n", divs[i],divs[i+1] - 1,cnts[i]);
            }
            
        }
        catch (ViewConfigException | SQLException ex)
        {
            Logger.getLogger(UseStats.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (LdvTableException ex)
        {
            Logger.getLogger(UseStats.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
