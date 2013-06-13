package de.shadowhunt.sonar.plugins.ignorecode.measures;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.MeasuresFilter;

import de.shadowhunt.sonar.plugins.ignorecode.model.IgnorePattern;
import de.shadowhunt.sonar.plugins.ignorecode.util.PatternDecoder;

public class IgnoreMeasureFilter implements MeasuresFilter<Collection<Measure>> {

	private static final Logger LOGGER = LoggerFactory.getLogger(IgnoreMeasureFilter.class);

	private final List<IgnorePattern> patterns;

	public IgnoreMeasureFilter(final Configuration configuration) {
		patterns = PatternDecoder.loadIgnorePatterns(configuration);
	}

	@Override
	public Collection<Measure> filter(@Nullable final Collection<Measure> measures) {
		if (measures == null) {
			return null;
		}

		final Collection<Measure> filtered = new ArrayList<Measure>(measures.size());
		for (final Measure measure : measures) {
			LOGGER.warn("SH: " + measure);
			filtered.add(measure);
		}
		return filtered;
	}
}
