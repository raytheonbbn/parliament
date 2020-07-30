package com.bbn.parliament.spring.boot.service;


import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.handler.InsertHandler;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.ClearHandler;
import com.bbn.parliament.jena.bridge.ActionRouter;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("graphStoreService")
public class GraphStoreService {
	
	private static final Logger LOG = LoggerFactory.getLogger(QueryService.class);
	
	@Autowired
	ActionRouter actionRouter;

	public void doGet(String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//all encompassing construct on specified graph
			
			ExportHandler handler = new ExportHandler();
			handler.handleFormURLEncodedRequest(req, resp);
			
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	
	public void doPut(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//delete graph
			String sparqlStmt;
			
			sparqlStmt = (graphURI == null) ? "DROP DEFAULT ;" : String.format("DROP GRAPH <%1s> ;", graphURI);
			
			actionRouter.execUpdate(sparqlStmt, "Parliament-GraphStoreService");
			
			//set graph
			 doPost(contentType, graphURI, requestEntity, req, resp);
			
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	
	public void doDelete(String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//delete graph
			ClearHandler handler = new ClearHandler();
			handler.handleFormURLEncodedRequest(graphURI, req, resp);
			
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	
	public void doPost(String contentType, String graphURI, HttpEntity<byte[]> requestEntity, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(contentType, graphURI, req.getRemoteAddr(), requestEntity, resp);
			
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	
	public void doPatch(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
		} catch (Exception e) {
			LOG.info(e.toString());
		}
	}
	


}
