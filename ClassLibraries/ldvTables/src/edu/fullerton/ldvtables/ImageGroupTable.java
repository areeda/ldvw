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
package edu.fullerton.ldvtables;

import com.areeda.jaDatabaseSupport.CType;
import com.areeda.jaDatabaseSupport.Column;
import com.areeda.jaDatabaseSupport.Database;
import com.areeda.jaDatabaseSupport.Table;
import edu.fullerton.ldvjutils.LdvTableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * Image groups are used to display related results.  user + groupname is unique but an image can
 * belong to as many groups as the user wants.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageGroupTable extends Table
{
    private final Column[] myCols =
    {
        //         name,            type            length          can't be null   index          unique         auto inc
        new Column("myId",          CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,   Boolean.TRUE,  Boolean.TRUE,  Boolean.TRUE),
        new Column("user",          CType.CHAR,     64,             Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("groupName",     CType.CHAR,     64,             Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
        new Column("imageID",       CType.INTEGER,  Integer.SIZE,   Boolean.TRUE,   Boolean.TRUE,  Boolean.FALSE, Boolean.FALSE),
    };

    PreparedStatement countPS;      // count images in a user's group
    
    public ImageGroupTable(Database db) throws SQLException
    {
        this.db = db;
        setName("ImageGroup");
        setCols(myCols);
        String cntq = "SELECT count(*) as cnt FROM " + getName() + " WHERE " 
                   + "user = ? and groupName = ?";
        countPS = db.prepareStatement(cntq, Statement.NO_GENERATED_KEYS);
    }
    
    /**
     * Add an image to a user's group.  
     * Note this will not duplicate images in a group and may be called multiple times.
     * @param user the user's cn
     * @param group group name
     * @param imageID record accession number in the image table
     * @return record number of new insertion or 0 if already there.
     * @throws LdvTableException if there is a duplicate image in this group
     * @throws SQLException a bug in my code, or perhaps out of disk space
     */
    public int addToGroup(String user, String group, Integer imageID) throws LdvTableException
    {
        int ret = 0;
        
        String dupq = "SELECT count(*) as cnt FROM " + getName() + " WHERE "
                      + "user = ? and groupName = ? and imageID = ?";
        try
        {
            PreparedStatement dupPS = db.prepareStatement(dupq, Statement.NO_GENERATED_KEYS);
            dupPS.setString(1, user);
            dupPS.setString(2,group);
            dupPS.setInt(3, imageID);
            ResultSet rs = dupPS.executeQuery();
            if (rs.next())
            {
                int cnt = rs.getInt("cnt");
                if (cnt > 1)
                {
                    String ermsg = String.format("Error: user: %s, group: %s, imageId: %d has multiple entries",
                                                 user,group,imageID);
                    throw new LdvTableException(ermsg);
                }
                else if (cnt == 0)
                {
                    String insq = "INSERT INTO "+ getName() + " (user,groupName,imageID) VALUES (?,?,?)";
                    PreparedStatement insPS = db.prepareStatement(insq, Statement.RETURN_GENERATED_KEYS);
                    insPS.setString(1, user);
                    insPS.setString(2, group);
                    insPS.setInt(3, imageID);
                    ret = insPS.executeUpdate();
                }
            }
            rs.close();
        }
        catch(SQLException ex)
        {
            throw new LdvTableException("SQL problem adding to group: " + ex.getLocalizedMessage());
        }
        return ret;
    }
    public void addToGroup(String user, String group, Collection<Integer> imgIds) throws LdvTableException
    {
        for (Integer id : imgIds)
        {
            addToGroup(user, group, id);
        }
    }
    /**
     * Get a set of all images in a user's group
     * @param user user's cn
     * @param group group name
     * @return set containing the image number of all members in the group
     * @throws SQLException 
     */
    public TreeSet<Integer> getGroupMembers(String user, String group) throws SQLException
    {
        TreeSet<Integer> ret = new  TreeSet<Integer>();
        String selq = "SELECT * FROM " + getName() + " WHERE user=? and groupName=?";
        PreparedStatement selPS = db.prepareStatement(selq, Statement.NO_GENERATED_KEYS);
        selPS.setString(1, user);
        selPS.setString(2, group);
        ResultSet rs = selPS.executeQuery();
        while (rs.next())
        {
            int img = rs.getInt("imageID");
            ret.add(img);
        }
        
        return ret;
    }
    /**
     * Count the number of images in a user's group
     * @param user user's cn
     * @param group group name
     * @return count of images in that group
     * @throws SQLException big in my code most probably
     */
    public int getCount(String user, String group) throws LdvTableException
    {
        int ret = 0;
        try
        {
            
            
            countPS.setString(1, user);
            countPS.setString(2, group);
            ResultSet rs = countPS.executeQuery();
            
            if (rs.next())
            {
                ret = rs.getInt("cnt");
            }
            
        }
        catch (Exception ex)
        {
            String ermsg = String.format("Getting group count: %1$s - %2$s", 
                                         ex.getClass().getSimpleName(), ex.getLocalizedMessage());
            throw new LdvTableException(ermsg);
        }
        return ret;
    }
    
    /**
     * Remove all images in this group
     * @param user user's cn
     * @param group group name
     * @throws SQLException 
     */
    public void deleteGroup(String user, String group) throws SQLException
    {
        String delq = "DELETE FROM " + getName() + " WHERE user=? and groupName=?";
        PreparedStatement delPS = db.prepareStatement(delq, Statement.NO_GENERATED_KEYS);
        delPS.setString(1, user);
        delPS.setString(2, group);
        delPS.execute();
    }
    /**
     * return a set of all groups owned by this user
     * @param user user's cn
     * @return set of group names
     * @throws SQLException probably my bug
     */
    public TreeSet<String> getGroups(String user) throws SQLException
    {
        TreeSet<String> ret = new TreeSet<String>();
        String selq = "SELECT * FROM " + getName() + " WHERE user=?";
        PreparedStatement selPS = db.prepareStatement(selq, Statement.NO_GENERATED_KEYS);
        selPS.setString(1, user);
        
        ResultSet rs = selPS.executeQuery();
        while (rs.next())
        {
            String grp = rs.getString("groupName");
            ret.add(grp);
        }

        return ret;
    }

    public ArrayList<Integer> getGroupMembers(String user, String group, int strt, int stop) throws LdvTableException
    {
        ArrayList<Integer> ret = new ArrayList<Integer>();
        
        try
        {
            String selq = "SELECT * FROM " + getName() + " WHERE user=? and groupName=? "
                          + "order by imageID limit ?,?";
            PreparedStatement selPS = db.prepareStatement(selq, Statement.NO_GENERATED_KEYS);
            selPS.setString(1, user);
            selPS.setString(2, group);
            selPS.setInt(3, strt);
            selPS.setInt(4, stop);
            ResultSet rs = selPS.executeQuery();
            while (rs.next())
            {
                int img = rs.getInt("imageID");
                ret.add(img);
            }
        }
        catch (Exception ex)
        {
            String ermsg = String.format("GetGroupMembers: %1$s,$2%s", ex.getClass().getSimpleName(),
                                         ex.getLocalizedMessage());
            throw new LdvTableException(ermsg);
        }
        return ret;
    }

    public void deleteFromGroup(String user, String group, Collection<Integer> ids) throws LdvTableException
    {
        String delq = "DELETE FROM " + getName() + " WHERE user = ? and groupName = ? and imageID = ?";
        try
        {
            PreparedStatement delPS = db.prepareStatement(delq, Statement.NO_GENERATED_KEYS);
            for (Integer id : ids)
            {
                delPS.setString(1, user);
                delPS.setString(2, group);
                delPS.setInt(3, id);
                delPS.execute();
            }
        }
        catch (SQLException ex)
        {
            throw new LdvTableException("SQL problem deleting from group: " + ex.getLocalizedMessage());
        }
    }

    public HashSet<ImageGroup> getAll() throws LdvTableException
    {
        Statement myStmt = null;
        ResultSet rs = null;
        try
        {
            HashSet<ImageGroup> ret = new HashSet<>();

            myStmt = db.createStatement(1);
            String query = "SELECT * from " + getName();
            rs = myStmt.executeQuery(query);
            while (rs.next())
            {
                ImageGroup imgGrp = new ImageGroup();
                imgGrp.fill(rs);
                ret.add(imgGrp);
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
            catch (SQLException ex2)
            {
                // don't report errors trying to report errors
            }
            throw new LdvTableException("Getting all image id's", ex);
        }
    }

    public void remove(Collection<Integer> orphans) throws SQLException
    {
        String baseStmt = "DELETE FROM " + getName() + " WHERE ";
        StringBuilder idList;
        
        HashSet<Integer> temp = new HashSet<>(orphans);
        while (temp.size() > 0)
        {
           ArrayList<Integer> delList = new ArrayList<>();
           int cnt = 0;
           for(Integer it : temp)
           {
               delList.add(it);
               cnt++;
               if (cnt > 50)
               {
                   break;
               }
           }
           idList = new StringBuilder();
           
           for(Integer it : delList)
           {
               if (idList.length() > 0)
               {
                   idList.append(" OR ");
               }
               idList.append(String.format(" myId = %1$d ", it));
               temp.remove(it);
               
           }
           if (idList.length() > 0)
           {
               db.execute(baseStmt + idList.toString());
           }
        }
    }
   
}
