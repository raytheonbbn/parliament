<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@page import="com.hp.hpl.jena.rdf.model.ResourceFactory"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.ExplorerUtil"%>
<%@page import="com.bbn.parliament.jena.Constants"%>
<%@page import="com.bbn.parliament.jena.graph.KbGraphFactory"%>
<%@page import="com.bbn.parliament.jena.graph.index.Index"%>
<%@page import="com.bbn.parliament.jena.graph.index.IndexManager"%>
<%@page import="org.joseki.util.Convert"%>
<%@page import="com.bbn.parliament.jena.graph.KbGraph"%>
<%@page import="com.bbn.parliament.jena.graph.index.IndexFactoryRegistry"%>
<%@page import="com.bbn.parliament.jena.graph.index.IndexFactory"%>
<%@page import="java.util.Collections"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<%@page import="com.bbn.parliament.jena.graph.KbGraphStore"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.NTriplesUtil"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.hp.hpl.jena.graph.Graph"%>
<%@page import="com.hp.hpl.jena.rdf.model.Model"%>
<%@page import="com.hp.hpl.jena.graph.Node"%>
<%@page import="com.hp.hpl.jena.rdf.model.Resource"%>
<%@page import="java.util.List"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.ParliamentBridge"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>Indexes</title>
    <link
      rel="stylesheet"
      type="text/css"
      href="stylesheets/joseki.css"
    />
    <script
      type="text/javascript"
      src="javascripts/jquery-1.4.2.min.js"
    ></script>
    <script type="text/javascript">
      var propNS = "<%=Constants.PFUNCTION_NAMESPACE%>";
      function enable(graphUri) {
          $
          .ajax({
            type : "POST",
            url : "sparql",
            data : "update=INSERT {} WHERE {<" + graphUri + "> <" + propNS + "enableIndexing> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> }"
          });

      setTimeout("window.location.reload()", 3000);
      }
      function disable(graphUri) {
          $
          .ajax({
            type : "POST",
            url : "sparql",
            data : "update=INSERT {} WHERE {<" + graphUri + "> <" + propNS + "enableIndexing> \"false\"^^<http://www.w3.org/2001/XMLSchema#boolean> }"
          });

      	setTimeout("window.location.reload()", 3000);
      }

      function rebuild(graphUri) {
        $
            .ajax({
              type : "POST",
              url : "sparql",
              data : "update=INSERT {} WHERE {<" + graphUri + "> <java:com.bbn.parliament.jena.pfunction.rebuildIndexes> \"true\"^^<http://www.w3.org/2001/XMLSchema#boolean> }"
            });
        setTimeout("window.location.reload()", 3000);
      }

    </script>
  </head>
<body>
  <h1>Indexes</h1>
    <div class="op-list">
    <h3><a href="index.jsp">Home</a></h3>
    <jsp:include page="index/000_operations.jsp" /></div>
<%
IndexFactoryRegistry registry = IndexFactoryRegistry.getInstance();
if (registry.getFactories().size() == 0) {
%>
    <p>
      <i>None loaded</i>
    </p>
<%
} else {
%>
    <p >
      <table id="indexes">
        <tr>
          <th>Graph</th>
<%
  boolean rebuilding = false;
  for (IndexFactory<?,?> f : registry.getFactories()) {
%>
          <th><%=f.getLabel() %></th>
<%
  }
%>
          <th></th>
        </tr>
<%
  List<String> graphNames = ModelManager.inst().getSortedModelNames();
  graphNames.remove(KbGraphStore.MASTER_GRAPH);
  graphNames.add(0, null);
  boolean alternate = false;
  for (String g : graphNames) {
    Graph graph = null;
    Model m = null;
    String graphUri = null;
    if (null == g || g.isEmpty()) {
      m = ModelManager.inst().getDefaultModel();
      graphUri = KbGraphStore.DEFAULT_GRAPH_URI;
      g = ExplorerUtil.getLabelForResource(ResourceFactory.createResource(graphUri), m);
    } else {
      m = ModelManager.inst().getModel(g);
      graphUri = g;
    }
    String rowClass = (alternate) ? "alternate" : "";

    alternate = !alternate;
    graph = m.getGraph();
    List<Index<?>> indexes = IndexManager.getInstance().getIndexes(graph);

    Map<IndexFactory<?,?>, Long> counts = new HashMap<IndexFactory<?,?>, Long>();
    Map<IndexFactory<?,?>, Long> factoryToIndex = new HashMap<IndexFactory<?,?>, Long>();
    boolean enabledAlready = indexes.size() > 0;
    for (IndexFactory<?, ?> f : registry.getFactories()) {
      for (Index<?> index : indexes) {
        if (f.equals(IndexManager.getInstance().getIndexFactory(index))) {
          counts.put(f, index.size());
          break;
        }
      }
    }
    String encGraphUri = Convert.encWWWForm(graphUri);
    Resource graphResource = ResourceFactory.createResource(graphUri);
%>
        <tr class="<%=rowClass %>">
          <td>
            <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(graphResource, false)%>"><%=g%></a>
          </td>
<%
    for (IndexFactory<?, ?> f : registry.getFactories()) {
       Long count = counts.get(f);

       if (null == count) {
%>
          <td class="count">-</td>
<%
       } else {
%>
          <td class="count"><%=count%></td>
<%
       }
    }
%>
          <td class="action">
<%
    if (!enabledAlready) {

%>
            <a href="javascript:enable('<%=encGraphUri%>')">Create All</a>
<%
    } else {
%>
            <span class="disabled">Create All</span>
<%
    }
%>
             |
<%
    if (enabledAlready) {
%>

             <a href="javascript:disable('<%=encGraphUri%>')">Delete All</a>
<%
    } else {
%>
            <span class="disabled">Delete All</span>
<%
    }
%>
          </td>
        </tr>
<%
  }
%>

      </table>
    </p>
<%
}
%>
</body>
</html>
