package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Decorator;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.DependsUpon;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasureUtils;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.Bucket;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.MeasurePersister;

import com.google.common.collect.ListMultimap;

import de.shadowhunt.sonar.plugins.ignorecode.model.LinePattern;
import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

/**
 * Generated code as identified by the coverage.ignore, must not be covered by any unit tests.
 * Therefore the {@link IgnoreMissingCoverageDecorator} goes through all coverage metrics and removes
 * all entries for identified sources
 */
public class IgnoreMissingCoverageDecorator implements Decorator {

	/**
	 * property name that points to the ignore file: will be read from the project configuration
	 */
	public static final String CONFIG_FILE = "sonar.ignorecoverage.configFile";

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreMissingCoverageDecorator.class);

	static final Map<String, Set<Integer>> loadIgnores(final Configuration configuration) {
		if (configuration == null) {
			return Collections.emptyMap();
		}

		final String fileLocation = configuration.getString(CONFIG_FILE);
		if (StringUtils.isBlank(fileLocation)) {
			LOGGER.info("no ignore file configured for property: {}", CONFIG_FILE);
			return Collections.emptyMap();
		}

		final File ignoreFile = new File(fileLocation);
		if (!ignoreFile.isFile()) {
			LOGGER.error("could not find ignore file: {}", ignoreFile);
			return Collections.emptyMap();
		}

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(ignoreFile);
			final Map<String, Set<Integer>> ignores = new HashMap<String, Set<Integer>>();

			final Collection<LinePattern> patterns = LinePattern.parse(fis);
			for (final LinePattern pattern : LinePattern.merge(patterns)) {
				ignores.put(pattern.getResource(), pattern.getLines());
			}
			LOGGER.info("loaded {} coverage ignores from {}", patterns.size(), ignoreFile);
			return ignores;
		} catch (final Exception e) {
			throw new SonarException("could not load ignores for file: " + ignoreFile, e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	private final Map<String, Set<Integer>> ignores;

	private final MeasurePersister persister;

	/**
	 * Create a new {@link IgnoreMissingCoverageDecorator} that removes all coverage metrics for ignored code
	 * @param persister {@link MeasurePersister} save the updated {@link Measure}s to the database (not supported via {@link DecoratorContext})
	 * @param configuration project {@link Configuration}
	 */
	public IgnoreMissingCoverageDecorator(final MeasurePersister persister, final Configuration configuration) {
		super();
		this.persister = persister;

		ignores = loadIgnores(configuration);
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

	private void overrideMeasure(final DecoratorContext context, final Measure measure) {
		try {
			overrideMeasure0(context, measure);
		} catch (final Exception e) {
			throw new SonarException("could not override measure", e);
		}
	}

	private void overrideMeasure0(final DecoratorContext context, final Measure measure) throws Exception {
		final Field indexField = context.getClass().getDeclaredField("index");
		indexField.setAccessible(true);
		final SonarIndex index = (DefaultIndex) indexField.get(context);

		final Field bucketField = index.getClass().getDeclaredField("buckets");
		bucketField.setAccessible(true);
		@SuppressWarnings("unchecked")
		final Map<Resource<?>, Bucket> buckets = (Map<Resource<?>, Bucket>) bucketField.get(index);

		final Resource<?> resource = context.getResource();
		final Bucket bucket = buckets.get(resource);

		final Field measuresByMetricField = bucket.getClass().getDeclaredField("measuresByMetric");
		measuresByMetricField.setAccessible(true);
		@SuppressWarnings("unchecked")
		final ListMultimap<String, Measure> measuresByMetric = (ListMultimap<String, Measure>) measuresByMetricField.get(bucket);
		final List<Measure> metricMeasures = measuresByMetric.get(measure.getMetric().getKey());
		final int measureIndex = metricMeasures.indexOf(measure);
		if (measureIndex < 0) {
			metricMeasures.add(measure);
		} else {
			metricMeasures.add(measureIndex, measure);
		}

		if (measure.getPersistenceMode().useDatabase()) {
			persister.saveMeasure(resource, measure);
		}
	}

	@Override
	public boolean shouldExecuteOnProject(final Project project) {
		return Java.INSTANCE.equals(project.getLanguage());
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
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
