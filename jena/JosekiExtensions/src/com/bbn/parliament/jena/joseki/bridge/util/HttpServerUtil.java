// Parliament is licensed under the BSD License from the Open Source
// Initiative, http://www.opensource.org/licenses/bsd-license.php
//
// Copyright (c) 2001-2009, BBN Technologies, Inc.
// All rights reserved.
package com.bbn.parliament.jena.joseki.bridge.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * Utility methods for HTTP servers/servlets.
 *
 * Taken from Sesame 1.2.6 with a few modifications.
 *
 * @author sallen
 **/
public class HttpServerUtil {

	private static DiskFileItemFactory _fileItemFactory = null;
	private static ServletFileUpload _fileUpload = null;

	private static File _tmpDir = null;
	private static int _threshold = -1;

	private static ServletFileUpload _getServletFileUpload() {

		if (null == _tmpDir) {
			throw new RuntimeException("HttpServerUtil must be initialized before attempting to create a ServletFileUpload class");
		}

		if (_fileUpload == null) {
			_fileItemFactory = new DiskFileItemFactory(_threshold, _tmpDir);

			// We will handle removing any temporary files manually
			_fileItemFactory.setFileCleaningTracker(null);

			_fileUpload = new ServletFileUpload(_fileItemFactory);
		}

		return _fileUpload;
	}

	public static void init(File tmpDir, int threshold) {
		_tmpDir = tmpDir;
		_threshold = threshold;
	}

	/**
	 * Checks whether the supplied request is a <tt>multipart/form-data</tt>
	 * POST request.
	 **/
	public static boolean isMultipartContent(HttpServletRequest request) {
		return ServletFileUpload.isMultipartContent(request);
	}

	/**
	 * Gets the trimmed value of a request parameter as a String. If the
	 * specified parameter does not exist or if it has a white-space-only value,
	 * <tt>null</tt> will be returned.
	 *
	 * @param request The request object to get the parameter from.
	 * @param paramName The name of the parameter.
	 * @return The trimmed value, or <tt>null</tt> if the specified parameter
	 * does not have a value.
	 **/
	public static String getParameter(HttpServletRequest request, String paramName) {
		return getParameter(request, paramName, null);
	}

	/**
	 * Gets the trimmed value of a request parameter as a String. If the
	 * specified parameter does not exist or if it has a white-space-only value,
	 * the supplied <tt>defaultValue</tt> will be returned.
	 *
	 * @param request The request object to get the parameter from.
	 * @param paramName The name of the parameter.
	 * @param defaultValue The value that should be returned when the specified
	 * parameter did not have a value.
	 * @return The trimmed value, or <tt>defaultValue</tt> if the specified
	 * parameter does not have a value.
	 **/
	public static String getParameter(HttpServletRequest request, String paramName, String defaultValue) {
		String result = request.getParameter(paramName);

		if (result == null) {
			result = defaultValue;
		}
		else {
			result = result.trim();

			if (result.length() == 0) {
				result = defaultValue;
			}
		}

		return result;
	}

	/**
	 * Parses a <tt>multipart/form-data</tt> POST request and returns its
	 * parameters as set of FileItems, mapped using their field name.
	 *
	 * @param request The request.
	 * @return a Map of FileItem objects, mapped using their field name (Strings).
	 * @exception IOException If the request could not be read/parsed.
	 **/
	public static Map<String,FileItem> parseMultipartFormRequest(HttpServletRequest request)
		throws IOException
		{
		try {
			List<FileItem> fileItems = _getServletFileUpload().parseRequest(request);
			Map<String, FileItem> resultMap = new HashMap<>(fileItems.size() * 2);

			for (FileItem fileItem : fileItems) {
				resultMap.put(fileItem.getFieldName(), fileItem);
			}

			return resultMap;
		}
		catch (FileUploadException e) {
			throw new IOException(e);
		}
		}

	/**
	 * Gets the trimmed value of a request parameter from a Map of FileItem
	 * objects, as returned by <tt>parseMultipartFormRequest()</tt>. If the
	 * specified parameter does not exist or if it has a white-space-only value,
	 * <tt>null</tt> will be returned. The values are assumed to be using the
	 * UTF-8 encoding.
	 *
	 * @param fileItemMap A Map of FileItem objects, mapped using their field
	 * name (Strings).
	 * @param paramName The name of the parameter.
	 * @return The trimmed value, or <tt>null</tt> if the specified parameter
	 * does not have a value.
	 **/
	public static String getParameter(Map<String, FileItem> fileItemMap, String paramName) {
		return getParameter(fileItemMap, paramName, null);
	}

	/**
	 * Gets the trimmed value of a request parameter from a Map of FileItem
	 * objects, as returned by <tt>parseMultipartFormRequest()</tt>. If the
	 * specified parameter does not exist or if it has a white-space-only value,
	 * the supplied <tt>defaultValue</tt> will be returned. The values are
	 * assumed to be using the UTF-8 encoding.
	 *
	 * @param fileItemMap A Map of FileItem objects, mapped using their field
	 * name (Strings).
	 * @param paramName The name of the parameter.
	 * @param defaultValue The value that should be returned when the specified
	 * parameter did not have a value.
	 * @return The trimmed value, or <tt>null</tt> if the specified parameter
	 * does not have a value.
	 **/
	public static String getParameter(Map<String, FileItem> fileItemMap, String paramName, String defaultValue) {
		String result = defaultValue;

		FileItem fileItem = fileItemMap.get(paramName);
		if (fileItem != null) {
			try {
				result = fileItem.getString("UTF-8").trim();
			}
			catch (UnsupportedEncodingException e) {
				// UTF-8 must be supported by all compliant JVM's,
				// this exception should never be thrown.
				throw new RuntimeException("UTF-8 character encoding not supported on this platform", e);
			}

			if (result.length() == 0) {
				result = defaultValue;
			}
		}

		return result;
	}

	/**
	 * Gets the binary value of a request parameter from a Map of FileItem
	 * objects, as returned by <tt>parseMultipartFormRequest()</tt>.
	 *
	 * @param fileItemMap A Map of FileItem objects, mapped using their field
	 * name (Strings).
	 * @param paramName The name of the parameter.
	 * @return The binary value, or <tt>null</tt> if the specified parameter
	 * does not have a value.
	 **/
	public static byte[] getBinaryParameter(Map<String, FileItem> fileItemMap, String paramName) {
		byte[] result = null;

		FileItem fileItem = fileItemMap.get(paramName);
		if (fileItem != null) {
			result = fileItem.get();
		}

		return result;
	}

	/**
	 * Gets the value of a request parameter as an InputStream from a Map of
	 * FileItem objects, as returned by <tt>parseMultipartFormRequest()</tt>.
	 *
	 * @param fileItemMap A Map of FileItem objects, mapped using their field
	 * name (Strings).
	 * @param paramName The name of the parameter.
	 * @return The value, or <tt>null</tt> if the specified parameter does not
	 * have a value.
	 **/
	public static InputStream getStreamParameter(Map<String, FileItem> fileItemMap, String paramName)
		throws IOException
		{
		InputStream result = null;

		FileItem fileItem = fileItemMap.get(paramName);
		if (fileItem != null) {
			result = fileItem.getInputStream();
		}

		return result;
		}

	/**
	 * Sets headers on the supplied response that prevent all kinds of
	 * browsers to cache it.
	 **/
	public static void setNoCacheHeaders(HttpServletResponse response) {
		//
		// according to http://vancouver-webpages.com/META/FAQ.html
		//
		response.setHeader("Cache-Control", "must-revalidate");
		response.setHeader("Cache-Control", "max-age=1");
		response.setHeader("Cache-Control", "no-cache");
		response.setIntHeader("Expires", 0);
		response.setHeader("Pragma", "no-cache");
	}

	/**
	 * Checks whether the sender of the supplied request can handle
	 * gzip-encoded data.
	 *
	 * @param request A HTTP request
	 * @return <tt>true</tt> if the sender of the request can handle
	 * gzip-encoded data, <tt>false</tt> otherwise.
	 **/
	public static boolean acceptsGZIPEncoding(HttpServletRequest request) {
		boolean result = false;

		String acceptEncoding = request.getHeader("accept-encoding");
		if (acceptEncoding != null) {
			// Microsoft Internet Explorer indicates that it accepts gzip
			// encoding, but it fails to handle it properly (see issue
			// [SES-28] in Sesame issue tracker).
			boolean isInternetExplorer = false;

			String userAgent = request.getHeader("user-agent");
			if (userAgent != null) {
				isInternetExplorer = userAgent.indexOf("MSIE") != -1;
			}

			result = isInternetExplorer == false &&
				acceptEncoding.toLowerCase().indexOf("gzip") != -1;
		}

		return result;
	}

	/**
	 * Opens an gzip-encoded output stream for the specified response. This
	 * method also sets the required header(s) to indicate that the response
	 * is gzip-encoded.
	 **/
	public static OutputStream openGZIPOutputStream(HttpServletResponse response)
		throws IOException
		{
		response.setHeader("Content-Encoding", "gzip");
		return new GZIPOutputStream(response.getOutputStream(), 4096);
		}
}
