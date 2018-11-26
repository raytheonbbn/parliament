package com.bbn.parliament.jena.joseki.bridge.tracker;

import java.util.Calendar;
import java.util.Date;

public class TrackableDTO {
	private long id;
	private boolean cancellable;
	private String status;
	private String display;
	private long currentTime;
	private long created;
	private Long started;
	private String creator;

	public TrackableDTO() {}

	public TrackableDTO(Trackable source) {
		this.id = source.getId();
		this.cancellable = source.isCancellable();
		this.status = source.getStatus().toString();
		this.display = source.getDisplay();
		this.currentTime = getCurrentDateAsLong();
		this.created = convertDateToLong(source.getCreatedTime(), false);
		this.started = convertDateToLong(source.getStartTime(), true);
		this.creator = source.getCreator();
	}

	private static Long getCurrentDateAsLong() {
		return convertDateToLong(null, false);
	}

	private static Long convertDateToLong(Date date, boolean returnNull) {
		Long timeInMillis = null;
		if (date != null || !returnNull) {
			Calendar calendar = Calendar.getInstance();
			if (date != null) {
				calendar.setTime(date);
			}
			timeInMillis = calendar.getTimeInMillis();
		}
		return timeInMillis;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public boolean isCancellable() {
		return cancellable;
	}

	public void setCancellable(boolean cancellable) {
		this.cancellable = cancellable;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public long getCurrentTime() {
		return currentTime;
	}

	public void setCurrentTime(long currentTime) {
		this.currentTime = currentTime;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public long getStarted() {
		return started;
	}

	public void setStarted(long started) {
		this.started = started;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(String creator) {
		this.creator = creator;
	}
}
