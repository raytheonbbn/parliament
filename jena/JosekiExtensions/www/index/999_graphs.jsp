<%@page import="com.hp.hpl.jena.rdf.model.ResourceFactory"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.Collections"%>

<%@page import="com.hp.hpl.jena.rdf.model.Resource" %>

<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.ExplorerUtil" %>
<%@page import="com.bbn.parliament.jena.graph.KbGraphStore" %>

   <h2>Graphs</h2>
   <div id="graph_list">
<table>
<%
List<String> graphs = ModelManager.inst().getSortedModelNames();
ExplorerUtil.BlankNodeLabeler bnodeLabeler = new ExplorerUtil.BlankNodeLabeler();
graphs.add(0, null);
boolean alternate = false;
for (String graph : graphs) {
  String displayString = null;
  Resource graphResource;
  boolean isDefault = false;
  if (graph == null || graph.equals("")) {
    graph = KbGraphStore.DEFAULT_GRAPH_URI;
    isDefault = true;
  }
  graphResource = ResourceFactory.createResource(graph);
  displayString = ExplorerUtil.getLabelForResource(graphResource, ModelManager.inst().getDefaultModel());
  alternate = !alternate;
%>
<tr class="<%=alternate ? "alternate" : ""%>">
  <td>

    <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(graphResource, false)%>">
      <% if (isDefault) { %>
        <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayString)%></span>
      <% } else { %>
        <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(graph)%></span>
      <% } %>
    </a>
  <td>
  <% if (!isDefault && null != displayString) { %>
    <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayString)%></span>
  </td>
  <% } %>
</tr>
<%
}
%>
</table>
</div>