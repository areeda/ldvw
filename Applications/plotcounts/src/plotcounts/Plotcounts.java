/*
 * Copyright (C) 2016 Joseph Areeda <joseph.areeda@ligo.org>
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
package plotcounts;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 * application to count images by type 
 * @author Joseph Areeda <joseph.areeda@ligo.org>
 */
public class Plotcounts
{

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        Plotcounts me = new Plotcounts();
        me.doit();
    }

    private int verbose = 6;

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
        }
        catch (ViewConfigException | SQLException ex)
        {
            Logger.getLogger(UseStats.class.getName()).log(Level.SEVERE, null, ex);
        }
        catch (LdvTableException ex)
        {
            Logger.getLogger(Plotcounts.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
