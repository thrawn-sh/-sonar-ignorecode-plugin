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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.WildcardPattern;

import de.shadowhunt.sonar.plugins.ignorecode.internal.ModifyMeasures;
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

    public static final Set<Metric> CONSUMED_METRICS;

    static {
        final Set<Metric> consumedMetrics = new HashSet<>();
        // unit test coverage
        consumedMetrics.add(CoreMetrics.CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.CONDITIONS_TO_COVER);
        consumedMetrics.add(CoreMetrics.COVERAGE);
        consumedMetrics.add(CoreMetrics.COVERAGE_LINE_HITS_DATA);
        consumedMetrics.add(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.LINES_TO_COVER);
        consumedMetrics.add(CoreMetrics.LINE_COVERAGE);
        consumedMetrics.add(CoreMetrics.UNCOVERED_CONDITIONS);
        consumedMetrics.add(CoreMetrics.UNCOVERED_LINES);
        // integration test coverage
        consumedMetrics.add(CoreMetrics.IT_CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.IT_CONDITIONS_TO_COVER);
        consumedMetrics.add(CoreMetrics.IT_COVERAGE);
        consumedMetrics.add(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA);
        consumedMetrics.add(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.IT_LINES_TO_COVER);
        consumedMetrics.add(CoreMetrics.IT_LINE_COVERAGE);
        consumedMetrics.add(CoreMetrics.IT_UNCOVERED_CONDITIONS);
        consumedMetrics.add(CoreMetrics.IT_UNCOVERED_LINES);
        // overall test coverage
        consumedMetrics.add(CoreMetrics.OVERALL_CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.OVERALL_CONDITIONS_TO_COVER);
        consumedMetrics.add(CoreMetrics.OVERALL_COVERAGE);
        consumedMetrics.add(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA);
        consumedMetrics.add(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE);
        consumedMetrics.add(CoreMetrics.OVERALL_LINES_TO_COVER);
        consumedMetrics.add(CoreMetrics.OVERALL_LINE_COVERAGE);
        consumedMetrics.add(CoreMetrics.OVERALL_UNCOVERED_CONDITIONS);
        consumedMetrics.add(CoreMetrics.OVERALL_UNCOVERED_LINES);
        CONSUMED_METRICS = Collections.unmodifiableSet(consumedMetrics);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreCoverageDecorator.class);

    static List<CoveragePattern> loadPatterns(final Configuration configuration) {
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

    private ModifyMeasures modifyMeasures = new ModifyMeasures();

    private final List<CoveragePattern> patterns;

    /**
     * Create a new {@link IgnoreCoverageDecorator} that removes all coverage metrics for ignored code
     *
     * @param configuration project {@link Configuration}
     */
    public IgnoreCoverageDecorator(final Configuration configuration) {
        patterns = loadPatterns(configuration);
    }

    @DependsUpon
    public Set<Metric> consumedMetrics() {
        return CONSUMED_METRICS;
    }

    @Override
    public void decorate(final Resource resource, final DecoratorContext context) {
        if (!ResourceUtils.isFile(resource)) {
            return;
        }

        final String resourceKey = resource.getKey();
        for (final CoveragePattern pattern : patterns) {
            final WildcardPattern wildcardPattern = WildcardPattern.create(pattern.getResourcePattern());
            if (!wildcardPattern.match(resourceKey)) {
                continue;
            }

            final Set<Integer> lines = pattern.getLines();
            if (lines.isEmpty()) {
                // empty is any line => already cleared by IgnoreCoverageMeasurementFilter
                break;
            }

            modifyMeasures.rewrite(context, lines);
        }
    }

    ModifyMeasures getModifyMeasures() {
        return modifyMeasures;
    }

    void setModifyMeasures(final ModifyMeasures modifyMeasures) {
        this.modifyMeasures = modifyMeasures;
    }

    @Override
    public boolean shouldExecuteOnProject(final Project project) {
        return true;
    }
}
