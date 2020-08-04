package com.bbn.parliament.spring.boot.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import com.bbn.parliament.jena.bridge.tracker.TrackableDTO;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("trackerService")
public class TrackerService {
	private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();

	@SuppressWarnings("static-method")
	public void getTrackables(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		Tracker tracker = Tracker.getInstance();
		List<TrackableDTO> trackables = tracker.getTrackableIDs().stream()
			.map(tracker::getTrackable)
			.filter(Objects::nonNull)
			.map(TrackableDTO::new)
			.collect(Collectors.toList());

		resp.setHeader("Content-Type", "application/json");
		@SuppressWarnings("resource")
		OutputStream responseBody = resp.getOutputStream();
		OBJ_MAPPER.writeValue(responseBody, trackables);
	}

	@SuppressWarnings("static-method")
	public void cancelTrackable(String id, HttpServletRequest res, HttpServletResponse resp) throws TrackableException {
		OptionalLong idNum = parseLongString(id);
		if (idNum.isPresent()) {
			Tracker.getInstance().cancel(idNum.getAsLong());
		}
	}

	private static OptionalLong parseLongString(String longStr) {
		if (longStr == null) {
			return OptionalLong.empty();
		}
		try {
			return OptionalLong.of(Long.parseLong(longStr));
		} catch (NumberFormatException ex) {
			return OptionalLong.empty();
		}
	}
}
