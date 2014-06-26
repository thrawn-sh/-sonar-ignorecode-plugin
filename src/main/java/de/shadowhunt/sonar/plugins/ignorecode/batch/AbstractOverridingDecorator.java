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
import java.lang.reflect.Field;
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
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.Bucket;
import org.sonar.batch.index.MeasurePersister;

import com.google.common.collect.ListMultimap;

import de.shadowhunt.sonar.plugins.ignorecode.model.LinePattern;

abstract class AbstractOverridingDecorator implements Decorator {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractOverridingDecorator.class);

	static final Map<String, Set<Integer>> loadIgnores(final Configuration configuration, final String property) {
		if (configuration == null) {
			return Collections.emptyMap();
		}

		final String fileLocation = configuration.getString(property);
		if (StringUtils.isBlank(fileLocation)) {
			LOGGER.info("no ignore file configured for property: {}", property);
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
			LOGGER.info("loaded {} ignores from {}", patterns.size(), ignoreFile);
			return ignores;
		} catch (final Exception e) {
			throw new SonarException("could not load ignores for file: " + ignoreFile, e);
		} finally {
			IOUtils.closeQuietly(fis);
		}
	}

	private final MeasurePersister persister;

	AbstractOverridingDecorator(final MeasurePersister persister) {
		this.persister = persister;
	}

	protected void overrideMeasure(final DecoratorContext context, final Measure measure) {
		try {
			overrideMeasure0(context, measure);
		} catch (final Exception e) {
			throw new SonarException("could not override measure", e);
		}
	}

	private void overrideMeasure0(final DecoratorContext context, final Measure measure) throws Exception {
		final Field indexField = context.getClass().getDeclaredField("index");
		indexField.setAccessible(true);
		final SonarIndex index = (SonarIndex) indexField.get(context);

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
	public final boolean shouldExecuteOnProject(final Project project) {
		return Java.INSTANCE.equals(project.getLanguage());
	}

	@Override
	public final String toString() {
		return getClass().getSimpleName();
	}
}
