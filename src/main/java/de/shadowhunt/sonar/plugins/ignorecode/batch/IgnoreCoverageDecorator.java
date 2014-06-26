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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.MeasurePersister;

import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

/**
 * Generated code as identified by the coverage.ignore, must not be covered by any unit tests.
 * Therefore the {@link IgnoreCoverageDecorator} goes through all coverage metrics and removes
 * all entries for identified sources
 */
public class IgnoreCoverageDecorator extends AbstractOverridingDecorator {

	/**
	 * property name that points to the ignore file: will be read from the project configuration
	 */
	public static final String CONFIG_FILE = "sonar.ignorecoverage.configFile";

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreCoverageDecorator.class);

	private final Map<String, Set<Integer>> ignores;

	/**
	 * Create a new {@link IgnoreCoverageDecorator} that removes all coverage metrics for ignored code
	 * @param persister {@link MeasurePersister} save the updated {@link Measure}s to the database (not supported via {@link DecoratorContext})
	 * @param configuration project {@link Configuration}
	 */
	public IgnoreCoverageDecorator(final MeasurePersister persister, final Configuration configuration) {
		super(persister);

		ignores = loadIgnores(configuration, CONFIG_FILE);
	}

	@Override
	public void decorate(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context) {
		if (!ResourceUtils.isFile(resource)) {
			return;
		}

		final String resourceKey = resource.getKey();
		LOGGER.info("processing resource with key: " + resourceKey);
		final Set<Integer> resourceIgnores = ignores.get(resourceKey);
		if (resourceIgnores == null) {
			LOGGER.debug("no coverage data must be filtered");
			return;
		}

		if (context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA) == null) {
			LOGGER.info("no coverage data available");
			return;
		}

		filterLineCoverage(context, resourceIgnores);
		filterConditionCoverage(context, resourceIgnores);
		updateCoverage(context);
	}

	/**
	 * Returns a {@link List} of {@link Metric} this decorator wants to work with
	 * @return the {@link List} of {@link Metric} this decorator wants to work with
	 */
	@DependsUpon
	public List<Metric> dependsUponMetrics() {
		return Arrays.asList(CoreMetrics.BRANCH_COVERAGE, //
				CoreMetrics.CONDITIONS_BY_LINE, //
				CoreMetrics.CONDITIONS_TO_COVER, //
				CoreMetrics.COVERAGE, //
				CoreMetrics.COVERAGE_LINE_HITS_DATA, //
				CoreMetrics.COVERED_CONDITIONS_BY_LINE, //
				CoreMetrics.LINES_TO_COVER, //
				CoreMetrics.LINE_COVERAGE, //
				CoreMetrics.UNCOVERED_CONDITIONS, //
				CoreMetrics.UNCOVERED_LINES);
	}

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
			LOGGER.debug("updating maesure CONDITIONS_BY_LINE from {} to {}", measure.getData(), data);
			measure.setData(data);
			overrideMeasure(context, measure);
		}

		final double toCover = LineValuePair.sumValues(conditionsByLines);
		{
			final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
			LOGGER.debug("updating maesure CONDITIONS_TO_COVER from {} to {}", measure.getValue(), toCover);
			measure.setValue(toCover);
			overrideMeasure(context, measure);
		}

		final List<LineValuePair> covered;
		{
			final Measure measure = context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
			covered = LineValuePair.parseDataString(measure.getData());
			LineValuePair.removeIgnores(covered, resourceIgnores);
			final String data = LineValuePair.toDataString(covered);
			LOGGER.debug("updating maesure COVERED_CONDITIONS_BY_LINE from {} to {}", measure.getData(), data);
			measure.setData(data);
			overrideMeasure(context, measure);
		}

		final double uncovered = (toCover == 0.0) ? 0.0 : (toCover - LineValuePair.sumValues(covered));
		{
			final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS);
			LOGGER.debug("updating maesure UNCOVERED_CONDITIONS from {} to {}", measure.getValue(), uncovered);
			measure.setValue(uncovered);
			overrideMeasure(context, measure);
		}

		{
			final Measure measure = context.getMeasure(CoreMetrics.BRANCH_COVERAGE);
			final double value;
			if (toCover > 0) {
				value = ((toCover - uncovered) * 100.0) / toCover;
			} else {
				value = 0.0;
			}
			LOGGER.debug("updating maesure BRANCH_COVERAGE from {} to {}", measure.getValue(), value);
			measure.setValue(value);
			overrideMeasure(context, measure);
		}
	}

	void filterLineCoverage(final DecoratorContext context, final Set<Integer> resourceIgnores) {
		final List<LineValuePair> coverageByLines;
		{
			final Measure measure = context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA);
			coverageByLines = LineValuePair.parseDataString(measure.getData());
			LineValuePair.removeIgnores(coverageByLines, resourceIgnores);
			final String data = LineValuePair.toDataString(coverageByLines);
			LOGGER.debug("updating maesure COVERAGE_LINE_HITS_DATA from {} to {}", measure.getData(), data);
			measure.setData(data);
			overrideMeasure(context, measure);
		}

		final double toCover = coverageByLines.size();
		{
			final Measure measure = context.getMeasure(CoreMetrics.LINES_TO_COVER);
			LOGGER.debug("updating maesure LINES_TO_COVER from {} to {}", measure.getValue(), toCover);
			measure.setValue(toCover);
			overrideMeasure(context, measure);
		}

		final double uncovered = (toCover == 0.0) ? 0.0 : (toCover - LineValuePair.sumValues(coverageByLines));
		{
			final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_LINES);
			LOGGER.debug("updating maesure UNCOVERED_LINES from {} to {}", measure.getValue(), uncovered);
			measure.setValue(uncovered);
			overrideMeasure(context, measure);
		}

		{
			final Measure measure = context.getMeasure(CoreMetrics.LINE_COVERAGE);
			final double value;
			if (toCover > 0) {
				value = ((toCover - uncovered) * 100.0) / toCover;
			} else {
				value = 0.0;
			}
			LOGGER.debug("updating maesure LINE_COVERAGE from {} to {}", measure.getValue(), value);
			measure.setValue(value);
			overrideMeasure(context, measure);
		}
	}

	void updateCoverage(final DecoratorContext context) {
		final double lines = MeasureUtils.getValue(context.getMeasure(CoreMetrics.LINES_TO_COVER), 0.0);
		final double conditions = MeasureUtils.getValue(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER), 0.0);

		final double uncoverdLines = MeasureUtils.getValue(context.getMeasure(CoreMetrics.UNCOVERED_LINES), 0.0);
		final double uncoverdConditions = MeasureUtils.getValue(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS), 0.0);

		final double coveredLines = lines - uncoverdLines;
		final double coveredConditions = conditions - uncoverdConditions;

		final double coverage = ((coveredLines + coveredConditions) * 100.0) / (lines + conditions);
		final Measure measure = context.getMeasure(CoreMetrics.COVERAGE);
		LOGGER.debug("updating maesure COVERAGE from {} to {}", measure.getValue(), coverage);
		measure.setValue(coverage);
		overrideMeasure(context, measure);
	}
}
