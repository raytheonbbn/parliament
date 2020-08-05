package com.bbn.parliament.spring.boot.service;


import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.bbn.parliament.jena.bridge.ActionRouter;
import com.bbn.parliament.jena.exception.ArchiveException;
import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.InsertHandler;

@Component("graphStoreService")
public class GraphStoreService {

	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);

	@Autowired
	ActionRouter actionRouter;

	public void doGet(String graphURI, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, MissingGraphException, DataFormatException {
		ExportHandler handler = new ExportHandler();
		handler.handleFormURLEncodedRequest(req, resp);
	}

	public void doPut(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, Exception {
		doDelete(graphURI);
		doPost(contentType, graphURI, requestEntity, req, resp);
	}

	public void doDelete(String graphURI) throws Exception {
		String sparqlStmt;
		sparqlStmt = (graphURI == null) ? "DROP DEFAULT ;" : String.format("DROP GRAPH <%1s> ;", graphURI);
		actionRouter.execUpdate(sparqlStmt, "Parliament-GraphStoreService");
	}

	public void doPost(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, DataFormatException, MissingGraphException, ArchiveException {
		InsertHandler handler = new InsertHandler();
		handler.handleRequest(contentType, graphURI, req.getRemoteAddr(), requestEntity, resp);
	}

	public void doFilePost(String contentType, String graphURI, MultipartFile[] files, HttpServletRequest req, HttpServletResponse resp)
			throws IOException, DataFormatException, MissingGraphException, ArchiveException {
		InsertHandler handler = new InsertHandler();
		handler.handleFileRequest("auto", graphURI, req.getRemoteAddr(), files, resp);
	}
}
