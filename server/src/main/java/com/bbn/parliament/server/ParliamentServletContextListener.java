package com.bbn.parliament.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

public class ParliamentServletContextListener implements ServletContextListener {
	private static final Logger LOG = LoggerFactory.getLogger(ParliamentServletContextListener.class);

	public ParliamentServletContextListener() {
		LOG.info("ParliamentServletContextListener created");
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		LOG.info("contextInitialized");
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		LOG.info("Shutting down parliament servlet");
		ParliamentBridge.getInstance().stop();
	}
}
