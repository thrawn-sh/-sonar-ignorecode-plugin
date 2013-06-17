package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.ArrayList;
import java.util.List;

public class CovarageValue {

	public static List<CovarageValue> parseDataString(final String data) {
		final List<CovarageValue> result = new ArrayList<CovarageValue>();
		for (final String parts : data.split(";")) {
			final String[] pair = parts.split("=");
			if ((pair != null) && (pair.length == 2)) {
				final int lineNumber = Integer.parseInt(pair[0]);
				final int value = Integer.parseInt(pair[1]);
				result.add(new CovarageValue(lineNumber, value));
			}
		}
		return result;
	}

	public static String toDataString(final List<CovarageValue> lineValues) {
		final StringBuilder sb = new StringBuilder();
		for (final CovarageValue lineValue : lineValues) {
			sb.append(lineValue.lineNumber);
			sb.append('=');
			sb.append(lineValue.value);
			sb.append(';');
		}
		// without last ';'
		return sb.substring(0, sb.length() - 1);
	}

	private final int lineNumber;

	private int value;

	public CovarageValue(final int lineNumber, final int value) {
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
		final CovarageValue other = (CovarageValue) obj;
		if (lineNumber != other.lineNumber) {
			return false;
		}
		if (value != other.value) {
			return false;
		}
		return true;
	}

	public int getLineNumber() {
		return lineNumber;
	}

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

	public void setValue(final int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LineValue [lineNumber=");
		builder.append(lineNumber);
		builder.append(", value=");
		builder.append(value);
		builder.append("]");
		return builder.toString();
	}
}
