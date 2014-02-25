<%--
This is the main error page
(c)2001 Kevin Boone/Web-Tomorrow
--%>

<%@ page isErrorPage="true" %>

<%
boolean isErrorPage = true;
%>

<%@ include file="_header.jsp" %>

<%
sunTimesBean.setErrorMessage
  ("Your data could not be processed<br>Please check that all the fields contain sensible, numeric values");
%>

<%@ include file="_mainform.jsp"  %>

<%@ include file="_footer.jsp" %>
