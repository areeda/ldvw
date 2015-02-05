/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package viewerplugin;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

/**
 *
 * @author areeda
 */
public class ScaledAxisNumberFormat  extends NumberFormat
{
    private double scale=1.;
    private int exp=0;
    String fmt;
    
    public void setExp(int e)
    {
        exp = e;
        scale = Math.pow(10, exp);
    }
    public void setMinMax(double min, double max)
    {
        // figure out how many decimal places and whether to use f or e format
        int mine = (int) Math.abs(Math.round(Math.log10(Math.abs(min))));
        int maxe = (int)  Math.abs(Math.round(Math.log10(Math.abs(max))));
        int mag = Math.max(mine, maxe);
        if (mag > 4)
        {
            fmt = "%1$.3e";
        }
        else if (mag > 2)
        {
            fmt = "%1$.2e";
        }
        else 
        {
            fmt = "%1$.3f";
        }
    }

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos)
    {
        String out;
        double it = number / scale;
        if (fmt == null)
        {   
            // @todo legacy stuff we need to get rid of
            if (Math.abs(exp) > 4)
            {
                out = String.format("%1$.4e", it);
            }
            else if (Math.abs(exp) > 2)
            {
                out = String.format("%1$.5f", it);
            }
            else if (Math.abs(exp) > 1)
            {
                out  = String.format("%1$.4f", it);
            }
            else if (Math.abs(exp) >= 0)
            {
                out = String.format("%1$.5f", it);
            }
            else
            {
                out  = String.format("%1$.4e", it);
            }
        }
        else
        {
            if (it == 0)
            {
                out = "0";
            }
            else
            {
                out = String.format(fmt, it);
            }
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
        Double num = Double.parseDouble(source.substring(parsePosition.getIndex()));
        return num;
    }
    
}
