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

import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;

import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

public class ModifyMeasures {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyMeasures.class);

    public static void rewrite(final DecoratorContext context, final Set<Integer> lines) {
        final List<LineValuePair> lineCoverage = rewriteLineHitsData(context, lines);
        if (lineCoverage == null) {
            LOGGER.debug("no coverage data available");
            return;
        }

        rewriteLinesToCover(context, lineCoverage);
        rewriteUncoveredLines(context, lineCoverage);
        rewriteLineCoverage(context, lineCoverage);

        final List<LineValuePair> conditionCoverage = rewriteLineHitsData(context, lines);
        if (conditionCoverage == null) {
            LOGGER.debug("no coverage data available");
            return;
        } else {
            rewriteConditionsToCover(context, conditionCoverage);
            rewriteUncoveredConditions(context, conditionCoverage);
            rewriteConditionCoverage(context, conditionCoverage);
        }

        rewriteOverallCoverage(context);
    }

    static void rewriteConditionCoverage(final DecoratorContext context, final List<LineValuePair> conditionCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.BRANCH_COVERAGE);
        final double newValue;
        if (conditionCoverage.isEmpty()) {
            newValue = 0.0;
        } else {
            final int covered = LineValuePair.countNotWithValue(conditionCoverage, 0.0);
            newValue = (covered * 100.0) / conditionCoverage.size();
        }
        LOGGER.warn("updating BRANCH_COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    @CheckForNull
    static List<LineValuePair> rewriteConditionsByLine(final DecoratorContext context, final Set<Integer> lines) {
        final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE);
        if (measure == null) {
            LOGGER.debug("no condition coverage data");
            return null;
        }

        final String data = measure.getData();
        final List<LineValuePair> conditionsByLine = LineValuePair.parseDataString(data);
        LineValuePair.removeIgnores(conditionsByLine, lines);

        final String newData = LineValuePair.toDataString(conditionsByLine);
        LOGGER.warn("updating CONDITIONS_BY_LINE ({} to {})", data, newData);
        measure.setData(newData);

        MeasuresStorage.replace(context, measure);
        return conditionsByLine;
    }

    static void rewriteConditionsToCover(final DecoratorContext context, final List<LineValuePair> conditionCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
        final double newValue = conditionCoverage.size();
        LOGGER.warn("updating CONDITIONS_TO_COVER ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    static void rewriteLineCoverage(final DecoratorContext context, final List<LineValuePair> lineCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.LINE_COVERAGE);
        final double newValue;
        if (lineCoverage.isEmpty()) {
            newValue = 0.0;
        } else {
            final int covered = LineValuePair.countNotWithValue(lineCoverage, 0.0);
            newValue = (covered * 100.0) / lineCoverage.size();
        }
        LOGGER.warn("updating LINE_COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    @CheckForNull
    static List<LineValuePair> rewriteLineHitsData(final DecoratorContext context, final Set<Integer> lines) {
        final Measure measure = context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA);
        if (measure == null) {
            LOGGER.debug("no coverage data");
            return null;
        }

        final String data = measure.getData();
        final List<LineValuePair> coverageByLines = LineValuePair.parseDataString(data);
        LineValuePair.removeIgnores(coverageByLines, lines);

        final String newData = LineValuePair.toDataString(coverageByLines);
        LOGGER.warn("updating COVERAGE_LINE_HITS_DATA ({} to {})", data, newData);
        measure.setData(newData);

        MeasuresStorage.replace(context, measure);
        return coverageByLines;
    }

    static void rewriteLinesToCover(final DecoratorContext context, final List<LineValuePair> lineCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.LINES_TO_COVER);
        final double newValue = lineCoverage.size();
        LOGGER.warn("updating LINES_TO_COVER ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    static void rewriteOverallCoverage(final DecoratorContext context) {
        final double lines = MeasureUtils.getValue(context.getMeasure(CoreMetrics.LINES_TO_COVER), 0.0);
        final double conditions = MeasureUtils.getValue(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER), 0.0);

        final double uncoveredLines = MeasureUtils.getValue(context.getMeasure(CoreMetrics.UNCOVERED_LINES), 0.0);
        final double uncoveredConditions = MeasureUtils.getValue(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS), 0.0);

        final double coveredLines = lines - uncoveredLines;
        final double coveredConditions = conditions - uncoveredConditions;

        final double newValue = ((coveredLines + coveredConditions) * 100.0) / (lines + conditions);
        final Measure measure = context.getMeasure(CoreMetrics.COVERAGE);
        LOGGER.debug("updating COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);
        MeasuresStorage.replace(context, measure);
    }

    static void rewriteUncoveredConditions(final DecoratorContext context, final List<LineValuePair> conditionCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS);
        final double newValue = LineValuePair.countWithValue(conditionCoverage, 0.0);
        LOGGER.warn("updating UNCOVERED_CONDITIONS ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    static void rewriteUncoveredLines(final DecoratorContext context, final List<LineValuePair> lineCoverage) {
        final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_LINES);
        final double newValue = LineValuePair.countWithValue(lineCoverage, 0.0);
        LOGGER.warn("updating UNCOVERED_LINES ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }
}
