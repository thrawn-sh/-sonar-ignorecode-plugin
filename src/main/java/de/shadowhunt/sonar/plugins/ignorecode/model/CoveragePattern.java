package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class CoveragePattern extends AbstractPattern {

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
     * @param line each line must consist out of the resourcePattern, rulePattern and lineValues,
     * separated by a ';' (for a description of lineValues see {@link #parseLineValues(String, String)}
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

        return parseLineValues(resourcePattern, lineValues);
    }

    /**
     * Create a new {@link CoveragePattern} for the given resourcePattern and rulePattern by parsing the lineValues
     *
     * @param resourcePattern pattern describing the resources that will be matched
     * @param lineValues pattern that describes the lines the {@link CoveragePattern} shall match, lines can be
     * given as values ([1,3]) or as ranges ([5-10]) or a combination of both ([1,3,5-10])
     *
     * @return the new {@link CoveragePattern} for the given resource by parsing the lineValues
     */
    public static CoveragePattern parseLineValues(final String resourcePattern, final String lineValues) {
        final CoveragePattern pattern = new CoveragePattern(resourcePattern);
        if (!"*".equals(lineValues)) {
            parseLineValues(pattern, lineValues);
        }
        return pattern;
    }

    private final String resourcePattern;

    public CoveragePattern(final String resourcePattern) {
        this.resourcePattern = resourcePattern;
    }

    /**
     * Returns a pattern that describes the resources that shall match
     *
     * @return the pattern that describes the resources that shall match
     */
    public String getResourcePattern() {
        return resourcePattern;
    }
}
