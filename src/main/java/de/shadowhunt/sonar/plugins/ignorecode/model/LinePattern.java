package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * {@link LinePattern} describes which lines of a resource shall be matched
 */
public final class LinePattern {

	/**
	 * Merges multiple {@link LinePattern} of the same resource into a single {@link LinePattern}
	 * @param patterns {@link Collection} of {@link LinePattern} that shall be merged
	 * @return {@link Collection} which only contains a single {@link LinePattern} for each resource 
	 */
	public static Collection<LinePattern> merge(final Collection<LinePattern> patterns) {
		final Map<String, LinePattern> cache = new HashMap<String, LinePattern>();
		for (final LinePattern pattern : patterns) {
			final LinePattern master = cache.get(pattern.resource);
			if (master == null) {
				cache.put(pattern.resource, pattern);
				continue;
			}

			master.lines.addAll(pattern.lines);
		}
		return cache.values();
	}

	/**
	 * Create a list of {@link LinePattern} from the given {@link InputStream}
	 * @param input containing one {@link LinePattern} per line (for a description of the
	 * line format see {@link #parseLine(String)}. Empty lines or comments (lines starting
	 * with '#') are ignored
	 * @return the list of {@link LinePattern} from the given {@link InputStream}
	 * @throws IOException in case the {@link InputStream} can not be read
	 */
	public static List<LinePattern> parse(final InputStream input) throws IOException {
		final List<LinePattern> patterns = new ArrayList<LinePattern>();
		for (final String line : IOUtils.readLines(input)) {
			if (StringUtils.isBlank(line) || (line.charAt(0) == '#')) {
				continue;
			}

			final LinePattern pattern = parseLine(line);
			patterns.add(pattern);
		}
		return patterns;
	}

	/**
	 * Create a new {@link LinePattern} from the given line describing the resource and
	 * the lines in the resource
	 * @param line each line must consist out of the full qualified resource name and lineValues,
	 * separated by a ';' (for a description of lineValues see {@link #parseLineValues(String, String)}
	 * @return the new {@link LinePattern} from the given line
	 */
	public static LinePattern parseLine(final String line) {
		final String[] fields = StringUtils.split(line, ';');
		if (fields.length != 2) {
			throw new IllegalArgumentException("The line does not define 2 fields separated by ';': " + line);
		}

		final String resource = fields[0];
		if (StringUtils.isBlank(resource)) {
			throw new IllegalArgumentException("The first field does not define a resource pattern: " + line);
		}

		final String lineValues = fields[1];
		if (StringUtils.isBlank(lineValues)) {
			throw new IllegalArgumentException("The second field does not define a range of lines: " + line);
		}

		return parseLineValues(resource, lineValues);
	}

	/**
	 * Create a new {@link LinePattern} for the given resource by parsing the lineValues
	 * @param resource full qualified class name of the java class
	 * @param lineValues pattern that describes the lines the {@link LinePattern} shall match, lines can be
	 * given as values ([1,3]) or as ranges ([5-10]) or a combination of both ([1,3,5-10])
	 * @return the new {@link LinePattern} for the given resource by parsing the lineValues
	 */
	public static LinePattern parseLineValues(final String resource, final String lineValues) {
		final LinePattern pattern = new LinePattern(resource);
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
		return pattern;
	}

	private final SortedSet<Integer> lines = new TreeSet<Integer>();

	private final String resource;

	LinePattern(final String resource) {
		this.resource = resource;
	}

	/**
	 * Add another line this {@link LinePattern} shall match
	 * @param line the {@link LinePattern} shall match
	 */
	public void addLine(final int line) {
		lines.add(line);
	}

	/**
	 * Add a range of lines this {@link LinePattern} shall match, all lines between
	 * from (including) and to (including) will be added
	 * @param from the first line in the range the {@link LinePattern} shall match, must be greater or equal than to
	 * @param to the last line in the range the {@link LinePattern} shall match, must be smaller or equal than from
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
		final LinePattern other = (LinePattern) obj;
		if (lines == null) {
			if (other.lines != null) {
				return false;
			}
		} else if (!lines.equals(other.lines)) {
			return false;
		}
		if (resource == null) {
			if (other.resource != null) {
				return false;
			}
		} else if (!resource.equals(other.resource)) {
			return false;
		}
		return true;
	}

	/**
	 * Returns a {@link SortedSet} of lines this {@link LinePattern} shall match
	 * @return the {@link SortedSet} of lines this {@link LinePattern} shall match
	 */
	public SortedSet<Integer> getLines() {
		return lines;
	}

	/**
	 * Returns a full qualified class name of the java class
	 * @return the full qualified class name of the java class
	 */
	public String getResource() {
		return resource;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = (prime * result) + ((lines == null) ? 0 : lines.hashCode());
		result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
		return result;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("LinePattern [resource=");
		builder.append(resource);
		builder.append(", lines=");
		builder.append(lines);
		builder.append("]");
		return builder.toString();
	}
}
