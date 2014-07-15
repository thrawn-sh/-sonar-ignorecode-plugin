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

/**
 * {@link IssuePattern} describes which rules on which lines of resources shall be matched
 */
public final class IssuePattern extends AbstractPattern {

    /**
     * Create a list of {@link IssuePattern} from the given {@link InputStream}
     *
     * @param input containing one {@link IssuePattern} per line (for a description of the
     * line format see {@link #parseLine(String)}. Empty lines or comments (lines starting
     * with '#') are ignored
     *
     * @return the list of {@link IssuePattern} from the given {@link InputStream}
     *
     * @throws IOException in case the {@link InputStream} can not be read
     */
    public static List<IssuePattern> parse(final InputStream input) throws IOException {
        final List<IssuePattern> patterns = new ArrayList<>();
        for (final String line : IOUtils.readLines(input)) {
            if (StringUtils.isBlank(line) || (line.charAt(0) == '#')) {
                continue;
            }

            final IssuePattern pattern = parseLine(line);
            patterns.add(pattern);
        }
        return patterns;
    }

    /**
     * Create a new {@link IssuePattern} from the given line describing the resourcePattern, the rulePattern and
     * the lines in the resource
     *
     * @param line each line must consist out of the resourcePattern, rulePattern and lineValues,
     * separated by a ';' (lines can be given as values ([1,3]) or as ranges ([5-10]) or a combination of both ([1,3,5-10]))
     *
     * @return the new {@link IssuePattern} from the given line
     */
    public static IssuePattern parseLine(final String line) {
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
            throw new IllegalArgumentException("The second field does not define a rule pattern: " + line);
        }

        final String lineValues = fields[2];
        if (StringUtils.isBlank(lineValues)) {
            throw new IllegalArgumentException("The third field does not define a range of lines: " + line);
        }

        final SortedSet<Integer> lines = parseLineValues(lineValues);
        return new IssuePattern(resourcePattern, rulePattern, lines);
    }

    private final String rulePattern;

    /**
     * Create a new {@link IssuePattern} with the given resourcePattern and rulePattern
     *
     * @param resourcePattern pattern that describes the resources this {@link IssuePattern} shall match
     * @param rulePattern pattern that describes the rules this {@link IssuePattern} shall match
     */
    public IssuePattern(final String resourcePattern, final String rulePattern, final SortedSet<Integer> lines) {
        super(resourcePattern, lines);
        this.rulePattern = rulePattern;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IssuePattern)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        final IssuePattern that = (IssuePattern) o;

        if (!rulePattern.equals(that.rulePattern)) {
            return false;
        }

        return true;
    }

    /**
     * Returns a pattern that describes the rules that shall match
     *
     * @return the pattern that describes the rules that shall match
     */
    public String getRulePattern() {
        return rulePattern;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + rulePattern.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("IssuePattern [resourcePattern=");
        builder.append(resourcePattern);
        builder.append(", rulePattern=");
        builder.append(rulePattern);
        builder.append(", lines=");
        builder.append(lines);
        builder.append(']');
        return builder.toString();
    }
}
