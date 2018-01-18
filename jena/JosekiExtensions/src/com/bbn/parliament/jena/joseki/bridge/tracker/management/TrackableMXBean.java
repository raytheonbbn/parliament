package com.bbn.parliament.jena.joseki.bridge.tracker.management;

import java.util.Date;

import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableException;

public interface TrackableMXBean {

   public enum Status {
      CREATED, RUNNING, CANCELLED, FINISHED, ERROR
   }

   public void cancel() throws TrackableException;

   public long getId();

   public boolean isCancellable();

   public Status getStatus();

   public Date getCreatedTime();

   public Date getStartTime();

   public Date getFinishedTime();

   public String getDisplay();

   public String getCreator();

}
