/*
 * Copyright (C) 2013 Joseph Areeda <joseph.areeda at ligo.org>
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
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class PageItemDropDownToggle extends PageItem
{
    private final String text;

    public PageItemDropDownToggle(String label)
    {
        text = label;
    }
    
    @Override
    public String getHtml() throws WebUtilException
    {
        String ret = String.format("%n     <a href=\"#\" class=\"dropdown-toggle\" "
                + "data-toggle=\"dropdown\">%1$s <b class=\"caret\"></b></a>%n", text);
        return ret;
    }
    
}
