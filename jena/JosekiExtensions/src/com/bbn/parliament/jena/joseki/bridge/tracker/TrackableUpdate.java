package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.beans.ConstructorProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bbn.parliament.jena.joseki.graph.ModelManager;

import com.hp.hpl.jena.update.UpdateAction;

public class TrackableUpdate extends Trackable {
   private static Logger _log = LoggerFactory.getLogger(TrackableUpdate.class);
   private String _query;

   @ConstructorProperties({"id", "query", "creator"})
   TrackableUpdate(long id, String query, String creator) {
      super(id, creator);
      _query = query;
   }

   public String getQuery() {
      return _query;
   }

   @Override
   protected void doCancel() {

   }

   @Override
   protected void doRun() throws TrackableException {
      _log.debug("UPDATE QUERY: \n{}", _query);
      _log.debug("OK/Update");

      UpdateAction.parseExecute(_query, ModelManager.inst().getDataset());

      _log.debug("UPDATE complete.");
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
      return _query;
   }
}
