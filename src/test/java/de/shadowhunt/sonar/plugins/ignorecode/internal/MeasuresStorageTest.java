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
package de.shadowhunt.sonar.plugins.ignorecode.internal;

import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.MetricFinder;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.ProjectTree;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.PersistenceManager;
import org.sonar.batch.index.ResourceKeyMigration;
import org.sonar.batch.issue.DeprecatedViolations;
import org.sonar.core.component.ScanGraph;
import org.sonar.core.measure.MeasurementFilters;

public class MeasuresStorageTest {

    private static class TestClass {

        private final int field = 5;
    }

    private DecoratorContext mockDecoratorContext(final Resource resource, final Measure measure, final SonarIndex index) {
        final List<DecoratorContext> childrenContexts = Collections.emptyList();
        final MeasurementFilters measurementFilters = new MeasurementFilters();
        return new DefaultDecoratorContext(resource, index, childrenContexts, measurementFilters);
    }

    private SonarIndex mockSonarIndex(final Resource resource, final Measure measure) {
        final Metric metric = measure.getMetric();

        final PersistenceManager persistence = Mockito.mock(PersistenceManager.class);
        Mockito.when(persistence.reloadMeasure(Mockito.any(Measure.class))).thenReturn(measure);
        final ProjectTree projectTree = Mockito.mock(ProjectTree.class);
        final Project project = new Project("test");
        Mockito.when(projectTree.getRootProject()).thenReturn(project);
        final MetricFinder metricFinder = Mockito.mock(MetricFinder.class);
        Mockito.when(metricFinder.findByKey(metric.getKey())).thenReturn(metric);
        final ScanGraph graph = Mockito.mock(ScanGraph.class);
        final DeprecatedViolations deprecatedViolations = Mockito.mock(DeprecatedViolations.class);
        final ResourceKeyMigration migration = Mockito.mock(ResourceKeyMigration.class);

        final DefaultIndex index = new DefaultIndex(persistence, projectTree, metricFinder, graph, deprecatedViolations, migration);
        index.start();
        index.index(resource);
        index.addMeasure(resource, measure);
        return index;
    }

    @Test
    public void testClear() throws Exception {
        final Resource resource = File.create("Test.java");
        final Measure measure = new Measure(CoreMetrics.COVERAGE, 42.0);
        final Metric metric = measure.getMetric();
        final SonarIndex index = mockSonarIndex(resource, measure);
        final DecoratorContext context = mockDecoratorContext(resource, measure, index);

        final MeasuresStorage measuresStorage = new MeasuresStorage();
        Assert.assertEquals("index must contain initial measure", measure, index.getMeasure(resource, metric));
        measuresStorage.clear(context, metric);
        Assert.assertNull("measure must be gone", index.getMeasure(resource, metric));
    }

    @Test
    public void testGetPrivateField() throws Exception {
        final TestClass tc = new TestClass();
        final int fieldValue = MeasuresStorage.getPrivateField(tc, "field");

        Assert.assertEquals("fieldValue must match", tc.field, fieldValue);
    }

    @Test(expected = Exception.class)
    public void testGetPrivateFieldMissing() throws Exception {
        final TestClass tc = new TestClass();
        final int fieldValue = MeasuresStorage.getPrivateField(tc, "missing");
        Assert.fail("missing field must lead to error");
    }

    @Test
    public void testReplace() throws Exception {
        final Resource resource = File.create("Test.java");
        final Measure measure = new Measure(CoreMetrics.COVERAGE, 42.0);
        final Metric metric = measure.getMetric();
        final SonarIndex index = mockSonarIndex(resource, measure);
        final DecoratorContext context = mockDecoratorContext(resource, measure, index);

        final MeasuresStorage measuresStorage = new MeasuresStorage();
        Assert.assertEquals("index must contain initial measure", measure, index.getMeasure(resource, metric));
        final Measure replacement = new Measure(CoreMetrics.COVERAGE, 23.0);
        measuresStorage.replace(context, replacement);
        Assert.assertEquals("measure must be gone", replacement, index.getMeasure(resource, metric));
    }

    @Test
    public void testReplaceNonExisting() throws Exception {
        final Resource resource = File.create("Test.java");
        final Measure measure = new Measure(CoreMetrics.COVERAGE, 42.0);
        final Metric metric = measure.getMetric();
        final SonarIndex index = mockSonarIndex(resource, measure);
        final DecoratorContext context = mockDecoratorContext(resource, measure, index);

        final MeasuresStorage measuresStorage = new MeasuresStorage();
        Assert.assertEquals("index must contain initial measure", measure, index.getMeasure(resource, metric));
        final Measure replacement = new Measure(CoreMetrics.COVERAGE, 23.0);
        measuresStorage.clear(context, metric);
        Assert.assertNull("measure must be gone", index.getMeasure(resource, metric));
        measuresStorage.replace(context, replacement);
        Assert.assertEquals("measure must be gone", replacement, index.getMeasure(resource, metric));
    }
}
