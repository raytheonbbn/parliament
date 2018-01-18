<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>

<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>

<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>Export Data</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
  </head>

  <body>
    <h1>Export Data</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>

    <div class="moreindent">

      <br />

      <h2>Export Repository</h2>

      <form action="bulk/export" method="post">

      <p>
      <input type="radio" name="exportAll" value="yes" checked="true" />Entire Repository (ZIP file)<br/>
      </p>
      <p>
      Data Format: &nbsp;
        <select name="dataFormat">
          <option value="N-TRIPLES">N-TRIPLES</option>
          <option value="N3">N3</option>
          <option value="RDF/XML">RDF/XML</option>
          <option value="TURTLE">Turtle</option>
        </select>
      </p>
      <p>
      <input type="submit" value="Export Repository" />
      </p>

      </form>

      <br/>

      <h2>Export Graph</h2>

      <form action="bulk/export" method="post">

      <p>
<%
   List<String> graphs = ModelManager.inst().getSortedModelNames();
   // Add a placeholder for the Default Graph
   graphs.add(0, "");

	for (String graph : graphs) {
	   String graphName = graph;
	   boolean isDefaultGraph = false;
		if ("".equals(graph)) {
		   isDefaultGraph = true;
		   graphName = "<i>Default Graph</i>";
		}
%>
           <input type="radio" name="graph" value="<%=isDefaultGraph ? "" : graphName%>"
              <%=isDefaultGraph ? "checked=\"true\"" : ""%>/><%=graphName%><br/>
<%
	}
%>
      </p>
      <p>
      Data Format: &nbsp;
        <select name="dataFormat">
          <option value="N-TRIPLES">N-TRIPLES</option>
          <option value="N3">N3</option>
          <option value="RDF/XML">RDF/XML</option>
          <option value="TURTLE">Turtle</option>
        </select>
      </p>
      <p>
        <input type="submit" value="Export Graph" />
      </p>
      </form>
      <br/>
    </div>
  </body>
</html>
