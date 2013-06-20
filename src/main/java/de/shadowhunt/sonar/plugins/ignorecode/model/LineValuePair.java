package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * Sonar stores {@code Measure} data as list of line value pairs.
 * {@link LineValuePair} represents such a value
 */
public class LineValuePair {

	/**
	 * Creates a {@link List} of {@link LineValuePair} from the given data
	 * @param data ';' separated list of line-value-pairs (each separated by '=').
	 * A possible data String may look like 1=5 or 1=5;2=4;5=6
	 * @return the {@link List} of {@link LineValuePair} from the given data
	 */
	public static List<LineValuePair> parseDataString(final String data) {
		final List<LineValuePair> result = new ArrayList<LineValuePair>();
		for (final String parts : data.split(";")) {
			if (StringUtils.isBlank(parts)) {
				continue;
			}

			final String[] pair = parts.split("=");
			if (pair.length != 2) {
				continue;
			}

			final String lineString = pair[0];
			final String valueString = pair[1];
			if (StringUtils.isEmpty(lineString) || StringUtils.isEmpty(valueString)) {
				continue;
			}

			final int lineNumber = Integer.parseInt(lineString);
			final int value = Integer.parseInt(valueString);
			result.add(new LineValuePair(lineNumber, value));
		}
		return result;
	}

	/**
	 * Removes all {@link LineValuePair} whose lineNumbers are part of the ignore {@link Set}
	 * @param pairs {@link List} of {@link LineValuePair} to filter 
	 * @param ignoreLines {@link Set} of lines to remove 
	 */
	public static void removeIgnores(final List<LineValuePair> pairs, final Set<Integer> ignoreLines) {
		for (final Iterator<LineValuePair> it = pairs.iterator(); it.hasNext();) {
			final LineValuePair pair = it.next();
			if (ignoreLines.contains(pair.getLineNumber())) {
				it.remove();
			}
		}
	}

	/**
	 * Sum up all values of the {@link LineValuePair}
	 * @param pairs {@link Collection} of {@link LineValuePair} to sum up their values 
	 * @return sum of all values
	 */
	public static int sumValues(final Collection<LineValuePair> pairs) {
		int sum = 0;
		for (final LineValuePair pair : pairs) {
			sum += pair.getValue();
		}
		return sum;
	}

	/**
	 * Creates a {@link String} representation for the {@link List} of {@link LineValuePair} 
	 * @param pairs {@link Collection} of {@link LineValuePair} to convert to a {@link String}
	 * @return {@link String} representation, fit to be parsed by {@link #parseDataString(String)}
	 */
	public static String toDataString(final Collection<LineValuePair> pairs) {
		final StringBuilder sb = new StringBuilder();
		for (final LineValuePair pair : pairs) {
			sb.append(pair.lineNumber);
			sb.append('=');
			sb.append(pair.value);
			sb.append(';');
		}

		final int length = sb.length();
		if (length <= 1) {
			return "";
		}
		// without last ';'
		return sb.substring(0, length - 1);
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
