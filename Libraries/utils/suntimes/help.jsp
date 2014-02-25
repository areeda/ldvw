<%@ page import="java.util.*,com.web_tomorrow.suntimes_web.*" errorPage="suntimeserror.jsp" %>

<%
boolean isErrorPage = false;
%>

<%@ include file="_header.jsp" %>

<table width=500><tr align=left width=500><td>

<h2>Overview</h2>

SunTimes calculates sunset and sunrise times at a specific location on a specific
day. It contains a fairly large database of geographical and timezone information for
various cities, or you can specify location and timezone information manually. 

<h2>Types of sunrise and sunset</h2>

There is no single, exact figure for sunrise or sunset. SunTimes provides four 
different values for each, as described below.
<p>
`True' sunrise and sunset conventionally refer to the times when the upper edge of the disk 
of the Sun appears on the horizon at the location of interest. 
Atmospheric conditions are assumed to be average, and the location is level. 
<p>
Even when the Sun is slightly below the horizon, a small amout of light is reflected from dust
particles in the atmosphere, and it is not quite dark; the period between true
sunset and it becoming too dark for practical purposes is called `twilight'. The duration of twilight
is more difficult to define than the times of sunrise and sunset, because light fades gradually
as the sun sets, and it is not always possible to be sure what `practical purposes' are.
The time period between true sunset and it becoming impractical to indulge in `ordinary'
outdoor activities (such as walking) is referred to as `civil twighlight'. `Civil sunset'
is the end of this period of twilight in the evening; `civil sunrise' is the beginning in
the morning. Because the amount of light available depends on the moon, atmospheric
conditions, and a number of other factors, we usually base the determination of 
civial sunset and sunrise on the times at which the Sun makes an angle of 96 degrees
to the horizon. This typically occurs 30-60 minutes after true sunset, or before true
sunrise. Note that most civilian sunrise and sunset tables list civil sunrise and sunset,
not true sunrise and sunset.
<p> 
`Nautical sunset' is the time in the evening at which it becomes impossible to distinguish
the horizon at sea. This typically occurs 1-2 hours after true sunset.
<p>
`Astronomical sunset' is the time at which it gets truly dark, and the faintest stars
can be seen. In many locations there is no astronomical sunset for most of the year.
<p>
Clearly the rise and set times will depend on the date.  In theory there are
also year-to-year variations in sunset/rise times, but they are incredibly
tiny. SunTimes requires a year to be specified only to
account for whether it is a leap year or not. For example, March 1st is one day
later on a leap year, compared to non-leap years. In practice this is unlikely
to affect the result by more than a few seconds. 

<h2>Entering data</h2>

<h3>Location data: using the list</h3>

Select a city from the drop-down list. If your location is not listed, there may be
a city nearby that will give similar results. If not, enter the values for
latitude, longitude and UTC offset manually, as described below. To tell SunTimes
to use the manaully specified values, ensure that the city list is showing `Enter values below'.
Please note that while the latitude and longitude of cities is unlikely to chance, countries
can and do change their time zone information, especially the start and end of daylight
savings time. Please use the manual entry if you have reason to think that these paramters
are volatile at your location.

<h3>Location data: using specific values</h3>

The <b>latitude</b> is specified in degrees and minutes; please don't enter fractional
degrees. Click the `north' or `south' box to indicate whether your entries are to be interpreted 
as north or south of the equator.
<p>
The <b>longitude</b> is specified in degrees and minutes; please don't enter fractional
degrees. Click the `east' or `west' box to indicate whether your entries are to be interpreted 
as east or west of the Greenwich meridian. 

<h3>Calculation date</h3>

This specifies the date on which the sunrise and sunset figures are required.
The date is specified as follows.

<ul>
<li> day: 1-31
<li> month: 1-12
<li> year: four-digit year. Note that this value is only used for determining
whether it is a leap year, and has little effect on the results.
</ul>


<h2>Disclaimer and legal info</h2>

The algorithm implemented by SunTimes is based on that described in the US Naval Observatory's
<i>Almanac for Computers</i>, (1990; Nautical Almanac Office, United States Naval
Observatory, Washington, DC 20392), and has been tested extensively against
the tables provided on this organization's Web site 
(<a href=http://aa.usno.navy.mil>aa.usno.navy.mil</a>).
This software has been made available in the hope that it will be useful.
There is no warranty, and the author accepts no responsibility for any
adverse consequences of its use. Please don't use it for making life-and-death
decisions: use the `official' tables from the
US Naval Observatory instead. 

</td></tr></table>

<%@ include file="_mainform.jsp"  %>

<%@ include file="_footer.jsp" %>


