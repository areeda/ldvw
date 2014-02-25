/*
 * Copyright (C) 2012 joe
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
package edu.fullerton.jspWebUtils;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * provides the basic HTML form functions, elements of the form are defined as their own class
 * The basic parts to a form are:
 *     Head - defines the form and submit action
 *     Hidden - hidden variables are passed back as is but not displayed to the user in any way
 *     Display - pageItems some may be entry some tables or text or images with no returned data
 *     End - Adds submit and cancel buttons (maybe) and closes the form tag
 * 
 * @author joe areeda
 */
public class PageForm extends PageItem
{

    private String action;   ///< form action (url to submit data)
    private String method="post";   ///< must be get or post
    private ArrayList<PageItem> items;
    private TreeMap<String,String> hidden;
    private String description;
    private String submit;   ///< text for the submit button
    private String cancel;   ///< text for the cancel button
    private boolean noSubmit;    ///< if they want their own submit buttons don't add defautls

    public PageForm()
    {
        items = new ArrayList<PageItem>();
        noSubmit = false;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getCancel()
    {
        return cancel;
    }

    public void setCancel(String cancel)
    {
        this.cancel = cancel;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method) throws WebUtilException
    {
        if (! (method.equalsIgnoreCase("get") || method.equalsIgnoreCase("post")))
            throw new WebUtilException(String.format("Invalid method for a form [%s", method));
        this.method = method;
    }

    public boolean isNoSubmit()
    {
        return noSubmit;
    }

    public void setNoSubmit(boolean noSubmit)
    {
        this.noSubmit = noSubmit;
    }

    public String getSubmit()
    {
        return submit;
    }

    public void setSubmit(String submit)
    {
        this.submit = submit;
    }
    
    public void add(PageItem pi) throws WebUtilException
    {
        if (pi == null)
            throw new WebUtilException("Attempt to add null item to PageForm");
        items.add(pi);
    }
    public void addHidden(String name, String value) throws WebUtilException
    {
        if (name == null || name.length() == 0)
            throw new WebUtilException("Attempt to add hidden value with no name");
        if (hidden==null)
            hidden = new TreeMap<String,String>();
        hidden.put(name,value);
    }
    /**
     * If the items need any javascript or style sheet add it to the page so it can go out in the
     * header before we send our contents.
     *
     * @param page the html we're building
     */
    @Override
    public void updateHeader(Page page)
    {
        for (PageItem pi : items)
        {
            pi.updateHeader(page);
        }
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getFormStart());
        sb.append(getFormHidden());
        for(PageItem pi : items)
        {
            sb.append(pi.getHtml());
        }
        sb.append(getFormEnd());
        
        return sb.toString();
    }
    public String getFormStart() throws WebUtilException
    {
        if (action == null || action.length() ==0)
            throw new WebUtilException("Form has no action.");
        
        StringBuilder sb = new StringBuilder();
        sb.append("<form enctype=\"multipart/form-data\" ");

        
        if (name.length() > 0)
            sb.append(" name=\"").append(name).append("\" ");

        
        if (id.length() > 0)
            sb.append(" id=\"").append(id).append("\" ");

        sb.append(" action=\"").append(action).append("\" method=\"").append(method).append("\">\n");
        
        return sb.toString();
    }
    public String getFormHidden()
    {
        StringBuilder sb = new StringBuilder();
        if (hidden != null)
        {
            for(String keyName: hidden.keySet())
            {
                sb.append("<input type=\"hidden\" name=\"").append(keyName).append("\" ");
                sb.append(" class =\"").append(keyName).append("\" ");
                sb.append(" value=\"");
                String val = hidden.get(keyName);
                val = val == null ? "" : val;
                sb.append(val );
                sb.append("\" />\n");
            }
        }
        return sb.toString();
    }
    public String getFormEnd()
    {
        StringBuilder sb = new StringBuilder();
        if (!noSubmit)
        {
            String submitText = (submit == null || submit.length() == 0) ?  "Submit" : submit;
            
            
            PageFormSubmit pfs = new PageFormSubmit("submit", submitText);
            sb.append(pfs.getHtml());
            
            if (cancel != null && cancel.length() > 0 )
            {
                PageFormSubmit pfc = new PageFormSubmit(cancel,cancel);
                sb.append(pfc.getHtml());
            }
        }
        sb.append("</form>\n");
        return sb.toString();
    }
}
