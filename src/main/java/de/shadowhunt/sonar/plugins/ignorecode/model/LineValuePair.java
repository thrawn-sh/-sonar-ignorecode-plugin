package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class LineValuePair {

	public static List<LineValuePair> parseDataString(final String data) {
		final List<LineValuePair> result = new ArrayList<LineValuePair>();
		for (final String parts : data.split(";")) {
			final String[] pair = parts.split("=");
			if ((pair != null) && (pair.length == 2)) {
				final int lineNumber = Integer.parseInt(pair[0]);
				final int value = Integer.parseInt(pair[1]);
				result.add(new LineValuePair(lineNumber, value));
			}
		}
		return result;
	}

	public static List<LineValuePair> removeIgnores(final List<LineValuePair> pairs, final Set<Integer> ignores) {
		for (final Iterator<LineValuePair> it = pairs.iterator(); it.hasNext();) {
			final LineValuePair pair = it.next();
			if (ignores.contains(pair.getLineNumber())) {
				it.remove();
			}
		}
		return pairs;
	}

	public static int sumValues(final List<LineValuePair> pairs) {
		int sum = 0;
		for (final LineValuePair pair : pairs) {
			sum += pair.getValue();
		}
		return sum;
	}

	public static String toDataString(final List<LineValuePair> pairs) {
		final StringBuilder sb = new StringBuilder();
		for (final LineValuePair pair : pairs) {
			sb.append(pair.lineNumber);
			sb.append('=');
			sb.append(pair.value);
			sb.append(';');
		}
		// without last ';'
		return sb.substring(0, sb.length() - 1);
	}

	private final int lineNumber;

	private int value;

	/**
	 * Create a new {@link LineValuePair} for the given line number and value
	 * @param lineNumber the line number of the {@link LineValuePair} (can not be modified afterwards)
	 * @param value the initial value of the {@link LineValuePair} (can be modified afterwards)
	 */
	public LineValuePair(final int lineNumber, final int value) {
		this.lineNumber = lineNumber;
		this.value = value;
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
		final LineValuePair other = (LineValuePair) obj;
		if (lineNumber != other.lineNumber) {
			return false;
		}
		if (value != other.value) {
			return false;
		}
		return true;
	}

	/**
	 * Returns the line number of the {@link LineValuePair}
	 * @return the line number of the {@link LineValuePair}
	 */
	public int getLineNumber() {
		return lineNumber;
	}

	/**
	 * Returns the value of the {@link LineValuePair}
	 * @return the value of the {@link LineValuePair}
	 */
	public int getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + lineNumber;
		result = (prime * result) + value;
		return result;
	}

	/**
	 * Set the value of the {@link LineValuePair}
	 * @param value new value of the {@link LineValuePair}
	 */
	public void setValue(final int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LineValuePair [lineNumber=");
		builder.append(lineNumber);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
