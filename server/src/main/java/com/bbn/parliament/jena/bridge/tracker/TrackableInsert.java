package com.bbn.parliament.jena.bridge.tracker;

import java.beans.ConstructorProperties;
import java.io.IOException;

import com.bbn.parliament.jena.exception.DataFormatException;
import com.bbn.parliament.jena.exception.MissingGraphException;
import com.bbn.parliament.jena.handler.Inserter;

public class TrackableInsert extends Trackable {
	private Inserter _inserter;
	private String _display;

	@ConstructorProperties({"id", "inserter"})
	TrackableInsert(long id, Inserter inserter, String creator) {
		super(id, creator);
		_inserter = inserter;
		if (_inserter.isImport()) {
			_display = "Import repository";
		} else {
			_display = "Insert data from ";
			if (null == _inserter.getFileName()) {
				_display += "form";
			} else {
				_display += _inserter.getFileName();
			}
		}
	}

	@Override
	protected void doCancel() {
	}

	@Override
	protected void doRun() throws IOException, DataFormatException, MissingGraphException {
		_inserter.run();
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
		return _display;
	}

	public Inserter getInserter() {
		return _inserter;
	}
}
