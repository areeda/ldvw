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
import edu.fullerton.ldvjutils.BaseChanSelection;
import edu.fullerton.ldvjutils.ChanInfo;
import edu.fullerton.ldvjutils.LdvTableException;
import edu.fullerton.ldvtables.ViewUser;
import edu.fullerton.viewerplugin.ChanDataBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    public static String fltPat = "^(([1-9][0-9]*\\.?[0-9]*)|(\\.[0-9]+))([Ee][+-]?[0-9]+)?$";
    
    private String name;
    private String description;
    private String namespace;
    
    private final ArrayList<PluginParameter> parameters;
    private final ArrayList<String> constants;
    private final HashMap<String, PluginAttribute> attributes;
    
    protected boolean inited = false;
    private File tempDir;
    private boolean useEquals;
    private boolean useQuotes;
    private int nDashes;
    private Map<String, String[]> paramMap;
    private PageItemString notes = null;
    private ViewUser vuser;
    private File tempFile;
    private String dashStr;
    private List<BaseChanSelection> baseSelections;
    
    PluginController()
    {
        parameters = new ArrayList<>();
        attributes = new HashMap<>();
        constants = new ArrayList<>();
        tempDir = null;
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
        enableText += "<br><br>";
        boolean enabled = getPrevValue(enableKey);
        PageFormCheckbox cb = new PageFormCheckbox(enableKey, enableText, enabled);
        cb.setId(enableKey + "_cb");
        String fun = String.format("boldTextOnCheckbox('%1$s_cb','%1$s_accLbl')", enableKey);
        cb.addEvent("onclick", fun);
        ret.add(cb);
        
        String intro = "Set appropriate parameters below:<br>";
        ret.add(new PageItemString (intro, false));
        
        if (notes != null)
        {
            ret.add(notes);
        }
        
        PageTable product = new PageTable();
        product.setClassName("SelectorTable");
        PageTableRow ptr;
        String prefix = getNamespace() + "_";
        for(PluginParameter p : parameters)
        {
            if (p.getType() != PluginParameter.Type.STANDARD )
            {
                p.setLastVal(paramMap.get(prefix + p.getFormName()));
                if (p.getType() == PluginParameter.Type.SWITCH)
                {
                    if (paramMap.get(prefix + p.getFormName()) != null)
                    {
                        p.setVal(true);
                    }
                }
                if (p.getType() == PluginParameter.Type.REFCHAN)
                {
                    p.setBaseSelections(baseSelections);
                }
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
     * @throws edu.fullerton.ldvjutils.LdvTableException
     */
    public String getCommandLine(ArrayList<ChanDataBuffer> dbuf, Map<String, String[]> paramMap) 
            throws WebUtilException, LdvTableException
    {
        StringBuilder cmd = new StringBuilder();
        List<String> args = getCommandArray(dbuf, paramMap);
        if (args == null || args.isEmpty())
        {
            throw new WebUtilException("No external command for this program.");
        }
        else
        {
            Pattern argQuoteNeeded = Pattern.compile("^[\\d\\w-=/]+$");
            
            for(String arg : args)
            {
                Matcher m = argQuoteNeeded.matcher(arg);
                if (useQuotes || !m.find())
                {
                    arg = "'" + arg + "'";
                }
                cmd.append(arg).append(" ");
            }
        }
        return cmd.toString();
    }
    public List<String> getCommandArray(ArrayList<ChanDataBuffer> dbuf, 
                        Map<String, String[]> paramMap) throws WebUtilException, LdvTableException
    {
        ArrayList<String> ret = new ArrayList<>();
        
        if (!inited)
        {
            init();
        }
        setFormParameters(paramMap);
        
        useEquals = getBoolAttribute("useEquals", false);
        useQuotes = getBoolAttribute("useQuotes", true);
        nDashes = getIntAttribute("nDashes", 2);
        dashStr = "";
        for(int i=0;i<nDashes;i++)
        {
            dashStr += '-';
        }
        String prog = getStringAttribute("program", false);
        if (prog.contains(","))
        {
            String[] progStrings = prog.split(",");
            for(String ps: progStrings)
            {
                ret.add(ps.trim());
            }
        }
        else
        {
            ret.add(prog);
        }
        
        for(String constant : constants)
        {
            ret.add(constant);
        }
            
        for (PluginParameter p : parameters)
        {
            PluginParameter.Type type = p.getType();
            ArrayList<String> arg = new ArrayList<>();
            switch(type)
            {
                case LIST:
                    arg = getListParameter(p);
                    break;
                case SWITCH:
                    arg = getSwitchParameter(p);
                    break;
                case NUMBER:
                case STRING:
                    arg = getSingleParam(p);
                    break;
                case NUMBERARRAY:
                    arg = getNumberArrayParameter(p);
                    break;
                case REFCHAN:
                    arg = getSingleParam(p);
                    break;
                case STANDARD:
                    arg = getStandardParameter(p, dbuf);
                    break;
                default:
                    throw new AssertionError("Unknown parameter type: " + type.name());
            }
            if (arg != null && !arg.isEmpty())
            {
                ret.addAll(arg);
            }
        }
        return ret;
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

    /**
     * Channel selections are used by reference channels
     * 
     * @return list of selected channels
     */
    public List<BaseChanSelection> getBaseSelections()
    {
        return baseSelections;
    }

    public void setBaseSelections(List<BaseChanSelection> baseSelections)
    {
        this.baseSelections = baseSelections;
    }

    /**
     * Constants are arguments to the program that don't depend on the form parameters
     * @param constant the arument to add
     */
    public void addConstant(String constant)
    {
        constants.add(constant);
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
    /**
     * Standard parameters are the global ones on the page defining the data to be plotted.
     * @param p
     * @param dbuf
     * @return
     * @throws WebUtilException
     * @throws LdvTableException 
     */
    private ArrayList<String> getStandardParameter(PluginParameter p, ArrayList<ChanDataBuffer> dbuf) throws WebUtilException, LdvTableException
    {
        ArrayList<String> ret = new ArrayList<>();
        String pname = p.getFormLabel();
        String argName = dashStr + p.getArgumentName();
        if (dbuf == null || dbuf.isEmpty())
        {
            throw new IllegalArgumentException("No data specified for getStandardParameter.");
        }
        StringBuilder val = new StringBuilder();

        switch(pname)
        {
            case "baseChannel":
            {
                HashSet<String> chans = new HashSet<>();
                if (p.getListStyle().equalsIgnoreCase("dmt"))
                {
                    val.append(argName).append("=");
                    val.append("[");
                }
                else
                {
                    ret.add(argName);
                }
                for(ChanDataBuffer buf : dbuf)
                {
                    String cname = buf.getChanInfo().getChanName();
                    int dotPos = cname.lastIndexOf(".");
                    if (dotPos > 0)
                    {
                        cname=cname.substring(0, dotPos);
                    }
                    if (!chans.contains(cname))
                    {
                        chans.add(cname);
                        if (p.getListStyle().equalsIgnoreCase("dmt"))
                        {
                            val.append(cname).append(" ");
                        }
                        else
                        {
                            ret.add(cname);
                        }
                    }
                }
                if (p.getListStyle().equalsIgnoreCase("dmt"))
                {
                    val.append("]");
                    ret.add(val.toString());
                }
            }
                break;
                
            case "channel":
            {
                if (p.getListStyle().equalsIgnoreCase("python"))
                {
                    ret.add(argName);
                }
                HashSet<String> chans = new HashSet<>();
                for (ChanDataBuffer buf : dbuf)
                {
                    if (p.getListStyle().equalsIgnoreCase("python"))
                    {
                        ChanInfo ci = buf.getChanInfo();
                        String nameStr = ci.getChanName();
                        switch (ci.getcType())
                        {
                            case "minute-trend":
                                nameStr += ",m-trend";
                                break;
                            case "second-trend":
                                nameStr += ",s-trend";
                                break;
                            case "RDS":
                                nameStr += ",reduced";
                                break;
                            case "online":
                                nameStr += ",online";
                                break;
                        }
                        if (!chans.contains(nameStr))
                        {
                            ret.add(nameStr);
                            chans.add(nameStr);
                        }
                    }
                    else if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(),buf.getChanInfo().getChanName()));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(buf.getChanInfo().getChanName());
                    }
                }
            }
                break;
                
            case "duration":
                {
                    ChanDataBuffer buf = dbuf.get(0);
                    Long duration = buf.getTimeInterval().getDuration();
                    if (buf.getChanInfo().getcType().equalsIgnoreCase("minute-trend"))
                    {
                        // adjust duration to stop at last minute
                        duration = duration / 60 * 60;
                    }
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(), duration.toString()));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(duration.toString());
                    }
                }
                break;
           
            case "email":
                String email = vuser.getMail();
                if (email != null && ! email.isEmpty())
                {
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(), email));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(email);
                    }
                }
                break;
                
            case "end":
            {
                HashSet<Long> endSet = new HashSet<>();
                for (ChanDataBuffer buf : dbuf)
                {
                    Long endGps = buf.getTimeInterval().getStopGps();
                    if (!endSet.contains(endGps))
                    {
                        endSet.add(endGps);
                        if (useEquals)
                        {
                            ret.add(getCmdArg(p.getArgumentName(), endGps.toString()));
                        }
                        else
                        {
                            ret.add(argName);
                            ret.add(endGps.toString());
                        }
                    }
                }
            }
                break;
                
            case "geometry":
                String[] geom = paramMap.get("geom");
                if (geom != null && geom.length > 0)
                {
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(), geom[0]));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(geom[0].toString());
                    }
                }
                break;
                
            case "userName":
                if (useEquals)
                {
                    ret.add(getCmdArg(p.getArgumentName(), vuser.getCn()));
                }
                else
                {
                    ret.add(argName);
                    ret.add(vuser.getCn());
                }
                break;
                
            case "refchan":
                // we pass reference channel to the program as the first channel not a separate arg
                break;
                
            case "server":
                if (!useEquals)
                {
                    ret.add(argName);
                }
                for(ChanDataBuffer buf : dbuf)
                {
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(), buf.getChanInfo().getServer()));
                    }
                    else
                    {
                        ret.add(buf.getChanInfo().getServer() );
                    }
                }
                break;
                
            case "start":
            {
                HashSet<Long> starts = new HashSet<>();
                if (!useEquals)
                {
                    ret.add(argName);
                }
                for(ChanDataBuffer buf : dbuf)
                {
                    Long startGps = buf.getTimeInterval().getStartGps();
                    if (buf.getChanInfo().getcType().equalsIgnoreCase("minute-trend"))
                    {
                        // adjust start time to beginning of next minute
                        startGps = (startGps + 59) / 60 * 60;
                    }
                    if (!starts.contains(startGps))
                    {
                        starts.add(startGps);
                        if (useEquals)
                        {
                            ret.add(getCmdArg(p.getArgumentName(), startGps.toString()));
                        }
                        else
                        {
                            ret.add(startGps.toString());
                        }
                    }
                }
            }
                break;
                
            case "startDbl":
            {
                HashSet<Double> starts = new HashSet<>();
                
                if (!useEquals)
                {
                    ret.add(argName);
                }
                for (ChanDataBuffer buf : dbuf)
                {
                    Double startGps = buf.getTimeInterval().getStartGpsD();
                    if (!starts.contains(startGps))
                    {
                        starts.add(startGps);
                        if (useEquals)
                        {
                            ret.add(getCmdArg(p.getArgumentName(), String.format("%1$.4f", startGps)));
                        }
                        else
                        {
                            ret.add(String.format("%1$.4f", startGps));
                        }
                    }
                }
            }
                break;
                
            case "tempDir":
                try
                {
                    if (tempDir == null)
                    {
                        tempDir = Files.createTempDirectory("ldvw_").toFile();
                    }
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(),tempDir.getAbsolutePath()));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(tempDir.getAbsolutePath());
                    }
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
                    if (tempFile == null)
                    {
                        tempFile = Files.createTempFile("ldvw", extension).toFile();
                    }
                    if (useEquals)
                    {
                        ret.add(getCmdArg(p.getArgumentName(), tempFile.getAbsolutePath()));
                    }
                    else
                    {
                        ret.add(argName);
                        ret.add(tempFile.getAbsolutePath());
                    }
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
        }
        return ret;
    }

    private ArrayList<String> getListParameter(PluginParameter p) throws WebUtilException
    {
        ArrayList<String> ret = new ArrayList<>();
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);
        String argName = p.getArgumentName();
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
                if (vals != null)
                {
                    for (String val : vals)
                    {
                        t += t.isEmpty() ? "" : ",";
                        t+= val;
                    }
                    if (vals.length > 1)
                    {
                        t = bracket1 + t + bracket2;
                    }
                    ret.add(getCmdArg(argName, t));
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

    private ArrayList<String> getNumberArrayParameter(PluginParameter p) throws WebUtilException
    {
        ArrayList<String> ret = new ArrayList<>();
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
        ret.add(getCmdArg(p.getArgumentName(), val));
        return ret;
    }

    /**
     * check if parameter exists
     * @param p - name of the paramter (without the namespace)
     * @return true if it's in the map
     */
    private boolean hasParam(PluginParameter p)
    {
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);
        return vals != null;
    }
    /**
     * Return argument for this parameter if applicable
     *
     * @param p a switch parameter
     * @return list containing zero or one item
     */
    private ArrayList<String> getSwitchParameter(PluginParameter p)
    {
        ArrayList<String> ret = new ArrayList<>();
        if (hasParam(p))
        {
            String arg = dashStr + p.getArgumentName();
            ret.add(arg);
        }
        return ret;
    }
    private ArrayList<String> getSingleParam(PluginParameter p)
    {
        ArrayList<String> ret = new ArrayList<>();
        String formName = getNamespace() + "_" + p.getFormName();
        String[] vals = paramMap.get(formName);

        if (vals != null && vals.length==1 && !vals[0].isEmpty())
        {
            p.setStringVal(vals[0]);
        }
        String val = p.getStringVal();
        if (val != null && !val.isEmpty())
        {
            if (useEquals)
            {
                String arg = p.getArgumentName() + "=" + val;
                ret.add(arg);
            }
            else
            {
                ret.add(dashStr + p.getArgumentName());
                ret.add(val);
            }
        }
        return ret;
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
    /**
     * As part of remembering where we came from, form values are passed back and forth to select
     * more. Here we use the previous value or default for the specified key
     *
     * @param key - Parameter name for this field
     * @param idx - Index into value array, 0 if only 1 value allowed
     * @param def - default value if no parameter or parameter is empty
     * @return
     */
    public String getPrevValue(String key, int idx, String def)
    {
        String ret = def;
        String[] prev = paramMap.get(key);
        if (prev != null && prev.length > idx && !prev[0].isEmpty())
        {
            ret = prev[idx];
        }
        return ret;
    }

    /**
     * Checkboxes are a bit difficult because their key only gets sent if it's checked. So we don't
     * really know if it's the first time thru with no values for anything or they unchecked it.
     *
     * @param key - parameter name
     * @return true if parameter is available
     */
    public boolean getPrevValue(String key)
    {
        boolean ret = paramMap.containsKey(key);
        return ret;
    }

    public String getNameSpace()
    {
        if (!inited)
        {
            init();
        }
        return getNameSpace();
    }
}
