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
package addimg2db;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvjutils.TimeAndDate;
import edu.fullerton.ldvtables.ImageGroupTable;
import edu.fullerton.ldvtables.ImageTable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.MimetypesFileTypeMap;
import viewerconfig.ViewConfigException;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Addimg2db
{
    String progName = "addimg2db";
    String version = "0.0.1";
    
    private Database db;
    
    
    private boolean auto;
    private String[] files;
    private String description;
    private String descAddon;
    private String group;
    private String user;
    private boolean gotErr;
    private String ermsg;
    private File file;
    private String mime;
    private InputStream instream;
    private ImageTable imgTbl;
    private ImageGroupTable imgGrpTbl;

    private Pattern fileSearchPat;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        Addimg2db me = new Addimg2db();
        int ret;
        ret = me.setup(args);
        if (ret == 0)
        {
            try
            {
                ret = me.doit();
            }
            catch (Exception ex)
            {
                System.err.print("Error: " + ex.getClass().getSimpleName() + " - ");
                System.err.println(ex.getLocalizedMessage());
            }
        }
        System.exit(ret);
    }

    private int setup(String[] args)
    {
        int ret = 0;
        AddImgCommandLine cmd = new AddImgCommandLine();
        if (cmd.parseCommand(args, progName, version))
        {
            user = cmd.getUser();
            group = cmd.getGroup();
            description = cmd.getDescription();
            files = cmd.getFiles();
            auto = cmd.isAuto();
        }
        else
        {
            ret = 2;
        }
        fileSearchPat = Pattern.compile("-(\\d+)-(\\d+)([smhdSMHD])\\..{3,4}$");
        return ret;
    }
    private int doit() throws LdvTableException, SQLException, ViewConfigException
    {
        ermsg = "";
        getDbTables();
        
        
        for(String filename : files)
        {
            
            if (!checkFile(filename))
            {
                try
                {
                    String desc=description + descAddon;
                    int imgId = imgTbl.addImg(user, instream, mime, "", desc, 0);
                    imgGrpTbl.addToGroup(user, group, imgId);
                    System.out.println(String.format("%1$s added, user: %2$s group: %3$s, desc: %4$s%n",
                                                     filename,user,group,desc));
                }
                catch (Exception ex)
                {
                    ermsg += "Error on " + filename + " - " + ex.getClass().getSimpleName();
                    ermsg += ": " + ex.getLocalizedMessage() + "\n";
                }
            }
        }
        if (!ermsg.isEmpty())
        {
            System.err.println("Problems found: \n" + ermsg);
        }
        return ermsg.isEmpty() ? 0 : 1;
    }

    private boolean checkFile(String filename)
    {
        file = new File(filename);
        boolean goterr = false;
        if (!file.exists())
        {
            ermsg += filename + " not found.\n";
            goterr = true;
        }
        if (!goterr && !file.canRead())
        {
            ermsg += filename + " is not readable.\n";
            goterr = true;
        }
        if (!goterr && !file.isFile())
        {
            ermsg += filename + " is not a 'normal' file\n";
            goterr = true;
        }
        if (!goterr && file.length() < 100)
        {
            goterr = true;
            ermsg += "File: " + filename + " is too small.\n";
        }
        try
        {
            if (!goterr)
            {
                MimetypesFileTypeMap mtfm = new MimetypesFileTypeMap("/etc/mime.types");
                mime = mtfm.getContentType(filename).toLowerCase();
            }
        }
        catch (Exception ex)
        {
            ermsg += "Error determining mime type: " + ex.getLocalizedMessage() + "\n";
            goterr = true;
        }
        
        if (!goterr && mime.contains("image"))
        {
            goterr |= ! (mime.contains("png") || mime.contains("jpeg") || mime.contains("gif"));
        }
        else if (!goterr && mime.startsWith("video"))
        {
            goterr |= ! (mime.contains("mp4") || mime.contains("ogg"));
        }
        instream = null;
        try
        {
            if (!goterr)
            {
                instream = getBytes(new FileInputStream(file));
            }
        }
        catch (Exception ex)
        {
            goterr=true;
            ermsg += "Error reading: " + filename + " - " + ex.getClass().getSimpleName();
            ermsg += ": " + ex.getLocalizedMessage() + "\n";
        }
        descAddon = "";
        if (!goterr && auto)
        {
            Matcher m = fileSearchPat.matcher(filename);
            if (m.find())
            {
                long gps = Long.parseLong(m.group(1));
                long dur = Long.parseLong(m.group(2));
                String unit = m.group(3).toLowerCase();
                if (unit.contentEquals("m"))
                {
                    dur *= 60;
                }
                else if (unit.contentEquals("h"))
                {
                    dur *= 60 * 60;
                }
                else if (unit.contentEquals("h"))
                {
                    dur *= 60 * 60 * 24;
                }
                String utc = TimeAndDate.gpsAsUtcString(gps);
                descAddon = String.format(" - Start: %1$s (%2$d), duration %3$ds.", utc,gps,dur);
            }
            else
            {
                goterr = true;
                ermsg += filename + "does not follow pattern for auto timestamp (*-<gps>-<dur>[smhd]";
            }
        }
        return goterr;
    }
    /**
     * Connect to the database and create table objects we need
     */
    private void getDbTables() throws LdvTableException, SQLException, ViewConfigException
    {
        if (db == null)
        {
            ViewerConfig vc = new ViewerConfig();
            db = vc.getDb();
            if (db == null)
            {
                throw new LdvTableException("Can't connect to LigoDV-web database");
            }
        }
        if (imgTbl == null)
        {
            imgTbl = new ImageTable(db);
        }
        if (imgGrpTbl == null)
        {
            imgGrpTbl = new ImageGroupTable(db);
        }
    }    
    private ByteArrayInputStream getBytes(InputStream ins) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final byte[] bytes = new byte[1024 * 512];
        int read;

        while ((read = ins.read(bytes)) != -1)
        {
            buf.write(bytes, 0, read);
        }
        ByteArrayInputStream ret = new ByteArrayInputStream(buf.toByteArray());
        return ret;
    }}
