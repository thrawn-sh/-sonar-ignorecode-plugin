package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.MeasurePersister;

import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

/**
 * Generated code as identified by the coverage.ignore, must not be covered by any unit tests.
 * Therefore the {@link IgnoreMissingCoverageDecorator} goes through all coverage metrics and removes
 * all entries for identified sources
 */
public class IgnoreMissingCoverageDecorator extends AbstractDecorator {

	public static final String CONFIG_FILE = "sonar.ignorecoverage.configFile";

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreMissingCoverageDecorator.class);

	private final Configuration configuration;

	private final MeasurePersister persister;

	/**
	 * Create a new {@link IgnoreMissingCoverageDecorator} that removes all coverage metrics for ignored code
	 * @param persister {@link MeasurePersister} save the updated {@link Measure}s to the database (not supported via {@link DecoratorContext})
	 * @param configuration project {@link Configuration}
	 */
	public IgnoreMissingCoverageDecorator(final MeasurePersister persister, final Configuration configuration) {
		super();
		this.persister = persister;
		this.configuration = configuration;

		loadIgnores();
	}

	@Override
	public void decorate(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context) {
		if (!ResourceUtils.isFile(resource)) {
			return;
		}

		final String resourceKey = resource.getKey();
		LOGGER.debug("processing resource with key: " + resourceKey);
		final Set<Integer> resourceIgnores = ignores.get(resourceKey);
		if ((resourceIgnores == null) || resourceIgnores.isEmpty()) {
			LOGGER.debug("no coverage data must be filtered");
			return;
		}

		if (context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA) == null) {
			LOGGER.debug("no coverage data available");
			return;
		}

		filterLineCoverage(resource, context, resourceIgnores);
		filterConditionCoverage(resource, context, resourceIgnores);
	}

	@DependsUpon
	public List<Metric> dependsUponMetrics() {
		return Arrays.asList(CoreMetrics.CONDITIONS_BY_LINE, //
				CoreMetrics.CONDITIONS_TO_COVER, //
				CoreMetrics.COVERAGE_LINE_HITS_DATA, //
				CoreMetrics.COVERED_CONDITIONS_BY_LINE, //
				CoreMetrics.LINES_TO_COVER, //
				CoreMetrics.LINE_COVERAGE, //
				CoreMetrics.UNCOVERED_CONDITIONS);
	}

	void filterConditionCoverage(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context, final Set<Integer> resourceIgnores) {
		final List<LineValuePair> conditionsByLines;
		{
			final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE);
			conditionsByLines = LineValuePair.parseDataString(measure.getData());
			LineValuePair.removeIgnores(conditionsByLines, resourceIgnores);
			measure.setData(LineValuePair.toDataString(conditionsByLines));
			persister.saveMeasure(resource, measure);
		}

		final int toCover = LineValuePair.sumValues(conditionsByLines);
		{
			final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
			measure.setValue((double) toCover);
			persister.saveMeasure(resource, measure);
		}

		final List<LineValuePair> covered;
		{
			final Measure measure = context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE);
			covered = LineValuePair.parseDataString(measure.getData());
			LineValuePair.removeIgnores(covered, resourceIgnores);
			measure.setData(LineValuePair.toDataString(covered));
			persister.saveMeasure(resource, measure);
		}

		final int uncovered = (toCover == 0) ? 0 : (toCover - LineValuePair.sumValues(covered));
		{
			final Measure measure = context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS);
			measure.setValue((double) uncovered);
			persister.saveMeasure(resource, measure);
		}
	}

	void filterLineCoverage(@SuppressWarnings("rawtypes") final Resource resource, final DecoratorContext context, final Set<Integer> resourceIgnores) {
		final List<LineValuePair> coverageByLines;
		{
			final Measure measure = context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA);
			coverageByLines = LineValuePair.parseDataString(measure.getData());
			LineValuePair.removeIgnores(coverageByLines, resourceIgnores);
			measure.setData(LineValuePair.toDataString(coverageByLines));
			persister.saveMeasure(resource, measure);
		}

		final int toCover = coverageByLines.size();
		{
			final Measure measure = context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER);
			measure.setValue((double) toCover);
			persister.saveMeasure(resource, measure);
		}

		final int covered = (toCover == 0) ? 0 : LineValuePair.sumValues(coverageByLines);
		{
			final Measure measure = context.getMeasure(CoreMetrics.LINE_COVERAGE);
			measure.setValue((double) covered);
			persister.saveMeasure(resource, measure);
		}
	}

	@Override
	protected String getConfigurationLocation() {
		if (configuration == null) {
			return null;
		}
		return configuration.getString(CONFIG_FILE);
	}
}
