/*
 * Copyright (C) 2012 Joseph Areeda <joseph.areeda at ligo.org>
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
package edu.fullerton.viewerplugin;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 *
 * @author Joseph Areeda <joseph.areeda at ligo.org>
 */
public class LogAxisNumberFormat extends NumberFormat
{
    private double scale = 1.;
    private int sexp = 0;

    public void setExp(int e)
    {
        sexp = e;
        scale = Math.pow(10, -sexp);
    }
    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
    {
        String out;
        double x = number * scale;
        int exp = (int) (Math.ceil(Math.log10(x)));
        if (exp > 4)
        {
            out = String.format("1e%1$d", exp);
        }
        else if (exp >=0)
        {
            out = String.format("%1$.3f", x);
        }
        else if (exp >= -4)
        {
            out = String.format("%1$.4f", x);
        }
        else
        {
            out = String.format("1e%1$d", exp);
        }
        return toAppendTo.append(out);
    }

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos)
    {
        
        return format((double)number, toAppendTo, pos);
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition)
    {
        Double num =  Double.parseDouble(source.substring(parsePosition.getIndex()));
        return num;
    }
    
}
