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
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class ParameterFactory
{

    /**
     * 
     * @param type
     * @param formLabel
     * @param formName
     * @param comment
     * @return
     */
    public static PluginParameter buildParam(PluginParameter.Type type, String formLabel, 
                                             String formName, String comment)
    {
        PluginParameter ret = null;
        switch(type)
        {
            case SWITCH:        // corresponds to html checkbox
                ret = new PluginSwitchParameter(formLabel,formName,comment);
                break;
            case STANDARD:
                ret = new PluginStandardParameter(formLabel,formName, comment);
                break;
            case LIST:          // corresponds to html pull down menu
                ret = new PluginListParameter(formLabel,formName,comment);
                break;
            case NUMBER:
                ret = new PluginNumberParameter(formLabel, formName, comment);
                break;
            case STRING:
                ret = new PluginStringParameter(formLabel, formName, comment);
                break;
            case NUMBERARRAY:
                ret = new PluginNumberArrayParameter(formLabel, formName, comment);
                break;
            default:
                throw new IllegalArgumentException("Unknown parameter type: " + type.name());
        }
        ret.setType(type);
        return ret;
    }

}
