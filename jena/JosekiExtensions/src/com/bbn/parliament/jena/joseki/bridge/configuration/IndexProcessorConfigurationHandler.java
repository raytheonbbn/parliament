// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.joseki.bridge.configuration;

import java.net.URI;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.graph.index.IndexFactory;
import com.bbn.parliament.jena.graph.index.IndexFactoryRegistry;
import com.bbn.parliament.jena.joseki.bridge.ParliamentBridge;
import com.bbn.parliament.jena.joseki.bridge.configuration.vocab.ConfigOnt;
import com.bbn.parliament.jena.joseki.graph.ModelManager;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * A {@link ConfigurationHandler} that registers a list of {@link IndexFactory}s
 * to support the runtime creation of indexes on a graph.
 *
 * @author rbattle
 */
public class IndexProcessorConfigurationHandler implements ConfigurationHandler {
	private static Logger log = LoggerFactory.getLogger(IndexProcessorConfigurationHandler.class);

	/** {@inheritDoc} */
	@Override
	public void initialize(Resource handle) throws ConfigurationException {
		StmtIterator si = handle.listProperties(ConfigOnt.index);
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			Resource index = s.getResource();
			Statement indexClass = index.getProperty(ConfigOnt.processorFactoryClass);
			String className = indexClass.getString();
			Class<?> uclass;
			@SuppressWarnings("rawtypes")
			Class<? extends IndexFactory> factoryClass;
			IndexFactory<?, ?> factory = null;
			log.info("Loading {}", className);
			try {
				uclass = Class.forName(className);
			} catch (ClassNotFoundException e) {
				log.warn("Could not find class {}", className);
				continue;
			} catch (RuntimeException e) {
				log.error("Error while loading " + className, e);
				continue;
			}
			if (!IndexFactory.class.isAssignableFrom(uclass)) {
				log.warn("{} is not a valid IndexProcessor", className);
				continue;
			}
			factoryClass = uclass.asSubclass(IndexFactory.class);
			try {
				factory = factoryClass.newInstance();
			} catch (InstantiationException e) {
				log.error("Error while instantiating " + className, e);
				continue;
			} catch (IllegalAccessException e) {
				log.error("Error while accessing " + className, e);
				continue;
			}
			Properties properties = createProperties(index);
			factory.configure(properties);
			IndexFactoryRegistry.getInstance().register(factory);
		}
		si = handle.listProperties(ConfigOnt.enabledByDefault);
		boolean enabledByDefault = true;
		while (si.hasNext()) {
			Statement s = si.nextStatement();
			if (s.getObject().isLiteral()) {
				enabledByDefault = s.getBoolean();
			}
		}
		IndexFactoryRegistry.getInstance().setIndexingEnabledByDefault(enabledByDefault);
	}

	private static Properties createProperties(Resource configuration) {
		Properties p = new Properties();
		StmtIterator si = configuration.listProperties();
		while (si.hasNext()) {
			Statement s = si.next();
			if (!s.getObject().isLiteral()) {
				continue;
			}
			String uri = s.getPredicate().getURI();

			Literal l = s.getObject().asLiteral();

			String value = l.getValue().toString();
			URI u = URI.create(uri);
			String prop = u.getFragment();
			p.put(prop, value);
		}

		return p;
	}

	/** {@inheritDoc} */
	@Override
	public void postModelInitialization(ParliamentBridge server, ModelManager manager) {
	}

	/** {@inheritDoc} */
	@Override
	public void preModelInitialization(ParliamentBridge server, ModelManager manager) {
	}
}