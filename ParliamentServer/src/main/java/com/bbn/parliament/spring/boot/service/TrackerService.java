package com.bbn.parliament.spring.boot.service;

import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.tracker.TrackableDTO;
import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.bridge.tracker.Tracker;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("trackerService")
public class TrackerService {
	private static final ObjectMapper OBJ_MAPPER = new ObjectMapper();

	@SuppressWarnings("static-method")
	public StreamingResponseBody getTrackables() {
		Tracker tracker = Tracker.getInstance();
		List<TrackableDTO> trackables = tracker.getTrackableIDs().stream()
			.map(tracker::getTrackable)
			.filter(Objects::nonNull)
			.map(TrackableDTO::new)
			.collect(Collectors.toList());

		return outputStream -> OBJ_MAPPER.writeValue(outputStream, trackables);
	}

	@SuppressWarnings("static-method")
	public void cancelTrackable(String id) throws TrackableException {
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
