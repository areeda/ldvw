<%--
_header.jsp
This file is included at the start of all pages
(c)2001 Kevin Boone/Web-Tomorrow
--%>

<jsp:useBean id="sunTimesBean" class="com.web_tomorrow.suntimes_web.SunTimesBean" scope="session"/>

<%
if (!isErrorPage)
  {
%> 
  <jsp:setProperty name="sunTimesBean" property="*"/>
<%
  }
%>

<html>
<header>
<title>
SunTimes at Web-Tomorrow
</title>
<body bgcolor="white" link="#4040F0" vlink="gray">

<table cellpadding=10 cellspacing=0 align=center border=0 align=center width=70%>
  <tr align=center>
    <td>
       <img src=suntimes_logo.gif border=0 alt="Logo"><p>
       Calculate sunrise and sunset times anywhere, on any date.<br>
       <a href=help.jsp>Click here</a> for information on the calculation and
       on how to interpret the results.
    </td>
  </tr>
    <tr align=center>
      <td height=5>
        &nbsp;
      </td>
    </tr>
</table>

<table border=0 cellspacing=0 cellpadding=0 align=center>
  <tr align=center>
    </td>
    <td>

