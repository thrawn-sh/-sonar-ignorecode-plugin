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
package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilter;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import de.shadowhunt.sonar.plugins.ignorecode.model.IssuePattern;

/**
 * Generated code as identified by the violations.ignore, must not conform to quality profiles.
 * Therefore the {@link IgnoreIssueFilter} goes through all {@link Issue} and removes
 * all entries for identified sources
 */
public class IgnoreIssueFilter implements IssueFilter {

    /**
     * property name that points to the ignore file: will be read from the project configuration
     */
    public static final String CONFIG_FILE = "sonar.ignoreviolations.configFile";

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreIssueFilter.class);

    static final List<IssuePattern> loadPatterns(final Configuration configuration) {
        if (configuration == null) {
            return Collections.emptyList();
        }

        final String fileLocation = configuration.getString(CONFIG_FILE);
        if (StringUtils.isBlank(fileLocation)) {
            LOGGER.info("no ignore file configured for property: {}", CONFIG_FILE);
            return Collections.emptyList();
        }

        final File ignoreFile = new File(fileLocation);
        if (!ignoreFile.isFile()) {
            LOGGER.error("could not find ignore file: {}", ignoreFile);
            return Collections.emptyList();
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(ignoreFile);
            final List<IssuePattern> patterns = IssuePattern.parse(fis);
            LOGGER.info("IgnoreIssueFilter: loaded {} violation ignores from {}", patterns.size(), ignoreFile);
            return patterns;
        } catch (final Exception e) {
            throw new SonarException("could not load ignores for file: " + ignoreFile, e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    static boolean match(final Issue issue, final IssuePattern pattern) {
        final boolean isMatchingResource = matchResource(issue.componentKey(), pattern.getResourcePattern());
        if (!isMatchingResource) {
            return false;
        }

        final boolean isMatchingRule = matchRule(issue.ruleKey(), pattern.getRulePattern());
        if (!isMatchingRule) {
            return false;
        }

        final Set<Integer> lines = pattern.getLines();
        if (lines.isEmpty()) {
            return true; // empty is any line
        }
        return lines.contains(issue.line());
    }

    static boolean matchResource(final String componentKey, final String pattern) {
        final String[] parts = componentKey.split(":");
        if (parts.length != 3) {
            return false;
        }
        LOGGER.debug("matching resource {} against pattern {} ", parts[2], pattern);
        return WildcardPattern.create(pattern).match(parts[2]);
    }

    static boolean matchRule(final RuleKey ruleKey, final String pattern) {
        return WildcardPattern.create(pattern).match(ruleKey.repository() + ":" + ruleKey.rule());
    }

    private final List<IssuePattern> patterns;

    /**
     * Create a new {@link IgnoreIssueFilter} that loads its patterns with {@link #CONFIG_FILE} key from the given {@link Configuration}
     *
     * @param configuration project {@link org.apache.commons.configuration.Configuration}
     */
    public IgnoreIssueFilter(final Configuration configuration) {
        patterns = loadPatterns(configuration);
    }

    @Override
    public boolean accept(final Issue issue, final IssueFilterChain chain) {
        for (final IssuePattern pattern : patterns) {
            if (match(issue, pattern)) {
                LOGGER.info("Violation {} switched off by {}", issue, pattern);
                return false;
            }
        }
        return chain.accept(issue);
    }

    @Override
    public final String toString() {
        return getClass().getSimpleName();
    }
}
