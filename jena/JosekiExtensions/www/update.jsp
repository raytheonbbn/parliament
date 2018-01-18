<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@page import="java.util.Collections"%>
<%@page import="java.util.List"%>
<%@page import="org.apache.jena.iri.IRI"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.bbn.parliament.jena.query.PrefixRegistry"%>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>SPARQL/Update</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
  </head>

  <body>
    <h1>SPARQL/Update Query</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>

    <br/>

    <div class="moreindent">

  	 <h2>SPARQL/Update Query</h2>

      <form class="queryForm" action="sparql" method="post">

	<p>

<%
Map<String, IRI> pm = PrefixRegistry.getInstance().getPrefixes();
List<String> prefixes = new ArrayList<String>(pm.keySet());
Collections.sort(prefixes);
%>
	<textarea name="update" cols="90" rows="30" spellcheck="false">
<% for (String key : prefixes) { String uri = pm.get(key).toString();%>
PREFIX <%=key %>: &lt;<%=uri %>&gt;<% } %>



INSERT DATA
{
#triples
}

DELETE DATA
{
#triples
}

</textarea></p>
    <p>
	  <input type="submit" value="Execute Update" />
	</p>
      </form>
   </div>

  </body>
</html>
