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

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.File;
import org.sonar.api.resources.Scopes;

public class IgnoreCoverageMeasurementFilterTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testAccept() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;[1-20]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final File file = File.create("src/java/net/example/Foo.java");
        Assert.assertTrue("measure modified but accepted", filter.accept(file, new Measure(CoreMetrics.COVERAGE, 42.0)));
    }

    @Test
    public void testAcceptAllLines() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;*");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final File file = File.create("src/java/net/example/Foo.java");
        Assert.assertFalse("matching on all lines", filter.accept(file, new Measure(CoreMetrics.COVERAGE, 42.0)));
    }

    @Test
    public void testAcceptDirectory() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final Directory directory = Mockito.mock(Directory.class);
        Mockito.when(directory.getScope()).thenReturn(Scopes.DIRECTORY);
        Assert.assertTrue("don't filter directories", filter.accept(directory, null));
    }

    @Test
    public void testAcceptNotFilteredMetric() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final File file = Mockito.mock(File.class);
        Mockito.when(file.getScope()).thenReturn(Scopes.FILE);
        final Metric metric = Mockito.mock(Metric.class);
        Mockito.when(metric.getKey()).thenReturn("test");
        Assert.assertTrue("don't filter directories", filter.accept(file, new Measure(metric, 42.0)));
    }

    @Test
    public void testAcceptNotMatchedResource() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;[1-20]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final File file = File.create("src/java/net/example/Bar.java");
        Assert.assertTrue("non matching file", filter.accept(file, new Measure(CoreMetrics.COVERAGE, 42.0)));
    }

    @Test
    public void testRewrite() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final Set<Integer> lines = Collections.emptySet();
        // unit test
        filter.rewrite(new Measure(CoreMetrics.COVERAGE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, 42.0), lines);
        // integration test
        filter.rewrite(new Measure(CoreMetrics.IT_COVERAGE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.IT_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.IT_COVERED_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.IT_COVERAGE_LINE_HITS_DATA, 42.0), lines);
        // overall test
        filter.rewrite(new Measure(CoreMetrics.OVERALL_COVERAGE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.OVERALL_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.OVERALL_COVERED_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.OVERALL_COVERAGE_LINE_HITS_DATA, 42.0), lines);
    }
}
