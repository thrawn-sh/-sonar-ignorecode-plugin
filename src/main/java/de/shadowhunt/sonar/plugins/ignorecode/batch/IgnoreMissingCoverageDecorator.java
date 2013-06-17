package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;

import de.shadowhunt.sonar.plugins.ignorecode.model.CovarageValue;
import de.shadowhunt.sonar.plugins.ignorecode.util.MetricUtils;

/**
 * Generated code as identified by the coverage.ignore, must not be covered by any unit tests.
 * Therefore the {@link IgnoreMissingCoverageDecorator} goes through all coverage metrics and removes
 * all entries for identified sources
 */
public class IgnoreMissingCoverageDecorator implements Decorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreMissingCoverageDecorator.class);

	private static void filterConditionCoverage(final DecoratorContext context, final Set<Integer> ignores) {
		final List<CovarageValue> conditionsByLines = removeIgnores(context, CoreMetrics.CONDITIONS_BY_LINE, ignores);
		context.saveMeasure(new Measure(CoreMetrics.CONDITIONS_BY_LINE, CovarageValue.toDataString(conditionsByLines)));

		final int toCover = sumValues(conditionsByLines);
		context.saveMeasure(new Measure(CoreMetrics.CONDITIONS_TO_COVER, (double) toCover));

		final List<CovarageValue> covered = removeIgnores(context, CoreMetrics.COVERED_CONDITIONS_BY_LINE, ignores);
		context.saveMeasure(new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, CovarageValue.toDataString(covered)));

		final int uncovered = (toCover == 0) ? 0 : (toCover - sumValues(covered));
		context.saveMeasure(new Measure(CoreMetrics.UNCOVERED_CONDITIONS, (double) uncovered));
	}

	private static void filterLineCoverage(final DecoratorContext context, final Set<Integer> ignores) {
		final List<CovarageValue> coverageByLines = removeIgnores(context, CoreMetrics.COVERAGE_LINE_HITS_DATA, ignores);
		context.saveMeasure(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, CovarageValue.toDataString(coverageByLines)));

		final int toCover = coverageByLines.size();
		context.saveMeasure(new Measure(CoreMetrics.LINES_TO_COVER, (double) toCover));

		final int covered = (toCover == 0) ? 0 : sumValues(coverageByLines);
		context.saveMeasure(new Measure(CoreMetrics.LINE_COVERAGE, (double) covered));
	}

	private static List<CovarageValue> removeIgnores(final DecoratorContext context, final Metric metric, final Set<Integer> ignores) {
		final List<CovarageValue> covarageValues = MetricUtils.getCovarageValues(context, metric);
		for (final Iterator<CovarageValue> it = covarageValues.iterator(); it.hasNext();) {
			final CovarageValue covarageValue = it.next();
			if (ignores.contains(covarageValue.getLineNumber())) {
				it.remove();
			}
		}
		return covarageValues;
	}

	private static int sumValues(final List<CovarageValue> covarageValues) {
		int sum = 0;
		for (final CovarageValue covarageValue : covarageValues) {
			sum += covarageValue.getValue();
		}
		return sum;
	}

	private final Map<String, Set<Integer>> ignores;

	/**
	 * Create a new {@link IgnoreMissingCoverageDecorator} that removes all coverage metrics for ignored code
	 * @param configuration project {@link Configuration} (retrieved by the container via injection) 
	 */
	public IgnoreMissingCoverageDecorator(final Configuration configuration) {
		ignores = new HashMap<String, Set<Integer>>();
	}

	@Override
	public void decorate(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context) {
		if (!ResourceUtils.isFile(resource)) {
			return;
		}

		final String resourceKey = resource.getKey();
		LOGGER.info("processing resource with key: " + resourceKey); // debug TODO
		final Set<Integer> resourceIgnores = ignores.get(resourceKey);
		if ((resourceIgnores == null) || resourceIgnores.isEmpty()) {
			LOGGER.info("no coverage data must be filtered"); // debug TODO 
			return;
		}

		if (context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA) == null) {
			LOGGER.info("no coverage data available"); // debug TODO
			return;
		}

		filterLineCoverage(context, resourceIgnores);
		filterConditionCoverage(context, resourceIgnores);
	}

	@DependsUpon
	public List<Metric> dependsUponMetrics() {
		return Arrays.asList(//
		CoreMetrics.LINES_TO_COVER, //
				CoreMetrics.LINES_TO_COVER, //
				CoreMetrics.CONDITIONS_TO_COVER, //
				CoreMetrics.UNCOVERED_CONDITIONS, //
				CoreMetrics.COVERAGE_LINE_HITS_DATA, //
				CoreMetrics.COVERED_CONDITIONS_BY_LINE, //
				CoreMetrics.CONDITIONS_BY_LINE);
	}

	@Override
	public boolean shouldExecuteOnProject(final Project project) {
		return true; // TODO
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
