<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">


<%@page import="com.bbn.parliament.jena.query.PrefixRegistry"%>
<%@page import="org.apache.jena.iri.IRI"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>


<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%><html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>Insert Data</title>
  <link rel="stylesheet" type="text/css" href="stylesheets/joseki.css" />
  </head>

  <body>
    <h1>Insert Data</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>

    <div class="moreindent">

    <br />

    <h2>Import Repository</h2>

    <form action="bulk/insert" method="post" enctype="multipart/form-data" accept-charset="UTF-8"
          onsubmit="return confirm('This will replace all existing data in the repository.  Are you sure?')" class="queryForm">
    <p>
       Import an entire repository from a previous Parliament export (ZIP file)
    </p>
    <p>
      <input type="file" name="statements" size="70"/>
    </p>
    <p>
      <input type="hidden" name="import" value="yes"/>
      <input type="submit" value="Import Repository" />
    </p>
    </form>


    <br />

    <h2>File Insert</h2>

    <form class="queryForm" action="bulk/insert" method="post" enctype="multipart/form-data" accept-charset="UTF-8">
       <p>Data to Insert (<select name="dataFormat">
             <option value="AUTO">Auto Detect</option>
             <option value="TURTLE">Turtle</option>
             <option value="N3">N3</option>
             <option value="N-TRIPLES">N-TRIPLES</option>
             <option value="RDF/XML">RDF/XML</option>
          </select> Format)</p>
       <p>
          <input type="file" name="statements" size="70"/>
       </p>
       <!-- <p>
          <input name="verifyData" type="checkbox" checked="checked" value="yes">
             Verify data before processing (only disable if you are sure the data is correct).
          </input>
       </p> -->
       <p>
       Named graph for insertion:
	  <select name="graph">
<%
   List<String> graphs2 = ModelManager.inst().getSortedModelNames();
	//Add a placeholder for the Default Graph
	graphs2.add(0, "");

	for (String graph : graphs2) {
	   String graphName = graph;
	   if ("".equals(graphName)) {
	      graphName = "Default Graph";
	   }
%>
<option value="<%=graph%>"><%=graphName%></option>
<%
   }
%>
	  </select>
	  </p>
	  <p>
	  <input type="submit" value="Insert File" />
	  </p>
    </form>

    <br/>


    <h2>Text Insert</h2>


      <form class="queryForm" action="bulk/insert" method="post">
	<p>Data to Insert (<select name="dataFormat">
     <option value="TURTLE">Turtle</option>
     <option value="N3">N3</option>
     <option value="N-TRIPLES">N-TRIPLES</option>
     <option value="RDF/XML">RDF/XML</option>
     </select> Format)</p>
	<p>
  <%
Map<String, IRI> pm = PrefixRegistry.getInstance().getPrefixes();
List<String> prefixes = new ArrayList<String>(pm.keySet());
Collections.sort(prefixes);
%>
  <textarea name="statements" cols="90" rows="30" spellcheck="false">
<% for (String key : prefixes) { String uri = pm.get(key).toString();%>
@prefix <%=key %>: &lt;<%=uri %>&gt; .<% } %>

</textarea>
	</p>
	<p>
	  Named graph for insertion:
	  <select name="graph">
<%
   List<String> graphs = ModelManager.inst().getSortedModelNames();
   graphs.add(0, "");
	for (String graph : graphs) {
	   String graphName = graph;
	   if ("".equals(graphName)) {
	      graphName = "Default Graph";
	   }
%>
<option value="<%=graph %>"><%=graphName %></option>
<%
}
%>
	  </select></p>
	  <p>
	  <input type="submit" value="Insert Data" />
	</p>
      </form>
      <br/>
    </div>
  </body>
</html>
