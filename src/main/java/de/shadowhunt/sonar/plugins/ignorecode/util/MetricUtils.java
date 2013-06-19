package de.shadowhunt.sonar.plugins.ignorecode.util;

import java.util.List;

import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;

import de.shadowhunt.sonar.plugins.ignorecode.model.LineValuePair;

public final class MetricUtils {

	public static List<LineValuePair> getCovarageValues(final DecoratorContext context, final Metric metric) {
		if (ValueType.DATA != metric.getType()) {
			throw new IllegalArgumentException("metric is no data type: " + metric);
		}

		final String data = context.getMeasure(metric).getData();
		return LineValuePair.parseDataString(data);
	}

	public static int getIntValue(final DecoratorContext context, final Metric metric) {
		if (ValueType.INT != metric.getType()) {
			throw new IllegalArgumentException("metric is no int type: " + metric);
		}

		return context.getMeasure(metric).getValue().intValue();
	}

	private MetricUtils() {
		// prevent instantiation
	}
}
