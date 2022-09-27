package com.bbn.parliament.spring_boot.service;

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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
			.collect(Collectors.toList());
	}

	public static String getRequestor(HttpHeaders headers, HttpServletRequest request) {
		InetSocketAddress hostAddr = headers.getHost();
		String host = (hostAddr == null)
			? request.getRemoteHost()
			: hostAddr.getHostString();
		int port = request.getRemotePort();
		String user = request.getRemoteUser();
		String fmt = (StringUtils.isBlank(user))
			? "%1$s:%2$d"
			: "%1$s:%2$d (%3$s)";
		return fmt.formatted(host, port, user);
	}

	public static MediaType mediaTypeFromString(String mediaType) {
		String[] pieces = Objects.requireNonNull(mediaType, "mediaType")
			.split("/", 2);
		if (pieces.length == 0) {
			throw new IllegalArgumentException(
				"'%1$s' is not a legal media type".formatted(mediaType));
		} else if (pieces.length == 1) {
			return new MediaType(pieces[0]);
		} else {
			return new MediaType(pieces[0], pieces[1]);
		}
	}
}
