<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%@page import="com.hp.hpl.jena.rdf.model.impl.IteratorFactory"%>
<%@page import="com.hp.hpl.jena.graph.Node"%>
<%@page import="com.hp.hpl.jena.rdf.model.ResourceFactory"%>
<%@page import="com.bbn.parliament.jena.graph.KbGraphStore"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.ActionRouter" %>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.NTriplesUtil" %>
<%@page import="com.bbn.parliament.jena.joseki.graph.ModelManager"%>
<%@page import="com.bbn.parliament.jena.joseki.bridge.util.ExplorerUtil"%>

<%@page import="com.hp.hpl.jena.rdf.model.Model" %>
<%@page import="com.hp.hpl.jena.rdf.model.Resource" %>
<%@page import="com.hp.hpl.jena.rdf.model.StmtIterator" %>
<%@page import="com.hp.hpl.jena.rdf.model.Statement" %>
<%@page import="com.hp.hpl.jena.rdf.model.RDFNode" %>
<%@page import="com.hp.hpl.jena.rdf.model.Property" %>

<%@page import="java.util.HashMap" %>
<%@page import="java.util.List" %>
<%@page import="java.util.ArrayList" %>
<%@page import="java.util.Collections" %>

<%
   String valueStr = request.getParameter("value");
//   String graphStr = request.getParameter("graph");
   String useLabelsStr = request.getParameter("useLabels");

   boolean useLabels = "yes".equalsIgnoreCase(useLabelsStr);

   ExplorerUtil.BlankNodeLabeler bnodeLabeler = new ExplorerUtil.BlankNodeLabeler();

	Model defaultGraph = ModelManager.inst().getDefaultModel();
	List<String> graphNames = ModelManager.inst().getSortedModelNames();
   // Add a placeholder for the Default Graph
   graphNames.add(0, "");

//   RDFNode value = model.createResource(valueStr);
//   Node node = com.hp.hpl.jena.graph.test.NodeCreateUtils.create(valueStr);

	RDFNode value = null;
	if (valueStr != null)
	{
		value = NTriplesUtil.parseValue(valueStr, defaultGraph);
	}


//	String repository = HttpServerUtil.getParameter(request, "repository");
%>

<html xmlns="http://www.w3.org/1999/xhtml">
  <head><title>Explore Repository: <%=(null != value) ? ExplorerUtil.getDisplayString(value, bnodeLabeler) : ""%></title>

  <link rel="stylesheet" type="text/css" href="stylesheets/queryResults.css" />
      <link
      rel="stylesheet"
      type="text/css"
      href="stylesheets/joseki.css"
    />
  </head>

  <body>
    <h1>Explore Repository</h1>
    <div class="op-list"><h3><a href="index.jsp">Home</a></h3> <jsp:include page="index/000_operations.jsp"/></div>
    <br/>

    <div class="indent">

    Showing statements for: <b><%=(null != value) ? ExplorerUtil.getDisplayString(value, bnodeLabeler) : ""%></b><br/>

    <form name="valueForm" action="explorer.jsp" method="get">
      <%
         if (null != value) {
      %>
		<input type="hidden" name="value" value="<%=ExplorerUtil.escapeDoubleQuotedAttValue(NTriplesUtil.toNTriplesString(value))%>"/>
		<%
		   }
		%>
<%
   String useLabelsChecked = useLabels ? "checked" : "";
%>
    <input type="checkbox" name="useLabels" value="yes" <%=useLabelsChecked%>
           onClick="JavaScript:document.valueForm.submit()"/>
    Use resource labels in overview
    </form>
    <br/>

<%
   //Lock code
try {

   // Aquire lock
   ActionRouter.getReadLock();
%>

    <!-- STATEMENTS WITH VALUE AS SUBJECT -->
    Statements with this value as subject:
    <table>
		<tr>
		   <th>graph</th>
			<th>subject</th>
			<th>predicate</th>
			<th>object</th>
		</tr>
<%
   boolean foundStatements = false;
   StmtIterator iter = null;
   boolean oddGraph = true;

   for (String graphName : graphNames) {
      boolean isDefaultGraph = "".equals(graphName);

      Model model = isDefaultGraph ? ModelManager.inst().getDefaultModel() : ModelManager.inst().getModel(graphName);
      String graphLabel = graphName;

      Resource graphNameResource = null;
      if (!isDefaultGraph) {
        graphNameResource = ResourceFactory.createResource(graphName);
        if (useLabels) {
          String label = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
          if (label != null) {
             graphLabel = label;
          }
        }
      } else {
         graphNameResource = ResourceFactory.createResource(KbGraphStore.DEFAULT_GRAPH_URI);
         graphLabel = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
      }


      int numStatements = 0;
	   if ((null != value) && (value instanceof Resource)) {
	   	iter = model.listStatements((Resource) value, (Property) null, (RDFNode) null);

	   	// Iterate through once to get the number of statements so we can set the rowspan
	   	try {
		   	while (iter.hasNext()) {
		   	   iter.nextStatement();
		   	   numStatements++;
		   	}
	   	}
	   	finally {
	   	   if (null != iter) {
	   	      iter.close();
	   	   }
	   	}

	   	iter = model.listStatements((Resource) value, (Property) null, (RDFNode) null);
		}
	   else {
	      iter = ExplorerUtil.getEmptyStmtIterator();
	   }

	   boolean firstIteration = true;
		try {
		   if (numStatements > 0) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
		  <td class="value" rowspan="<%=numStatements%>">

			     <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(graphNameResource, useLabels)%>">
			        <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(graphLabel)%></span>
			     </a>

		  </td>
<%
   foundStatements = true;
				while (iter.hasNext()) {
					Statement stat = iter.nextStatement();

					Property pred = stat.getPredicate();
					RDFNode obj = stat.getObject();

					String displayPred = ExplorerUtil.getDisplayString(pred, bnodeLabeler);
					String displayObj = ExplorerUtil.getDisplayString(obj, bnodeLabeler);

					if (useLabels) {
						String label = ExplorerUtil.getLabelForResource(pred, model);
						if (label != null) {
							displayPred = label;
						}

						if (obj instanceof Resource) {
							label = ExplorerUtil.getLabelForResource((Resource)obj, model);
							if (label != null) {
								displayObj = label;
							}
						}
					}

			      if (!firstIteration) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
<%
   }
			      firstIteration = false;
%>
			<td class="centered">-</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(pred, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayPred)%></span>
				</a>
			</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(obj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayObj)%></span>
				</a>
			</td>
		</tr>
<%
   }
		   }
		}
		finally {
		   if (null != iter) {
		      iter.close();
		   }
		}

		if (!firstIteration) {
			oddGraph = !oddGraph;
		}
   }

	if (!foundStatements) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
			<td colspan="4" align="center" class="value">-- no statements found --</td>
		</tr>
<%
   }
%>
	</table>

	<br/>


<!-- STATEMENTS WITH VALUE AS PREDICATE -->
Statements with this value as predicate:
    <table>
		<tr>
		   <th>graph</th>
			<th>subject</th>
			<th>predicate</th>
			<th>object</th>
		</tr>
<%
   foundStatements = false;
	oddGraph = true;

   for (String graphName : graphNames) {
      boolean isDefaultGraph = "".equals(graphName);

      Model model = isDefaultGraph ? ModelManager.inst().getDefaultModel() : ModelManager.inst().getModel(graphName);
	   String graphLabel = graphName;

	   Resource graphNameResource = null;
	   if (!isDefaultGraph)
	   {
	      graphNameResource = ResourceFactory.createResource(graphName);
	      if (useLabels) {
				String label = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
				if (label != null) {
					graphLabel = label;
				}
	      }
	   } else {
         graphNameResource = ResourceFactory.createResource(KbGraphStore.DEFAULT_GRAPH_URI);
         graphLabel = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
       }

	   int numStatements = 0;
	   if ((null != value) && (value instanceof Resource)) {
	   	iter = model.listStatements((Resource) null, value.as(Property.class), (RDFNode) null);

	   	// Iterate through once to get the number of statements so we can set the rowspan
   try {
      while (iter.hasNext()) {
   iter.nextStatement();
   numStatements++;
      }
   }
   finally {
      if (null != iter) {
   iter.close();
      }
   }

   iter = model.listStatements((Resource) null, value.as(Property.class), (RDFNode) null);
		}
	   else {
	      iter = ExplorerUtil.getEmptyStmtIterator();
	   }

		boolean firstIteration = true;
		try {
			if (numStatements > 0) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
		  <td class="value" rowspan="<%=numStatements%>">
			     <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(graphNameResource, useLabels)%>">
			        <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(graphLabel)%></span>
			     </a>
		  </td>
<%
   foundStatements = true;
      while (iter.hasNext()) {
   Statement stat = iter.nextStatement();

					Resource subj = stat.getSubject();
					RDFNode obj = stat.getObject();

					String displaySubj = ExplorerUtil.getDisplayString(subj, bnodeLabeler);
					String displayObj = ExplorerUtil.getDisplayString(obj, bnodeLabeler);

					if (useLabels) {
						String label = ExplorerUtil.getLabelForResource(subj, model);
						if (label != null) {
						    displaySubj = label;
						}

						if (obj instanceof Resource) {
							label = ExplorerUtil.getLabelForResource((Resource)obj, model);
							if (label != null) {
								displayObj = label;
							}
						}
					}

				   if (!firstIteration) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
<%
   }
				   firstIteration = false;
%>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(subj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displaySubj)%></span>
				</a>
			</td>
			<td class="centered">-</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(obj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayObj)%></span>
				</a>
			</td>
		</tr>
<%
   }
			}
		}
		finally {
		   if (null != iter) {
      iter.close();
   }
		}

		if (!firstIteration) {
			oddGraph = !oddGraph;
		}
   }

	if (!foundStatements) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
			<td colspan="4" align="center" class="value">-- no statements found --</td>
		</tr>
<%
   }
%>
	</table>

	<br/>

<!-- STATEMENTS WITH URI AS OBJECT -->
Statements with this value as object:
    <table>
		<tr>
			<th>graph</th>
			<th>subject</th>
			<th>predicate</th>
			<th>object</th>
		</tr>
<%
   foundStatements = false;
	oddGraph = true;

	for (String graphName : graphNames) {
	   boolean isDefaultGraph = "".equals(graphName);

      Model model = isDefaultGraph ? ModelManager.inst().getDefaultModel() : ModelManager.inst().getModel(graphName);
	   String graphLabel = graphName;

	   Resource graphNameResource = null;
	   if (!isDefaultGraph)
	   {
	      graphNameResource = ResourceFactory.createResource(graphName);
	      if (useLabels) {
				String label = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
				if (label != null) {
					graphLabel = label;
				}
	      }
	   } else {
        graphNameResource = ResourceFactory.createResource(KbGraphStore.DEFAULT_GRAPH_URI);
        graphLabel = ExplorerUtil.getLabelForResource(graphNameResource, defaultGraph);
       }

	   int numStatements = 0;
		if (null != value) {
			iter = model.listStatements((Resource) null, (Property) null, value);

		   // Iterate through once to get the number of statements so we can set the rowspan
   try {
      while (iter.hasNext()) {
   iter.nextStatement();
   numStatements++;
      }
   }
   finally {
      if (null != iter) {
   iter.close();
      }
   }

   iter = model.listStatements((Resource) null, (Property) null, value);
		}
	   else {
			iter = ExplorerUtil.getEmptyStmtIterator();
	   }

		boolean firstIteration = true;
		try {
		   if (numStatements > 0) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
		  <td class="value" rowspan="<%=numStatements%>">

			     <a href="explorer.jsp?<%=ExplorerUtil.getQueryString(graphNameResource, useLabels)%>">
			        <span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(graphLabel)%></span>
			     </a>


		  </td>
<%
   foundStatements = true;
				while (iter.hasNext()) {
   Statement stat = iter.nextStatement();

					Resource subj = stat.getSubject();
					Property pred = stat.getPredicate();

					String displaySubj = ExplorerUtil.getDisplayString(subj, bnodeLabeler);
					String displayPred = ExplorerUtil.getDisplayString(pred, bnodeLabeler);

					if (useLabels) {
					   String label = ExplorerUtil.getLabelForResource(subj, model);
						if (label != null) {
						    displaySubj = label;
						}

						label = ExplorerUtil.getLabelForResource(pred, model);
						if (label != null) {
							displayPred = label;
						}
					}
					if (!firstIteration) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
<%
   }
					firstIteration = false;
%>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(subj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displaySubj)%></span>
				</a>
			</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(pred, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayPred)%></span>
				</a>
			</td>
			<td class="centered">-</td>
		</tr>
<%
   }
   }
      }
		finally {
   if (null != iter) {
      iter.close();
   }
      }

		if (!firstIteration) {
			oddGraph = !oddGraph;
		}
   }

	if (!foundStatements) {
%>
		<tr class="<%=oddGraph ? "odd" : "even"%>">
			<td colspan="4" align="center" class="value">-- no statements found --</td>
		</tr>
<%
   }
%>
	</table>

<%
   // If we are looking at a named graph, then add a table that contains all of
   // the statements in that named graph
   if (value.isURIResource()) {
      Resource resource = value.as(Resource.class);

      Model model = null;
      if (KbGraphStore.DEFAULT_GRAPH_URI.equals(resource.getURI())) {
         model = ModelManager.inst().getDefaultModel();
      } else if (ModelManager.inst().containsModel(resource.getURI())) {
        model = ModelManager.inst().getModel(resource.getURI());
      }
      if (null != model) {
        iter = model.listStatements((Resource) null, (Property) null, (RDFNode) null);

        foundStatements = false;
%>

<br/>

<!-- ALL STATEMENTS IN NAMED GRAPH -->
Statements in this graph:
   <table>
		<tr>
			<th>subject</th>
			<th>predicate</th>
			<th>object</th>
		</tr>
<%
   while (iter.hasNext()) {
      Statement stat = iter.nextStatement();
      Resource subj = stat.getSubject();
				Property pred = stat.getPredicate();
				RDFNode obj = stat.getObject();

				foundStatements = true;

				String displaySubj = ExplorerUtil.getDisplayString(subj, bnodeLabeler);
				String displayPred = ExplorerUtil.getDisplayString(pred, bnodeLabeler);
				String displayObj = ExplorerUtil.getDisplayString(obj, bnodeLabeler);

				if (useLabels) {
				   String label = ExplorerUtil.getLabelForResource(subj, model);
					if (label != null) {
					    displaySubj = label;
					}

					label = ExplorerUtil.getLabelForResource(pred, model);
					if (label != null) {
						displayPred = label;
					}

					if (obj instanceof Resource) {
						label = ExplorerUtil.getLabelForResource((Resource)obj, model);
						if (label != null) {
							displayObj = label;
						}
					}
				}
%>
      <tr>
         <td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(subj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displaySubj)%></span>
				</a>
			</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(pred, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayPred)%></span>
				</a>
			</td>
			<td class="value">
				<a href="explorer.jsp?<%=ExplorerUtil.getQueryString(obj, useLabels)%>">
					<span style="white-space:pre"><%=ExplorerUtil.escapeCharacterData(displayObj)%></span>
				</a>
			</td>
		</tr>
<%
   }
   if (!foundStatements) {
%>
		<tr>
			<td colspan="3" align="center" class="value">-- no statements found --</td>
		</tr>
<%
   }
   iter.close();
	   }
   }

}
finally {
      // Release lock
      ActionRouter.releaseReadLock();
   }
%>

   </table>
	<br/>

    </div>

  </body>
</html>
