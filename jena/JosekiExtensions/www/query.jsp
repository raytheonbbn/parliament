<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="org.apache.jena.iri.IRI"%>
<%@page import="com.bbn.parliament.jena.query.PrefixRegistry"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>


<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>

<html xmlns="http://www.w3.org/1999/xhtml">

  <head><title>Query</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
<script type="text/javascript">
function processDisplay(form) {
	value = null;
	for (i = 0; i < form.display.length; i++) {
		option = form.display[i];
		if (option.checked) {
			value = option.value;
			break;
		}
	}

	form.stylesheet.value = "";
	form.output.value = "";
	if (value == "html") {
		form.stylesheet.value="/xml-to-html.xsl";
	} else if (value == "count") {
		form.stylesheet.value = "/xml-to-count.xsl";
	} else if (value == "csv") {
		form.stylesheet.value = "/xml-to-csv.xsl";
	} else if (value == "xml") {
		form.output.value = "xml";
	} else if (value == "custom") {
		form.stylesheet.value = form.custom.value;
	} else if (value == "json") {
		form.output.value = "json";
	}
}
</script>
  </head>

  <body>
    <h1>Query</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>

    <br />

    <div class="moreindent">

    <h2>SELECT or CONSTRUCT query</h2>

      <form action="sparql" method="post" onsubmit="processDisplay(this);" class="queryForm">

<p>
<!-- <div class="border"> -->
<%
Map<String, IRI> pm = PrefixRegistry.getInstance().getPrefixes();
List<String> prefixes = new ArrayList<String>(pm.keySet());
Collections.sort(prefixes);
%>
<textarea name="query" id="code" cols="90" rows="30" spellcheck="false">
<% for (String key : prefixes) { String uri = pm.get(key).toString();%>
PREFIX <%=key %>: &lt;<%=uri %>&gt;<% } %>


SELECT DISTINCT
?class
WHERE {
   ?class a owl:Class .
   FILTER (!isblank(?class))
}

</textarea>
	  <br/>
	  If SELECT query, display as:<br/>
	  <input type="radio" value="html" name="display" checked="true"/>HTML table<br/>
	  <input type="radio" value="count" name="display"/>Count only<br/>
	  <input type="radio" value="csv" name="display"/>CSV<br/>
	  <input type="radio" value="xml" name="display"/>SPARQL result set<br/>
	  <input type="radio" value="custom" name="display"/>Custom XSLT <input type="text" name="custom" value=""/><br/>
	  <input type="radio" value="json" name="display"/>JSON<br/>

	  <input type="hidden" value="" name="stylesheet"/>
	  <input type="hidden" value="" name="output" />
<!--
	  <input name="stylesheet" size="25" value="/xml-to-html.xsl" />
	  or JSON output: <input type="checkbox" name="output" value="json"/>

	  <br/>-->
	  <input type="submit" value="Get Results" />
	</p>
   </form>
   </div>
   <br/>
   <div class="named-graph-list"><jsp:include page="index/999_graphs.jsp" /></div>
  </body>
</html>
