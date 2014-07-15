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
import java.util.List;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public final class CoveragePattern extends AbstractPattern {

    /**
     * Create a list of {@link CoveragePattern} from the given {@link java.io.InputStream}
     *
     * @param input containing one {@link CoveragePattern} per line (for a description of the
     * line format see {@link #parseLine(String)}. Empty lines or comments (lines starting
     * with '#') are ignored
     *
     * @return the list of {@link CoveragePattern} from the given {@link java.io.InputStream}
     *
     * @throws java.io.IOException in case the {@link java.io.InputStream} can not be read
     */
    public static List<CoveragePattern> parse(final InputStream input) throws IOException {
        final List<CoveragePattern> patterns = new ArrayList<>();
        for (final String line : IOUtils.readLines(input)) {
            if (StringUtils.isBlank(line) || (line.charAt(0) == '#')) {
                continue;
            }

            final CoveragePattern pattern = parseLine(line);
            patterns.add(pattern);
        }
        return patterns;
    }

    /**
     * Create a new {@link CoveragePattern} from the given line describing the resourcePattern, the rulePattern and
     * the lines in the resource
     *
     * @param line each line must consist out of the resourcePattern and lineValues,
     * separated by a ';'
     *
     * @return the new {@link CoveragePattern} from the given line
     */
    public static CoveragePattern parseLine(final String line) {
        final String[] fields = StringUtils.split(line, ';');
        if (fields.length != 2) {
            throw new IllegalArgumentException("The line does not define 2 fields separated by ';': " + line);
        }

        final String resourcePattern = fields[0];
        if (StringUtils.isBlank(resourcePattern)) {
            throw new IllegalArgumentException("The first field does not define a resource pattern: " + line);
        }

        final String lineValues = fields[1];
        if (StringUtils.isBlank(lineValues)) {
            throw new IllegalArgumentException("The third field does not define a range of lines: " + line);
        }

        final SortedSet<Integer> lines = parseLineValues(lineValues);
        return new CoveragePattern(resourcePattern, lines);
    }

    public CoveragePattern(final String resourcePattern, final SortedSet<Integer> lines) {
        super(resourcePattern, lines);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssuePattern)) {
            return false;
        }
        return super.equals(o);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("CoveragePattern [resourcePattern=");
        builder.append(resourcePattern);
        builder.append(", lines=");
        builder.append(lines);
        builder.append(']');
        return builder.toString();
    }
}
