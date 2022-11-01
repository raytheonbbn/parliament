package com.bbn.parliament.spring_boot.controller;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.spring_boot.service.TrackerService;

@RestController
public class TrackerController {
	private static final String ENDPOINT = "/parliament/tracker";

	private final TrackerService trackerService;

	@Autowired
	public TrackerController(TrackerService service) {
		trackerService = Objects.requireNonNull(service, "service");
	}

	@GetMapping(value = ENDPOINT, produces = "application/json")
	public StreamingResponseBody getTrackables() {
		return trackerService.getTrackables();
	}

	@PostMapping(value = ENDPOINT, params = "id")
	public void cancelTrackable(@RequestParam(value = "id") String id) throws TrackableException {
		trackerService.cancelTrackable(id);
	}
}
