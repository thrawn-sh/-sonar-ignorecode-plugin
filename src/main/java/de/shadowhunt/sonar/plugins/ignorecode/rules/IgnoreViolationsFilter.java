package de.shadowhunt.sonar.plugins.ignorecode.rules;

import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.rules.ViolationFilter;
import org.sonar.api.utils.WildcardPattern;

import de.shadowhunt.sonar.plugins.ignorecode.model.IgnorePattern;
import de.shadowhunt.sonar.plugins.ignorecode.util.PatternDecoder;

public class IgnoreViolationsFilter implements ViolationFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreViolationsFilter.class);

	private final List<IgnorePattern> patterns;

	public IgnoreViolationsFilter(final Configuration configuration) {
		patterns = PatternDecoder.loadIgnorePatterns(configuration);
	}

	@Override
	public boolean isIgnored(final Violation violation) {
		final boolean debugEnabled = LOGGER.isDebugEnabled();
		for (final IgnorePattern pattern : patterns) {
			if (match(violation, pattern)) {
				if (debugEnabled) {
					LOGGER.debug("Violation " + violation + " switched off by " + pattern);
				}
				return true;
			}
		}
		return false;
	}

	public boolean match(final Violation violation, final IgnorePattern pattern) {
		boolean match = matchResource(violation.getResource(), pattern.getResourcePattern());
		match = match && matchRule(violation.getRule(), pattern.getRulePattern());

		final Integer line = violation.getLineId();
		final Set<Integer> lines = pattern.getLines();
		if (match && (lines != null) && (line != null)) {
			match = lines.contains(line);
		}
		return match;
	}

	boolean matchResource(final Resource<?> resource, final WildcardPattern pattern) {
		return (resource != null) && (resource.getKey() != null) && pattern.match(resource.getKey());
	}

	boolean matchRule(final Rule rule, final WildcardPattern pattern) {
		if (rule != null) {
			final StringBuilder sb = new StringBuilder();
			sb.append(rule.getRepositoryKey());
			sb.append(':');
			sb.append(rule.getKey());

			return pattern.match(sb.toString());
		}
		return false;
	}
}
