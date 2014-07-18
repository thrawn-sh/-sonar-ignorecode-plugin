package de.shadowhunt.sonar.plugins.ignorecode.batch;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.SonarException;

import de.shadowhunt.sonar.plugins.ignorecode.internal.ModifyMeasures;
import de.shadowhunt.sonar.plugins.ignorecode.model.CoveragePattern;

public class IgnoreCoverageDecoratorTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testDecorate() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;[1-20]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(configuration);
        decorator.setModifyMeasures(Mockito.mock(ModifyMeasures.class));

        final org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/java/net/example/Foo.java");
        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        decorator.decorate(file, context);
    }

    @Test
    public void testDecorateAllLines() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;*");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(configuration);

        final org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/java/net/example/Foo.java");
        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        decorator.decorate(file, context);
    }

    @Test
    public void testDecorateDirectory() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(configuration);

        final Directory directory = Mockito.mock(Directory.class);
        Mockito.when(directory.getScope()).thenReturn(Scopes.DIRECTORY);
        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        decorator.decorate(directory, context);
    }

    @Test
    public void testDecorateNotMatchedResource() throws Exception {
        final java.io.File configFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(configFile);
        writer.println("src/java/net/example/Foo.java;[1-20]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(configFile.getAbsolutePath());

        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(configuration);
        decorator.setModifyMeasures(Mockito.mock(ModifyMeasures.class));

        final org.sonar.api.resources.File file = org.sonar.api.resources.File.create("src/java/net/example/Bar.java");
        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        decorator.decorate(file, context);
    }

    @Test
    public void testLoadPatternsEmptyConfigFile() {
        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn("");
        final List<CoveragePattern> patterns = IgnoreCoverageDecorator.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void testLoadPatternsFile() throws IOException {
        final File tempFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("**/*;*");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
        final List<CoveragePattern> patterns = IgnoreCoverageDecorator.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertEquals("List must contain the exact number of entries", 1, patterns.size());
    }

    @Test(expected = SonarException.class)
    public void testLoadPatternsInvalidFile() throws IOException {
        final File tempFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("invalid");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
        IgnoreCoverageDecorator.loadPatterns(configuration);
        Assert.fail("must not load invalid file");
    }

    @Test
    public void testLoadPatternsNoConfigFile() {
        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn("no file");
        final List<CoveragePattern> patterns = IgnoreCoverageDecorator.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void testLoadPatternsNull() throws Exception {
        final List<CoveragePattern> patterns = IgnoreCoverageDecorator.loadPatterns(null);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void testShouldExecuteOnProject() throws Exception {
        final Configuration configuration = Mockito.mock(Configuration.class);
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(configuration);
        final Project project = Mockito.mock(Project.class);
        Assert.assertTrue("decorate every project", decorator.shouldExecuteOnProject(project));
    }
}
