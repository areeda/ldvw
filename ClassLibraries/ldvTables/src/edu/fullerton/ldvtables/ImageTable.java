/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
package edu.fullerton.ldvtables;

import com.areeda.jaDatabaseSupport.*;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.jspWebUtils.WebUtils;
import edu.fullerton.ldvjutils.LdvTableException;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents the database table storing analysis results.  Most are images of plots but may be any mime type that gets sent without hmtl
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageTable extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null  index        unique        auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
        new Column("user",          CType.CHAR, 64, Boolean.FALSE,  Boolean.FALSE, Boolean.TRUE, Boolean.FALSE),
        new Column("created",       CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("lastAccess",    CType.TIMESTAMP, Long.SIZE,     Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("mime",          CType.CHAR, 16, Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("imgMd5",        CType.CHAR, 32, Boolean.FALSE,  Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("image",         CType.BLOB,     3000000,        Boolean.FALSE, Boolean.FALSE, Boolean.FALSE, Boolean.FALSE),
        new Column("groupName",     CType.CHAR,     64,             Boolean.FALSE, Boolean.TRUE,  Boolean.FALSE,  Boolean.FALSE),
        new Column("description",   CType.STRING,   16000,      Boolean.FALSE,  Boolean.FALSE, Boolean.TRUE, Boolean.FALSE),
        new Column("resultNum",     CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,  Boolean.TRUE, Boolean.TRUE, Boolean.TRUE),
    };
    private int lastImageSize = 0;
    private final SimpleDateFormat dateTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    

    public ImageTable(Database db) throws SQLException
    {
        this.db = db;
        setName("Images");
        setCols(myCols);
    }

    public int addImg(String userName, InputStream bis, String mime) throws SQLException, NoSuchAlgorithmException, IOException
    {
        return addImg(userName,bis,mime,"","",0);
    }
    /**
     * 
     * @param userName
     * @param bis
     * @param mime
     * @param group
     * @param description
     * @param resNum
     * @return the new entries ID number
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    public int addImg(String userName, InputStream bis, String mime, String group, String description, int resNum) 
            throws SQLException, NoSuchAlgorithmException, IOException
    {
        int ret = 0;

        String md5 = Utils.md5sum(bis);
        String ckdup = "SELECT myId, imgMd5 FROM " + getName() + " WHERE imgMd5='" + md5 + "'";
        ResultSet crs = db.executeQuery(ckdup);
        if (crs != null && crs.next())
        {
            ret = crs.getInt("myId");
        }

        if (ret == 0)
        {
            String addit = "INSERT INTO " + getName() + " SET created=?, lastAccess=?, mime=?,imgMd5=?, "
                           + "image=?, user=?, groupName=?, description=?, resultNum=?";
            PreparedStatement ps = db.prepareStatement(addit, Statement.RETURN_GENERATED_KEYS);
            Date now = new Date();
            Timestamp nowts = new Timestamp(now.getTime());
            ps.setTimestamp(1, nowts);
            ps.setTimestamp(2, nowts);
            ps.setString(3, mime);
            ps.setString(4, md5);
            bis.reset();
            ps.setBlob(5, bis);
            ps.setString(6, userName);
            ps.setString(7, group);
            ps.setString(8, description);
            ps.setInt(9, resNum);

            int r = db.executeUpdate(ps);

            if (r == 1)
            {
                ResultSet rs = ps.getGeneratedKeys();
                if (rs.next())
                {
                    ret = rs.getInt(1);
                }
            }
        }
        return ret;
    }

    /**
     * send the requested result from the database (nominally an image but may be any mime type)
     * @param response Servlet object for setting mime type, content length, and getting output stream ...
     * @param imgId record id in our table
     * @throws SQLException
     * @throws IOException 
     */
    public void sendImg(HttpServletResponse response, int imgId) throws SQLException, IOException
    {
        sendImg(response, imgId, 0);
    }
    public void sendImg(HttpServletResponse response, int imgId, int width) throws SQLException, IOException
    {
        //@todo get the HttpServletResponse out of this package.  We should no depend on the servlet environment.
        String q = String.format("SELECT * FROM %1$s WHERE myId=%2$d", getName(), imgId);
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs != null && rs.next())
            {
                String mime = rs.getString("mime");
                response.setContentType(mime);
                Blob image = rs.getBlob("image");
                if (width >= 32)
                {
                    image = resizeImg(image,width);
                }
                response.setContentLength((int) image.length());
                String expDate= WebUtils.getHttpDate(365);
                response.setHeader("Expires", expDate);
                ServletOutputStream outstrm = response.getOutputStream();
                outstrm.write(image.getBytes(1, (int) image.length()));
                lastImageSize = (int) image.length();

                
                String q1 = "UPDATE " + getName() + " SET lastAccess=now() where myId=?";
                try
                {
                    PreparedStatement ps1 = db.prepareStatement(q1, Statement.NO_GENERATED_KEYS);
                    ps1.setInt(1, imgId);
                    ps1.executeUpdate();
                }
                catch (SQLException ex)
                {
                }
                outstrm.flush();
            }
        }
        catch (Exception ex)
        {
            System.err.println("Error sending image:" + ex.getClass().getSimpleName() + " - " +
                               ex.getLocalizedMessage());
        }
    }

    /**
     * return the size of the last image sent by sendImg
     * @return size in bytes of the image content (not including html headers)
     */
    public int getLastImageSize()
    {
        return lastImageSize;
    }

    public ArrayList<Integer> getImgList(int strt, int stop,String userWanted) throws SQLException, LdvTableException
    {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        String usrSrch = "";
        if (userWanted.length() > 0 && !userWanted.equalsIgnoreCase("any") && !userWanted.equalsIgnoreCase("all"))
        {
            usrSrch = " WHERE user = \"" + userWanted + "\" ";
        }
        String subq = String.format("(SELECT DISTINCT imgMd5,myId,user,lastAccess,mime,created from %1$s %2$s group by imgMd5) as t2", getName(),usrSrch);

        String q = String.format("select myId, user,lastAccess,mime from %1$s  order by created desc limit %2$d,%3$d",
                                 subq, strt, stop - strt);
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                ret.add(rs.getInt("myId"));
            }
            rs.close();
        }
        catch (Exception ex)
        {
            String ermsg = "Error in GetImgList: " + ex.getLocalizedMessage() + "  SQL query: " + q;
            throw new LdvTableException(ermsg);
        }
        return ret;
    }

    public long getDistinctRecordCount(String userName) throws LdvTableException
    {
        long ret = 0;
        try
        {
            String q = "select count(DISTINCT imgMd5) as nrec from " + getName();
            if (userName != null && userName.length() > 0 
                && !userName.equalsIgnoreCase("all") && !userName.equalsIgnoreCase("any"))
            {
                q += " WHERE user=\"" + userName + "\"";
            }

            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = rs.getLong("nrec");
            }
            rs.close();

        }
        catch (SQLException ex)
        {
            throw new LdvTableException(ex);
        }
        return ret;
    }

    public PageItemList getIdInfo(int id) throws LdvTableException
    {
        PageItemList ret = new PageItemList();
        String t;
        String q = "SELECT user,created,mime,length(image) as isize,description FROM " + getName() + 
                   " WHERE myId=" + Integer.toString(id);

        
        try
        {
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                String user = rs.getString("user");
                t = user == null || user.length() == 0 || user.equals("<name not available>") ? "no user listed" : user;
                Timestamp created = rs.getTimestamp("created");

                t += ", " + dateTimeFmt.format(created.getTime());
                String mime = rs.getString("mime");
                t += ", " + (mime == null ? "unknown mime" : mime);
                int isize = rs.getInt("isize");
                t += ", size: " + WebUtils.hrInteger(isize) + " bytes";
                ret.addLine(t);
                String desc = rs.getString("description");
                desc = desc == null ? "" : desc;
                PageItemString descStr= new PageItemString(desc, false);
                ret.add(descStr);
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException(ex);
        }
        return ret;
    }
    /**
     * query table for number of images for each user
     * @return TreeMa<UserName,Number of Images>
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public TreeMap<String,Integer> getCountByUser() throws LdvTableException
    {
        TreeMap<String,Integer> ret = new TreeMap<>();
        
        String q = "select count(*) as cnt, user from Images group by user;";
        
        try
        {
            ResultSet rs = db.executeQuery(q);
            while (rs.next())
            {
                String user = rs.getString("user");
                Integer cnt = rs.getInt("cnt");
                if (user != null && cnt != null)
                {
                    ret.put(user, cnt);
                }
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException(ex);
        }
        return ret;      
    }
    public PageTable getStats() throws WebUtilException, LdvTableException
    {
        PageTable ret = new PageTable();
        String[] hdr = { "User", "Plot count" };
        
        PageTableRow h = new PageTableRow(hdr);
        h.setRowType(PageTableRow.RowType.HEAD);
        ret.addRow(h);
        TreeMap<String, Integer> countByUser = getCountByUser();
        
        boolean odd = true;
        for(String user : countByUser.keySet())
        {
            PageTableRow r = new PageTableRow();
            // change color on alternating rows of table
            r.setClassName(odd ? "odd" : "even");
            odd = !odd;
            r.add(user);
            r.add(countByUser.get(user));
            ret.addRow(r);
        }
        return ret;
    }

    private Blob resizeImg(Blob image, int width) throws SQLException, IOException
    {
        InputStream instream = image.getBinaryStream();
        BufferedImage inImg = ImageIO.read(instream);
        int oldwdt = inImg.getWidth();
        int oldhgt = inImg.getHeight();
        // See if there is anything to do
        if (oldwdt != width && oldwdt > 0)
        {
            float factor = ((float)width)/oldwdt;
            int newhgt = Math.round(oldhgt*factor);
            
            BufferedImage resizedImage = new BufferedImage(width, newhgt, BufferedImage.TYPE_BYTE_INDEXED);
            Graphics2D g = resizedImage.createGraphics();
            g.setComposite(AlphaComposite.Src);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(inImg, 0, 0, width, newhgt, null);
            g.dispose();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, "png", bos);
            image.setBytes(1, bos.toByteArray());
            image.truncate(bos.size());
        }
        
        return image;
    }

    /**
     * update an existing image's description field
     * 
     * @param imgId - id of the record
     * @param desc - html of image description
     */
    public void addDescription(int imgId, String desc)
    {
        String upd = "UPDATE " + getName() + " SET description = ? where myId = ?";
        try
        {
            PreparedStatement ps = db.prepareStatement(upd, Statement.NO_GENERATED_KEYS);
            ps.setString(1, desc);
            ps.setInt(2, imgId);
            ps.executeUpdate();
        }
        catch (Exception ex)
        {
        }
    }
    /**
     * Get the description field from the Image Table
     * 
     * @param imgId record id
     * @return description or null string ("") never null
     */
    public String getDescription(int imgId)
    {
        String sel = "SELECT description from " + getName() + " WHERE myId = ?";
        String ret = "";
        try
        {
            PreparedStatement ps = db.prepareStatement(sel, Statement.NO_GENERATED_KEYS);
            ps.setInt(1, imgId);
            ResultSet rs = db.executeQuery(ps);
            if (rs.next())
            {
                ret = rs.getString("description");
            }
        }
        catch (SQLException ex)
        {
            Logger.getLogger(ImageTable.class.getName()).log(Level.SEVERE, null, ex);
        }
        ret = ret == null ? "" : ret;
        return ret;
    }
    public HashSet<Integer> getAllIds() throws LdvTableException
    {
        Statement myStmt = null;
        ResultSet rs = null;
        try
        {
            HashSet<Integer> ret = new HashSet<>();
            
            myStmt = db.createStatement(1);
            String query = "SELECT myId from " + getName();
            rs = myStmt.executeQuery(query);
            while(rs.next())
            {
                ret.add(rs.getInt("myId"));
            }
            rs.close();
            myStmt.close();
            return ret;
        }
        catch (SQLException ex)
        {
            try
            {
                if (rs != null)
                {
                    rs.close();
                }
                if (myStmt != null)
                {
                    myStmt.close();
                }
            }
            catch(SQLException ex2)
            {
                // don't report errors trying to report errors
            }
            throw new LdvTableException("Getting all image id's", ex);
        }
    }

    /**
     * look up the mime type of a stored image
     * @param id image id (record key)
     * @return the mime type stored for the image
     */
    public String getMime(Integer id) throws LdvTableException
    {
        String ret = "";
        try
        {
            String q = "Select mime from " + getName() + " Where myId=" + id.toString();
            ResultSet rs = db.executeQuery(q);
            if (rs.next())
            {
                ret = rs.getString("mime");
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("getting a mime type", ex);
        }
        return ret;
    }
}
