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
package edu.fullerton.ldvw;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
class AjaxHelper
{


    String processRequest(HttpServletRequest request, HttpServletResponse response)
    {
        String ermsg = "";

        String mime = "application/json";
        String want = request.getParameter("data").toLowerCase();
        JSONArray results = new JSONArray();
        
        if (want.equalsIgnoreCase("test"))
        {
            results.put("One thing");
            results.put("Another");
            results.put("Still another");
            results.put("OK one more");
        }
        if (results.length() > 0 && ermsg.isEmpty())
        {
            ServletOutputStream outstrm = null;
            try
            {
                response.setContentType(mime);
                String outStr = results.toString();
                response.setContentLength(outStr.length());
                outstrm = response.getOutputStream();
                
                outstrm.print(outStr);
            }
            catch (IOException ex)
            {
                ermsg = ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            }
            finally
            {
                try
                {
                    outstrm.close();
                }
                catch (IOException ex)
                {
                    Logger.getLogger(AjaxHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            
        }
        return ermsg;
    }
    
}
