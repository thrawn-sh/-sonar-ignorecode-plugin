package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.index.MeasurePersister;

public class IgnoreMissingCoverageDecoratorTest {

	@Test
	@Ignore
	public void decorate() throws IOException {
		final File tempFile = File.createTempFile("coverage-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("net.example.foo.Bar;[2-5]");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreMissingCoverageDecorator decorator = new IgnoreMissingCoverageDecorator(persister, configuration);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		Mockito.when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(new Measure());

		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
	}

	@Test
	public void decorateNoCoverageData() throws IOException {
		final File tempFile = File.createTempFile("coverage-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("net.example.foo.Bar;[2-5]");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreMissingCoverageDecorator decorator = new IgnoreMissingCoverageDecorator(persister, configuration);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);

		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
	}

	@Test
	public void decorateNoFile() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreMissingCoverageDecorator decorator = new IgnoreMissingCoverageDecorator(persister, null);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		final Directory dir = new Directory("net/example/foo");
		decorator.decorate(dir, context);
	}

	@Test
	public void decorateNoIgnores() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreMissingCoverageDecorator decorator = new IgnoreMissingCoverageDecorator(persister, null);

		final DecoratorContext context = Mockito.mock(DecoratorContext.class);
		final JavaFile file = new JavaFile("net.example.foo.Bar");
		decorator.decorate(file, context);
	}

	@Test
	public void loadPatternsEmptyConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn("");
		final Map<String, Set<Integer>> ignores = IgnoreMissingCoverageDecorator.loadIgnores(configuration);
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
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		final Map<String, Set<Integer>> ignores = IgnoreMissingCoverageDecorator.loadIgnores(configuration);
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
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		IgnoreMissingCoverageDecorator.loadIgnores(configuration);
		Assert.fail("must not load invalid file");
	}

	@Test
	public void loadPatternsNoConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn("no file");
		final Map<String, Set<Integer>> ignores = IgnoreMissingCoverageDecorator.loadIgnores(configuration);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
	}

	@Test
	public void loadPatternsNull() {
		final Map<String, Set<Integer>> ignores = IgnoreMissingCoverageDecorator.loadIgnores(null);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
	}

	@Test
	public void shouldExecuteOnProject() {
		final MeasurePersister persister = new MeasurePersister(null, null, null, null);
		final IgnoreMissingCoverageDecorator decorator = new IgnoreMissingCoverageDecorator(persister, null);

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
