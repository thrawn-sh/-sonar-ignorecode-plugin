/**
 * This file is part of Sonar Ignore Code Plugin.
 *
 * Sonar Ignore Code Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sonar Ignore Code Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sonar Ignore Code Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * {@link LinePattern} describes which lines of a resource shall be matched
 */
public final class LinePattern extends AbstractPattern {

    /**
     * Merges multiple {@link LinePattern} of the same resource into a single {@link LinePattern}
     *
     * @param patterns {@link Collection} of {@link LinePattern} that shall be merged
     *
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
     *
     * @param input containing one {@link LinePattern} per line (for a description of the
     * line format see {@link #parseLine(String)}. Empty lines or comments (lines starting
     * with '#') are ignored
     *
     * @return the list of {@link LinePattern} from the given {@link InputStream}
     *
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
     *
     * @param line each line must consist out of the full qualified resource name and lineValues,
     * separated by a ';' (for a description of lineValues see {@link #parseLineValues(String, String)}
     *
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
     *
     * @param resource full qualified class name of the java class
     * @param lineValues pattern that describes the lines the {@link LinePattern} shall match, lines can be
     * given as values ([1,3]) or as ranges ([5-10]) or a combination of both ([1,3,5-10])
     *
     * @return the new {@link LinePattern} for the given resource by parsing the lineValues
     */
    public static LinePattern parseLineValues(final String resource, final String lineValues) {
        final LinePattern pattern = new LinePattern(resource);
        if (!"*".equals(lineValues)) {
            parseLineValues(pattern, lineValues);
        }
        return pattern;
    }

    private final String resource;

    LinePattern(final String resource) {
        this.resource = resource;
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
        final LinePattern other = (LinePattern) obj;
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
     * Returns a full qualified class name of the java class
     *
     * @return the full qualified class name of the java class
     */
    public String getResource() {
        return resource;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = (prime * result) + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("LinePattern [resource=");
        builder.append(resource);
        builder.append(", getLines()=");
        builder.append(getLines());
        builder.append(']');
        return builder.toString();
    }
}
