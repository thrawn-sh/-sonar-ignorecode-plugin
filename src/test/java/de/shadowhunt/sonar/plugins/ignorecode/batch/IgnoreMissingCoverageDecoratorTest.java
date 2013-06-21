package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.utils.SonarException;

public class IgnoreMissingCoverageDecoratorTest {

	@Test
	public void loadPatternsNull() {
		final Map<String, Set<Integer>> ignores = IgnoreMissingCoverageDecorator.loadIgnores(null);
		Assert.assertNotNull("Map must not be null", ignores);
		Assert.assertTrue("Map must be empty", ignores.isEmpty());
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
		writer.write("resource;[2-5]");
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
		writer.write("invalid");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreMissingCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		IgnoreMissingCoverageDecorator.loadIgnores(configuration);
		Assert.fail("must not load invalid file");
	}
}
