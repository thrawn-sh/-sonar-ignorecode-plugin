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

import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.WildcardPattern;
import org.sonar.core.measure.MeasurementFilter;

import de.shadowhunt.sonar.plugins.ignorecode.internal.ModifyMeasures;
import de.shadowhunt.sonar.plugins.ignorecode.model.CoveragePattern;

public class IgnoreCoverageMeasurementFilter implements MeasurementFilter {

    private static final Set<Metric> COVERAGE_METRICS = IgnoreCoverageDecorator.CONSUMED_METRICS;

    private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreCoverageMeasurementFilter.class);

    private ModifyMeasures modifyMeasures = new ModifyMeasures();

    private final List<CoveragePattern> patterns;

    public IgnoreCoverageMeasurementFilter(final Configuration configuration) {
        patterns = IgnoreCoverageDecorator.loadPatterns(configuration);
    }

    @Override
    public boolean accept(final Resource resource, final Measure measure) {
        if (!ResourceUtils.isFile(resource)) {
            return true;
        }

        if (!COVERAGE_METRICS.contains(measure.getMetric())) {
            return true;
        }

        final String metricKey = measure.getMetricKey();
        LOGGER.warn("{}: {}", resource.getKey(), metricKey);

        for (final CoveragePattern pattern : patterns) {
            final WildcardPattern wildcardPattern = WildcardPattern.create(pattern.getResourcePattern());
            if (!wildcardPattern.match(resource.getKey())) {
                continue;
            }

            final Set<Integer> lines = pattern.getLines();
            if (lines.isEmpty()) {
                // empty is any line => remove all measures
                return false;
            }

            rewrite(measure, lines);
        }
        return true;
    }

    private void rewrite(final Measure measure, final Set<Integer> lines) {
        final String metricKey = measure.getMetricKey();
        switch (metricKey) {
            case CoreMetrics.CONDITIONS_BY_LINE_KEY:
                modifyMeasures.filterLineValuePairs(measure, lines);
                break;
            case CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY:
                modifyMeasures.filterLineValuePairs(measure, lines);
                break;
            case CoreMetrics.COVERAGE_LINE_HITS_DATA_KEY:
                modifyMeasures.filterLineValuePairs(measure, lines);
                break;
        }
    }
}
