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

package edu.fullerton.plugindefn;

import edu.fullerton.jspWebUtils.PageFormCheckbox;
import edu.fullerton.jspWebUtils.PageItemList;
import edu.fullerton.jspWebUtils.PageItemString;
import edu.fullerton.jspWebUtils.PageTable;
import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines a plugin in terms of its User Interface and execution properties
 * 
 * A plugin is a command line program that gets its parameters from the command line or a 
 * configuration file.  It outputs one or more images as results.
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public abstract class PluginController
{
    private String name;
    private String description;
    private String namespace;
    
    private final ArrayList<PluginParameter> parameters;
    private final HashMap<String, PluginAttribute> attributes;
    
    boolean inited = false;
    private File tempDir;
    private boolean useEquals;
    private boolean useQuotes;
    private int nDashes;
    private Map<String, String[]> paramMap;
    private PageItemString notes = null;
    private ViewUser vuser;
    private File tempFile;
    
    PluginController()
    {
        parameters = new ArrayList<>();
        attributes = new HashMap<>();
    }

    /**
     * initialize the plugin definition tables
     */
    protected abstract void init();
    
    /**
     * Create the UI to accept the parameters we need.
     * 
     * @param enableKey name of the checkbox parameter that says make this plot
     * @param nSel how many channels are selected
     * @return items to be added to the plot product accordion
     * 
     * @throws WebUtilException
     */
    public PageItemList getSelector(String enableKey, int nSel) throws WebUtilException
    {
        PageItemList ret = new PageItemList();
        if (!inited)
        {
            init();
        }
        String enableText = "Generate " + getName();
        enableText += nSel > 1 ? "s<br><br>" : "<br><br>";
        ret.add(new PageFormCheckbox(enableKey, enableText));
        
        String intro = "Set appropriate parameters below:<br>";
        ret.add(new PageItemString (intro, false));
        
        if (notes != null)
        {
            ret.add(notes);
        }
        
        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        PageTableRow ptr;
        
        for(PluginParameter p : parameters)
        {
            if (p.getType() != PluginParameter.Type.STANDARD)
            {
                ptr = p.getSelectorRow(getNamespace());
                product.addRow(ptr);
            }
        }
        ret.add(product);
        return ret;
    }
    
    /**
     * Given our definitions and the form parameters generate the command line to create the plot(s)
     * @param dbuf selected channels and time
     * @param paramMap form parameters
     * @return the command to run
     * @throws edu.fullerton.jspWebUtils.WebUtilException
     */
    public String getCommandLine(ArrayList<ChanDataBuffer> dbuf, Map<String, String[]> paramMap) throws WebUtilException
    {
        if (!inited)
        {
            init();
        }
        setFormParameters(paramMap);
        StringBuilder cmd = new StringBuilder();
        String prog = getStringAttribute("program", false);
        cmd.append(prog);
        
        useEquals = getBoolAttribute("useEquals", false);
        useQuotes = getBoolAttribute("useQuotes", true);
        nDashes = getIntAttribute("nDashes", 2);
        
        for (PluginParameter p : parameters)
        {
            PluginParameter.Type type = p.getType();
            switch(type)
            {
                case LIST:
                    String lparam = getListParameter(p);
                    cmd.append(lparam);
                    break;
                case SWITCH:
                    unimplemented("swith parameters not available yet");
                    break;
                case NUMBER:
                    String singleParam=getSingleParam(p);
                    cmd.append(singleParam);
                    break;
                case STRING:
                    unimplemented("string parameters not available yet");
                    break;
                case NUMBERARRAY:
                    String naparam = getNumberArrayParameter(p);
                    cmd.append(naparam);
                    break;
                case STANDARD:
                    String sparam = getStandardParameter(p, dbuf);
                    cmd.append(sparam);
                    break;
                default:
                    throw new AssertionError(type.name());
            }
        }
        return cmd.toString();
    }
    
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    
    public List<PluginParameter> getParameters()
    {
        return parameters;
    }

    public void addParameter(PluginParameter parameter)
    {
        parameters.add(parameter);
    }

    public PluginParameter getParameter(String formName)
    {
        for(PluginParameter p : parameters)
        {
            if (p.getFormName().equalsIgnoreCase(formName))
            {
                return p;
            }
        }
        return null;
    }
    public Map<String, PluginAttribute> getAttributes()
    {
        return attributes;
    }

    public void addAttribute(PluginAttribute attribute)
    {
        attributes.put(attribute.getName(), attribute);
    }

    public String getNamespace()
    {
        return namespace;
    }

    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    public File getTempDir()
    {
        return tempDir;
    }

    public void setTempDir(File tempDir)
    {
        this.tempDir = tempDir;
    }

    public String getStringAttribute(String attributeName, boolean canBeNull) throws WebUtilException
    {
        PluginAttribute attr = attributes.get(attributeName);
        String ret = null;
        if (attr != null)
        {
            ret = attr.getStringValue();
        }
        if (ret == null)
        {
            String ermsg = String.format("Missing attribute: %1$s for program: %2$s",
                                         attributeName, getName());
            throw new WebUtilException(ermsg);
        }
        return ret;
    }

    public boolean getBoolAttribute(String attributeName, boolean b)
    {
        boolean ret = b;
        PluginAttribute attr = attributes.get(attributeName);
        if (attr != null)
        {
            ret = attr.getBoolValue();
        }
        return ret;
    }

    public int getIntAttribute(String attributeName, int i)
    {
        int ret = i;
        PluginAttribute attr = attributes.get(attributeName);
        if (attr != null)
        {
            ret = attr.getIntValue();
        }
        return ret;
    }

    private String getStringAttribute(String attributeName, String defaultVal)
    {
        String ret = defaultVal;
        PluginAttribute attr = attributes.get(attributeName);
        if (attr != null)
        {
            ret = attr.getStringValue();
        }
        return ret;
    }
    private String getStandardParameter(PluginParameter p, ArrayList<ChanDataBuffer> dbuf) throws WebUtilException
    {
        String ret = "";
        String pname = p.getFormLabel();
        switch(pname)
        {
            case "baseChannel":
                for(ChanDataBuffer buf : dbuf)
                {
                    String cname = buf.getChanInfo().getChanName();
                    int dotPos = cname.lastIndexOf(".");
                    if (dotPos > 0)
                    {
                        cname=cname.substring(0, dotPos);
                    }
                    ret += getCmdArg(p.getArgumentName(), cname);
                }
                break;
                
            case "channel":
                for (ChanDataBuffer buf : dbuf)
                {
                    ret += getCmdArg(p.getArgumentName(), buf.getChanInfo().getChanName());
                }
                break;
                
            case "duration":
                for (ChanDataBuffer buf : dbuf)
                {
                    Long endGps = buf.getTimeInterval().getStopGps();
                    Long startGps = buf.getTimeInterval().getStartGps();
                    Long duration = endGps-startGps;
                    ret += getCmdArg(p.getArgumentName(), duration.toString());
                }
                break;
           
            case "email":
                String email = vuser.getMail();
                if (email != null && ! email.isEmpty())
                {
                    ret += getCmdArg(p.getArgumentName(), email);
                }
                break;
                
            case "end":
                for (ChanDataBuffer buf : dbuf)
                {
                    Long endGps = buf.getTimeInterval().getStopGps();
                    ret += getCmdArg(p.getArgumentName(), endGps.toString());
                }
                break;
                
            case "geometry":
                String[] geom = paramMap.get("geom");
                if (geom != null)
                {
                    ret += getCmdArg(p.getArgumentName(), geom[0]);
                }
                break;
                
            case "userName":
                ret += getCmdArg(p.getArgumentName(), vuser.getCn());
                break;
                
            case "server":
                for(ChanDataBuffer buf : dbuf)
                {
                    ret += getCmdArg(p.getArgumentName(), buf.getChanInfo().getServer());
                }
                break;
                
            case "start":
                for(ChanDataBuffer buf : dbuf)
                {
                    Long startGps = buf.getTimeInterval().getStartGps();
                    ret += getCmdArg(p.getArgumentName(), startGps.toString());
                }
                break;
                
            case "tempDir":
                try
                {
                    tempDir = Files.createTempDirectory("ldvw_").toFile();
                    
                    ret = getCmdArg(p.getArgumentName(),tempDir.getAbsolutePath());
                }
                catch (IOException ex)
                {
                    throw new WebUtilException("Can't create a temproary directory for :" + getName(), ex);
                }
                break;
                
            case "tempFile":
                String extension = p.getFormName();
                try
                {
                    tempFile = Files.createTempFile("ldvw", extension).toFile();
                    ret = getCmdArg(p.getArgumentName(), tempFile.getAbsolutePath());
                }
                catch (IOException ex)
                {
                    throw new WebUtilException("Can't create a temproary file for :" + getName(), ex);
                }
                break;
            default:
                String ermsg = String.format("Parameter controller: Unknown standard parameter (%1$s)"
                        + " for %2$s", pname, getName());
                throw new WebUtilException(ermsg);
        }
        return ret;
    }

    /**
     * Handle the variations in how command line arguments are specified.  We are controlled by the
     * fields useEquals and nDashes .
     * 
     * @param name 
     * @param value
     * @return formatted argument ready for insertion into command line
     */
    private String getCmdArg(String name, String value)
    {
        String ret = "";
        for(int i=0;i<nDashes;i++)
        {
            ret += "-";
        }
        ret += name;
        if (value != null && !value.isEmpty())
        {
            ret += useEquals ? "=" : " ";
            if (useQuotes)
            {
                ret += "'" + value + "'";
            }
            else
            {
                ret += value;
            }
            ret += " ";
        }
        return " " + ret + " ";
    }

    private String getListParameter(PluginParameter p) throws WebUtilException
    {
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);
        String argName = p.getArgumentName();
        String ret = " ";
        String listType = p.getListStyle();
        if (listType.isEmpty())
        {
            listType = getStringAttribute("listType", true);
        }
        switch(listType)
        {
            case "dmt":
            case "comma":
                String bracket1 = listType.equalsIgnoreCase("dmt") ? "{" : "";
                String bracket2 = listType.equalsIgnoreCase("dmt") ? "}" : "";
                String t="";
                for (String val : vals)
                {
                    t += t.isEmpty() ? "" : ",";
                    t+= val;
                }
                if (vals.length > 1)
                {
                    t = bracket1 + t + bracket2;
                    ret = getCmdArg(argName, t);
                }
                else
                {
                    ret = getCmdArg(argName, t);
                }
                break;
            case "":
            default:
                unimplemented("List type: " + listType + " is not supported.") ;
                break;
        }
        return ret;
    }

    private void unimplemented(String ermsg)
    {
        throw new UnsupportedOperationException(ermsg);
    }
    
    public void setFormParameters(Map<String, String[]> pmap)
    {
        paramMap = pmap;
    }

    private String getNumberArrayParameter(PluginParameter p) throws WebUtilException
    {
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);
        
        if (vals != null && vals.length == 1)
        {
            p.setStringVal(vals[0]);
        }
        
        String val = p.getStringVal();
        if (getStringAttribute("listType", true).equalsIgnoreCase("dmt"))
        {
            val="["+val+"]";
        }
        return getCmdArg(p.getArgumentName(), val);
    }

    private String getSingleParam(PluginParameter p)
    {
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);

        if (vals != null && vals.length==1 && !vals[0].isEmpty())
        {
            p.setStringVal(vals[0]);
        }
        String val = p.getStringVal();
        return getCmdArg(p.getArgumentName(), val);
    }

    public void setNotes(PageItemString notes)
    {
        this.notes = notes;
    }

    public void setVuser(ViewUser vuser)
    {
        this.vuser = vuser;
    }

    public File getTempFile()
    {
        return tempFile;
    }
    
}
