<%-- 
    Document   : index
    Created on : Aug 15, 2014, 7:54:45 PM
    Author     : Joseph Areeda <joseph.areeda at ligo.org>
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>LigoDV-web search channel list</title>
    </head>
    <body>
        <h1>Channel list search</h1>
        <p>
            This servlet https://<%=request.getLocalName()%><%=request.getContextPath()%>
            provides an easy RESTful interface for searching the
            channel database used by ldvw.
        </p>
        <br><h2>Search parameters</h2>
        <br><p>
            The following table summarizes the parameters used to limit the search.  All are optional,
            however the full channel list currently contains over 23 million entries.  The default
            limit of a request is 10,000 matches.
        </p>
        <div style="margin-left: 1em;">
            <table style="border: 1;"> 
                <thead>
                    <tr>
                        <th>Parameter</th><th>Value</th><th>Description</th>
                    </tr>
                </thead>
                <tbody>
                    <tr> <td>server</td>     <td>string</td>             <td>URL of NDS server eg: nds.ligo-wa.caltech.edu</td> </tr>
                    <tr> <td>ifo</td>        <td>string</td>             <td>part of channel name before: eg: L1, HVE-EX</td> </tr>
                    <tr> <td>subsys</td>     <td>string</td>             <td>part of channel name between : and - eg: IMC, PEM, PSL</td> </tr>
                    <tr> <td>fsCmp</td>      <td>compare operator</td>   <td><, <=, =, >=, or ></td> </tr>
                    <tr> <td>fs</td>         <td>number</td>             <td>sample frequency for comparison</td> </tr>
                    <tr> <td>ctype</td>      <td>string</td>             <td>raw, online, rds, second-trend, minute-trends, static </td> </tr>
                    <tr> <td>dtype</td>      <td>string</td>             <td>int-16, int-32, flt-32, flt-64, cpx-64</td> </tr>
                    <tr> <td>chnamefilt</td> <td>ldvw match pattern</td> <td>see below</td> </tr>
                    <tr> <td>bsearch</td>    <td>string</td>             <td>base channel search pattern</td> </tr>
                    <tr> <td>base</td>       <td>none</td>              <td>if present base channels are returned otherwise individual channel</td> </tr>
                </tbody>
            </table>
        </div>
        <p>
            The ldvw match pattern uses a simple but inefficient syntax.  It is oriented toward the user 
            who does not know the exact channel name.  The help text from ldvw is:
        </p>
        <p>
            <div style="margin-left: 1em;">
                Enter a full channel name or parts of a channel name case is not significant.<br><br>

                If parts are separated by spaces they must all be present for example<br><br>

                "H1:PSL pda" will match H1:PSL-DETC_ISS_PDA_EXCMON, H1:PSL-DETC_ISS_PDA_GAIN, H1:PSL-ISS_PDA_MAX...<br><br>

                The pipe symbol "|" signifies or so "H1:PSL-ISS PDA | H1:PSL-FSS " will match all of the ones above plus ones in the Frequency Stabilization Servo group
            </div>
        </p>
        <p>
            The base channel search pattern is more complex but also more efficient.  It is oriented
            toward fast searching
        </p>
        <p>
            <div style="margin-left: 1em;">
                Enter a full channel name, case is significant.  Note that the trend types 
                must not be included.<br><br>

                Wildcards may be used.  ? matches any single char, * matches zero or more characters<br><br>

            </div>
        </p>
        
        <h2>Authentication</h2>
        <div style="margin-left: 1em;">
            <p>
                We are waiting for permission to offer this service unauthenticated, which will 
                simplify the client code.
            </p>
        </div>
        
        <h2>Examples</h2>
        <div style="margin-left: 1em;">
            <p>
                curl --get --basic --user "user:password" --data "fsCmp=>=&fs=1024&ifo=L1&subsys=IMC" https://<%=request.getLocalName()%><%=request.getContextPath()%>/search
                <br>Will return 406 channels from the LLO Input Mode Cleaner with sample frequencies &ge; 1024 Hz.

            </p>
        </div>
    </body>
</html>
