import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.exec.http.QueryExecutionHTTP;

void issueSelectQuery(String sparqlSelectQuery) {
	try (QueryExecution exec = QueryExecutionHTTP.newBuilder()
			.endpoint("http://localhost:8089/parliament/sparql")
			.sendMode(QuerySendMode.asPostForm)
			.query(sparqlSelectQuery)
			.build()) {
		for (ResultSet rs = exec.execSelect(); rs.hasNext();) {
			QuerySolution qs = rs.next();
			// Do something useful with the query solution here
		}
	}
}
