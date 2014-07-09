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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.configuration.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.batch.SonarIndex;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.database.model.MeasureMapper;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Directory;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.SonarException;
import org.sonar.batch.DefaultDecoratorContext;
import org.sonar.batch.index.Bucket;
import org.sonar.batch.index.DefaultIndex;
import org.sonar.batch.index.MeasurePersister;
import org.sonar.batch.index.MemoryOptimizer;
import org.sonar.batch.index.PersistenceManager;
import org.sonar.batch.index.ResourcePersister;
import org.sonar.core.persistence.MyBatis;

public class IgnoreMissingCoverageDecoratorTest {

    private static DecoratorContext createDecoratorContextMock(final Resource resource, final Measure... measures) throws Exception {
        final Bucket bucket = new Bucket(resource);
        for (Measure measure : measures) {
            bucket.addMeasure(measure);
        }
        final PersistenceManager pm = Mockito.mock(PersistenceManager.class);
        final Answer<Measure> answer = new Answer<Measure>() {

            @Override
            public Measure answer(final InvocationOnMock invocation) throws Throwable {
                return (Measure) invocation.getArguments()[0];
            }
        };
        Mockito.when(pm.reloadMeasure(Mockito.any(Measure.class))).thenAnswer(answer);

        final SonarIndex index = new DefaultIndex(pm, null, null, null, null, null);
        final Map<Resource, Bucket> buckets = AbstractOverridingDecorator.getPrivateField(index, "buckets");
        buckets.put(resource, bucket);
        return new DefaultDecoratorContext(resource, index, null, null);
    }

    private static FileSystem createFileSystemMock(final String language) {
        FileSystem fileSystem = Mockito.mock(FileSystem.class);
        SortedSet<String> languages = new TreeSet<String>();
        languages.add(language);
        Mockito.when(fileSystem.languages()).thenReturn(languages);
        return fileSystem;
    }

    private static MeasurePersister createMeasurePersisterMock() {
        final ResourcePersister resourcePersister = Mockito.mock(ResourcePersister.class);
        final Snapshot snapshot = Mockito.mock(Snapshot.class);
        Mockito.when(resourcePersister.getSnapshotOrFail(Matchers.<Resource>any())).thenReturn(snapshot);

        final MyBatis mybatis = Mockito.mock(MyBatis.class);
        final SqlSession session = Mockito.mock(SqlSession.class);
        Mockito.when(mybatis.openSession()).thenReturn(session);
        final MeasureMapper mapper = Mockito.mock(MeasureMapper.class);
        Mockito.when(session.getMapper(MeasureMapper.class)).thenReturn(mapper);

        final MemoryOptimizer optimizer = Mockito.mock(MemoryOptimizer.class);

        return new MeasurePersister(mybatis, resourcePersister, null, optimizer);
    }

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @SuppressWarnings("deprecation")
    private Project createProjectMock() {
        final ProjectFileSystem fileSystem = Mockito.mock(ProjectFileSystem.class);
        Mockito.when(fileSystem.getBasedir()).thenReturn(temporaryFolder.getRoot());
        final Project project = Mockito.mock(Project.class);
        Mockito.when(project.getFileSystem()).thenReturn(fileSystem);
        return project;
    }

    @Test
    public void decorate() throws Exception {
        final File tempFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("src/main/java/net/example/foo/Bar.java;[2-5]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

        final FileSystem fileSystem = createFileSystemMock("java");
        final MeasurePersister persister = createMeasurePersisterMock();
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(fileSystem, persister, configuration);

        final Measure cbl = new Measure(CoreMetrics.CONDITIONS_BY_LINE, "3=1;6=2");
        final Measure ctc = new Measure(CoreMetrics.CONDITIONS_TO_COVER, 3.0);
        final Measure ccbl = new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "6=1");
        final Measure uc = new Measure(CoreMetrics.UNCOVERED_CONDITIONS, 2.0);
        final Measure ul = new Measure(CoreMetrics.UNCOVERED_LINES, 100.0);
        final Measure clh = new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=1;2=0;3=0;4=0;5=0;6=1");
        final Measure ltc = new Measure(CoreMetrics.LINES_TO_COVER, 6.0);
        final Measure lc = new Measure(CoreMetrics.LINE_COVERAGE, 2.0);
        final Measure bc = new Measure(CoreMetrics.BRANCH_COVERAGE, 75.0);
        final Measure c = new Measure(CoreMetrics.COVERAGE, 80.0);

        final Project project = createProjectMock();
        final Resource file = org.sonar.api.resources.File.fromIOFile(new File(temporaryFolder.getRoot(), "src/main/java/net/example/foo/Bar.java"), project);

        final DecoratorContext context = createDecoratorContextMock(file, cbl, ctc, ccbl, uc, ul, clh, ltc, lc, bc, c);
        decorator.decorate(file, context);

        Assert.assertEquals("CONDITIONS_BY_LINE must match", "6=2", cbl.getData());
        Assert.assertEquals("CONDITIONS_TO_COVER must match", Double.valueOf(2.0), ctc.getValue());
        Assert.assertEquals("COVERED_CONDITIONS_BY_LINE must match", "6=1", ccbl.getData());
        Assert.assertEquals("UNCOVERED_CONDITIONS must match", Double.valueOf(1.0), uc.getValue());

        Assert.assertEquals("COVERAGE_LINE_HITS_DATA must match", "1=1;6=1", clh.getData());
        Assert.assertEquals("LINES_TO_COVER must match", Double.valueOf(2.0), ltc.getValue());
        //Assert.assertEquals("LINE_COVERAGE must match", Double.valueOf(2.0), lc.getValue());
    }

    @Test
    public void decorateNoCoverageData() throws IOException {
        final File tempFile = temporaryFolder.newFile("coverage.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("src/main/java/net/example/foo/Bar.java;[2-5]");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreCoverageDecorator.CONFIG_FILE)).thenReturn(tempFile.getPath());

        final FileSystem fileSystem = createFileSystemMock("java");
        final MeasurePersister persister = createMeasurePersisterMock();
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(fileSystem, persister, configuration);

        final DecoratorContext context = Mockito.mock(DecoratorContext.class);

        final Project project = createProjectMock();
        final org.sonar.api.resources.File file = org.sonar.api.resources.File.fromIOFile(new File(temporaryFolder.getRoot(), "src/main/java/net/example/foo/Bar"), project);

        decorator.decorate(file, context);
    }

    @Test
    public void decorateNoFile() throws IOException {
        final FileSystem fileSystem = createFileSystemMock("java");
        final MeasurePersister persister = createMeasurePersisterMock();
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(fileSystem, persister, null);

        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        final Project project = createProjectMock();
        final Directory dir = Directory.fromIOFile(temporaryFolder.newFolder("net"), project);
        decorator.decorate(dir, context);
    }

    @Test
    public void decorateNoIgnores() {
        final FileSystem fileSystem = createFileSystemMock("java");
        final MeasurePersister persister = createMeasurePersisterMock();
        final IgnoreCoverageDecorator decorator = new IgnoreCoverageDecorator(fileSystem, persister, null);

        final DecoratorContext context = Mockito.mock(DecoratorContext.class);
        final Project project = createProjectMock();
        final org.sonar.api.resources.File file = org.sonar.api.resources.File.fromIOFile(new File(temporaryFolder.getRoot(), "net/example/foo/Bar"), project);
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
        final File tempFile = temporaryFolder.newFile("coverage.txt");
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
        final File tempFile = temporaryFolder.newFile("violations.txt");
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
        final MeasurePersister persister = createMeasurePersisterMock();
        final Project project = Mockito.mock(Project.class);

        final FileSystem javaFileSystem = createFileSystemMock("java");
        final IgnoreCoverageDecorator java = new IgnoreCoverageDecorator(javaFileSystem, persister, null);
        Assert.assertTrue("execute on java projects", java.shouldExecuteOnProject(project));

        final FileSystem otherFileSystem = createFileSystemMock("php");
        final IgnoreCoverageDecorator other = new IgnoreCoverageDecorator(otherFileSystem, persister, null);
        Assert.assertFalse("don't execute on non java projects", other.shouldExecuteOnProject(project));
    }
}
