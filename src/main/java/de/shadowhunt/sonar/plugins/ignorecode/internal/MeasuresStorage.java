package de.shadowhunt.sonar.plugins.ignorecode.internal;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import com.google.common.collect.ListMultimap;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.Bucket;

public class MeasuresStorage {

    public static void clear(final DecoratorContext context, final Metric metric) {
        try {
            clear0(context, metric);
        } catch (final Exception e) {
            throw new SonarException("could not replace measure", e);
        }
    }

    static void clear0(DecoratorContext context, Metric metric) throws Exception {
        final String metricKey = metric.getKey();

        final ListMultimap<String, Measure> measuresByMetric = getMeasuresByMetric(context);
        final List<Measure> metricMeasures = measuresByMetric.get(metricKey);
        if (metricMeasures == null) {
            return;
        }
        metricMeasures.clear();
    }

    @CheckForNull
    static ListMultimap<String, Measure> getMeasuresByMetric(final DecoratorContext context) throws Exception {
        final SonarIndex index = getPrivateField(context, "index");
        if (index == null) {
            return null;
        }

        final Map<Resource, Bucket> buckets = getPrivateField(index, "buckets");
        if (buckets == null) {
            return null;
        }

        final Bucket bucket = buckets.get(context.getResource());
        return getPrivateField(bucket, "measuresByMetric");
    }

    @SuppressWarnings("unchecked")
    @CheckForNull
    static <E> E getPrivateField(final Object object, final String fieldName) throws Exception {
        final Class<?> contextClass = object.getClass();
        final Field field = contextClass.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (E) field.get(object);
    }

    public static void replace(final DecoratorContext context, final Measure measure) {
        try {
            replace0(context, measure);
        } catch (final Exception e) {
            throw new SonarException("could not replace measure", e);
        }
    }

    static void replace0(DecoratorContext context, Measure measure) throws Exception {
        final String metricKey = measure.getMetricKey();

        final ListMultimap<String, Measure> measuresByMetric = getMeasuresByMetric(context);
        final List<Measure> metricMeasures = measuresByMetric.get(metricKey);
        if (metricMeasures == null) {
            return;
        }

        final int index = metricMeasures.indexOf(measure);
        if (index > -1) {
            metricMeasures.set(index, measure);
        } else {
            metricMeasures.add(measure);
        }
    }
}
