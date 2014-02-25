<%-- 
    Document   : index
    Created on : Apr 19, 2012, 6:46:23 PM
    Author     : Joseph Areeda <joe@areeda.com>

NB: It is important that there are no characters sent to the browser
    from this jsp.  Everything is sent from the viewer.
--%><%@page import="edu.fullerton.ldvw.ViewManager"%><%
/**
* The JSP page only calls the Java program, we do everything there.
**/
    ViewManager mgr = new ViewManager();
    mgr.oldRequest(request, response);
    mgr = null;  // try to release all memory
%>