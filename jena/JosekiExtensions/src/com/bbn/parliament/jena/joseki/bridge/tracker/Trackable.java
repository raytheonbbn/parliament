package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.beans.ConstructorProperties;
import java.util.Calendar;
import java.util.Date;
import java.util.Observable;

import com.bbn.parliament.jena.joseki.bridge.tracker.management.TrackableMXBean;
import com.bbn.parliament.jena.joseki.bridge.util.NTriplesUtil;

public abstract class Trackable extends Observable implements TrackableMXBean, Comparable<Trackable>  {
   protected final long _id;
   protected final String _creator;
   protected final Date _createdTime;
   protected Date _finishedTime;
   protected Date _startTime;
   protected Status _status;
   protected boolean _setFinishedOnRun;

   @ConstructorProperties({"id"})
   Trackable(long id, String creator) {
      _id = id;
      _creator = creator;
      _createdTime = Calendar.getInstance().getTime();
      _status = Status.CREATED;
      _setFinishedOnRun = true;
   }

   @Override
	public String getCreator() {
      return _creator;
   }

   @Override
	public long getId() {
      return _id;
   }

   protected abstract void doCancel() throws TrackableException;
   protected abstract void doRun() throws TrackableException;
   protected abstract void release();

   @Override
	public void cancel() throws TrackableException {
      if(isCancellable()) {
         doCancel();
         _finishedTime = Calendar.getInstance().getTime();
         setStatus(Status.CANCELLED);
      }
   }

   public void run() throws TrackableException {
      synchronized (_status) {
         if (!Status.CREATED.equals(_status)) {
            // cannot rerun something
            return;
         }
      }
      setStarted();
      if (!Tracker.getInstance().isShuttingDown()) {
         try {
            doRun();
         } catch (TrackableException e) {
            setError();
            throw e;
         } catch (Exception e) {
            setError();
            throw new TrackableException(e);
         }
      }
      if (_setFinishedOnRun) {
         setFinished();
      }
   }

   public boolean isError() {
      synchronized (_status) {
         return Status.ERROR.equals(_status);
      }
   }

   public boolean isFinished() {
      synchronized (_status) {
         return Status.FINISHED.equals(_status);
      }
   }

   public boolean isRunning() {
      synchronized (_status) {
         return Status.RUNNING.equals(_status);
      }
   }

   public boolean isCancelled() {
      synchronized (_status) {
         return Status.CANCELLED.equals(_status);
      }
   }
   protected void setError() {
      setStatus(Status.ERROR);
   }

   protected void setStarted() {
      _startTime = Calendar.getInstance().getTime();
      setStatus(Status.RUNNING);
   }

   protected void setFinished() {
      _finishedTime = Calendar.getInstance().getTime();
      setStatus(Status.FINISHED);
   }

   protected void setCancelled() {
      setStatus(Status.CANCELLED);
   }

   protected void setStatus(Status status) {
      synchronized(_status) {
         _status = status;
         setChanged();
      }
      notifyObservers(_status);
   }

   @Override
	public Status getStatus() {
      synchronized(_status) {
         return _status;
      }
   }

   @Override
	public Date getCreatedTime() {
      return _createdTime;
   }

   @Override
	public Date getFinishedTime() {
      return _finishedTime;
   }

   @Override
	public Date getStartTime() {
      return _startTime;
   }

   @Override
	public int compareTo(Trackable o) {
      return this.getCreatedTime().compareTo(o.getCreatedTime());
   }
}
