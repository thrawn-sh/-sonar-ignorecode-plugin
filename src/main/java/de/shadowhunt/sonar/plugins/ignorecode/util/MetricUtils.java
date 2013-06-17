package de.shadowhunt.sonar.plugins.ignorecode.util;

import java.util.List;

import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metric.ValueType;

import de.shadowhunt.sonar.plugins.ignorecode.model.CovarageValue;

public class MetricUtils {

	public static int getIntValue(final DecoratorContext context, final Metric metric) {
		if (!ValueType.INT.equals(metric.getType())) {
			throw new IllegalArgumentException("metric is no int type: " + metric);
		}

		return context.getMeasure(metric).getValue().intValue();
	}

	public static List<CovarageValue> getCovarageValues(final DecoratorContext context, final Metric metric) {
		if (!ValueType.DATA.equals(metric.getType())) {
			throw new IllegalArgumentException("metric is no data type: " + metric);
		}

		final String data = context.getMeasure(metric).getData();
		return CovarageValue.parseDataString(data);
	}
}
