import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

void issueSelectQuery(String sparqlSelectQuery) {
	try (QueryExecution exec = QueryExecutionFactory.sparqlService(
			"http://localhost:8089/parliament/sparql",
			sparqlSelectQuery)) {
		for (ResultSet rs = exec.execSelect(); rs.hasNext();) {
			QuerySolution qs = rs.next();
			// Do something useful with the query solution set here
		}
	}
}
