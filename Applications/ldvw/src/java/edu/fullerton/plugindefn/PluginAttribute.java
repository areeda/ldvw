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

/**
 * An attribute is one of a fixed list of settings that define behavior of the plugin
 * 
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PluginAttribute
{

    public enum AttType
    {
        BOOLEAN, STRING, INTEGER
    }
    
    private final String name;
    private final Object val;
    private final AttType attributeType;

    public PluginAttribute(String name, Boolean value)
    {
        this.name = name;
        this.val = value;
        attributeType = AttType.BOOLEAN;
    }
    
    public PluginAttribute(String name, String value)
    {
        this.name = name;
        this.val = value;
        attributeType = AttType.STRING;
    }
    
    public PluginAttribute(String name, Integer value)
    {
        this.name = name;
        this.val = value;
        attributeType = AttType.INTEGER;
    }
    public String getName()
    {
        return name;
    }
    
    public Object getValue()
    {
        return val;
    }
    
    public Boolean getBoolValue()
    {
        if (attributeType != AttType.BOOLEAN)
        {
            throw new IllegalArgumentException("GetXXXValue called for argument of wrong type.");
        }
        return (Boolean) val;
    }
    
    public String getStringValue()
    {
        if (attributeType != AttType.STRING)
        {
            throw new IllegalArgumentException("GetXXXValue called for argument of wrong type.");
        }
        return (String) val;
    }
    int getIntValue()
    {
        if (attributeType != AttType.INTEGER)
        {
            throw new IllegalArgumentException("GetXXXValue called for argument of wrong type.");
        }
        return (Integer) val;
    }
}
