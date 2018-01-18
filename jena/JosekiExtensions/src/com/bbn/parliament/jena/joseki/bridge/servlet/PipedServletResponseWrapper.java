// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.

package com.bbn.parliament.jena.joseki.bridge.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author sallen */
public class PipedServletResponseWrapper extends HttpServletResponseWrapper {
	private static final Logger log = LoggerFactory.getLogger(PipedServletResponseWrapper.class);

	private PipedInputStream _in;
	private PipedOutputStream _out;

	private Thread _thread;

	public PipedServletResponseWrapper(HttpServletResponse response) throws IOException {
		super(response);

		_in = new PipedInputStream();
		_out = new PipedOutputStream(_in);
	}

	public void run(Runnable task) {
		// TODO: Use the java.util.concurrent Executors instead of creating a new Thread
		_thread = new Thread(task);
		_thread.start();
	}

	public void join() throws InterruptedException {
		_thread.join();
	}

	public InputStream getInputStream() {
		return _in;
	}

	@Override
	public PrintWriter getWriter() {
		return new PrintWriter(_out);
	}

	@Override
	public ServletOutputStream getOutputStream() {
		return new ServletOutputStream()
		{
			@Override
			public void write(int c) throws IOException {
				_out.write(c);
			}

			@Override
			public void close() throws IOException {
				_out.close();
			}

			@Override
			public boolean isReady() {
				//TODO: This needs to be implemented
				log.warn("Unimplemented method isReady called");
				return true;
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
				//TODO: This needs to be implemented
				log.warn("Unimplemented method setWriteListener called");
			}
		};
	}

	@Override
	public void flushBuffer() {
		try {
			_out.flush();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		return "PipedServletResponseWrapper";
	}
}
