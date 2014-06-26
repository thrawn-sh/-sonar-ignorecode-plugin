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
package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
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
		Assert.assertEquals("SortedSet must contain the exact number of entries", 3, full.size());
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
		Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
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
		Assert.assertEquals("Collection must contain the exact number of entries", 2, merge.size());

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
	public void parse() throws IOException {

		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintWriter writer = new PrintWriter(baos);
		writer.println("# comment");
		writer.println();
		writer.println("a;[2,4-6]");
		writer.close();

		final InputStream is = new ByteArrayInputStream(baos.toByteArray());
		try {
			final List<LinePattern> lines = LinePattern.parse(is);
			Assert.assertNotNull("List must not be null", lines);
			Assert.assertEquals("List must contain the exact number of entries", 1, lines.size());

			final LinePattern lp = lines.get(0);
			Assert.assertEquals("resource name must match", "a", lp.getResource());

			final SortedSet<Integer> full = lp.getLines();
			Assert.assertNotNull("SortedSet must not be null", full);
			Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
			Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
			Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
			Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
			Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
		} finally {
			IOUtils.closeQuietly(is);
			IOUtils.closeQuietly(baos);
		}
	}

	@Test
	public void parseCombined() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[1,3,5-7]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);
		Assert.assertEquals("resource name must match", "a", linePattern.getResource());

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 5, lines.size());
		Assert.assertTrue("SortedSet must contain line 1", lines.contains(1));
		Assert.assertTrue("SortedSet must contain line 3", lines.contains(3));
		Assert.assertTrue("SortedSet must contain line 5", lines.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", lines.contains(6));
		Assert.assertTrue("SortedSet must contain line 7", lines.contains(7));
	}

	@Test
	public void parseEmpty() throws IOException {
		final InputStream is = new ByteArrayInputStream(new byte[0]);
		try {
			final List<LinePattern> lines = LinePattern.parse(is);
			Assert.assertNotNull("List must not be null", lines);
			Assert.assertEquals("List must contain the exact number of entries", 0, lines.size());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingBroken() {
		LinePattern.parseLine(";[2-3]");
		Assert.fail("must not parse invalid input");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingLines() {
		LinePattern.parseLine("a; ");
		Assert.fail("must not parse invalid input");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingResource() {
		LinePattern.parseLine(" ;[2-3]");
		Assert.fail("must not parse invalid input");
	}

	@Test
	public void parseLineMixed() {
		final LinePattern lp = LinePattern.parseLine("a;[2,4-6]");
		Assert.assertEquals("resource name must match", "a", lp.getResource());

		final SortedSet<Integer> full = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
		Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
	}

	@Test
	public void parseLineRange() {
		final LinePattern lp = LinePattern.parseLine("a;[2-6]");
		Assert.assertEquals("resource name must match", "a", lp.getResource());

		final SortedSet<Integer> full = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 5, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
		Assert.assertTrue("SortedSet must contain line 3", full.contains(3));
		Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
	}

	@Test
	public void parseLineSingle() {
		final LinePattern lp = LinePattern.parseLine("a;[2]");
		Assert.assertEquals("resource name must match", "a", lp.getResource());

		final SortedSet<Integer> full = lp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 1, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
	}

	@Test
	public void parseLineValuesRange() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[2-6]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 5, lines.size());
		Assert.assertTrue("SortedSet must contain line 2", lines.contains(2));
		Assert.assertTrue("SortedSet must contain line 3", lines.contains(3));
		Assert.assertTrue("SortedSet must contain line 4", lines.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", lines.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", lines.contains(6));
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineValuesRangeException() {
		LinePattern.parseLineValues("a", "[6-2]");
		Assert.fail("must not allow to switch from and to");
	}

	@Test
	public void parseLineValuesSingle() {
		final LinePattern linePattern = LinePattern.parseLineValues("a", "[2]");
		Assert.assertNotNull("LinePattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 1, lines.size());
		Assert.assertTrue("SortedSet must contain line 2", lines.contains(2));
	}
}
