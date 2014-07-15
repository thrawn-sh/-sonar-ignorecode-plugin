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
package de.shadowhunt.sonar.plugins.ignorecode.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.KeyValueFormat;

public class ModifyMeasures {

    private static final KeyValueFormat.IntegerConverter CONVERTER = KeyValueFormat.newIntegerConverter();

    static final String COVERED_CONDITIONS = CoreMetrics.COVERED_CONDITIONS_BY_LINE_KEY;

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyMeasures.class);

    static final String TOTAL_CONDITIONS = CoreMetrics.CONDITIONS_BY_LINE_KEY;

    static int countWithValue(final Map<Integer, Integer> data, final double value) {
        int count = 0;
        for (final Map.Entry<Integer, Integer> entry : data.entrySet()) {
            if (entry.getValue() == value) {
                count++;
            }
        }
        return count;
    }

    static void removeIgnores(final Map<Integer, ?> data, final Set<Integer> lines) {
        for (final Integer line : lines) {
            data.remove(line);
        }
    }

    static int sumValues(final Map<Integer, Integer> data) {
        int result = 0;
        for (final int value : data.values()) {
            result += value;
        }
        return result;
    }

    private MeasuresStorage measuresStorage = new MeasuresStorage();

    public void clear(final DecoratorContext context, final Metric metric) {
        measuresStorage.clear(context, metric);
    }

    @CheckForNull
    Map<String, Map<Integer, Integer>> filterConditionsData(final DecoratorContext context, final Set<Integer> lines) {
        final Measure conditions = context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE);
        if (conditions == null) {
            LOGGER.debug("no condition coverage data");
            return null;
        }

        final Map<String, Map<Integer, Integer>> result = new HashMap<>(2);

        final Map<Integer, Integer> conditionsByLine = filterLineValuePairs(conditions, lines);

        measuresStorage.replace(context, conditions);
        result.put(TOTAL_CONDITIONS, conditionsByLine);

        final Measure coveredConditions = context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
        final Map<Integer, Integer> coveredConditionsByLine = filterLineValuePairs(coveredConditions, lines);

        measuresStorage.replace(context, coveredConditions);
        result.put(COVERED_CONDITIONS, coveredConditionsByLine);

        return result;
    }

    public Map<Integer, Integer> filterLineValuePairs(final Measure measure, final Set<Integer> lines) {
        final String originalData = measure.getData();
        final Map<Integer, Integer> conditionsByLine = KeyValueFormat.parse(originalData, CONVERTER, CONVERTER);
        removeIgnores(conditionsByLine, lines);

        final String filteredData = KeyValueFormat.format(conditionsByLine, CONVERTER, CONVERTER);
        LOGGER.warn("updating {} ({} to {})", measure.getMetricKey(), originalData, filteredData);
        measure.setData(filteredData);
        return conditionsByLine;
    }

    @CheckForNull
    Map<Integer, Integer> filterLinesData(final DecoratorContext context, final Set<Integer> lines) {
        final Measure measure = context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA);
        if (measure == null) {
            LOGGER.debug("no coverage data");
            return null;
        }

        final Map<Integer, Integer> coverageByLines = filterLineValuePairs(measure, lines);
        measuresStorage.replace(context, measure);

        return coverageByLines;
    }

    MeasuresStorage getMeasuresStorage() {
        return measuresStorage;
    }

    public void rewrite(final DecoratorContext context, final Set<Integer> lines) {
        LOGGER.debug("processing {}", context.getResource().getKey());

        final Map<Integer, Integer> linesData = filterLinesData(context, lines);
        if (linesData == null) {
            LOGGER.debug("no coverage data available");
            return;
        }

        final long linesCountTotal = rewriteLinesCountTotal(context, linesData);
        final long linesCountUncovered = rewriteLinesCountUncovered(context, linesData);
        rewriteLinesCoveragePercentage(context, linesCountTotal, linesCountUncovered);

        final Map<String, Map<Integer, Integer>> conditionsData = filterConditionsData(context, lines);
        final long conditionsCountTotal;
        final long conditionsCountUncovered;
        if (conditionsData == null) {
            LOGGER.debug("no coverage data available");
            conditionsCountTotal = 0L;
            conditionsCountUncovered = 0L;
        } else {
            conditionsCountTotal = rewriteConditionsCountTotal(context, conditionsData);
            conditionsCountUncovered = rewriteConditionsCountUncovered(context, conditionsData);
            rewriteConditionsCoveragePercentage(context, conditionsCountTotal, conditionsCountUncovered);
        }

        rewriteOverallCoverage(context, linesCountTotal, linesCountUncovered, conditionsCountTotal, conditionsCountUncovered);
    }

    long rewriteConditionsCountTotal(final DecoratorContext context, final Map<String, Map<Integer, Integer>> conditionData) {
        final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
        final double newValue = sumValues(conditionData.get(TOTAL_CONDITIONS));
        LOGGER.debug("updating CONDITIONS_TO_COVER ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return (long) newValue;
    }

    long rewriteConditionsCountUncovered(final DecoratorContext context, final Map<String, Map<Integer, Integer>> conditionData) {
        final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS);

        final double conditionsCount = sumValues(conditionData.get(TOTAL_CONDITIONS));
        final double coveredConditionsCount = sumValues(conditionData.get(COVERED_CONDITIONS));
        final double newValue = Math.max((conditionsCount - coveredConditionsCount), 0.0);
        LOGGER.debug("updating UNCOVERED_CONDITIONS ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return (long) newValue;
    }

    double rewriteConditionsCoveragePercentage(final DecoratorContext context, final long total, final long uncovered) {
        final Measure measure = context.getMeasure(CoreMetrics.BRANCH_COVERAGE);
        final double newValue;
        if (total == 0L) {
            newValue = 0.0;
        } else {
            final long covered = (total - uncovered);
            newValue = covered * 100.0 / total;
        }

        LOGGER.debug("updating LINE_COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return newValue;
    }

    long rewriteLinesCountTotal(final DecoratorContext context, final Map<Integer, Integer> lineData) {
        final Measure measure = context.getMeasure(CoreMetrics.LINES_TO_COVER);
        final double newValue = lineData.size();
        LOGGER.debug("updating LINES_TO_COVER ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return (long) newValue;
    }

    long rewriteLinesCountUncovered(final DecoratorContext context, final Map<Integer, Integer> lineData) {
        final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_LINES);
        final double newValue = countWithValue(lineData, 0.0);
        LOGGER.debug("updating UNCOVERED_LINES ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return (long) newValue;
    }

    double rewriteLinesCoveragePercentage(final DecoratorContext context, final long total, final long uncovered) {
        final Measure measure = context.getMeasure(CoreMetrics.LINE_COVERAGE);
        final double newValue;
        if (total == 0L) {
            newValue = 0.0;
        } else {
            final long covered = (total - uncovered);
            newValue = covered * 100.0 / total;
        }

        LOGGER.debug("updating LINE_COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return newValue;
    }

    double rewriteOverallCoverage(final DecoratorContext context, final long lines, final long linesUncovered, final long conditions, final long conditionsUncovered) {
        final Measure measure = context.getMeasure(CoreMetrics.COVERAGE);

        final double newValue;
        final long total = lines + conditions;
        if (total == 0L) {
            newValue = 0.0;
        } else {
            final long coveredLines = lines - linesUncovered;
            final long coveredConditions = conditions - conditionsUncovered;
            newValue = ((coveredLines + coveredConditions) * 100.0) / (lines + conditions);
        }

        LOGGER.debug("updating COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        measuresStorage.replace(context, measure);
        return newValue;
    }

    void setMeasuresStorage(final MeasuresStorage measuresStorage) {
        this.measuresStorage = measuresStorage;
    }
}
