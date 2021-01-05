package com.bbn.parliament.jena.query;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

public class DAWGManifestEntry {
	private final Resource entry;
	private File testDir;
	private String name;
	private File result;
	private File query;
	private final List<File> data;
	private final List<File> graphData;

	public DAWGManifestEntry(Resource resourceEntry) {
		entry = resourceEntry;
		testDir = null;
		name = null;
		result = null;
		query = null;
		data = new ArrayList<>();
		graphData = new ArrayList<>();
	}

	public void addQuerySolution(QuerySolution qs, File testDirectory) {
		testDir = testDirectory;
		Resource entryVar = qs.getResource("entry");
		if (entryVar == null) {
			throw new IllegalStateException("Entry URI is null in query result");
		} else if (!entryVar.equals(entry)) {
			throw new IllegalStateException("Entry URIs don't match");
		}
		String nameVar = getStringLiteral(qs, "name");
		if (name != null && !name.equals(nameVar)) {
			throw new IllegalStateException("Two names for one manifest entry");
		} else {
			name = nameVar;
		}
		File resultVar = getFileLiteral(qs, testDir, "result");
		if (result != null && !result.equals(resultVar)) {
			throw new IllegalStateException("Two names for one manifest entry");
		} else {
			result = resultVar;
		}
		File queryVar = getFileLiteral(qs, testDir, "query");
		if (query != null && !query.equals(queryVar)) {
			throw new IllegalStateException("Two names for one manifest entry");
		} else {
			query = queryVar;
		}
		File dataVar = getFileLiteral(qs, testDir, "data");
		if (dataVar != null) {
			data.add(dataVar);
		}
		File graphDataVar = getFileLiteral(qs, testDir, "graphData");
		if (graphDataVar != null) {
			graphData.add(graphDataVar);
		}
	}

	// This unusual implementation is due to sometimes-encoding of what should be
	// string literals as relative URIs in some of the DAWG manifest files.
	private static String getStringLiteral(QuerySolution qs, String varName) {
		RDFNode node = qs.get(varName);
		if (node == null) {
			return null;
		} else if (node.isLiteral()) {
			return node.asLiteral().getLexicalForm();
		} else if (node.isURIResource()) {
			return node.asResource().getLocalName();
		} else {
			throw new IllegalStateException("Result node has unrecognized type "
				+ node.getClass().getName());
		}
	}

	private static File getFileLiteral(QuerySolution qs, File testDir, String varName) {
		String fileName = getStringLiteral(qs, varName);
		return (fileName == null) ? null : new File(testDir, fileName);
	}

	public String getCurrentTest() {
		return String.format("%1$s/%2$s", testDir.getName(), name);
	}

	public String getName() {
		return name;
	}

	public File getResult() {
		return result;
	}

	public File getQuery() {
		return query;
	}

	public List<File> getData() {
		return data;
	}

	public List<File> getGraphData() {
		return graphData;
	}

	@Override
	public String toString() {
		return String.format("DAWG test '%1$s' (%2$s)", name, entry.getLocalName());
	}
}
