package com.bbn.parliament.jena.query.index.pfunction;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbn.parliament.jena.Constants;
import com.bbn.parliament.jena.graph.KbGraphStore;
import com.bbn.parliament.jena.graph.index.Index;
import com.bbn.parliament.jena.graph.index.IndexException;
import com.bbn.parliament.jena.graph.index.IndexManager;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.sparql.engine.ExecutionContext;
import com.hp.hpl.jena.sparql.engine.binding.Binding;
import com.hp.hpl.jena.sparql.pfunction.PropFuncArgType;

/**
 * A property function that enables or disables indexes. Enabling indexes
 * creates new instances of all configured indexes. Disabling them removes and
 * deletes them. Enabling indexes will rebuild the indexes so any existing data
 * will be added to the indexes. If the graph already has indexes enabled,
 * re-enabling indexes does nothing.
 * <br><br>
 * The following shows how to enable indexes:
 *
 * <blockquote><code>
 * PREFIX parPF: &lt;http://parliament.semwebcentral.org/pfunction#&gt;<br>
 * INSERT {} WHERE {<br>
 * &nbsp;&nbsp;&nbsp;(&lt;http://example.org/graph1&gt; &lt;http://example.org/graph2&gt;)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;parPF:enableIndexing "true"^^xsd:boolean .<br>
 * }
 * </code></blockquote>
 *
 * @author rbattle
 */
public class EnableIndexing extends PFGraphsAsSubject {

	public static final String URI = Constants.PFUNCTION_NAMESPACE + "enableIndexing";
	private static final Logger LOG = LoggerFactory.getLogger(EnableIndexing.class);

	private boolean enabled = false;

	public EnableIndexing() {
		super(PropFuncArgType.PF_ARG_SINGLE);
	}

	@Override
	protected boolean checkObject(Node node) {
		if (!node.isLiteral()) {
			return false;
		}

		Object value = node.getLiteralValue();
		if (!(value instanceof Boolean)) {
			return false;
		}
		enabled = ((Boolean) value).booleanValue();
		return true;
	}

	@Override
	protected boolean processGraphObject(Binding binding, Node graphName,
		Graph graph, Node object, ExecutionContext context) {
		if (enabled) {
			// don't add indexes for a graph that already has them.
			if (IndexManager.getInstance().hasIndexes(graph)) {
				return false;
			}
			List<Index<?>> indexes = IndexManager.getInstance()
				.createAndRegisterAll(graph, graphName);
			for (Index<?> index : indexes) {
				try {
					index.open();
				} catch (IndexException e) {
					LOG.error("Could not open index.  Unregistering all indexes", e);
					// unregister indexes
					IndexManager.getInstance().unregisterAll(graph, graphName);
					return false;
				}
			}
			IndexManager.getInstance().rebuild(graph);
		} else {
			IndexManager.getInstance().unregisterAll(graph, graphName);
		}
		if (context.getDataset() instanceof KbGraphStore) {
			KbGraphStore graphStore = (KbGraphStore) context.getDataset();
			graphStore.setIndexingEnabled(graphName, enabled);
		}
		return true;
	}
}
