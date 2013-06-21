package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * {@link ViolationPattern} describes which rules on which lines of resources shall be matched
 */
public final class ViolationPattern extends AbstractPattern {

	/**
	 * Create a list of {@link ViolationPattern} from the given {@link InputStream}
	 * @param input containing one {@link ViolationPattern} per line (for a description of the
	 * line format see {@link #parseLine(String)}. Empty lines or comments (lines starting
	 * with '#') are ignored
	 * @return the list of {@link ViolationPattern} from the given {@link InputStream}
	 * @throws IOException in case the {@link InputStream} can not be read
	 */
	public static List<ViolationPattern> parse(final InputStream input) throws IOException {
		final List<ViolationPattern> patterns = new ArrayList<ViolationPattern>();
		for (final String line : IOUtils.readLines(input)) {
			if (StringUtils.isBlank(line) || (line.charAt(0) == '#')) {
				continue;
			}

			final ViolationPattern pattern = parseLine(line);
			patterns.add(pattern);
		}
		return patterns;
	}

	/**
	 * Create a new {@link ViolationPattern} from the given line describing the resourcePattern, the rulePattern and
	 * the lines in the resource
	 * @param line each line must consist out of the resourcePattern, rulePattern and lineValues,
	 * separated by a ';' (for a description of lineValues see {@link #parseLineValues(String, String, String)}
	 * @return the new {@link ViolationPattern} from the given line
	 */
	public static ViolationPattern parseLine(final String line) {
		final String[] fields = StringUtils.split(line, ';');
		if (fields.length != 3) {
			throw new IllegalArgumentException("The line does not define 3 fields separated by ';': " + line);
		}

		final String resourcePattern = fields[0];
		if (StringUtils.isBlank(resourcePattern)) {
			throw new IllegalArgumentException("The first field does not define a resource pattern: " + line);
		}

		final String rulePattern = fields[1];
		if (StringUtils.isBlank(rulePattern)) {
			throw new IllegalArgumentException("The second field does not define a range of lines: " + line);
		}

		final String lineValues = fields[2];
		if (StringUtils.isBlank(lineValues)) {
			throw new IllegalArgumentException("The third field does not define a range of lines: " + line);
		}

		return parseLineValues(resourcePattern, rulePattern, lineValues);
	}

	/**
	 * Create a new {@link ViolationPattern} for the given resourcePattern and rulePattern by parsing the lineValues
	 * @param resourcePattern pattern describing the resources that will be matched
	 * @param rulePattern pattern describing the rules that will be matched
	 * @param lineValues pattern that describes the lines the {@link ViolationPattern} shall match, lines can be
	 * given as values ([1,3]) or as ranges ([5-10]) or a combination of both ([1,3,5-10])
	 * @return the new {@link ViolationPattern} for the given resource by parsing the lineValues
	 */
	public static ViolationPattern parseLineValues(final String resourcePattern, final String rulePattern, final String lineValues) {
		final ViolationPattern pattern = new ViolationPattern(resourcePattern, rulePattern);
		if (!"*".equals(lineValues)) {
			parseLineValues(pattern, lineValues);
		}
		return pattern;
	}

	private final String resourcePattern;

	private final String rulePattern;

	/**
	 * Create a new {@link ViolationPattern} with the given resourcePattern and rulePattern
	 * @param resourcePattern pattern that describes the resources this {@link ViolationPattern} shall match
	 * @param rulePattern pattern that describes the rules this {@link ViolationPattern} shall match
	 */
	public ViolationPattern(final String resourcePattern, final String rulePattern) {
		this.resourcePattern = resourcePattern;
		this.rulePattern = rulePattern;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ViolationPattern other = (ViolationPattern) obj;
		if (resourcePattern == null) {
			if (other.resourcePattern != null) {
				return false;
			}
		} else if (!resourcePattern.equals(other.resourcePattern)) {
			return false;
		}
		if (rulePattern == null) {
			if (other.rulePattern != null) {
				return false;
			}
		} else if (!rulePattern.equals(other.rulePattern)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a pattern that describes the resources that shall match
	 * @return the pattern that describes the resources that shall match
	 */
	public String getResourcePattern() {
		return resourcePattern;
	}

	/**
	 * Returns a pattern that describes the rules that shall match
	 * @return the pattern that describes the rules that shall match
	 */
	public String getRulePattern() {
		return rulePattern;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = (prime * result) + ((resourcePattern == null) ? 0 : resourcePattern.hashCode());
		result = (prime * result) + ((rulePattern == null) ? 0 : rulePattern.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ViolationPattern [resourcePattern=");
		builder.append(resourcePattern);
		builder.append(", rulePattern=");
		builder.append(rulePattern);
		builder.append(", getLines()=");
		builder.append(getLines());
		builder.append("]");
		return builder.toString();
	}
}
