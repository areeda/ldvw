/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.fullerton.jspWebUtils;

/**
 * Display a clickable image that links to a url
 * @author areeda
 */
public class PageItemImageLink extends PageItem
{
    private String url;
    private PageItemImage img=null;
    private String target=null;
    
    private PageItemImageLink()
    {
        
    }
    
    /**
     * Constructor for link that replaces current page
     * @param url link to the new page
     * @param img image to display
     */
    public PageItemImageLink(String url, PageItemImage img)
    {
        this.url = url;
        this.img = img;
    }
    
    /**
     * Constructor for link that opens a new page or displays to a frame
     * @param url link to new page
     * @param img image to display
     * @param target 
     */
    public PageItemImageLink(String url, PageItemImage img, String target)
    {
        this.url = url;
        this.img = img;
        this.target = target;
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        if (url == null || img == null)
        {
            throw new WebUtilException("Image link is missing the url and/or image.");
        }

        String ret = "<a " + getAttributes() + "href=\"";
        ret += url + "\"";
        if (target != null && target.length() > 0)
        {
            ret += " TARGET=\"" + target + "\" ";
        }

        ret += ">" + img.getHtml() + "</a>";
        return ret;
    }

}
