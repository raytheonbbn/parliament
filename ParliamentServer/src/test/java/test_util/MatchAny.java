package test_util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;

public class MatchAny implements ResultMatcher {
	private static final Logger LOG = LoggerFactory.getLogger(MatchAny.class);

	private final ResultMatcher[] matchers;

	public MatchAny(ResultMatcher... matchers) {
		this.matchers = matchers;
	}

	@Override
	public void match(MvcResult result) {
		List<Throwable> errors = new ArrayList<>();
		for (ResultMatcher matcher : matchers) {
			try {
				matcher.match(result);
				LOG.debug("{}: success!", matcher.getClass().getSimpleName());
				return;
			} catch (Exception | AssertionError ex) {
				errors.add(ex);
				LOG.debug("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
			}
		}

		String message = errors.stream()
			.map(ex -> "%1$s: %2$s".formatted(ex.getClass().getSimpleName(), ex.getMessage()))
			.collect(Collectors.joining(
				"%n   ".formatted(),
				"%1$s failure:%n   ".formatted(MatchAny.class.getSimpleName()),
				""));
		throw new AssertionError(message);
	}
}
