package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.bbn.parliament.jena.joseki.bridge.tracker.Trackable;
import com.bbn.parliament.jena.joseki.bridge.tracker.TrackableException;
import com.bbn.parliament.jena.joseki.bridge.tracker.Tracker;

public class TrackerServlet extends HttpServlet {
	private static final long serialVersionUID = -7345801993380970907L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException {
		List<Long> ids = Tracker.getInstance().getTrackableIDs();
		StringBuilder json = new StringBuilder();
		json.append("[\n");
		for (long id : ids) {
			Trackable trackable = Tracker.getInstance().getTrackable(id);
			// make sure trackable still exists
			if (trackable == null) {
				continue;
			}
			json.append(trackable.toJSON());
			if (ids.indexOf(id) < ids.size() - 1) {
				json.append(",");
			}
			json.append("\n");
		}
		json.append("]");
		resp.setHeader("Content-Type", "application/json");
		resp.getOutputStream().println(json.toString());
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
