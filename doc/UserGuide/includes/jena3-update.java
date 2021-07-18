import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

void issueUpdate(String sparqlUpdate)
{
	UpdateRequest request = UpdateFactory.create(sparqlUpdate);
	UpdateExecutionFactory
		.createRemote(request, "http://localhost:8089/parliament/sparql")
		.execute();
}
