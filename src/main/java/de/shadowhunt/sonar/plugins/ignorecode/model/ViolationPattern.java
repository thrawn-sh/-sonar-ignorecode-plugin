package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.utils.WildcardPattern;

public final class ViolationPattern {

	static void addLines(final SortedSet<Integer> lines, final int from, final int to) {
		if (to < from) {
			throw new IllegalArgumentException("from: " + from + " must be greater than to: " + to);
		}
		for (int line = from; line <= to; line++) {
			lines.add(line);
		}
	}

	public static ViolationPattern createIgnorePattern(final String resourcePattern, final String rulePattern, final String lines, final String raw) {
		SortedSet<Integer> lineSet = null;
		if (!StringUtils.equals(lines, "*")) {
			lineSet = new TreeSet<Integer>();
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

		return new ViolationPattern(resourcePattern, rulePattern, lineSet, raw);
	}

	private final SortedSet<Integer> lines;

	private final String raw;

	private final WildcardPattern resourcePattern;

	private final WildcardPattern rulePattern;

	private ViolationPattern(final String resourcePattern, final String rulePattern, final SortedSet<Integer> lines, final String raw) {
		this.resourcePattern = WildcardPattern.create(resourcePattern);
		this.rulePattern = WildcardPattern.create(rulePattern);
		this.lines = lines;
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
		final ViolationPattern other = (ViolationPattern) obj;
		if (raw == null) {
			if (other.raw != null) {
				return false;
			}
		} else if (!raw.equals(other.raw)) {
			return false;
		}
		return true;
	}

	public SortedSet<Integer> getLines() {
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
