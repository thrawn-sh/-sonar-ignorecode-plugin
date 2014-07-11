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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;

import de.shadowhunt.sonar.plugins.ignorecode.internal.MeasuresStorage;
import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

public class ModifyMeasures {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyMeasures.class);

    public static void rewrite(final DecoratorContext context, final Set<Integer> lines) {
        if (context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA) == null) {
            LOGGER.debug("no coverage data available");
            return;
        }

        final List<LineValuePair> lvp = rewriteLineHitsData(context, lines);
        rewriteLinesToCover(context, lvp);
        rewriteUncoveredLines(context, lvp);
        rewriteLineCoverage(context, lvp);

        rewriteOverallCoverage(context);
    }

    static void rewriteLineCoverage(final DecoratorContext context, final List<LineValuePair> lvp) {
        final Measure measure = context.getMeasure(CoreMetrics.LINE_COVERAGE);
        final double newValue;
        if (lvp.isEmpty()) {
            newValue = 0.0;
        } else {
            final int covered = LineValuePair.countNotWithValue(lvp, 0.0);
            newValue = (covered * 100.0) / lvp.size();
        }
        LOGGER.warn("updating LINE_COVERAGE ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    static List<LineValuePair> rewriteLineHitsData(final DecoratorContext context, final Set<Integer> lines) {
        final Measure measure = context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA);

        final List<LineValuePair> coverageByLines = LineValuePair.parseDataString(measure.getData());
        LineValuePair.removeIgnores(coverageByLines, lines);

        final String newData = LineValuePair.toDataString(coverageByLines);
        LOGGER.warn("updating COVERAGE_LINE_HITS_DATA ({} to {})", measure.getData(), newData);
        measure.setData(newData);

        MeasuresStorage.replace(context, measure);
        return coverageByLines;
    }

    static void rewriteLinesToCover(final DecoratorContext context, final List<LineValuePair> lvp) {
        final Measure measure = context.getMeasure(CoreMetrics.LINES_TO_COVER);
        final double newValue = lvp.size();
        LOGGER.warn("updating LINES_TO_COVER ({} to {})", measure.getValue(), newValue);
        measure.setValue(newValue);

        MeasuresStorage.replace(context, measure);
    }

    static void rewriteUncoveredLines(final DecoratorContext context, final List<LineValuePair> lvp) {
        final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_LINES);
        final double newValue = LineValuePair.countWithValue(lvp, 0.0);
        LOGGER.warn("updating UNCOVERED_LINES ({} to {})", measure.getValue(), newValue);
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

    // FIXME
    void filterConditionCoverage(final DecoratorContext context, final Set<Integer> resourceIgnores) {
        final List<LineValuePair> conditionsByLines;
        {
            final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE);
            if (measure == null) {
                LOGGER.debug("no conditions in resource => skipping");
                // resource without conditional code
                return;
            }

            conditionsByLines = LineValuePair.parseDataString(measure.getData());
            LineValuePair.removeIgnores(conditionsByLines, resourceIgnores);
            final String data = LineValuePair.toDataString(conditionsByLines);
            LOGGER.debug("updating measure CONDITIONS_BY_LINE from {} to {}", measure.getData(), data);
            measure.setData(data);
            MeasuresStorage.replace(context, measure);
        }

        final double toCover = LineValuePair.sumValues(conditionsByLines);
        {
            final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
            LOGGER.debug("updating measure CONDITIONS_TO_COVER from {} to {}", measure.getValue(), toCover);
            measure.setValue(toCover);
            MeasuresStorage.replace(context, measure);
        }

        final List<LineValuePair> covered;
        {
            final Measure measure = context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
            covered = LineValuePair.parseDataString(measure.getData());
            LineValuePair.removeIgnores(covered, resourceIgnores);
            final String data = LineValuePair.toDataString(covered);
            LOGGER.debug("updating measure COVERED_CONDITIONS_BY_LINE from {} to {}", measure.getData(), data);
            measure.setData(data);
            MeasuresStorage.replace(context, measure);
        }

        final double uncovered = (toCover == 0.0) ? 0.0 : (toCover - LineValuePair.sumValues(covered));
        {
            final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS);
            LOGGER.debug("updating measure UNCOVERED_CONDITIONS from {} to {}", measure.getValue(), uncovered);
            measure.setValue(uncovered);
            MeasuresStorage.replace(context, measure);
        }

        {
            final Measure measure = context.getMeasure(CoreMetrics.BRANCH_COVERAGE);
            final double value;
            if (toCover > 0) {
                value = ((toCover - uncovered) * 100.0) / toCover;
            } else {
                value = 0.0;
            }
            LOGGER.debug("updating measure BRANCH_COVERAGE from {} to {}", measure.getValue(), value);
            measure.setValue(value);
            MeasuresStorage.replace(context, measure);
        }
    }
}
