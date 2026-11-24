import org.apache.jena.sparql.exec.http.UpdateExecutionHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;

void issueUpdate(String sparqlUpdate)
{
	UpdateRequest request = UpdateFactory.create(sparqlUpdate);
	UpdateExecutionHTTP.service("http://localhost:8089/parliament/sparql")
		.update(request).build().execute();
}
