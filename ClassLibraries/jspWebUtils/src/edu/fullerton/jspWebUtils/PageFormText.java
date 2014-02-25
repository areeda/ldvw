/*
 * Copyright (C) 2012 Joseph Areeda <joe@areeda.com>
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

/**
 * Text input for an html form, may be single line or multiple line
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageFormText extends PageFormItem
{
    private int maxLen;             // max character accepted
    private String defaultValue;
    private boolean password;       // use the password type to hide input
    private int nLines;             // define a text area with multiple lines if > 1
    private boolean useEditor;      // make this area editible with js editor
    
    public PageFormText(String name, String defaultValue)
    {
        this.name = name;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        maxLen = 0;
        size=0;
        nLines = 1;
    }
    public PageFormText(String name, String defaultValue, int siz)
    {
        this.name = name;
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        maxLen = 0;
        size = siz;
        nLines = 1;
    }
    public String getDefaultValue()
    {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    public int getMaxLen()
    {
        return maxLen;
    }

    public void setMaxLen(int maxLen)
    {
        this.maxLen = maxLen;
    }

    public boolean isPassword()
    {
        return password;
    }

    public void setPassword(boolean password)
    {
        this.password = password;
    }

    public int getnLines()
    {
        return nLines;
    }

    public void setnLines(int nLines)
    {
        this.nLines = nLines;
    }

    public boolean isUseEditor()
    {
        return useEditor;
    }

    public void setUseEditor(boolean useEditor)
    {
        this.useEditor = useEditor;
        this.setClassName("editable");
    }

    @Override
    public String getHtml()
    {
        StringBuilder sb = new StringBuilder();

        if (password)
        {
            sb.append("<input type=\"password\"");
        }
        else if (nLines > 1)
        {
            sb.append("<textarea");
        }
        else
        {
            sb.append("<input type=\"text\"");
        }
        
        sb.append(" name = \"").append(getName()).append("\" ");

        if (size > 0 && nLines > 1)
        {
            sb.append(String.format("cols=\"%1$d\" ", size));
        }
        else if (size > 0)
        {
            sb.append(String.format("size= \"%1$d\" ", size));
        }
        else if (maxLen > 0)
        {
            sb.append(String.format("size=\"%1$d\" ", size < 60 && size > 0 ? size : 60));
        }
        if (maxLen > 0)
        {
            sb.append(String.format(" maxlength=\"%1$d\" ",maxLen));
        }
        if (nLines > 2)
        {
            sb.append(String.format(" rows=\"%1$d\" ",nLines));
        }
        if (defaultValue.length() > 0)
        {
            sb.append(String.format(" value=\"%1$s\" ",defaultValue));
        }
        sb.append(getAttributes());
        if (nLines < 2)
        {
            sb.append("/>\n");
        }
        else
        {
            sb.append(">\n</textarea>\n");
        }

        return sb.toString();
    }
    
    @Override
    public void updateHeader(Page page)
    {
        if (useEditor)
        {
            page.includeJS("tinymce/tinymce.min.js");
            page.addHeadJS
             (
                "    tinymce.init({\n" +
                "        selector: \"textarea.editable\"\n" +
                "     });\n" +
                ""
              );
        }
    }
}
