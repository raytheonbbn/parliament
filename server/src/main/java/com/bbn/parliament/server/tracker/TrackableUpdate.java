package com.bbn.parliament.server.tracker;

import java.beans.ConstructorProperties;

import org.apache.jena.update.UpdateAction;

import com.bbn.parliament.server.exception.TrackableException;
import com.bbn.parliament.server.graph.ModelManager;

public class TrackableUpdate extends Trackable {
	private String updateStatement;

	@ConstructorProperties({"id", "query", "creator"})
	TrackableUpdate(long id, String query, String creator) {
		super(id, creator);
		updateStatement = query;
	}

	public String getQuery() {
		return updateStatement;
	}

	@Override
	protected void doCancel() {
	}

	@Override
	protected void doRun() throws TrackableException {
		UpdateAction.parseExecute(updateStatement, ModelManager.inst().getDataset());
	}

	@Override
	public boolean isCancellable() {
		return false;
	}

	@Override
	public void release() {
	}

	@Override
	public String getDisplay() {
		return updateStatement;
	}
}
