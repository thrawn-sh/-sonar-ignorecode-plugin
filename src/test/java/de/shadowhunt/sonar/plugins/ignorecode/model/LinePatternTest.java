package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

import junit.framework.Assert;

import org.junit.Test;

public class LinePatternTest {

	@Test
	public void addLine() {
		final LinePattern lp = new LinePattern("test");
		lp.addLine(1);
		lp.addLine(3);
		lp.addLine(5);

		final SortedSet<Integer> full = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must not contain any entries", 3, full.size());
		Assert.assertTrue("SortedSet must contain line 1", full.contains(1));
		Assert.assertTrue("SortedSet must contain line 3", full.contains(3));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
	}

	@Test
	public void addLines() {
		final LinePattern lp = new LinePattern("test");
		lp.addLines(2, 2);
		lp.addLines(4, 6);

		final SortedSet<Integer> full = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must not contain any entries", 4, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
		Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addLinesException() {
		final LinePattern lp = new LinePattern("test");
		lp.addLines(6, 4);
		Assert.fail("must not allow to switch from and to");
	}

	@Test
	public void empty() {
		final LinePattern lp = new LinePattern("test");

		final SortedSet<Integer> empty = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", empty);
		Assert.assertEquals("SortedSet must not contain any entries", 0, empty.size());
	}

	@Test
	public void merge() {
		final Collection<LinePattern> input = new ArrayList<LinePattern>();
		input.add(LinePattern.parseLineValues("a", "[2-6]"));
		input.add(LinePattern.parseLineValues("a", "[5-8]"));
		input.add(LinePattern.parseLineValues("a", "[1,9]"));
		input.add(LinePattern.parseLineValues("b", "[7]"));
		final Collection<LinePattern> merge = LinePattern.merge(input);
		Assert.assertNotNull("Collection must not be null", merge);
		Assert.assertEquals("Collection must not contain any entries", 2, merge.size());

		for (final LinePattern lp : merge) {
			final String resource = lp.getResource();
			if ("a".equals(resource)) {
				Assert.assertEquals("", LinePattern.parseLineValues("a", "[1-9]"), lp);
			} else {
				Assert.assertEquals("", LinePattern.parseLineValues("b", "[7]"), lp);
			}
		}
	}

	@Test
	public void mergeEmptry() {
		final Collection<LinePattern> empty = Collections.emptyList();
		final Collection<LinePattern> merge = LinePattern.merge(empty);
		Assert.assertNotNull("Collection must not be null", merge);
		Assert.assertEquals("Collection must not contain any entries", 0, merge.size());
	}

	@Test
	public void parseCombined() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[1,3,5-7]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must not contain any entries", 5, lines.size());
		Assert.assertTrue("SortedSet must contain line 1", lines.contains(1));
		Assert.assertTrue("SortedSet must contain line 3", lines.contains(3));
		Assert.assertTrue("SortedSet must contain line 5", lines.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", lines.contains(6));
		Assert.assertTrue("SortedSet must contain line 7", lines.contains(7));
	}

	@Test
	public void parseRange() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[2-6]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must not contain any entries", 5, lines.size());
		Assert.assertTrue("SortedSet must contain line 2", lines.contains(2));
		Assert.assertTrue("SortedSet must contain line 3", lines.contains(3));
		Assert.assertTrue("SortedSet must contain line 4", lines.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", lines.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", lines.contains(6));
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseRangeException() {
		LinePattern.parseLineValues("a", "[6-2]");
		Assert.fail("must not allow to switch from and to");
	}

	@Test
	public void parseSingle() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[2]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must not contain any entries", 1, lines.size());
		Assert.assertTrue("SortedSet must contain line 2", lines.contains(2));
	}
}
