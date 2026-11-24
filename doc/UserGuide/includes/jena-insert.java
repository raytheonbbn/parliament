import java.util.Iterator;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.exec.http.UpdateExecutionHTTP;
import org.apache.jena.sparql.modify.request.QuadDataAcc;
import org.apache.jena.sparql.modify.request.UpdateDataInsert;

void issueInsert(Model model, Node graphUri)
{
	QuadDataAcc acc = new QuadDataAcc();
	for (Iterator<Statement> iter = model.listStatements(); iter.hasNext();) {
		Triple triple = iter.next().asTriple();
		if (graphUri == null) {
			acc.addTriple(triple);
		} else {
			acc.addQuad(new Quad(graphUri, triple));
		}
	}
	UpdateExecutionHTTP.service("http://localhost:8089/parliament/sparql")
		.update(new UpdateDataInsert(qd)).build().execute();
}
