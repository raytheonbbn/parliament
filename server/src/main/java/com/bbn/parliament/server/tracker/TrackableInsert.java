package com.bbn.parliament.server.tracker;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.bbn.parliament.server.exception.DataFormatException;
import com.bbn.parliament.server.exception.MissingGraphException;
import com.bbn.parliament.server.handler.Inserter;

public class TrackableInsert extends Trackable {
	private List<Inserter> inserters;
	private String display;

	@ConstructorProperties({"id", "inserter"})
	TrackableInsert(long id, List<Inserter> inserters, String creator) {
		super(id, creator);
		this.inserters = inserters;
		display = inserters.stream()
			.map(TrackableInsert::createDisplayString)
			.collect(Collectors.joining("; "));
	}

	private static String createDisplayString(Inserter inserter) {
		if (inserter.isImport()) {
			return "Import repository";
		} else if (null == inserter.getFileName()) {
			return "Insert data from form";
		} else {
			return "Insert data from " + inserter.getFileName();
		}
	}

	@Override
	protected void doCancel() {
	}

	@Override
	protected void doRun() throws IOException, DataFormatException, MissingGraphException {
		for (Inserter inserter : inserters) {
			inserter.run();
		}
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
		return display;
	}

	public long getNumStatements() {
		return inserters.stream()
			.collect(Collectors.summingLong(Inserter::getNumStatements));
	}
}
