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

package edu.fullerton.ldvservlet;

import edu.fullerton.jspWebUtils.Page;
import edu.fullerton.jspWebUtils.PageItemImage;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ImageTable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import viewerconfig.ViewerConfig;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class Upload extends HttpServlet
{
    String version = "0.2.01";
    private long loadTime;
    ViewerConfig viewerConfig;
    private Pattern fileNumPat;
    
    /**
     * Initialization on loading servlet, one time things like
     *
     * Load our configuration file. Make sure all the tables exist.
     *
     * @throws javax.servlet.ServletException
     */
    @Override
    public void init() throws ServletException
    {
        loadTime = System.currentTimeMillis();
        String[] tableNames =
        {
            "edu.fullerton.ldvtables.ErrorLog",
            "edu.fullerton.ldvtables.HelpTextTable",
            "edu.fullerton.ldvtables.ImageCoordinateTbl",
            "edu.fullerton.ldvtables.ImageGroupTable",
            "edu.fullerton.ldvtables.ImageTable",
            "edu.fullerton.ldvtables.UseLog",
            "edu.fullerton.ldvtables.ViewUser"
        };
        ServletSupport servSupport = new ServletSupport();
        servSupport.checkDb(tableNames);
        viewerConfig = servSupport.getViewerConfig();
        fileNumPat = Pattern.compile("^\\D+(\\d+)$");
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        long startTime = System.currentTimeMillis();

        if (! ServletFileUpload.isMultipartContent(request))
        {
            throw new ServletException("This action requires a multipart form with a file attached.");
        }
        ServletSupport servletSupport;
        
        servletSupport = new ServletSupport();
        servletSupport.init(request, viewerConfig, false);
        
        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Configure a repository (to ensure a secure temp location is used)
        ServletContext servletContext = this.getServletConfig().getServletContext();
        File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        factory.setRepository(repository);

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        ImageTable imageTable;
        String viewServletPath = request.getContextPath() + "/view";

        try
        {
            imageTable = new ImageTable(servletSupport.getDb());
        }
        catch (SQLException ex)
        {
            String ermsg = "Image upload: can't access the Image table: "
                           + ex.getClass().getSimpleName() + " " + ex.getLocalizedMessage();
            throw new ServletException(ermsg);
        }
        try
        {
            HashMap<String, String> params = new HashMap<>();
            ArrayList<Integer> uploadedIds = new ArrayList<>();
            
            Page vpage = servletSupport.getVpage();
            vpage.setTitle("Image upload");
            // Parse the request
            List<FileItem> items = upload.parseRequest(request);
            int cnt = items.size();
            for(FileItem item: items)
            {
                if (item.isFormField())
                {
                    String name = item.getFieldName();
                    String value = item.getString();
                    if (! value.isEmpty())
                    {
                        params.put(name, value);
                    }
                }
            }
            for (FileItem item : items)
            {
                if (! item.isFormField())
                {
                    int imgId = addFile(item, params, vpage,  
                            servletSupport.getVuser().getCn(), imageTable);
                    if (imgId != 0)
                    {
                        uploadedIds.add(imgId);
                    }
                }
            }
            if (!uploadedIds.isEmpty())
            {
                showImages(vpage,uploadedIds, imageTable, viewServletPath);
            }
            servletSupport.showPage(response);
        }
        catch (FileUploadException ex)
        {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        throw new ServletException("This action cannot be called with a GET request.");
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo()
    {
        return "name: Image Upload Manager\n"
               + "version: " + version + "\n"
               + "author: Joseph Areeda, Joshua Smith\n"
               + "copyright: 2012-2014\n"
               + "license:  GNU Public 3.0\n";
    }// </editor-fold>

    private int addFile(FileItem item, HashMap<String, String> params, Page vpage, 
                        String userName, ImageTable imageTable) throws ServletException
    {
        int ret = 0;

        String fieldName = item.getFieldName();
        String fileName = item.getName();
        String contentType = item.getContentType();
        boolean isInMemory = item.isInMemory();
        long sizeInBytes = item.getSize();
        if (sizeInBytes >= 100 && !fileName.isEmpty())
        {
            Matcher m = fileNumPat.matcher(fieldName);
            if (m.find())
            {
                String descriptionName = "desc" + m.group(1);
                String description = params.get(descriptionName);
                description = description == null ? "" : description;
                if (isValid(contentType))
                {
                    try
                    {
                        byte[] buf = item.get();
                        ByteArrayInputStream bis = new ByteArrayInputStream(buf);
                        int imgId = imageTable.addImg(userName, bis, contentType);
                        if (!description.isEmpty())
                        {
                            imageTable.addDescription(imgId, description);
                        }
                        ret = imgId;
                        vpage.add("uploaded " + fileName + " - " + description);
                        vpage.addBlankLines(1);
                    }
                    catch (IOException | SQLException | NoSuchAlgorithmException ex)
                    {
                        String ermsg = "Error reading data for " + fileName 
                                       + ex.getClass().getSimpleName() + " " + ex.getLocalizedMessage();
                        vpage.add(ermsg);
                        vpage.addBlankLines(1);
                    }
                }
                else
                {
                    vpage.add(String.format("The file: %1$s has an unsuported mime type: %2$s", 
                                            fileName, contentType));
                    vpage.addBlankLines(1);
                }
            }
        }
        return ret;
    }

    private boolean isValid(String mime)
    {
        boolean ret;
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
            ret = false;
        }
        return ret;
    }

    private void showImages(Page vpage, ArrayList<Integer> uploadedIds, ImageTable imageTable,
                            String servletPath)
    {
        try
        {
            PageTable table = new PageTable();
            table.setBorder(3);
            table.setCellpadding(15);
            table.setCellspacing(5);
            for (Integer id : uploadedIds)
            {
                PageItemList pil = new PageItemList();
                pil.add(getImageIdLine(imageTable, id));
                pil.addBlankLines(2);
                String url = String.format("%1$s?act=getImg&amp;imgId=%2$d", servletPath, id);
                String imgName = String.format("Image #%1$d", id);
                PageItemImage img = new PageItemImage(url, imgName, null);
                pil.add(img);
                pil.addBlankLines(2);
                table.addRow(pil);
            }
            vpage.add(table);
        }
        catch (WebUtilException ex)
        {
            Logger.getLogger(Upload.class.getName()).log(Level.SEVERE, null, ex);
        }

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
