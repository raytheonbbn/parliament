// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.bridge.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.bridge.ParliamentBridge;
import com.bbn.parliament.jena.bridge.configuration.vocab.ConfigOnt;
import com.bbn.parliament.jena.graph.ModelManager;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.rulesys.GenericRuleReasoner;
import com.hp.hpl.jena.reasoner.rulesys.Rule;

/**
 * A {@link ConfigurationHandler} that adds Jena rules to a {@link Reasoner}.
 *
 * @author rbattle
 */
public class JenaRuleConfigurationHandler implements ReasonerConfigurationHandler {
	private static final Logger LOG = LoggerFactory.getLogger(JenaRuleConfigurationHandler.class);

	protected List<Rule> _rules;

	public JenaRuleConfigurationHandler() {
		_rules = new ArrayList<>();
	}

	/** {@inheritDoc} */
	@Override
	public void configure(Reasoner reasoner) throws ConfigurationException {
		if (_rules.size() == 0) {
			LOG.warn("No rules were specified.");
		} else if (reasoner instanceof GenericRuleReasoner) {
			LOG.info("Adding rules to existing reasoner");
			((GenericRuleReasoner) reasoner).addRules(_rules);
		} else {
			LOG.info("Reasoner is type {}", reasoner.getClass());
		}
	}

	public List<Rule> getRules() {
		return _rules;
	}

	/** {@inheritDoc} */
	@Override
	public void initialize(Resource handle) throws ConfigurationException {
		Property ruleFile = ResourceFactory.createProperty(ConfigOnt.NS + "ruleFile");
		if (handle.hasProperty(ruleFile)) {
			StmtIterator si = handle.listProperties(ruleFile);
			while (si.hasNext()) {
				String uri = si.nextStatement().getResource().getURI();
				StringBuilder sb = new StringBuilder();
				try (InputStream is = URI.create(uri).toURL().openStream()) {
					for (int i = -1; -1 != (i = is.read());) {
						sb.append((char) i);
					}
				} catch (IOException ex) {
					throw new ConfigurationException(ex);
				}
				_rules.addAll(Rule.parseRules(sb.toString()));
			}
		} else {
			LOG.warn("There are no rule files to load");
		}
	}

	/** {@inheritDoc} */
	@Override
	public void postModelInitialization(ParliamentBridge server, ModelManager manager) {
	}

	/** {@inheritDoc} */
	@Override
	public void preModelInitialization(ParliamentBridge server, ModelManager manager) {
		manager.addReasonerHandler(this);
	}
}
