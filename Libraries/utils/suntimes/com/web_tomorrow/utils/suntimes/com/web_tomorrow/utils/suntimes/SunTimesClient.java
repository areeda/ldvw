package com.web_tomorrow.utils.suntimes;
import com.web_tomorrow.utils.javapopt.Popt;
import java.util.*;
import java.io.*;

/**
SunTimesClient is a simple command-line client for the
SunTimes class. It allows the display of sunset and sunrise,
or twilight, at any location on any date. See the
overview for usage details.
<p>
(c)2001 Kevin Boone/Web-Tomorrow, all rights reserved
*/
public class SunTimesClient extends Exception
{
/**
main() implements a command-line test program for the SunTimes class.
*/
public static void main (String[] args)
  {
  final String APPNAME = "suntimes";
  final String VERSION = "1.0";

  // This command-line program uses JavaPopt to parse its arguments. See the
  // documentation for JavaPopt for more information
  Popt popt = new Popt (args); 
  
  // Tell it what arguments we accept Note that latitude and longitude are
  // mandatory, but in future versions of the program I may want to have these
  // determined by the city name, for example. So at present they are defined
  // as optional for Popt purposes, and checked after parsing the command line
  popt.addSwitchSpec ("offset", 'o', "offset", false, "offset from UTC in hours, later is +ve", 
      "offset", Double.class, false);
  popt.addSwitchSpec ("showzones", (char)0, "showzones", false, "show timezones", false);
  popt.addSwitchSpec ("twilight", 't', "twilight", false, "show twilight, not rise/set", false);
  popt.addSwitchSpec ("zone", 'z', "zone", false, "timezone ID", "zone", String.class, false);
  popt.addSwitchSpec ("utc", 'u', "utc", false, "display UTC (rather than local time)", false);
  popt.addSwitchSpec ("version", 'v', "version", false, "show version", false);
  popt.addSwitchSpec ("zenith", 'z', "zenith", false, "Sun's zenith, in degrees", "zenith", Double.class, false);
  popt.addNonSwitchSpec ("latitude", "latitude", false, "latitude in deg., N is +ve", Double.class);
  popt.addNonSwitchSpec ("longitude", "longitude", false, "longitude in deg., E is +ve", Double.class);
  popt.addNonSwitchSpec ("day", "day", false, "day of month (1-31)", Integer.class);
  popt.addNonSwitchSpec ("month", "month", false, "month (1-12)", Integer.class);
  popt.addNonSwitchSpec ("year", "year", false, "four-digit year", Integer.class);
  // Add help arguments for --help, -h, and -?
  popt.addHelp (true, true, true);

  // Parse the command line and process check for `help' and `verion' arguments
  if (popt.parse() == false)
    {
    System.err.println (popt.getErrorMessage());
    System.err.println ("Usage: suntimes " + popt.getShortUsage());
    System.err.println (popt.getUsage());
    }
  else if (popt.supplied("help"))
    {
    System.out.println ("Usage: suntimes " + popt.getShortUsage());
    System.out.println (popt.getUsage());
    }
  else if (popt.supplied("version"))
      {
      System.out.println (APPNAME + " version " + VERSION + " (c)2000 Kevin Boone/Web-Tomorrow");
      }
  else if (popt.supplied("showzones"))
    {
    String s[] = TimeZone.getAvailableIDs();
    for (int i = 0; i < s.length; i++)
      {
      TimeZone tz = TimeZone.getTimeZone (s[i]);
      System.out.println (s[i] + ", " + tz.getRawOffset() / 3600.0 / 1000.0 + " hr");
      }
    }
  else
    {
    if (popt.getLeftoverArgs().length != 0)
      System.err.println (APPNAME + " warning: additional arguments ignored");

    // We need to do a calculation. 
    Calendar now = Calendar.getInstance(); 
    int day = popt.getIntOrDefault ("day", now.get(now.DAY_OF_MONTH)); 
    // Note the ugly way we have to get the current month, because Java does
    //  not guarantee to number months from 1
    int monthOfYear = now.get(now.MONTH) - now.JANUARY + 1;
    int month = popt.getIntOrDefault ("month", monthOfYear); 
    int year = popt.getIntOrDefault ("year", now.get(now.YEAR)); 
    boolean utc = popt.supplied ("utc"); 

    boolean haveLat = false;
    boolean haveLongt = false;
    boolean utcOk = false; 

    double latitude = 0;
    double longitude = 0;
    double offset = 0;

    // Note explicith zenith overrides `twighlight', so check twighlight flag
    // first
    double zenith = SunTimes.ZENITH;
    if (popt.supplied("zenith"))
      zenith = popt.getDouble ("zenith");
    else if (popt.supplied("twilight"))
      zenith = SunTimes.CIVIL_ZENITH;

    if (popt.supplied("zenith") && popt.supplied("twilight"))
      System.err.println (APPNAME + " warning: --zenith overrides --twighlight");

    if (popt.supplied("zone"))
      {
      String zoneID = popt.getString("zone");
      // Note that this method never fails; if the
      //  zone is invalid, it silently returns GMT!
      TimeZone tz = TimeZone.getTimeZone (zoneID);
      // Note that getOffset requires a two-digit year!
      offset = tz.getOffset 
        (GregorianCalendar.AD, year - 1900, month, day, 1, 0) / (3600.0 * 1000.0);
      utcOk = true;
      }

    // values supplied on the command line over-ride the calculated figures
    if (popt.supplied("latitude"))
      {
      latitude = popt.getDouble ("latitude");
      haveLat = true;
      }

    if (popt.supplied("longitude"))
      {
      longitude = popt.getDouble ("longitude");
      haveLongt = true;
      }

    if (utc)
      utcOk = true;

    if (popt.supplied("offset"))
      {
      if (utc)
        System.err.println (APPNAME + " warning: over-riding `--utc' option with specified offset");
      offset = popt.getDouble ("offset");
      utcOk = true;
      }

    if (haveLat && haveLongt && utcOk)
      {
      try
        {
        Time sunriseUTC = SunTimes.getSunriseTimeUTC
          (year, month, day, longitude, latitude, zenith);
        Time sunsetUTC = SunTimes.getSunsetTimeUTC
          (year, month, day, longitude, latitude, zenith);
 
	Time sunrise = null;
	Time sunset = null;

        if (utc)
          {
	  sunrise = sunriseUTC;
	  sunset = sunsetUTC;
          }
        else
          {
	  sunrise = new Time (sunriseUTC.getFractionalHours() + offset);
	  sunset = new Time (sunsetUTC.getFractionalHours() + offset);
          }
        System.out.print (day + "/" + month + "/" + year + "  " + latitude + "," + longitude + " ");
        if (utc)
          System.out.println ("(UTC)"); 
        else
          System.out.println ("(UTC+ " + offset + " hr)"); 
        
        System.out.println ("sunrise: " + sunrise); 
        System.out.println ("sunset: " + sunset); 
        }
      catch (SunTimesException e)
        {
        System.err.println (APPNAME + ": " + e.getMessage());
        }
      }
    else
      {
      // At least one crucial piece of info missing: tell user
      if (!haveLat)
        {
        System.err.println (APPNAME + ": no latitude value provided");
        }
      if (!haveLongt)
        {
        System.err.println (APPNAME + ": no longitude value provided");
        }
      if (!utcOk)
        {
        System.err.println (APPNAME + ": specified options require a time offset from UTC, which");
        System.err.println ("   can be set using the --offset argument");
        }
      System.err.println ("Enter `suntimes --help' for more information");
      }
    }
  }
}

