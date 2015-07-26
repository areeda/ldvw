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

import edu.fullerton.jspWebUtils.PageTableRow;
import edu.fullerton.jspWebUtils.WebUtilException;
import edu.fullerton.ldvjutils.BaseChanSelection;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for all possible parameters to a plugin
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public abstract class PluginParameter
{
    private String formLabel;   // String label for display
    private String formName;    // with prefix becomes the parameter name on the form
    private String comment;     // additional information for display
    private String argumentName;// argument name for command line program without --
 
    private String help;
    private Type type;
    
    protected int nDecimals;
    protected boolean useScientific = false;

    public final String fpRegex = "^(([+-]?\\d+\\.?\\d*)|([+-]?\\.\\d+))([Ee][+-]?\\d+)?$";
    protected String[] lastVal; // parameter value from submitted form
    protected List<BaseChanSelection> baseSelections;
    
    public enum Type 
    {
        SWITCH, LIST, NUMBER, STRING, NUMBERARRAY, STANDARD, REFCHAN
    }

    public PluginParameter(String formLabel, String formName, String comment)
    {
        this.formLabel = formLabel;
        this.formName = formName;
        this.comment = comment;
    }

    @Override
    public String toString()
    {
        return String.format("%1$s - %2$s (%3$s)", formLabel, comment, formName);
    }
    /**
     * 
     * @return internal name of this parameter
     */
    public String getFormLabel()
    {
        return formLabel;
    }

    /**
     * 
     * @param paramName internal name of this parameter
     * @return 
     */
    public PluginParameter setParameterName(String paramName)
    {
        this.formLabel = paramName;
        return this;
    }

    /**
     * A form input is named with a prefix and this name
     * @return the main part of the form name
     */
    public String getFormName()
    {
        return formName;
    }

    public PluginParameter setFormName(String formName)
    {
        this.formName = formName;
        return this;
    }

    public String getComment()
    {
        return comment;
    }

    public PluginParameter setComment(String comment)
    {
        this.comment = comment;
        return this;
    }

    public String getHelp()
    {
        return help;
    }

    public PluginParameter setHelp(String help)
    {
        this.help = help;
        return this;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    /**
     * Return the name of the command line argument for extrernal program
     * @return argument name without --
     */
    public String getArgumentName()
    {
        return argumentName;
    }

    public PluginParameter setArgumentName(String argument)
    {
        this.argumentName = argument;
        return this;
    }

    /**
     * Base channel selections are used by some parameters such as the reference chan selector
     */
    
    public List<BaseChanSelection> getBaseSelections()
    {
        return baseSelections;
    }

    public void setBaseSelections(List<BaseChanSelection> baseSelections)
    {
        this.baseSelections = baseSelections;
    }
    
    abstract PageTableRow getSelectorRow(String namespace) throws WebUtilException;

    
    // the get/set methods must be overriden for the type appropriate for the class
    // we define them ALL here so users deal only with the base class and derived classes
    // only have to implement the ones they need.
    
    public boolean getBoolValue() 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setVal(boolean val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    
    public PluginParameter setVal(String val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    
    public PluginParameter setVal(ArrayList<String> val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public PluginParameter setVal(String[] val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public String[] getStringArrayValue() 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setVal(double val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public double getNumberVal() 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public String getStringVal() 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public PluginParameter setStringVal(String val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setMultiSel(boolean b)
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }

    public boolean getMultiSel()
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    
    public PluginParameter setStringDefault(String val) 
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setBoolDefault(boolean val)
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setListStyle(String val)
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public String getListStyle()
    {
        throw new UnsupportedOperationException("This parameter does not have that value type");
    }
    public PluginParameter setnDecimals (int n)
    {
        nDecimals = n;
        return this;
    }
    public PluginParameter setScientific(boolean b)
    {
        useScientific = b;
        return this;
    }
    /**
     * set the last value returned by the client, so we can remember as we skip from page to page.
     * @param param - value from the parameter map
     */
    public void setLastVal(String[] param)
    {
        lastVal = param;
    }

    
}
