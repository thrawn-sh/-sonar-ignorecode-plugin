package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.MemoryOptimizer;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.persistence.MyBatis;

public class IgnoreMissingCoverageDecoratorTest {

	private static MeasurePersister createMeasurePersisterMock() {
		final ResourcePersister resourcePersister = Mockito.mock(ResourcePersister.class);
		final Snapshot snapshot = Mockito.mock(Snapshot.class);
		Mockito.when(resourcePersister.getSnapshotOrFail(Matchers.<Resource<?>> any())).thenReturn(snapshot);

		final MyBatis mybatis = Mockito.mock(MyBatis.class);
		final SqlSession session = Mockito.mock(SqlSession.class);
		Mockito.when(mybatis.openSession()).thenReturn(session);
		final MeasureMapper mapper = Mockito.mock(MeasureMapper.class);
		Mockito.when(session.getMapper(MeasureMapper.class)).thenReturn(mapper);

		final MemoryOptimizer optimizer = Mockito.mock(MemoryOptimizer.class);

		return new MeasurePersister(mybatis, resourcePersister, null, optimizer);
	}

	@Test
	public void decorate() throws IOException {
		final File tempFile = File.createTempFile("coverage-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("net.example.foo.Bar;[2-5]");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

		final MeasurePersister persister = createMeasurePersisterMock();
		final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(persister, configuration);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		Mockito.when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(new Measure());

		final Measure cbl = new Measure(CoreMetrics.CONDITIONS_BY_LINE, "3=1;6=2");
		Mockito.when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(cbl);
		final Measure ctc = new Measure(CoreMetrics.CONDITIONS_TO_COVER, 3.0);
		Mockito.when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(ctc);
		final Measure ccbl = new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "6=1");
		Mockito.when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(ccbl);
		final Measure uc = new Measure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0);
		Mockito.when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(uc);

		final Measure clh = new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1;2=0;3=0;4=0;5=0;6=1");
		Mockito.when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(clh);
		final Measure ltc = new Measure(CoreMetrics.LINES_TO_COVER, 6.0);
		Mockito.when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(ltc);
		final Measure lc = new Measure(CoreMetrics.LINE_COVERAGE, 2.0);
		Mockito.when(context.getMeasure(CoreMetrics.LINE_COVERAGE)).thenReturn(lc);

		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
		Assert.assertEquals("CONDITIONS_BY_LINE must match", "6=2", cbl.getData());
		Assert.assertEquals("CONDITIONS_TO_COVER must match", 2.0, ctc.getValue());
		Assert.assertEquals("COVERED_CONDITIONS_BY_LINE must match", "6=1", ccbl.getData());
		Assert.assertEquals("UNCOVERED_CONDITIONS must match", 1.0, uc.getValue());

		Assert.assertEquals("COVERAGE_LINE_HITS_DATA must match", "1=1;6=1", clh.getData());
		Assert.assertEquals("LINES_TO_COVER must match", 2.0, ltc.getValue());
		Assert.assertEquals("LINE_COVERAGE must match", 2.0, lc.getValue());
	}

	@Test
	public void decorateNoCoverageData() throws IOException {
		final File tempFile = File.createTempFile("coverage-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("net.example.foo.Bar;[2-5]");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(persister, configuration);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);

		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
	}

	@Test
	public void decorateNoFile() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(persister, null);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		final Directory dir = new Directory("net/example/foo");
		decorator.decorate(dir, context);
	}

	@Test
	public void decorateNoIgnores() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(persister, null);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
	}

	@Test
	public void loadPatternsEmptyConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn("");
		final Map<String, Set<Integer>> ignores = AbstractOverridingDecorator.loadIgnores(configuration, IgnoreCoverageDecorator.CONFIG_FILE);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
	}

	@Test
	public void loadPatternsFile() throws IOException {
		final File tempFile = File.createTempFile("coverage-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("resource;[2-5]");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		final Map<String, Set<Integer>> ignores = AbstractOverridingDecorator.loadIgnores(configuration, IgnoreCoverageDecorator.CONFIG_FILE);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertEquals("Map must contain the exact number of entries", 1, ignores.size());
	}

	@Test(expected = SonarException.class)
	public void loadPatternsInvalidFile() throws IOException {
		final File tempFile = File.createTempFile("violations-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("invalid");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		AbstractOverridingDecorator.loadIgnores(configuration, IgnoreCoverageDecorator.CONFIG_FILE);
		Assert.fail("must not load invalid file");
	}

	@Test
	public void loadPatternsNoConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn("no file");
		final Map<String, Set<Integer>> ignores = AbstractOverridingDecorator.loadIgnores(configuration, IgnoreCoverageDecorator.CONFIG_FILE);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
	}

	@Test
	public void loadPatternsNull() {
		final Map<String, Set<Integer>> ignores = AbstractOverridingDecorator.loadIgnores(null, IgnoreCoverageDecorator.CONFIG_FILE);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
	}

	@Test
	public void shouldExecuteOnProject() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(persister, null);

		final Project java = Mockito.mock(Project.class);
		java.setPom((MavenProject) null);
		Mockito.when(java.getLanguage()).thenReturn(Java.INSTANCE);
		Assert.assertTrue("execute on java projects", decorator.shouldExecuteOnProject(java));

		final Project other = Mockito.mock(Project.class);
		Mockito.when(other.getLanguage()).thenReturn(null);
		other.setPom((MavenProject) null);
		Assert.assertFalse("don't execute on non java projects", decorator.shouldExecuteOnProject(other));
	}
}
