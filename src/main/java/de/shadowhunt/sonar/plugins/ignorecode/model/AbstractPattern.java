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

import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

/**
 * {@link AbstractPattern} is the base for all patterns that must handle lines
 */
abstract class AbstractPattern {

    /**
     * Add a range of lines this {@link AbstractPattern} shall match, all lines between
     * from (including) and to (including) will be added
     *
     * @param from the first line in the range the {@link AbstractPattern} shall match, must be greater or equal than to
     * @param to the last line in the range the {@link AbstractPattern} shall match, must be smaller or equal than from
     *
     * @throws IllegalArgumentException if from is not greater or equal than to
     */
    static void addLines(final SortedSet<Integer> lines, final int from, final int to) {
        if (to < from) {
            throw new IllegalArgumentException("from: " + from + " must be greater or equal than to: " + to);
        }
        for (int line = from; line <= to; line++) {
            lines.add(line);
        }
    }

    static SortedSet<Integer> parseLineValues(final String lineValues) {
        final SortedSet<Integer> lines = new TreeSet<>();
        if ("*".equals(lineValues)) {
            return lines;
        }

        final String s = StringUtils.substringBetween(StringUtils.trim(lineValues), "[", "]");
        final String[] parts = StringUtils.split(s, ',');
        for (final String part : parts) {
            if (StringUtils.contains(part, '-')) {
                final String[] range = StringUtils.split(part, '-');
                final int from = Integer.parseInt(range[0]);
                final int to = Integer.parseInt(range[1]);
                addLines(lines, from, to);
            } else {
                lines.add(Integer.parseInt(part));
            }
        }
        return lines;
    }

    protected final SortedSet<Integer> lines;

    protected final String resourcePattern;

    protected AbstractPattern(final String resourcePattern, final SortedSet<Integer> lines) {
        this.resourcePattern = resourcePattern;
        this.lines = new TreeSet<>(lines);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractPattern)) {
            return false;
        }

        final AbstractPattern that = (AbstractPattern) o;

        if (!lines.equals(that.lines)) {
            return false;
        }
        if (!resourcePattern.equals(that.resourcePattern)) {
            return false;
        }

        return true;
    }

    /**
     * Returns a {@link SortedSet} of all lines the pattern shall match
     *
     * @return the {@link SortedSet} of all lines the pattern shall match
     */
    public SortedSet<Integer> getLines() {
        return new TreeSet<>(lines);
    }

    /**
     * Returns a pattern that describes the resources that shall match
     *
     * @return the pattern that describes the resources that shall match
     */
    public String getResourcePattern() {
        return resourcePattern;
    }

    @Override
    public int hashCode() {
        int result = lines.hashCode();
        result = 31 * result + resourcePattern.hashCode();
        return result;
    }

}
