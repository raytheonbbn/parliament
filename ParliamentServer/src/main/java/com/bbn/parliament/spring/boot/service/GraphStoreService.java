package com.bbn.parliament.spring.boot.service;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.handler.InsertHandler;
import com.bbn.parliament.jena.handler.ExportHandler;
import com.bbn.parliament.jena.handler.ClearHandler;
import com.bbn.parliament.jena.bridge.ActionRouter;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

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
	
	public void doPut(String graphURI, HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//delete graph
			String sparqlStmt;
			
			if (graphURI == null) {
				sparqlStmt = "DROP DEFAULT ;";
			}
			else {
				sparqlStmt = String.format("DROP GRAPH <%1s> ;", graphURI);	
			}
			actionRouter.execUpdate(sparqlStmt, "Parliament-GraphStoreService");
			
			//set graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
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
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		
		try {
			//update graph
			InsertHandler handler = new InsertHandler();
			handler.handleRequest(req, resp);
			
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
