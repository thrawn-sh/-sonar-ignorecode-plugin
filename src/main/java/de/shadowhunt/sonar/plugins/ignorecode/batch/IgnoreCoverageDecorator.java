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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import de.shadowhunt.sonar.plugins.ignorecode.internal.MeasuresStorage;
import de.shadowhunt.sonar.plugins.ignorecode.model.CoveragePattern;

/**
 * Generated code as identified by the coverage.ignore, must not be covered by any unit tests.
 * Therefore the {@link IgnoreCoverageDecorator} goes through all coverage metrics and removes
 * all entries for identified sources
 */
public class IgnoreCoverageDecorator implements Decorator {

    /**
     * property name that points to the ignore file: will be read from the project configuration
     */
    public static final String CONFIG_FILE = "sonar.ignorecoverage.configFile";

    private static final Set<Metric> COVERAGE_METRICS;

    static {
        final Set<Metric> coverageMetrics = new HashSet<>();
        coverageMetrics.add(CoreMetrics.BRANCH_COVERAGE);
        coverageMetrics.add(CoreMetrics.CONDITIONS_BY_LINE);
        coverageMetrics.add(CoreMetrics.CONDITIONS_TO_COVER);
        coverageMetrics.add(CoreMetrics.COVERAGE);
        coverageMetrics.add(CoreMetrics.COVERAGE_LINE_HITS_DATA);
        coverageMetrics.add(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
        coverageMetrics.add(CoreMetrics.LINES_TO_COVER);
        coverageMetrics.add(CoreMetrics.LINE_COVERAGE);
        coverageMetrics.add(CoreMetrics.UNCOVERED_CONDITIONS);
        coverageMetrics.add(CoreMetrics.UNCOVERED_LINES);
        COVERAGE_METRICS = Collections.unmodifiableSet(coverageMetrics);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreCoverageDecorator.class);

    static final List<CoveragePattern> loadPatterns(final Configuration configuration) {
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
            final List<CoveragePattern> patterns = CoveragePattern.parse(fis);
            LOGGER.info("loaded {} coverage ignores from {}", patterns.size(), ignoreFile);
            return patterns;
        } catch (final Exception e) {
            throw new SonarException("could not load ignores for file: " + ignoreFile, e);
        } finally {
            IOUtils.closeQuietly(fis);
        }
    }

    private final boolean execute;

    private final List<CoveragePattern> patterns;

    /**
     * Create a new {@link IgnoreCoverageDecorator} that removes all coverage metrics for ignored code
     *
     * @param configuration project {@link Configuration}
     */
    public IgnoreCoverageDecorator(final Configuration configuration, final FileSystem fileSystem) {
        execute = fileSystem.languages().contains("java");
        patterns = loadPatterns(configuration);
    }

    private void clearCoverageMeasures(final DecoratorContext context) {
        for (Metric metric : COVERAGE_METRICS) {
            MeasuresStorage.clear(context, metric);
        }
    }

    @Override
    public void decorate(final Resource resource, final DecoratorContext context) {
        if (!ResourceUtils.isFile(resource)) {
            return;
        }

        final CoveragePattern pattern = findPattern(resource);
        if (pattern == null) {
            return;
        }

        final Set<Integer> lines = pattern.getLines();
        if (lines.isEmpty()) {
            // empty is any line => remove all measures
            clearCoverageMeasures(context);
            return;
        }

        ModifyMeasures.rewrite(context, lines);
    }

    @DependsUpon
    public Set<Metric> dependsUponMetrics() {
        return COVERAGE_METRICS;
    }

    @CheckForNull
    private CoveragePattern findPattern(final Resource resource) {
        for (final CoveragePattern pattern : patterns) {
            final WildcardPattern wildcardPattern = WildcardPattern.create(pattern.getResourcePattern());
            if (wildcardPattern.match(resource.getKey())) {
                return pattern;
            }
        }
        return null;
    }

    @Override
    public boolean shouldExecuteOnProject(final Project project) {
        return execute;
    }
}
