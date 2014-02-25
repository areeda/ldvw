package com.web_tomorrow.suntimes_web;
import com.web_tomorrow.utils.suntimes.*;
import com.web_tomorrow.utils.datetime.*;
import java.util.*; 

public class SunTimesBean
{
protected String city;
protected boolean posLat;
protected int dLat;
protected int mLat;
protected boolean posLong;
protected int dLong;
protected int mLong;
protected int day;
protected int month;
protected int year;
protected double utcOffset;
protected String errorMessage;
protected boolean hasResultsFlag;
protected String astronomicalSunrise;
protected String civilSunrise;
protected String nauticalSunrise;
protected String trueSunrise;
protected String astronomicalSunset;
protected String civilSunset;
protected String nauticalSunset;
protected String trueSunset;

public SunTimesBean()
  {
  city = getSelectBelowText();
  posLat = true;
  dLat = 0;
  mLat = 0;
  posLong = true;
  dLong = 0;
  mLong = 0;
  Calendar c = Calendar.getInstance();
  day = c.get(c.DATE);
  month = c.get(c.MONTH) - c.JANUARY + 1;
  year = c.get(c.YEAR); 
  utcOffset = 0;
  errorMessage = null;
  hasResultsFlag = false;
  }

// Returns an array of zone ids for which we have both timezone
//  and geogrphical information. This will be used by JSP pages to present
//  a selectiob box with city data in
public static String[] getUseableCityIds()
  {
  Vector v = new Vector();

  String[] zones = TimeZone.getAvailableIDs();
  Arrays.sort (zones);
  for (int i = 0; i < zones.length; i++)
    {
    LatLongDataElement llde = LatLong.getLatLongDataElement (zones[i]);
    if (llde != null)
      v.add (zones[i]);
    }

  String[] a = new String[v.size()];
  for (int i = 0; i < v.size(); i++)
    a[i] = (String)v.elementAt(i);
  return a;
  }

public String getCity() { return city; }
public void setCity(String _city) { city = _city; }

public String getCityOrDefault()
  {
  if (city != null && city.length() > 0) return city;
  return getSelectBelowText();
  }

public String getSelectBelowText()
  {
  return "(Enter values below)";
  }

public boolean getPosLong () { return posLong; }
public boolean getPosLat () { return posLat; }
public int getDlat () { return dLat; }
public int getMlat () { return mLat; }
public int getDlong () { return dLong; }
public int getMlong () { return mLong; }
public void setPosLong(boolean f) { posLong = f; }
public void setPosLat(boolean f) { posLat = f; }
public void setDlat(int f) { dLat = f; }
public void setMlat(int f) { mLat = f; }
public void setDlong(int f) { dLong = f; }
public void setMlong(int f) { mLong = f; }
public void setDay(int f) { day = f; }
public void setMonth(int f) { month = f; }
public void setYear(int f) { year = f; }
public int getDay() { return day; }
public int getMonth() { return month; }
public int getYear() { return year; }
public double getUtcOffset() { return utcOffset; }
public void setUtcOffset (double f) { utcOffset = f; }
public String getErrorMessage () { return errorMessage; }
public boolean hasResults() { return hasResultsFlag; }

/**
Check the params and calculate. If the paramters are not useful, or
the calculation fails, set the errorMessage variable. Otherwise,
set hasResults
*/
public void validateAndCalculate()
  {
  hasResultsFlag = false;
  errorMessage = null;

  if (city != null && city.length() > 0 && !city.equalsIgnoreCase(getSelectBelowText()))
    {
    LatLongDataElement llde = LatLong.getLatLongDataElement (city);
    setDlat(llde.getDLat());
    setMlat(llde.getMLat());
    setDlong(llde.getDLong());
    setMlong(llde.getMLong());
    setPosLong(llde.getPosLong());
    setPosLat(llde.getPosLat());
    TimeZone tz = TimeZone.getTimeZone(city);
    long offsetMillis = tz.getOffset(GregorianCalendar.AD, year, month - 1, day, 1, 0);
    setUtcOffset(offsetMillis/(3600*1000));
    }
   
  try
    {
    double longitude = dLong + (double)mLong / 60.0;
    if (!posLong) longitude = -longitude;
    double latitude = dLat + (double)mLat / 60.0;
    if (!posLat) latitude = -latitude;

    try 
      { 
      Time utc_astronomicalSunrise = SunTimes.getSunriseTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.ASTRONOMICAL_ZENITH); 
      Time t_astronomicalSunrise = new Time(utc_astronomicalSunrise.getFractionalHours() + utcOffset); 
      astronomicalSunrise = t_astronomicalSunrise.toString(); 
      }
    catch (Exception e) { astronomicalSunrise = e.getMessage(); }
    
    try 
      { 
      Time utc_nauticalSunrise = SunTimes.getSunriseTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.NAUTICAL_ZENITH);
      Time t_nauticalSunrise = new Time(utc_nauticalSunrise.getFractionalHours() + utcOffset); 
      nauticalSunrise = t_nauticalSunrise.toString(); 
      }
    catch (Exception e) { nauticalSunrise = e.getMessage(); }

    try 
      { 
      Time utc_civilSunrise = SunTimes.getSunriseTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.CIVIL_ZENITH);
      Time t_civilSunrise = new Time(utc_civilSunrise.getFractionalHours() + utcOffset); 
      civilSunrise = t_civilSunrise.toString(); 
      }
    catch (Exception e) { civilSunrise = e.getMessage(); }

    try 
      { 
      Time utc_trueSunrise = SunTimes.getSunriseTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.ZENITH);
      Time t_trueSunrise = new Time(utc_trueSunrise.getFractionalHours() + utcOffset); 
      trueSunrise = t_trueSunrise.toString(); 
      }
    catch (Exception e) { trueSunrise = e.getMessage(); }

    try 
      { 
      Time utc_astronomicalSunset = SunTimes.getSunsetTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.ASTRONOMICAL_ZENITH);
      Time t_astronomicalSunset = new Time(utc_astronomicalSunset.getFractionalHours() + utcOffset); 
      astronomicalSunset = t_astronomicalSunset.toString(); 
      }
    catch (Exception e) { astronomicalSunset = e.getMessage(); }

    try 
      { 
      Time utc_nauticalSunset = SunTimes.getSunsetTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.NAUTICAL_ZENITH);
      Time t_nauticalSunset = new Time(utc_nauticalSunset.getFractionalHours() + utcOffset); 
      nauticalSunset = t_nauticalSunset.toString(); 
      }
    catch (Exception e) { nauticalSunset = e.getMessage(); }

    try 
      { 
      Time utc_civilSunset = SunTimes.getSunsetTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.CIVIL_ZENITH);
      Time t_civilSunset = new Time(utc_civilSunset.getFractionalHours() + utcOffset); 
      civilSunset = t_civilSunset.toString(); 
      }
    catch (Exception e) { civilSunset = e.getMessage(); }

    try 
      { 
      Time utc_trueSunset = SunTimes.getSunsetTimeUTC (year, month, day, 
        longitude, latitude, SunTimes.ZENITH);
      Time t_trueSunset = new Time(utc_trueSunset.getFractionalHours() + utcOffset); 
      trueSunset = t_trueSunset.toString(); 
      }
    catch (Exception e) { trueSunset = e.getMessage(); }

    hasResultsFlag = true;
    }
  catch (Exception e)
    {
    errorMessage = e.getMessage(); 
    }
  }

public void setErrorMessage(String s)
  {
  errorMessage = s;
  }

public String getAstronomicalSunrise() { return astronomicalSunrise; }
public String getNauticalSunrise() { return nauticalSunrise; }
public String getCivilSunrise() { return civilSunrise; }
public String getTrueSunrise() { return trueSunrise; }
public String getAstronomicalSunset() { return astronomicalSunset; }
public String getNauticalSunset() { return nauticalSunset; }
public String getCivilSunset() { return civilSunset; }
public String getTrueSunset() { return trueSunset; }

}




