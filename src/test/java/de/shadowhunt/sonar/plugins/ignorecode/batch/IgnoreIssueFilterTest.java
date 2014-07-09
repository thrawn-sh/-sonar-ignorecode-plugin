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
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.batch.IssueFilterChain;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.SonarException;
import org.sonar.core.issue.DefaultIssueBuilder;

import de.shadowhunt.sonar.plugins.ignorecode.model.IssuePattern;

public class IgnoreIssueFilterTest {

    private static final IssueFilterChain DEFAULT_CHAIN;

    private static final Issue DEFAULT_ISSUE;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    static {
        DEFAULT_CHAIN = Mockito.mock(IssueFilterChain.class);
        Mockito.when(DEFAULT_CHAIN.accept(Matchers.<Issue>any())).thenReturn(true);

        final DefaultIssueBuilder ib = new DefaultIssueBuilder();
        ib.componentKey("group:project:src/main/java/net/example/foo/Bar.java");
        ib.ruleKey(RuleKey.of("pmd", "AbstractClassWithoutAbstractMethod"));
        ib.line(5);
        DEFAULT_ISSUE = ib.build();
    }

    @Test
    public void isIgnored() throws IOException {
        final File tempFile = temporaryFolder.newFile("issue.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("**/*;pmd:AbstractClassWithoutAnyMethod;*");
        writer.println("**/*;*;*");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreIssueFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
        final IgnoreIssueFilter filter = new IgnoreIssueFilter(configuration);

        Assert.assertFalse("mating ignore", filter.accept(DEFAULT_ISSUE, DEFAULT_CHAIN));
    }

    @Test
    public void isIgnoredNoIgnores() {
        final IgnoreIssueFilter filter = new IgnoreIssueFilter(null);
        Assert.assertTrue("no ignores => all false", filter.accept(DEFAULT_ISSUE, DEFAULT_CHAIN));
    }

    @Test
    public void loadPatternsEmptyConfigFile() {
        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreIssueFilter.CONFIG_FILE)).thenReturn("");
        final List<IssuePattern> patterns = IgnoreIssueFilter.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void loadPatternsFile() throws IOException {
        final File tempFile = temporaryFolder.newFile("issue.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("**/*;*;*");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreIssueFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
        final List<IssuePattern> patterns = IgnoreIssueFilter.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertEquals("List must contain the exact number of entries", 1, patterns.size());
    }

    @Test(expected = SonarException.class)
    public void loadPatternsInvalidFile() throws IOException {
        final File tempFile = temporaryFolder.newFile("issue.txt");
        final PrintWriter writer = new PrintWriter(tempFile);
        writer.println("invalid");
        writer.close();

        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreIssueFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
        IgnoreIssueFilter.loadPatterns(configuration);
        Assert.fail("must not load invalid file");
    }

    @Test
    public void loadPatternsNoConfigFile() {
        final Configuration configuration = Mockito.mock(Configuration.class);
        Mockito.when(configuration.getString(IgnoreIssueFilter.CONFIG_FILE)).thenReturn("no file");
        final List<IssuePattern> patterns = IgnoreIssueFilter.loadPatterns(configuration);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void loadPatternsNull() {
        final List<IssuePattern> patterns = IgnoreIssueFilter.loadPatterns(null);
        Assert.assertNotNull("List must not be null", patterns);
        Assert.assertTrue("List must be empty", patterns.isEmpty());
    }

    @Test
    public void matchAnyResourceMatchingRuleMatchingLine() {
        final IssuePattern pattern = new IssuePattern("**/*", "pmd:AbstractClassWithoutAbstractMethod");
        pattern.addLine(5);

        Assert.assertTrue("must match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchMatchingResourceAnyRuleMatchingLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Bar.java", "*");
        pattern.addLine(5);

        Assert.assertTrue("must match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchMatchingResourceMatchingRuleAnyLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Bar.java", "pmd:AbstractClassWithoutAbstractMethod");

        Assert.assertTrue("must match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchMatchingResourceMatchingRuleMatchingLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Bar.java", "pmd:AbstractClassWithoutAbstractMethod");
        pattern.addLine(5);

        Assert.assertTrue("must match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchMatchingResourceMatchingRuleNotMatchingLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Bar.java", "pmd:AbstractClassWithoutAnyMethod");
        pattern.addLine(4);

        Assert.assertFalse("must not match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchMatchingResourceNotMatchingRuleMatchingLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Bar.java", "pmd:AbstractClassWithoutAnyMethod");
        pattern.addLine(5);

        Assert.assertFalse("must not match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchNotMatchingResourceMatchingRuleMatchingLine() {
        final IssuePattern pattern = new IssuePattern("src/main/java/net/example/foo/Foo.java", "pmd:AbstractClassWithoutAnyMethod");
        pattern.addLine(5);

        Assert.assertFalse("must not match", IgnoreIssueFilter.match(DEFAULT_ISSUE, pattern));
    }

    @Test
    public void matchResource() {
        final String componentKey = "group:project:src/main/java/net/example/foo/Bar.java";

        Assert.assertTrue("**/* pattern must match", IgnoreIssueFilter.matchResource(componentKey, "**/*"));
        Assert.assertTrue("exact pattern must match", IgnoreIssueFilter.matchResource(componentKey, "src/main/java/net/example/foo/Bar.java"));
        Assert.assertTrue("net.** pattern must match", IgnoreIssueFilter.matchResource(componentKey, "src/main/java/net/**"));
        Assert.assertTrue("net.example.foo.B?r pattern must match", IgnoreIssueFilter.matchResource(componentKey, "src/main/java/net/example/foo/B?r.java"));

        Assert.assertFalse("* pattern must not match", IgnoreIssueFilter.matchResource(componentKey, "*"));
        Assert.assertFalse("empty pattern must not match", IgnoreIssueFilter.matchResource(componentKey, ""));
        Assert.assertFalse("net.**.Foo pattern must match", IgnoreIssueFilter.matchResource(componentKey, "src/main/java/net/**/Foo"));
    }

    @Test
    public void matchRuleKey() {
        final RuleKey rule = RuleKey.of("pmd", "AbstractClassWithoutAbstractMethod");
        Assert.assertTrue("* pattern must match", IgnoreIssueFilter.matchRule(rule, "*"));
        Assert.assertTrue("exact pattern must match", IgnoreIssueFilter.matchRule(rule, "pmd:AbstractClassWithoutAbstractMethod"));
        Assert.assertTrue("pmd:* pattern must match", IgnoreIssueFilter.matchRule(rule, "pmd:*"));
        Assert.assertTrue("pmd:Abstract* pattern must match", IgnoreIssueFilter.matchRule(rule, "pmd:Abstract*"));

        Assert.assertFalse("empty pattern must not match", IgnoreIssueFilter.matchRule(rule, ""));
        Assert.assertFalse("pmd:AbstractClassWithoutAnyMethod pattern must not match", IgnoreIssueFilter.matchRule(rule, "pmd:AbstractClassWithoutAnyMethod"));
    }
}
