package com.bbn.parliament.spring.boot.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bbn.parliament.jena.bridge.tracker.TrackableException;
import com.bbn.parliament.spring.boot.service.TrackerService;

@RestController
public class TrackerController {
	private static final String ENDPOINT = "/parliament/tracker";

	@Autowired
	TrackerService trackerService;

	//HEAD mapping automatically supported by GET mapping
	@GetMapping(value = ENDPOINT)
	public void getTrackables(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		trackerService.getTrackables(req, resp);
	}

	@PostMapping(value = ENDPOINT, params = "id")
	public void cancelTrackable(@RequestParam(value = "id") String id,
			HttpServletRequest res, HttpServletResponse resp) throws TrackableException {
		trackerService.cancelTrackable(id, res, resp);
	}
}
