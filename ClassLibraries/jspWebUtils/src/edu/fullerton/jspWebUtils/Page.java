/*
 * Copyright (C) 2012 Joseph Areeda<joseph.areeda@ligo.org>
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * represents the entire HTML document including headers, scripts, titles...
 * The frameworks that use this model tend to build the document in stages and for some
 * functions just ignore it for others when the document is complete it will be sent all at once.
 * 
 * For example:
 * Page myPage = new Page();
 * myPage.setTitle("my coolness shows");
 * myPage.add(new PageItemHeader("title",1);
 * 
 * if (action.equals("sendImage"))
 * {
 *    sendImage("test.jpg");
 * }
 * else
 * {
 *    out.append(myPage.getHtml());
 * }
 * 
 * @author Joseph Areeda<joseph.areeda@ligo.org>
 */
public class Page
{
    protected String title="";
    protected String docType="<!DOCTYPE html>";
    protected String contentType = "text/html"; ///< default
    protected String charset = "UTF-8";
    protected String className="";
    protected Integer refreshInterval = -1;     ///< for automatic refresh, interval in seconds
    protected String refreshURL;                ///< for automatic refresh optional url
    
    protected ArrayList<PageItem> head;
    protected ArrayList<PageItem> body;
    protected ArrayList<PageItem> foot;
    
    protected String jsRoot;                    ///< directory containing our java script files
    protected String cssRoot;                   ///< direcotry containing our css files
    protected ArrayList<String> jsIncludes;     ///< paths for javascript includes
    protected ArrayList<String> cssIncludes;    ///< paths for css includes
    
    protected ArrayList<String> headJs;         ///< in-line javaScript routines for head section
    protected ArrayList<String> bodyJs;         ///< in-line javaScript routines for body section
    protected ArrayList<String> readyJs;        ///< init scripts run when jQuery(document).ready
    protected ArrayList<String> loadJs;         ///< init scripts run when window.onload
    
    protected boolean addStats=true;            ///< whether or now we should add page time/queries to footer
    protected long crTime;                      ///< System time our constructor was called
    protected int queryCount;                   ///< Set externally to the number of db queries used to create the page
    private String lastCSS;
    
    public Page()
    {
        crTime = System.currentTimeMillis();
        head = new ArrayList<PageItem>();
        body = new ArrayList<PageItem>();
        foot = new ArrayList<PageItem>();
        
        headJs  = new ArrayList<String>();
        bodyJs  = new ArrayList<String>();
        readyJs = new ArrayList<String>();
        loadJs  = new ArrayList<String>();
        
        jsIncludes = new ArrayList<String>();
        cssIncludes = new ArrayList<String>();
    }
    /**
     * Statistics such as page generation time and number of db queries can be added to footer
     * @return true if we're adding stats
     */
    public boolean isAddStats()
    {
        return addStats;
    }

    /**
     * Should we add stats to the footer such as time and count of db queries?
     * @param addStats 
     */
    public void setAddStats(boolean addStats)
    {
        this.addStats = addStats;
    }
    public void addBlankLines(int n)
    {
        add(new PageItemBlanks(n));
    }
    public void addLine(String s)
    {
        add(new PageItemString(s, false));
        add(new PageItemBlanks(1));
    }
    public void addHorizontalRule()
    {
        add(new PageItemString("<hr/>\n", false));
    }
    /**
     * Current page title directive
     * @return value of title
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * Specify value of title directive
     * @param title string to use
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDocType()
    {
        return docType;
    }

    public void setDocType(String docType)
    {
        this.docType = docType;
    }

    public int getQueryCount()
    {
        return queryCount;
    }

    public void setQueryCount(int queryCount)
    {
        this.queryCount = queryCount;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public String getCharset()
    {
        return charset;
    }

    public void setCharset(String charset)
    {
        this.charset = charset;
    }
    
    
    /**
     * Add this JavaScript routine to the HEAD section.
     * 
     * @param script JavaScript statements only the <script> tags will be added by page generator
     */
    public void addHeadJS(String script)
    {
        addUnique(headJs,script);
    }
    
    /**
     * Add this JavaScript routine to the BODY section.  All scripts are added at the beginning of the BODY.
     * @param script Javascript statements only the <script> tags will be added by page generator
     */
    public void addBodyJS(String script)
    {
        addUnique(bodyJs,script);
    }
    
    /**
     * Add this JavaScript routine to the (document).ready section. These are added at the beginning of
     * the BODY.  The ready functions happen as early as possible so jQuery can add functionality to the
     * items before additional items such as images are loaded
     *
     * @param script Javascript statements only the <script> tags will be added by page generator
     */
    public void addReadyJS(String script)
    {
        addUnique(readyJs, script);
    }
    
    public void addLoadJS(String script)
    {
        addUnique(loadJs, script);
    }
    /**
     * Add a PageItem or object value to the body of this page
     * @param it the item
     */
    public void add(Object it)
    {
        PageItem pi = getPageItem(it);
        body.add(pi);
    }
    
    /**
     * Add a Pageitem or object's value to the head section (top of page not the html header)
     * 
     * @param it the titem to add
     * @see PageItemString constructor for how non-page items are converted
     */
    public void addHead(Object it)
    {
        PageItem pi = getPageItem(it);
        head.add(pi);
    }
    
    public void addFoot(Object it)
    {
        PageItem pi = getPageItem(it);
        foot.add(pi);
    }

    /**
     * The default root for javascript files
     * 
     * @param jsRoot 
     */
    public void setJsRoot(String jsRoot)
    {
        if (!jsRoot.endsWith("/"))
        {
            jsRoot += "/";
        }
        this.jsRoot = jsRoot;
    }
    
    /**
     * The default root for javascript files
     *
     * @param cssRoot
     */
    public void setCssRoot(String cssRoot)
    {
        if (!cssRoot.endsWith("/"))
        {
            cssRoot += "/";
        }
        this.cssRoot = cssRoot;
    }
    /**
     * If path starts with / it is used as is else the jsRoot will be prepended
     * 
     * @param path 
     */
    public void includeJS(String path)
    {
        String inpath=fixJsPath(path);
        addUnique(jsIncludes,inpath);
    }
    public void includeCSS(String path)
    {
        String inpath=fixCssPath(path);
        addUnique(cssIncludes,inpath);
    }
    /**
     * Allow one CSS to always be last so we can override anything screwed up by update headers
     * 
     * @param path relative path to the css file
     */
    public void setLastCSS(String path)
    {
        String inpath=fixCssPath(path);
        lastCSS = inpath;
    }
    /**
     * Get the HTML representation of everything added to the Page
     * No header contents or mime type is set
     * The probably mime type would be text/html
     * 
     * @return one big string from <HTML> to </HTML> 
     */
    public String getHTML() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        updateHeader();         // go through all items to see if they need js or other header info
        if (lastCSS != null && ! lastCSS.isEmpty())
        {
            cssIncludes.add(lastCSS);
        }
        sb.append(getHdr());
        sb.append(getBody());
        sb.append(getFooter());
        
        return sb.toString();
    }
    /**
     * Set the (css) class name for the body of the page
     * 
     * @param name class name
     */
    public void setBodyClass(String name)
    {
        className=name;
    }
    /**
     * Set the page to auto refresh
     * 
     * @param sec - how long to wait in seconds
     * @param url - what url to display next (null or empty string refreshes same page)
     */
    public void setAutoRefresh(int sec, String url) throws WebUtilException
    {
        String ermsg = "";
        if (sec < 1 || sec > 3600)
        {
            ermsg += String.format("Auto refresh time seems unreasonable: %1$,d\n",sec);
        }
        else
        {
            refreshInterval = sec;
        }
        if (url != null && !url.isEmpty())
        {
            try
            {
                URL rurl = new URL(url);
                refreshURL = url;
            }
            catch (MalformedURLException ex)
            {
                ermsg += String.format("Auto refresh url is invalid [%1$s]\n", url);
            }
        }
        if (!ermsg.isEmpty())
        {
            throw new WebUtilException(ermsg);
        }
    }
    //==================== internal methods======================
    /**
     * Add a generic Object to the page.  This does what you'd expect if the .toString methods returns what you want to display
     * @param it the Object
     */
    protected PageItem getPageItem(Object it)
    {
        PageItem pi;
        if (it instanceof PageItem)
        {
            pi = (PageItem)it;
        }
        else
        {
            pi = new PageItemString(it);
        }
        return pi;
    }

    /**
     * For included javascript files, adjusts the path if necessary adding the JsRoot.
     * 
     * @param path relative to page url or absolute path (reltive to web url) to include file 
     * @return adjusted path
     */
    protected String fixJsPath(String path)
    {
        String myPath = path;
        if (!path.toLowerCase().startsWith("http://") && ! path.startsWith("/") && jsRoot != null )
        {
            myPath = jsRoot + path;
        }
        return myPath;
    }

    /**
     * For included css files, adjusts the path if necessary adding the cssRoot.
     *
     * @param path relative to page url or absolute path (reltive to web url) to include file
     * @return adjusted path
     */
    protected String fixCssPath(String path)
    {
        String myPath = path;
        if (!path.toLowerCase().startsWith("http://") && !path.startsWith("/") && jsRoot != null)
        {
            myPath = cssRoot + path;
        }
        return myPath;
    }

    /**
     * Some pageItems may require header items such as additional javaScript or style sheets
     * So this goes through each and gives them the chance to let us know.
     * 
     */
    protected void updateHeader()
    {
        for (PageItem pi : head)
        {
            pi.updateHeader(this);
        }
        for (PageItem pi : body)
        {
            pi.updateHeader(this);
        }
        for (PageItem pi : foot)
        {
            pi.updateHeader(this);
        }

    }
    /**
     * This method creates the header for the HTML with all the includes and the the PageItems for the header.
     * 
     * @return the start of the HTML.
     */
    private String getHdr()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append(docType).append("\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("    <meta http-equiv=\"Content-Type\" content=\"");
        sb.append(getContentType());
        sb.append("; charset=");
        sb.append(getCharset());
        sb.append("\"/>\n");
        sb.append("    <title>").append(title).append("</title>\n");

        for(String script: jsIncludes)
        {
            sb.append("    <script src=\"");
            sb.append(script);
            sb.append("\"></script>\n");
        }

        for(String js : headJs)
        {
            sb.append("      <script >");
            sb.append(js);
            sb.append("\n      </script>\n");
        }
        for(String cs : cssIncludes)
        {
            sb.append("     <link rel=\"stylesheet\" type=\"text/css\" href=\"");
            sb.append(cs);
            sb.append("\" />\n");
        }
        if (refreshInterval >= 0)
        {
            sb.append("    <meta http-equiv=\"refresh\" content=\"");
            sb.append(Integer.toString(refreshInterval));
            if (refreshURL != null && !refreshURL.isEmpty())
            {
                sb.append(";url=");
                sb.append(refreshURL);
            }
            sb.append("\">\n");
        }
        sb.append("</head>\n");

        
        return sb.toString();
    }
    /**
     * Process each item that goes into the body section.
     * NB:  This routine does not close the <body> tag, that is left for the Footer so it must be called afterwards
     * 
     * @return html code for the body
     */
    protected String getBody() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "<body");
        if (className.length() > 0)
            sb.append(" class=\"").append(className).append("\"");
        sb.append(">\n\n");
        
        if (!readyJs.isEmpty())
        {
            sb.append("<script>\n");
            sb.append("   jQuery(document).ready(function()\n");
            sb.append("   {\n");
            for(String js: readyJs)
            {
                sb.append("        ");
                sb.append(js);
                sb.append("\n");
            }
            sb.append("   });\n");
            sb.append("</script>\n");
        }
        
        if (!loadJs.isEmpty())
        {
            sb.append("<script>\n");
            sb.append("   window.onload=function()\n");
            sb.append("   {\n");
            for (String js : loadJs)
            {
                sb.append("        ");
                sb.append(js);
                sb.append("\n");
            }
            sb.append("   };\n");
            sb.append("</script>\n"); 
        }
        if (!bodyJs.isEmpty())
        {
            for(String js: bodyJs)
            {
                sb.append("<script>\n");
                sb.append(js);
                sb.append("\n</script>\n");
            }
        }
        
        for (PageItem pi : body)
        {
            sb.append(pi.getHtml());
        }

        return sb.toString();
    }
    protected String getFooter() throws WebUtilException
    {
        StringBuilder sb = new StringBuilder();
        if (foot.size() > 0 || isAddStats())
        {
            sb.append("<br><br>\n");
            for(PageItem pi : foot)
            {
                sb.append(pi.getHtml());
            }
            if (isAddStats())
            {
                double elap = (System.currentTimeMillis() - crTime)/1000.;
                PageItemString elapstr = new PageItemString(String.format("Page generated in %1$.2f seconds.  ", elap));
                elapstr.setClassName("footer");
                sb.append(elapstr.getHtml());
            }
        }
        sb.append("</body>\n</html>\n");
        
        return sb.toString();
    }
    
    /**
     * Search the current list to see if the new string is already there, if not add it.  No dups.
     * @param list the current list
     * @param it the new item
     */
    private void addUnique(List<String> list, String it)
    {
        boolean addIt = true;
        
        for(String cur : list)
        {
            if (cur.equals(it))
            {
                addIt = false;
                break;
            }
        }
        if (addIt)
        {
            list.add(it);
        }
    }
}
