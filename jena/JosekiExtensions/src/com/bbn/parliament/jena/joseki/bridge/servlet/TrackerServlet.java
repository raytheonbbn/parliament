package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableDTO;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TrackerServlet extends HttpServlet {
	private static final long serialVersionUID = -7345801993380970907L;
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		Tracker tracker = Tracker.getInstance();

		List<TrackableDTO> trackables = tracker.getTrackableIDs()
			.stream()
			.map(tracker::getTrackable)
			.filter(Objects::nonNull)
			.map(TrackableDTO::new)
			.collect(Collectors.toList());

		resp.setHeader("Content-Type", "application/json");
		@SuppressWarnings("resource")
		OutputStream responseBody = resp.getOutputStream();
		MAPPER.writeValue(responseBody, trackables);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		String trackerNumParam = req.getParameter("id");
		if (null != trackerNumParam) {
			cancelQuery(trackerNumParam);
		}
	}

	private static void cancelQuery(String trackerNumParam) throws ServletException {
		long id = Long.parseLong(trackerNumParam);
		try {
			Tracker.getInstance().cancel(id);
		}
		catch(TrackableException e) {
			throw new ServletException(e);
		}
	}
}
