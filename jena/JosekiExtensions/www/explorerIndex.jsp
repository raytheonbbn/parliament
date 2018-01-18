<!doctype html public "-//w3c//dtd html 4.0 transitional//en">
<%@page language="java"%>
<%@page contentType = "text/html;charset=UTF-8"%>

<%@page import="com.bbn.parliament.jena.joseki.bridge.util.NTriplesUtil" %>
<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.ExplorerUtil"%>

<%@page import="com.hp.hpl.jena.query.Query" %>
<%@page import="com.hp.hpl.jena.query.QueryFactory" %>
<%@page import="com.hp.hpl.jena.query.QueryExecution" %>
<%@page import="com.hp.hpl.jena.query.QueryExecutionFactory" %>
<%@page import="com.hp.hpl.jena.query.ResultSet" %>
<%@page import="com.hp.hpl.jena.rdf.model.Model" %>
<%@page import="com.hp.hpl.jena.rdf.model.Resource" %>
<%@page import="com.hp.hpl.jena.rdf.model.RDFNode" %>

<%
   // This is the maximum number of classes or properties displayed on the main Explorer Index page.
	int limit = 20;

   //String graphStr = request.getParameter("graph");
   //String graphStr = null;

   ExplorerUtil.BlankNodeLabeler bnodeLabeler = new ExplorerUtil.BlankNodeLabeler();

	Model model = ModelManager.inst().getDefaultModel();

	QueryExecution classQE = null;
	QueryExecution propertyQE = null;
try
{
//   String classQueryStr = "PREFIX rdf:  <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
//       "SELECT DISTINCT ?class WHERE { [] rdf:type ?class . filter ( !isblank(?class) ) } LIMIT " + limit;
   String classQueryStr = "PREFIX owl:  <http://www.w3.org/2002/07/owl#> \n" +
       "SELECT DISTINCT ?class WHERE { ?class a owl:Class . filter ( !isblank(?class) ) } LIMIT " + limit;
	Query classQuery = QueryFactory.create(classQueryStr);
	classQE = QueryExecutionFactory.create(classQuery, model);
	ResultSet classes = classQE.execSelect();

	String propertyQueryStr = "PREFIX owl:  <http://www.w3.org/2002/07/owl#> \n" +
	    "SELECT DISTINCT ?property WHERE { { ?property a owl:ObjectProperty } UNION { ?property a owl:DatatypeProperty } } LIMIT " + limit;
	Query propertyQuery = QueryFactory.create(propertyQueryStr);
	propertyQE = QueryExecutionFactory.create(propertyQuery, model);
	ResultSet properties = propertyQE.execSelect();
%>

<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>Explore Repository</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/queryResults.css" />
    <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
  <script lang="JavaScript">

String.prototype.trim = function () {
    return this.replace(/^\s*/, "").replace(/\s*$/, "");
}

function processDisplay(form) {
   var value = form.value.value.trim();
   if (value.charAt(0) != '<')
   {
      form.value.value = '<' + value + '>';
   }
   return true;
}
  </script>
  </head>

<body onLoad="document.valueForm.value.focus()">

<h1>Explore Repository</h1>
<div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>
<br/>

<div class="indent">

<form name="valueForm" action="explorer.jsp" method="get" onsubmit="return processDisplay(this)" accept-charset="UTF-8">
Enter a URI to start the exploration with:
<br/>
<input type="TEXT" name="value" size="70"/>
<input type="SUBMIT" value="Explore"/>
</form>
</div>

<br/>
<div class="named-graph-list"><jsp:include page="index/999_graphs.jsp" /></div>

<%
	if (((null != classes) && classes.hasNext()) || ((null != properties) && properties.hasNext())) {
%>

<br/>
<h2>Classes and Properties</h2>
<div class="indent">

	<table border="0" class="explorerIndexTable">
		<tr>
			<th>Classes</th>
			<th>Properties</th>
		</tr>
		<tr valign="top">
			<td>
<%

	if (null != classes)
	{
		while(classes.hasNext())
		{
			RDFNode value = classes.nextSolution().get("class");
			if (value.isResource())
			{
%>
			   <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(value, false)%>">
				   <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(ExplorerUtil.getDisplayString(value, bnodeLabeler))%></span>
			   </a>
			   <br/>
<%
			}
		}
	}
%>
			</td>
			<td>
<%

	if (null != properties)
	{
		while(properties.hasNext())
		{
			RDFNode value = properties.nextSolution().get("property");
			if (value.isResource())
			{
	%>
			   <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(value, false)%>">
				   <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(ExplorerUtil.getDisplayString(value, bnodeLabeler))%></span>
			   </a>
			   <br/>
	<%
			}
		}
	}
%>
			</td>
		</tr>
	</table>
<%
	}

}
finally {
   	if (null != classQE)
   	{
   		classQE.close();
   	}
   	if (null != propertyQE)
   	{
   		propertyQE.close();
   	}
	}
%>

</div>

</body>
</html>
