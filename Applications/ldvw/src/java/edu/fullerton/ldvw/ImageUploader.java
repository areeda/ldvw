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
package edu.fullerton.ldvw;

import com.areeda.jaDatabaseSupport.Database;
import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageForm;
import edu.fullerton.jspWebUtils.PageFormFileUpload;
import edu.fullerton.jspWebUtils.PageFormSubmit;
import edu.fullerton.jspWebUtils.PageFormText;
import edu.fullerton.jspWebUtils.PageItemBlanks;
import edu.fullerton.jspWebUtils.PageItemHeader;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ImageTable;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ViewUser;
import viewerplugin.GUISupport;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;
/**
 * Controls uploading and saving of external images
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ImageUploader extends GUISupport
{
    public ImageUploader(Database db, Page vpage, ViewUser vuser)
    {
        super(db,vpage,vuser);
    }
    private final int maxFiles = 10;     // how many slots in the form for uploads
    
    /**
     * Add the upload form to the current page
     */
    void addForm()
    {
        try
        {
            vpage.add(new PageItemHeader("External Image Upload", 3));
            vpage.addBlankLines(1);
            vpage.add("You may use this form to upload images created with other programs to the database."
                    + "Image types of png, jpeg and gif are supported.");
            vpage.addBlankLines(1);
            vpage.add("Unfortunately there's no way to verify the file is one of those types before "
                    + "it is uploaded, so please be sure it will be accepted before hitting the Upload Files"
                    + "button.  It will save you time.");
            vpage.addBlankLines(1);
            PageForm frm = new PageForm();
            frm.setMethod("post");
            frm.setName("fileUploader");
            frm.setAction(contextPath + "/upload");
            
            frm.addHidden("act", "upload");
            frm.addHidden("submitAct", "doupload");
            frm.setNoSubmit(true);
            
            PageTable uploadFields = new PageTable();
            
            for (Integer i = 1; i <= maxFiles; i++)
            {
                PageTableRow r = new PageTableRow();
                String partname = "file" + i.toString();
                PageFormFileUpload up = new PageFormFileUpload();
                up.setName(partname);
                up.setId("partname");
                up.setAllowMultiple(true);
                
                r.add(up);
                String descname = "desc" + i.toString();
                PageItemList pil = new PageItemList();
                pil.add(new PageItemString("Description:"));
                PageFormText desc = new PageFormText(descname, "");
                desc.setSize(36);
                pil.add(desc);
                r.add(pil);
                r.setClassAll("noborder");
                uploadFields.addRow(r);
            }
            frm.add(uploadFields);
            frm.add(new PageItemBlanks(2));
            frm.add(new PageFormSubmit("Upload Files", "uploadFiles"));
            vpage.add(frm);
        }
        catch (WebUtilException ex)
        {
            vpage.add("Error creating upload form" + ex.getMessage());
        }
        
    }

    /**
     * process the response to the upload form
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public void procReq() throws WebUtilException
    {
        ArrayList<Integer> uploadedIds = new ArrayList<>();
        ImageTable imageTable;
        try
        {
                imageTable = new ImageTable(db);
        }
        catch (SQLException ex)
        {
            WebUtilException wex = new WebUtilException("Processing upload request", ex);
            throw wex;
        }
        
        for (Integer i=1;i<=maxFiles;i++)
        {
            try
            {
                String partname = "file" + i.toString();
                Part part=null;
               
                
                if (part != null)
                {
                    String contentDisposition = part.getHeader("content-disposition");
                    
                    String filename="";
                    for(String content : contentDisposition.split(";"))
                    {
                        if (content.trim().startsWith("filename"))
                        {
                            filename = content.substring(content.indexOf('=') + 1).trim();
                            filename = filename.replace("\"", "");
                            break;
                        }
                    }
                    if (isValid(filename))
                    {
                        vpage.add("uploaded " + filename);
                        vpage.addBlankLines(1);
                        InputStream ins = part.getInputStream();
                        ByteArrayInputStream bis = getBytes(ins);
                        int imgId = imageTable.addImg(vuser.getCn(), bis, getMimeType(filename));
                        String descname = "desc" + i.toString();
                        String desc = "";
                        if (desc != null && !desc.isEmpty())
                        {
                            imageTable.addDescription(imgId, desc);
                        }
                        uploadedIds.add(imgId);
                    }
                    
                }
                
            }
            catch (Exception ex)
            {
                WebUtilException wex = new WebUtilException("Processing upload request", ex);
                throw wex;
            }
            
        }
        showNewImage(imageTable,uploadedIds);
    }

    /**
     * Check if the uploaded file has an acceptable extension
     * @param filename
     * @return 
     */
    private boolean isValid(String filename) throws IOException
    {
        boolean ret = false;
        if (filename != null && !filename.isEmpty())
        {
            String mime = getMimeType(filename);
            if (mime.contains("image"))
            {
                ret = mime.contains("png") || mime.contains("jpeg") || mime.contains("gif");
            }
            else if (mime.startsWith("video"))
            {
                ret = mime.contains("mp4") || mime.contains("ogg");
            }
            else
            {
                vpage.add(String.format("The file: %1$s has an unsuported mime type: %2$s", filename,mime));
                vpage.addBlankLines(1);
            }
        }
        else
        {
//            vpage.add("Empty filename seen.");
//            vpage.addBlankLines(1);
        }
        return ret;
    }

    private String getMimeType(String filename) throws IOException
    {
        MimetypesFileTypeMap mtfm = new MimetypesFileTypeMap("/etc/mime.types");
        String mime = mtfm.getContentType(filename).toLowerCase();
        return mime;
    }

    private ByteArrayInputStream getBytes(InputStream ins) throws IOException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final byte[] bytes = new byte[1024*512];
        int read;
        
        while ((read = ins.read(bytes)) != -1)
        {
            buf.write(bytes, 0, read);
        }
        ByteArrayInputStream ret = new ByteArrayInputStream(buf.toByteArray());
        return ret;
    }

    private void showNewImage(ImageTable imgTbl, ArrayList<Integer> ids) throws WebUtilException
    {
        PageTable table = new PageTable();
        table.setBorder(3);
        table.setCellpadding(15);
        table.setCellspacing(5);
        for (Integer id : ids)
        {
            PageItemList pil = new PageItemList();
            pil.add(getImageIdLine(imgTbl, id));
            pil.addBlankLines(2);
            String url = String.format("%1$s/?act=getImg&amp;imgId=%2$d",servletPath, id);
            String imgName = String.format("Image #%1$d", id);
            PageItemImage img = new PageItemImage(url, imgName, null);
            pil.add(img);
            pil.addBlankLines(2);
            table.addRow(pil);
        }
        vpage.add(table);
    }
    
    private PageItemList getImageIdLine(ImageTable imgTbl, int id) throws WebUtilException 
    {
        PageItemList idLine = new PageItemList();
        

        idLine.add(String.format("New image # %d, - ", id));
        try
        {
            idLine.add(imgTbl.getIdInfo(id));
        }
        catch (LdvTableException ex)
        {
            WebUtilException wex = new WebUtilException("Processing upload request", ex);
            throw wex;            
        }

        return idLine;
    }
    
    
}
