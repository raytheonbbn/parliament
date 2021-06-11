import java.util.Iterator;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateRequest;

void issueInsert(Model model, Node graphUri)
{
	QuadDataAcc acc = new QuadDataAcc();
	for (Iterator<Statement> iter = model.listStatements(); iter.hasNext();) {
		Statement statement = iter.next();
		acc.addQuad(new Quad(graphUri, statement.asTriple()));
	}

	UpdateRequest updateRequest = new UpdateRequest(new UpdateDataInsert(acc));
	updateRequest.setPrefixMapping(model);
	UpdateExecutionFactory
		.createRemote(updateRequest, "http://localhost:8089/parliament/sparql")
		.execute();
}
