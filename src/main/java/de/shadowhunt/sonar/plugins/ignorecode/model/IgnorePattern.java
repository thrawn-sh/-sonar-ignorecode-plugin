package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

public final class IgnorePattern {

	static void addLines(final Set<Integer> lines, final int from, final int to) {
		if (to < from) {
			throw new IllegalArgumentException("from: " + from + " must be greater than to: " + to);
		}
		for (int line = from; line <= to; line++) {
			lines.add(line);
		}
	}

	public static IgnorePattern createIgnorePattern(final String resourcePattern, final String rulePattern, final String lines, final String raw) {
		Set<Integer> lineSet = null;
		if (!StringUtils.equals(lines, "*")) {
			lineSet = new LinkedHashSet<Integer>();
			final String s = StringUtils.substringBetween(StringUtils.trim(lines), "[", "]");
			final String[] parts = StringUtils.split(s, ',');
			for (final String part : parts) {
				if (StringUtils.contains(part, '-')) {
					final String[] range = StringUtils.split(part, '-');
					addLines(lineSet, Integer.parseInt(range[0]), Integer.parseInt(range[1]));
				} else {
					lineSet.add(Integer.valueOf(part));
				}
			}
		}

		return new IgnorePattern(resourcePattern, rulePattern, lineSet, raw);
	}

	private final Set<Integer> lines;

	private final String raw;

	private final WildcardPattern resourcePattern;

	private final WildcardPattern rulePattern;

	private IgnorePattern(final String resourcePattern, final String rulePattern, final Set<Integer> lines, final String raw) {
		this.resourcePattern = WildcardPattern.create(resourcePattern);
		this.rulePattern = WildcardPattern.create(rulePattern);
		this.lines = Collections.unmodifiableSet(lines);
		this.raw = raw;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final IgnorePattern other = (IgnorePattern) obj;
		if (raw == null) {
			if (other.raw != null) {
				return false;
			}
		} else if (!raw.equals(other.raw)) {
			return false;
		}
		return true;
	}

	public Set<Integer> getLines() {
		return lines;
	}

	public WildcardPattern getResourcePattern() {
		return resourcePattern;
	}

	public WildcardPattern getRulePattern() {
		return rulePattern;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((raw == null) ? 0 : raw.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return raw;
	}
}
