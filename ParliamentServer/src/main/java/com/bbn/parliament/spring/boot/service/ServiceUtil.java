package com.bbn.parliament.spring.boot.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

final class ServiceUtil {
	private ServiceUtil() {}	// prevent instantiation

	public static List<AcceptableMediaType> getAcceptList(String format, HttpHeaders headers) {
		Optional<AcceptableMediaType> formatType = AcceptableMediaType.find(format);
		Stream<Optional<AcceptableMediaType>> streamFromAccept = headers.getAccept().stream()
			.sorted(Comparator.comparingDouble(mt -> - mt.getQualityValue()))	// minus to reverse
			.map(mt -> AcceptableMediaType.find(mt.getType(), mt.getSubtype()));
		return Stream.concat(Stream.of(formatType), streamFromAccept)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.collect(Collectors.toUnmodifiableList());
	}

	public static String getRequestor(HttpHeaders headers, HttpServletRequest request) {
		String host = headers.getHost().getHostString();
		String user = request.getRemoteUser();
		return (user == null || user.isEmpty())
			? host
			: String.format("%1$s (%2$s)", host, user);
	}

	public static MediaType mediaTypeFromString(String mediaType) {
		String[] pieces = Objects.requireNonNull(mediaType, "mediaType")
			.split("/", 2);
		if (pieces.length == 0) {
			throw new IllegalArgumentException(String.format(
				"'%1$s' is not a legal media type", mediaType));
		} else if (pieces.length == 1) {
			return new MediaType(pieces[0]);
		} else {
			return new MediaType(pieces[0], pieces[1]);
		}
	}
}
