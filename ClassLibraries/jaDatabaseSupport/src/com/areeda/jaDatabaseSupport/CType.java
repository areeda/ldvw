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

package com.areeda.jaDatabaseSupport;

import com.areeda.jaDatabaseSupport.Database.DbType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Column Types and rich enum with code to handle each data type
 * input/output conversions, creating sql definitions ...
 * 
 * @author joe areeda
 */
public enum CType
{

    /**
     * Default column type is to just return the string
     */
    STRING
    {

        @Override
        public String parse(String in)
        {
            String ret = in == null ? "" : in;
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret;
            switch(dbt)
            {
                case MYSQL:
                    ret = "TEXT";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
                default:
                    ret = "CHAR";
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            String ret = dbType();
            switch (dbt)
            {
                case MYSQL:
                    if (len > 0 && len < 256)
                    {
                        ret = "CHAR(" + len + ")";
                    }
                    else if (len < 65536 || len == 0)
                    {
                        ret = "TEXT";
                    }
                    else
                    {
                        ret = "LONGTEXT";
                    }
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            return Utils.sqlQuote((String) v);
        }
    },
    BLOB
    {
       @Override
        public Object parse(String in)
        {
            String ret = null;
            return ret;
        }

        @Override
        public String dbType()
        {
            return "BLOB";
        } 
  
        @Override
        public String dbType(int len)
        {
            String ret = dbType();
            switch (dbt)
            {
                case MYSQL:
                    if (len > 0 && len < 256)
                    {
                        ret = "BINARY(" + len + ")";
                    }
                    else if (len < 65536 || len == 0)
                    {
                        ret = "BLOB";
                    }
                    else
                    {
                        ret = "LONGBLOB";
                    }
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            return Utils.sqlQuote((String) v);
        }
      
    },
    IGNORE
    {

        @Override
        public Object parse(String in)
        {
            String ret = in == null ? "" : in;
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "CHAR";
            switch (dbt)
            {
                case MYSQL:
                    ret = "TEXT";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            String ret = dbType();
            switch (dbt)
            {
                case MYSQL:
                    if (len > 0 && len < 256)
                    {
                        ret = "CHAR(" + len + ")";
                    }
                    else if (len < 65536)
                    {
                        ret = "TEXT";
                    }
                    else
                    {
                        ret = "LONGTEXT";
                    }
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }

            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            return Utils.sqlQuote((String) v);
        }
    },
    /**
     *  a true false value store as a char(1) in mySql and text in sqlite
     */
    BOOLEAN
    {

        @Override
        public Boolean parse(String in)
        {
            String t = in.trim();
            Boolean ret = null;
            if (t.startsWith("1"))
            {
                ret = true;
            }
            else if (t.startsWith("0"))
            {
                ret = false;
            }

            return ret;

        }

        @Override
        public String dbType()
        {
            String ret = "CHAR(1)";
            switch (dbt)
            {
                case MYSQL:
                    ret = "CHAR(1)";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int l)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = null;
            if (v instanceof Boolean)
            {
                ret = (Boolean) v ? "1" : "0";
            }
            return ret;
        }
    },
    CHAR
    {

        @Override
        public String parse(String in)
        {
            String ret = in == null ? "" : in;
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "CHAR";
            switch (dbt)
            {
                case MYSQL:
                    ret = "CHAR";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            String ret = dbType();

            if (len > 0 && dbt != Database.DbType.SQLITE)
            {
                ret = "CHAR(" + len + ")";
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            return Utils.sqlQuote((String) v);
        }
    },
    /**
     * Dates come in various formats so far I've found
     * mm/dd/yy
     * mmddyyyy
     * mm/dd/yy HH:MM A/PM
     *
     */
    DATE
    {

        @Override
        public Calendar parse(String in)
        {
            GregorianCalendar ret = null;
            Integer m = 0, d = 0, y = 0;
            if (in != null && in.length() > 7)
            {
                Pattern dslash = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2,4})");

                if (Pattern.matches("\\d+", in))
                {
                    String dt = in.trim();
                    if (dt.length() == 8)
                    {
                        m = Integer.parseInt(dt.substring(0, 2));
                        d = Integer.parseInt(dt.substring(2, 4));
                        y = Integer.parseInt(dt.substring(4));
                    }
                }
                else
                {
                    Matcher mtch = dslash.matcher(in);
                    if (mtch.find())
                    {
                        String ms = mtch.group(1), ds = mtch.group(2), ys = mtch.group(3);
                        m = Integer.parseInt(ms);
                        d = Integer.parseInt(ds);
                        y = Integer.parseInt(ys);
                    }
                }
                if (y < 22)
                {
                    y += 2000;
                }
                else
                {
                    if (y < 200)       // I don't expect to see anything greater than 99 but some weirdos handled y2k with 101
                    {
                        y += 1900;
                    }
                }
                if (y < 1800 || y > 2100 || m < 1 || m > 12)
                {
                    m = y = d = 0;
                }
                else
                {
                    int[] days =
                    {
                        1, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
                    };
                    GregorianCalendar it = new GregorianCalendar(y, 1, 1);
                    if (it.isLeapYear(y))
                    {
                        days[2] = 29;
                    }
                    if (d < 0)
                    {
                        d = 1;
                    }
                    else
                    {
                        if (d > days[m])
                        {
                            d = days[m];
                        }
                    }

                    GregorianCalendar c = new GregorianCalendar(y, m - 1, d);
                    ret = c;
                }
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret="DATE";
            switch (dbt)
            {
                case MYSQL:
                    ret = "DATE";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = "0000-00-00";
            if (v != null)
            {
                GregorianCalendar it = (GregorianCalendar) v;
                Integer y = it.get(Calendar.YEAR), m = it.get(Calendar.MONTH) + 1, d = it.get(Calendar.DAY_OF_MONTH);

                ret = String.format("%1$04d-%2$02d-%3$02d", y, m, d);
            }
            ret = Utils.sqlQuote(ret);

            return ret;
        }
    },
    /**
     * DATETIME has various input formats, long or double (unix timestamp), various strings
     */
    TIMESTAMP
    {

        @Override
        public Calendar parse(String val)
        {
            GregorianCalendar ret;
            Long timeMs = 0L;

            if (val.matches("^\\d+$"))
            {
                timeMs = Long.parseLong(val) * 1000;
            }
            else if (val.matches("^[\\d\\.]+"))
            {
                timeMs = Math.round(Double.parseDouble(val) * 1000.);
            }
            ret = new GregorianCalendar();
            ret.setTimeInMillis(timeMs);

            return ret;
        }

        /**
         *
         * @return The SQL data type for this format
         */
        @Override
        public String dbType()
        {
            String ret = "TIMESTAMP";
            switch (dbt)
            {
                case MYSQL:
                    ret = "TIMESTAMP";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();   // length is ignored
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = "0000-00-00 0:00:00";
            if (v != null)
            {
                GregorianCalendar it = (GregorianCalendar) v;
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Long t = it.getTimeInMillis();
                if (t > 0)
                {
                    ret = sdf.format(it.getTime());
                }
            }
            ret = Utils.sqlQuote(ret);

            return ret;

        }
    },
    /**
     * Plain old integer field
     */
    INTEGER
    {

        @Override
        public Integer parse(String in)
        {
            Integer ret = null;

            in = in.trim();

            if (in.matches("\\d+"))
            {
                ret = Integer.parseInt(in);
            }
            else if (in.matches("[\\d\\.]+"))
            {
                Double t = Double.parseDouble(in);
                Long lt = Math.round(t);
                ret = lt.intValue();
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            return dbType(4);
        }

        /**
         * @param int len - length in bytes (2=small, 4=reg, >4 big
         */
        @Override
        public String dbType(int len)
        {
            String ret = "";
            switch (dbt)
            {
                case MYSQL:
                    if (len <= 1)
                    {
                        ret = "TINYINT";
                    }
                    else if (len <= 2)
                    {
                        ret = "SMALLINT";
                    }
                    else if (len <= 4)
                    {
                        ret = "INTEGER";
                    }
                    else
                    {
                        ret = "BIGINT";
                    }
                    break;
                case SQLITE:
                    ret = "INTEGER";
                    break;
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = "NULL";
            if (v != null)
            {
                ret = v.toString();
            }
            return ret;
        }
    },
    LONG
    {

        @Override
        public Long parse(String in)
        {
            Long ret = null;

            in = in.trim();

            if (in.matches("\\d+"))
            {
                ret = Long.parseLong(in);
            }
            else if (in.matches("[\\d\\.]+"))
            {
                Double t = Double.parseDouble(in);
                ret = Math.round(t);
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "";
            switch (dbt)
            {
                case MYSQL:
                    ret = "BIGINT";
                    break;
                case SQLITE:
                    ret = "INTEGER";
                    break;
            }
            return ret;
        }

        /**
         * @param int len - length in bytes (2=small, 4=reg, >4 big
         */
        @Override
        public String dbType(int len)
        {
            return dbType();    // ignore length
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = "NULL";
            if (v != null)
            {
                ret = v.toString();
            }
            return ret;
        }
    },
    /**
     *
     */
    FLOAT
    {
        @Override
        public Double parse(String val)
        {
            return Double.parseDouble(val);
        }

        @Override
        public String dbType()
        {
            String ret = "";
            switch (dbt)
            {
                case MYSQL:
                    ret = "FLOAT";
                    break;
                case SQLITE:
                    ret = "REAL";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            String ret = "";
            switch (dbt)
            {
                case MYSQL:
                    ret = "FLOAT";
                    if (len >= 8)
                    {
                        ret = "DOUBLE PRECISION";
                    }
                    break;
                case SQLITE:
                    ret = "REAL";
                    break;
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            return v.toString();
        }
    },
    /**
     * Several formats for phone numbers:
     * 907-269-0751
     * (626) 300-4602
     * 1-800-478-7250
     * 1-800-wx-brief
     */
    PHONE
    {

        @Override
        public String parse(String in)
        {
            String ret = null;

            in = in.trim();

            Pattern p1 = Pattern.compile("(1-)?(\\d{3})-(\\d{3})-(\\d{4})");

            Matcher m1 = p1.matcher(in);
            if (m1.find() && m1.groupCount() == 4)
            {
                String ac = m1.group(2);
                String ex = m1.group(3);
                String nm = m1.group(4);

                ret = ac + "-" + ex + "-" + nm;
            }
            else
            {
                if (in.contains("1-800-WX-BRIEF"))
                {
                    ret = "1-800-WX-BRIEF";
                }
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "CHAR(16)";
            switch (dbt)
            {
                case MYSQL:
                    ret = "CHAR(16)";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            return Utils.sqlQuote((String) v);
        }
    },
    /**
     * Radio Frequency
     */
    FREQ
    {

        @Override
        public String parse(String in)
        {
            String ret = null;
            in = in.trim();
            try
            {
                Double f = Double.parseDouble(in);
                if (((int) (f * 1000. + 0.5)) % 10 == 0)
                {
                    ret = String.format("%1$.2f", f);
                }
                else
                {
                    ret = String.format("%1$.3f", f);
                }
            }
            catch (Throwable e)
            {
                // just leave ret as null
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "CHAR(8)";
            switch (dbt)
            {
                case MYSQL:
                    ret = "CHAR(8)";
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;

        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            return (String) v;
        }
    },
    /**
     * Lattitude or Longitude in h-m-s.xxx(N,S,E,W)
     * South and West are negative by convention
     */
    LATLON
    {

        @Override
        public Double parse(String in)
        {
            Double ret = null;
            Pattern p1 = Pattern.compile("(\\d+)-(\\d+)-([\\d\\.]+)([NSEWnsew])");
            Matcher m1 = p1.matcher(in);

            if (m1.find())
            {
                if (m1.groupCount() == 4)
                {
                    Integer d = Integer.parseInt(m1.group(1));
                    Integer m = Integer.parseInt(m1.group(2));
                    Double s = Double.parseDouble(m1.group(3));
                    String h = m1.group(4);

                    s += ((d * 60.) + m) * 60.;
                    if (h.compareToIgnoreCase("s") == 0 || h.compareToIgnoreCase("w") == 0)
                    {
                        s = -s;
                    }
                    ret = s;
                }
            }
            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "DOUBLE";
            switch (dbt)
            {
                case MYSQL:
                    ret = "DOUBLE PRECISION";
                    break;
                case SQLITE:
                    ret = "REAL";
                    break;
            }

            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = String.format("%1$.5f", (Double) v);
            return ret;
        }
    },
    /**
     * Latitude or Longitude in seconds xxxx.xxxx(N,S,E,W)
     */
    LATLONSEC
    {

        @Override
        public Double parse(String in)
        {
            Double ret = null;
            Pattern p1 = Pattern.compile("([\\d\\.]+)([NSEWnsew])");
            Matcher m1 = p1.matcher(in);
            if (m1.find())
            {
                Double s = Double.parseDouble(m1.group(1));
                String h = m1.group(2);
                if (h.compareToIgnoreCase("s") == 0 || h.compareToIgnoreCase("w") == 0)
                {
                    s = -s;
                }
                ret = s;
            }

            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "DOUBLE";
            switch (dbt)
            {
                case MYSQL:
                    ret = "DOUBLE PRECISION";
                    break;
                case SQLITE:
                    ret = "REAL";
                    break;
            }

            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = String.format("%1$.5f", (Double) v);
            return ret;
        }
    },
    /**
     * Latitude or Longitude in seconds DDMMSST(N,S,E,W)
     */
    LATLONDEC
    {

        @Override
        public Double parse(String in)
        {
            Double ret = null;
            Pattern p1 = Pattern.compile("([\\d]+)([NSEWnsew])");
            Matcher m1 = p1.matcher(in);
            if (m1.find())
            {
                Long l = Long.parseLong(m1.group(1));
                Double ss = (l % 1000) / 10.;
                Long mm = (l / 1000) % 100;
                Long dd = (l / 100000) % 100;
                Double s = ss + mm / 60. + dd / 3600.;
                String h = m1.group(2);
                if (h.compareToIgnoreCase("s") == 0 || h.compareToIgnoreCase("w") == 0)
                {
                    s = -s;
                }
                ret = s;
            }

            return ret;
        }

        @Override
        public String dbType()
        {
            String ret = "DOUBLE";
            switch (dbt)
            {
                case MYSQL:
                    ret = "DOUBLE PRECISION";
                    break;
                case SQLITE:
                    ret = "REAL";
                    break;
            }
            return ret;
        }

        @Override
        public String dbType(int len)
        {
            return dbType();
        }

        @Override
        public String dbVal(Object v)
        {
            String ret = String.format("%1$.5f", (Double) v);
            return ret;
        }
    },

    /**
     * Integer array output as a CSV string
     */
    INTARRAY
    {
        @Override
        public Integer[] parse(String val)
        {
            ArrayList<Integer> out = new ArrayList<Integer>();
            StringTokenizer st = new StringTokenizer(val,",");
            while(st.hasMoreTokens())
            {
                String it = st.nextToken();
                Integer i = 0;
                try
                {
                    i=Integer.parseInt(it);
                }
                catch(Exception e)
                {
                    // ignore we'll pass back zero
                }
                out.add(i);
            }
            Integer[] ret = new Integer[out.size()];
            ret = out.toArray(ret);
            return ret;
        }

        @Override
        public String dbType()
        {
            return "text";
        }

        @Override
        public String dbType(int len)
        {
            String ret = "LONGTEXT";
            switch (dbt)
            {
                case MYSQL:
                    if (len < 256)
                    {
                        ret = "TINYTEXT";
                    }
                    else if (len < 65536)
                    {
                        ret = "TEXT";
                    }
                    else if (len < 16777216)
                    {
                        ret = "MEDIUMTEXT";
                    }
                    break;
                case SQLITE:
                    ret = "TEXT";
                    break;
            }
            return ret;
        }

        @Override
        public String dbVal(Object v)
        {
            StringBuilder ret = new StringBuilder();
            Integer[] in = (Integer[]) v;
            for(Integer i : in)
            {
                if (ret.length() > 0)
                {
                    ret.append(",");
                }
                ret.append(i.toString());
            }
            String ret2 = Utils.sqlQuote(ret.toString());
            return ret2;
        }

    }

    ;   // END OF THE ENUM
    private static Database.DbType dbt = Database.DbType.MYSQL;     // database type

    /**
     * Parse the input string as appropriate for this format
     * @param val - input string
     * @return an Object of the appropriate type for this format
     */
    abstract public Object parse(String val);

    /**
     *
     * @return The SQL data type for this format
     */
    abstract public String dbType();

    abstract public String dbType(int len);

    abstract public String dbVal(Object v);

    static public CType fromString(String typ) throws InstantiationException
    {
        HashMap<String, CType> names = new HashMap<String, CType>();
        names.put("STRING", STRING);
        names.put("BOOLEAN", BOOLEAN);
        names.put("CHAR", CHAR);
        names.put("DATE", DATE);
        names.put("INTEGER", INTEGER);
        names.put("LONG", LONG);
        names.put("FLOAT", FLOAT);
        names.put("PHONE", PHONE);
        names.put("FREQ", FREQ);
        names.put("LATLON", LATLON);
        names.put("LATLONDEC", LATLONDEC);
        names.put("LATLONSEC", LATLONSEC);
        names.put("IGNORE", IGNORE);
        names.put("TIMESTAMP", TIMESTAMP);

        if (!names.containsKey(typ))
        {
            throw (new InstantiationException("Unkown CType: " + typ));
        }
        return names.get(typ);
    }

    static void setDbt(DbType dbt)
    {
        CType.dbt = dbt;
    }

}
