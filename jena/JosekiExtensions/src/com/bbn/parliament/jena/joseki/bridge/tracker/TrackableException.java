package com.bbn.parliament.jena.joseki.bridge.tracker;

public class TrackableException extends Exception {

   private static final long serialVersionUID = 4301590416097682579L;


   public TrackableException(String message) {
      super(message);
   }

   public TrackableException(Throwable cause) {
      super(cause);
   }

   public TrackableException(String message, Throwable cause) {
      super(message, cause);
   }

}
