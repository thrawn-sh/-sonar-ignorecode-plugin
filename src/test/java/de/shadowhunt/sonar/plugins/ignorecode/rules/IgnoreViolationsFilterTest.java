package de.shadowhunt.sonar.plugins.ignorecode.rules;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.resources.JavaFile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.Violation;
import org.sonar.api.utils.SonarException;

import de.shadowhunt.sonar.plugins.ignorecode.model.ViolationPattern;

public class IgnoreViolationsFilterTest {

	@Test
	public void matchAnyResourceMatchingRuleMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("*", "pmd:AbstractClassWithoutAbstractMethod");
		pattern.addLine(5);

		Assert.assertTrue("must match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchMatchingResourceAnyRuleMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Bar", "*");
		pattern.addLine(5);

		Assert.assertTrue("must match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchMatchingResourceMatchingRuleAnyLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Bar", "pmd:AbstractClassWithoutAbstractMethod");

		Assert.assertTrue("must match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchMatchingResourceMatchingRuleMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Bar", "pmd:AbstractClassWithoutAbstractMethod");
		pattern.addLine(5);

		Assert.assertTrue("must match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchMatchingResourceMatchingRuleNotMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Bar", "pmd:AbstractClassWithoutAnyMethod");
		pattern.addLine(4);

		Assert.assertFalse("must not match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchMatchingResourceNotMatchingRuleMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Bar", "pmd:AbstractClassWithoutAnyMethod");
		pattern.addLine(5);

		Assert.assertFalse("must not match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchNotMatchingResourceMatchingRuleMatchingLine() {
		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);

		final ViolationPattern pattern = new ViolationPattern("net.example.foo.Foo", "pmd:AbstractClassWithoutAnyMethod");
		pattern.addLine(5);

		Assert.assertFalse("must not match", IgnoreViolationsFilter.match(violation, pattern));
	}

	@Test
	public void matchResource() {
		final JavaFile resource = new JavaFile("net.example.foo.Bar");
		Assert.assertTrue("* pattern must match", IgnoreViolationsFilter.matchResource(resource, "*"));
		Assert.assertTrue("**/* pattern must match", IgnoreViolationsFilter.matchResource(resource, "**/*"));
		Assert.assertTrue("exact pattern must match", IgnoreViolationsFilter.matchResource(resource, "net.example.foo.Bar"));
		Assert.assertTrue("net.** pattern must match", IgnoreViolationsFilter.matchResource(resource, "net.**"));
		Assert.assertTrue("net.example.foo.B?r pattern must match", IgnoreViolationsFilter.matchResource(resource, "net.example.foo.B?r"));

		Assert.assertFalse("empty pattern must not match", IgnoreViolationsFilter.matchResource(resource, ""));
		Assert.assertFalse("net.**.Foo pattern must match", IgnoreViolationsFilter.matchResource(resource, "net.**.Foo"));
	}

	@Test
	public void matchRule() {
		final Rule rule = Rule.create("pmd", "AbstractClassWithoutAbstractMethod");
		Assert.assertTrue("* pattern must match", IgnoreViolationsFilter.matchRule(rule, "*"));
		Assert.assertTrue("exact pattern must match", IgnoreViolationsFilter.matchRule(rule, "pmd:AbstractClassWithoutAbstractMethod"));
		Assert.assertTrue("pmd:* pattern must match", IgnoreViolationsFilter.matchRule(rule, "pmd:*"));
		Assert.assertTrue("pmd:Abstract* pattern must match", IgnoreViolationsFilter.matchRule(rule, "pmd:Abstract*"));

		Assert.assertFalse("empty pattern must not match", IgnoreViolationsFilter.matchRule(rule, ""));
		Assert.assertFalse("pmd:AbstractClassWithoutAnyMethod pattern must not match", IgnoreViolationsFilter.matchRule(rule, "pmd:AbstractClassWithoutAnyMethod"));
	}

	@Test
	public void loadPatternsNull() {
		final List<ViolationPattern> patterns = IgnoreViolationsFilter.loadPatterns(null);
		Assert.assertNotNull("List must not be null", patterns);
		Assert.assertTrue("List must be empty", patterns.isEmpty());
	}

	@Test
	public void loadPatternsNoConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreViolationsFilter.CONFIG_FILE)).thenReturn("no file");
		final List<ViolationPattern> patterns = IgnoreViolationsFilter.loadPatterns(configuration);
		Assert.assertNotNull("List must not be null", patterns);
		Assert.assertTrue("List must be empty", patterns.isEmpty());
	}

	@Test
	public void loadPatternsEmptyConfigFile() {
		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreViolationsFilter.CONFIG_FILE)).thenReturn("");
		final List<ViolationPattern> patterns = IgnoreViolationsFilter.loadPatterns(configuration);
		Assert.assertNotNull("List must not be null", patterns);
		Assert.assertTrue("List must be empty", patterns.isEmpty());
	}

	@Test
	public void loadPatternsFile() throws IOException {
		final File tempFile = File.createTempFile("violations-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("*;*;*");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreViolationsFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		final List<ViolationPattern> patterns = IgnoreViolationsFilter.loadPatterns(configuration);
		Assert.assertNotNull("List must not be null", patterns);
		Assert.assertEquals("List must contain the exact number of entries", 1, patterns.size());
	}

	@Test(expected = SonarException.class)
	public void loadPatternsInvalidFile() throws IOException {
		final File tempFile = File.createTempFile("violations-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("invalid");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreViolationsFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		IgnoreViolationsFilter.loadPatterns(configuration);
		Assert.fail("must not load invalid file");
	}

	@Test
	public void isIgnoredNoIgnores() {
		final IgnoreViolationsFilter filter = new IgnoreViolationsFilter(null);

		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);
		Assert.assertFalse("no ignores => all false", filter.isIgnored(violation));
	}

	@Test
	public void isIgnored() throws IOException {
		final File tempFile = File.createTempFile("violations-", ".tmp");
		final PrintWriter writer = new PrintWriter(tempFile);
		writer.println("*;pmd:AbstractClassWithoutAnyMethod;*");
		writer.println("*;*;*");
		writer.close();

		final Configuration configuration = Mockito.mock(Configuration.class);
		Mockito.when(configuration.getString(IgnoreViolationsFilter.CONFIG_FILE)).thenReturn(tempFile.getAbsolutePath());
		final IgnoreViolationsFilter filter = new IgnoreViolationsFilter(configuration);

		final Violation violation = Violation.create(Rule.create("pmd", "AbstractClassWithoutAbstractMethod"), new JavaFile("net.example.foo.Bar"));
		violation.setLineId(5);
		Assert.assertTrue("mating ignore", filter.isIgnored(violation));
	}
}
