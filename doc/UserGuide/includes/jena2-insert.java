import java.util.Iterator;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Quad;
import com.hp.hpl.jena.sparql.modify.request.QuadDataAcc;
import com.hp.hpl.jena.sparql.modify.request.UpdateDataInsert;
import com.hp.hpl.jena.update.UpdateExecutionFactory;
import com.hp.hpl.jena.update.UpdateRequest;

void issueInsert(Model model, Node graphUri)
{
	QuadDataAcc acc = new QuadDataAcc();
	for (Iterator<Statement> iter = model.listStatements(); iter.hasNext();) {
		Statement statement = iter.next();
		acc.addQuad(new Quad(graphUri, statement.asTriple()));
	}

	UpdateRequest updateRequest = new UpdateRequest(new UpdateDataInsert(acc));
	updateRequest.setPrefixMapping(model);

	try {
		UpdateExecutionFactory
			.createRemote(updateRequest, "http://localhost:8089/parliament/sparql")
			.execute();
	} catch (NullPointerException ex) {
		StackTraceElement topFrame = ex.getStackTrace()[0];
		if ("org.openjena.riot.web.HttpOp".equals(topFrame.getClassName())
				&& "httpResponse".equals(topFrame.getMethodName())
				&& 345 == topFrame.getLineNumber()) {
			// Ignoring spurious NPE
		} else {
			throw new RuntimeException("Re-thrown NPE:", ex);
		}
	}
}
