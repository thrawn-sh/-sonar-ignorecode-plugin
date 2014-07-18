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
    public void testRewrite() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageMeasurementFilter filter = new IgnoreCoverageMeasurementFilter(configuration);

        final Set<Integer> lines = Collections.emptySet();
        filter.rewrite(new Measure(CoreMetrics.COVERAGE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, 42.0), lines);
        filter.rewrite(new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, 42.0), lines);
    }
}
