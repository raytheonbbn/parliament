package com.bbn.parliament.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.jena.atlas.web.ContentType;

public class MultiPartBodyPublisherBuilder {
	private static abstract class Part {
		protected static final ContentType DEFAULT_CONTENT_TYPE = ContentType.create("application/octet-stream");

		public final String name;

		public Part(String name) {
			this.name = name;
		}
	}

	private static final class FinalPart extends Part {
		public FinalPart() {
			super(null);
		}
	}

	private static final class StringPart extends Part {
		public final String value;

		public StringPart(String name, String value) {
			super(name);
			this.value = value;
		}
	}

	private static final class FilePart extends Part {
		public final File value;
		public final ContentType contentType;

		public FilePart(String name, File value, ContentType contentType) {
			super(name);
			this.value = value;
			this.contentType = (contentType == null) ? DEFAULT_CONTENT_TYPE : contentType;
		}
	}

	private static final class StreamPart extends Part {
		public final Supplier<InputStream> value;
		public final String fileName;
		public final ContentType contentType;

		public StreamPart(String name, Supplier<InputStream> value, String fileName, ContentType contentType) {
			super(name);
			this.value = value;
			this.fileName = fileName;
			this.contentType = (contentType == null) ? DEFAULT_CONTENT_TYPE : contentType;
		}
	}

	private static final int BUFFER_SIZE = 8192;

	private final String boundary;
	private final List<Part> parts;

	public MultiPartBodyPublisherBuilder() {
		boundary = "%1$s-%2$s"
			.formatted(UUID.randomUUID(), UUID.randomUUID())
			.substring(0, 65);	// max boundary length is 70 chars
		parts = new ArrayList<>();
	}

	public String getRequestContentType() {
		return "multipart/form-data; boundary=%1$s".formatted(boundary);
	}

	public HttpRequest.BodyPublisher build() {
		return HttpRequest.BodyPublishers.ofByteArrays(createPartIterator());
	}

	/** Exposed to the whole package to allow easier testing */
	Iterable<byte[]> createPartIterator() {
		if (parts.size() == 0) {
			throw new IllegalStateException("Must have at least one part to build multipart message.");
		}
		parts.add(new FinalPart());	// Add the final boundary part
		return () -> new PartIterator(parts, boundary);
	}

	public MultiPartBodyPublisherBuilder addPart(String name, String value) {
		parts.add(new StringPart(name, value));
		return this;
	}

	public MultiPartBodyPublisherBuilder addPart(String name, File value) throws IOException {
		parts.add(new FilePart(name, value, ContentType.create(Files.probeContentType(value.toPath()))));
		return this;
	}

	public MultiPartBodyPublisherBuilder addPart(String name, File value, ContentType contentType) {
		parts.add(new FilePart(name, value, contentType));
		return this;
	}

	public MultiPartBodyPublisherBuilder addPart(String name, File value, Function<File, ContentType> contentTypeDeducer) {
		parts.add(new FilePart(name, value, contentTypeDeducer.apply(value)));
		return this;
	}

	public MultiPartBodyPublisherBuilder addPart(String name, Supplier<InputStream> value, String fileName, ContentType contentType) {
		parts.add(new StreamPart(name, value, fileName, contentType));
		return this;
	}

	private static class PartIterator implements Iterator<byte[]> {
		private final Iterator<Part> iter;
		private final String boundary;
		private InputStream currentInputStream;

		private boolean done;
		private byte[] next;

		public PartIterator(Iterable<Part> iterable, String boundary) {
			iter = iterable.iterator();
			this.boundary = boundary;
			currentInputStream = null;
			done = false;
			next = null;
		}

		@Override
		public boolean hasNext() {
			if (done) {
				return false;
			}
			if (next != null) {
				return true;
			}
			try {
				next = computeNext();
			} catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			if (next == null) {
				done = true;
			}
			return !done;
		}

		@Override
		public byte[] next() {
			if (!hasNext()) {
				throw new NoSuchElementException();
			}
			byte[] res = next;
			next = null;
			return res;
		}

		private byte[] computeNext() throws IOException {
			if (currentInputStream == null) {
				if (!iter.hasNext()) {
					return null;
				}
				Part nextPart = iter.next();
				if (nextPart instanceof StringPart nextStringPart) {
					return formatBytes("""
						--%1$s
						Content-Disposition: form-data; name=%2$s
						Content-Type: text/plain; charset=UTF-8

						%3$s
						""", boundary, nextStringPart.name, nextStringPart.value);
				} else if (nextPart instanceof FinalPart) {
					return formatBytes("--%1$s--", boundary);
				} else {
					String fileName;
					ContentType contentType;
					if (nextPart instanceof FilePart nextFilePart) {
						File file = nextFilePart.value;
						fileName = file.getName();
						contentType = nextFilePart.contentType;
						currentInputStream = Files.newInputStream(file.toPath());
					} else if (nextPart instanceof StreamPart nextStreamPart) {
						fileName = nextStreamPart.fileName;
						contentType = nextStreamPart.contentType;
						currentInputStream = nextStreamPart.value.get();
					} else {
						throw new IllegalStateException("Unrecognized part type '%1$s'"
							.formatted(nextPart.getClass().getName()));
					}
					return formatBytes("""
						--%1$s
						Content-Disposition: form-data; name=%2$s; filename=%3$s
						Content-Type: %4$s

						""", boundary, nextPart.name, fileName, contentType.getContentTypeStr());
				}
			} else {
				byte[] buffer = new byte[BUFFER_SIZE];
				int numBytesRead = currentInputStream.read(buffer);
				if (numBytesRead == BUFFER_SIZE) {
					return buffer;
				} else if (numBytesRead > 0) {
					return Arrays.copyOf(buffer, numBytesRead);
				} else {
					currentInputStream.close();
					currentInputStream = null;
					return formatBytes("\n");
				}
			}
		}

		private static byte[] formatBytes(String fmtStr, Object... args) {
			return fmtStr
				.replaceAll("\n", "\r\n")
				.formatted(args)
				.getBytes(StandardCharsets.UTF_8);
		}
	}
}
