package com.bbn.parliament.jena.graph.index.spatial.sql.postgres;

import java.util.Properties;

import org.junit.After;
import org.junit.Ignore;

import com.bbn.parliament.jena.graph.index.spatial.AbstractThreadTest;
import com.bbn.parliament.jena.graph.index.spatial.sql.PersistentStore;

/** @author rbattle */
@Ignore
public class PostgresThreadTest extends AbstractThreadTest {
	@SuppressWarnings("static-method")
	@After
	public void asdf() {
		PersistentStore ps =  PersistentStore.getInstance();
		System.out.println(String.format("%d active, %d idle", ps.getNumActive(), ps.getNumIdle()));
	}

	/** {@inheritDoc} */
	@Override
	protected Properties getProperties() {
		return PostgresPropertyFactory.create();
	}
}
