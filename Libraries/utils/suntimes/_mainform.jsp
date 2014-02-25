<form action="index.jsp" method=get>

<%
String selectedCity = sunTimesBean.getCityOrDefault();
String northChecked = "";
String southChecked = "";
String eastChecked = "";
String westChecked = "";
if (sunTimesBean.getPosLat())
  northChecked = "checked";
else
  southChecked = "checked";
if (sunTimesBean.getPosLong())
  eastChecked = "checked";
else
  westChecked = "checked";

if (request.getParameter("submit") != null && !isErrorPage)
  sunTimesBean.validateAndCalculate();
%>

<%
if (sunTimesBean.getErrorMessage() != null)
  {
%>
  <table cellpadding=10 cellspacing=0 align=center border=0 align=center width=70%>
    <tr align=center bgcolor=red>
      <td>
	<%=sunTimesBean.getErrorMessage()%> 
      </td>
    </tr>
    <tr align=center>
      <td height=20>
        &nbsp;
      </td>
    </tr>
  </table>
<%
  }
else if (sunTimesBean.hasResults())
  {
%>
  <table cellpadding=10 cellspacing=0 align=center border=0 align=center width=70%>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Astronimcal sunrise
      </td>
      <td height=20>
         <%=sunTimesBean.getAstronomicalSunrise()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Nautical sunrise
      </td>
      <td height=20>
         <%=sunTimesBean.getNauticalSunrise()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Civil sunrise
      </td>
      <td height=20>
         <%=sunTimesBean.getCivilSunrise()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         True sunrise
      </td>
      <td height=20>
         <%=sunTimesBean.getTrueSunrise()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
        &nbsp;
      </td>
      <td height=20>
        &nbsp;
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         True sunset
      </td>
      <td height=20>
         <%=sunTimesBean.getTrueSunset()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Civil sunset
      </td>
      <td height=20>
         <%=sunTimesBean.getCivilSunset()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Nautical sunset
      </td>
      <td height=20>
         <%=sunTimesBean.getNauticalSunset()%>
      </td>
    </tr>

    <tr align=center bgcolor=#C0F0C0>
      <td>
         Astronimcal sunset
      </td>
      <td height=20>
         <%=sunTimesBean.getAstronomicalSunset()%>
      </td>
    </tr>

    <tr align=center>
      <td>
        &nbsp;
      </td>
      <td height=20>
        &nbsp;
      </td>
    </tr>

  </table>
<%
  }
%>

<table cellpadding=10 cellspacing=0 align=center border=0 width=70%>
  <tr bgcolor=#C0C0F0>
    <td>
      City
    </td>
    <td>
      <select name=city>
	<option><%=sunTimesBean.getSelectBelowText()%></option>
      <%
      String[] cities = sunTimesBean.getUseableCityIds();
      for (int i = 0; i < cities.length; i++)
	{
	if (cities[i].equalsIgnoreCase (selectedCity))
	  {
      %>
	  <option selected>
      <%
	  }
	else
	  {
      %>
	  <option>
      <% 
	  }
      %>
	<%= cities[i] %>
	</option>
      <%
	}
      %>
      </select>
    </td>
  </tr>

  <tr>
    <td height=10>
      &nbsp;
    </td>
    <td>
      Enter <i>either</i> a city from the list above, or fill in the latitude,
      longitude and time difference below
    </td>
  </tr>

  <tr bgcolor=#C0C0F0>
    <td>
      Latitude
    </td>
    <td>
      <input name=dlat value="<%=sunTimesBean.getDlat()%>" size=3> degrees, 
      <input name=mlat value="<%=sunTimesBean.getMlat()%>" size=3> minutes
      <input name=posLat type=radio <%=northChecked%> value="true"> north 
      <input name=posLat type=radio <%=southChecked%> value="false"> south 
    </td>
  </tr>

  <tr bgcolor=#C0C0F0>
    <td >
      Longitude
    </td>
    <td>
      <input name=dlong value="<%=sunTimesBean.getDlong()%>" size=3> degrees, 
      <input name=mlong value="<%=sunTimesBean.getMlong()%>" size=3> minutes
      <input name=posLong type=radio <%=eastChecked%> value="true"> east 
      <input name=posLong type=radio <%=westChecked%> value="false"> west 
    </td>
  </tr>

  <tr bgcolor=#C0C0F0>
    <td>
      Time difference from UTC 
    </td>
    <td>
      <input name=utcOffset value="<%=sunTimesBean.getUtcOffset()%>" size=4> <br>
      Positive time differences are <i>later</i> than UTC (GMT), negative are <i>earlier</i>
    </td>
  </tr>

  <tr>
    <td height=10>
      &nbsp;
    </td>
    <td>
      Enter the date for which the calculation is required
    </td>
  </tr>

  <tr bgcolor=#D0D0F0>
    <td>
      Date
    </td>
    <td>
      day <input name=day value="<%=sunTimesBean.getDay()%>" size=2> &nbsp;&nbsp;&nbsp; 
      month <input name=month value="<%=sunTimesBean.getMonth()%>" size=2> &nbsp;&nbsp;&nbsp;
      year (4-digit) <input name=year value="<%=sunTimesBean.getYear()%>" size=4> 
    </td>
  </tr>

</table>

<table cellpadding=0 cellspacing=10 align=center>
  <tr>
    <td>
      <input type=submit name=submit value="Calculate">
    </td>
  </tr>
</table>

</form>


