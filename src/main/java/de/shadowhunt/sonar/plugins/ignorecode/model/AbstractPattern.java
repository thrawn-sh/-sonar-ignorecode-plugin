package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

public abstract class AbstractPattern {

	protected final SortedSet<Integer> lines = new TreeSet<Integer>();

	protected static <T extends AbstractPattern> void parseLineValues(final T pattern, final String lineValues) {
		final String s = StringUtils.substringBetween(StringUtils.trim(lineValues), "[", "]");
		final String[] parts = StringUtils.split(s, ',');
		for (final String part : parts) {
			if (StringUtils.contains(part, '-')) {
				final String[] range = StringUtils.split(part, '-');
				final int from = Integer.parseInt(range[0]);
				final int to = Integer.parseInt(range[1]);
				pattern.addLines(from, to);
			} else {
				pattern.addLine(Integer.parseInt(part));
			}
		}
	}

	/**
	 * Add another line this {@link AbstractPattern} shall match
	 * @param line the {@link AbstractPattern} shall match
	 */
	public void addLine(final int line) {
		lines.add(line);
	}

	/**
	 * Add a range of lines this {@link AbstractPattern} shall match, all lines between
	 * from (including) and to (including) will be added
	 * @param from the first line in the range the {@link AbstractPattern} shall match, must be greater or equal than to
	 * @param to the last line in the range the {@link AbstractPattern} shall match, must be smaller or equal than from
	 * @throws IllegalArgumentException if from is not greater or equal than to
	 */
	public void addLines(final int from, final int to) {
		if (to < from) {
			throw new IllegalArgumentException("from: " + from + " must be greater or equal than to: " + to);
		}
		for (int line = from; line <= to; line++) {
			lines.add(line);
		}
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
		final AbstractPattern other = (AbstractPattern) obj;
		if (lines == null) {
			if (other.lines != null) {
				return false;
			}
		} else if (!lines.equals(other.lines)) {
			return false;
		}
		return true;
	}

	public SortedSet<Integer> getLines() {
		return new TreeSet<Integer>(lines);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((lines == null) ? 0 : lines.hashCode());
		return result;
	}
}
