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

import com.areeda.jaDatabaseSupport.Database;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;

/**
 * Encapsulate the lowest level of configuration parameters kept in a user editable file
 * Entries in the file are of the form:
 * <parameter> = value
 * # signifies a comment and may be anywhere, ignoring the rest of the line
 * blanks are not significant except inside a quoted value
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ViewerConfig
{
    private final TreeMap<String,String> params;
    private final TreeMap<String,String> env;
    
    private final String configDir = "/usr/local/ldvw/";
    
    private final String defFile = "ldvw.conf";
    
    private File configFile;
    private final Properties appProperties;
    private String configFileName=null;

    /**
     * default constructor initializes parameter Map
     */
    public ViewerConfig()
    {
        params = new TreeMap<>();
        appProperties = new Properties();
        env = new TreeMap<>();
    }
    /**
     * Read the default configuration files
     * 
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void readConfig() throws FileNotFoundException, IOException
    {
        if (configFileName == null)
        {
            configFileName = configDir + defFile;
        }
        readConfig(configFileName);
        readProps(configDir + get("PropertyFile"));
    }
    
    /**
     * Read a specific configuration file
     * 
     * @param fname full path to the file
     * @throws FileNotFoundException
     * @throws IOException 
     */
    public void readConfig(String fname) throws FileNotFoundException, IOException
    {
        configFile = new File(fname);
        BufferedReader br = new BufferedReader(new FileReader(configFile));
        String line;
        Pattern ppat = Pattern.compile("([^\\s]+)\\s*=\\s*([^\\s\\\"]+|\\\".*\\\")");
        
        while( (line = br.readLine())!= null)
        {
            line = line.trim();
            int p = line.indexOf("#");
            if (p > 0)
            {
                line = line.substring(0, p);
            }
            else if (p == 0)
            {
                line = "";
            }
            line = line.trim();
            if (!line.isEmpty())
            {
                Matcher pmat = ppat.matcher(line);
                if (pmat.find())
                {
                    String key=pmat.group(1);
                    String val = pmat.group(2);
                    if (key.startsWith("$"))
                    {
                        key = key.substring(1);
                        env.put(key, val);
                    }
                    else
                    {
                        params.put(key, val);
                    }
                }
            }
        }
        br.close();
    }
    /**
     * Get a specific key from the configuration file
     * @param key name of the parameter
     * @return parameter value or null if it doesn't exist
     */
    public String get(String key)
    {
        String ret = params.get(key);
        ret = ret == null ? "" : ret;
        return ret;
    }
    /**
     * Read the default config file if needed and create a Database object from spec
     * 
     * @return Database object or null if any problems
     * @throws viewerconfig.ViewConfigException
     * @see Database
     */
    public Database getDb() throws ViewConfigException
    {
        return getDb("");
    }
    public Database getDb(String prefix) throws ViewConfigException
    {
        Database db=null;
        int tries = 0;
        while (db == null && tries < 3)
        {
            try
            {
                tries++;
                
                if (configFile == null)
                {
                    readConfig();
                }
                String host = get(prefix + "host");
                String user = get(prefix + "user");
                String password = get(prefix + "password");
                String dbName = get(prefix + "database");
                if (host.isEmpty() || user.isEmpty() || password.isEmpty() || dbName.isEmpty())
                {
                    String errMsg = String.format("Database for prefix (%1%s) is missing parameter(s)"
                            + "in configuration file.", prefix);
                }
                db = new Database(dbName, user, password);
                db.setHost(host);
                db.makeConnection();
            }
            catch (IOException | SQLException | ClassNotFoundException ex)
            {
                db = null;
                String ermsg = "Unable to connect to viewer database"
                               + ex.getClass().getSimpleName() + ": "
                               + ex.getLocalizedMessage();
                throw new ViewConfigException(ermsg);
            }
        }
        return db;
    }
    /**
     * Get a string for use as a log entry for connect
     * 
     * @return formatted string with no secrets
     */
    public String getLog()
    {
        String host = get("host");
        String dbName = get("database");
        return String.format("Host: %1$s, db: %2$s", host, dbName);
    }

    private void readProps(String path)
    {
        
    }
    
    private void saveProps(String propFilename) throws ViewConfigException
    {
        FileOutputStream out = null;
        try
        {
            out = new FileOutputStream(propFilename);
            appProperties.store(out, "---LigoDV-web properties---");
            out.close();
        }
        catch (Exception ex)
        {
            String ermsg = "Saving properties: " + ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
            throw new ViewConfigException(ermsg);
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
            }
            catch (IOException ex)
            {
                String ermsg = "Saving properties: " + ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage();
                throw new ViewConfigException(ermsg);
            }
        }
    }

    public void add(String key, String[] ary)
    {
        JSONArray data = new JSONArray(ary);
        String arrStr = data.toString();
        appProperties.put(key, arrStr);
    }

    public String[] getStringArray(String key)
    {
        String jstr = appProperties.getProperty(key);
        JSONArray data = new JSONArray(jstr);
        int len = data.length();
        String[] ret = new String[len];
        for (int i = 0; i < len; i++)
        {
            ret[i] = data.getString(i);
        }
        return ret;
    }
    public void setConfigFileName(String configFileName)
    {
        this.configFileName = configFileName;
    }

    public TreeMap<String, String> getEnv()
    {
        return env;
    }
    
}
