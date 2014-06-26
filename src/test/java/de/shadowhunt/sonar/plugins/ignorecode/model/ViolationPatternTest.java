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
import java.util.List;
import java.util.SortedSet;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class ViolationPatternTest {

	@Test
	public void addLine() {
		final ViolationPattern vp = new ViolationPattern("resourcePattern", "rulePattern");
		vp.addLine(1);
		vp.addLine(3);
		vp.addLine(5);

		final SortedSet<Integer> full = vp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 3, full.size());
		Assert.assertTrue("SortedSet must contain line 1", full.contains(1));
		Assert.assertTrue("SortedSet must contain line 3", full.contains(3));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
	}

	@Test
	public void addLines() {
		final ViolationPattern vp = new ViolationPattern("resourcePattern", "rulePattern");
		vp.addLines(2, 2);
		vp.addLines(4, 6);

		final SortedSet<Integer> full = vp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
		Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
	}

	@Test(expected = IllegalArgumentException.class)
	public void addLinesException() {
		final ViolationPattern vp = new ViolationPattern("resourcePattern", "rulePattern");
		vp.addLines(6, 4);
		Assert.fail("must not allow to switch from and to");
	}

	@Test
	public void empty() {
		final ViolationPattern vp = new ViolationPattern("resourcePattern", "rulePattern");

		final SortedSet<Integer> empty = vp.getLines();
		Assert.assertNotNull("SortedSet must not be null", empty);
		Assert.assertEquals("SortedSet must not contain any entries", 0, empty.size());
	}

	@Test
	public void parse() throws IOException {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final PrintWriter writer = new PrintWriter(baos);
		writer.println("# comment");
		writer.println();
		writer.println("resourcePattern;rulePattern;[2,4-6]");
		writer.close();

		final InputStream is = new ByteArrayInputStream(baos.toByteArray());
		try {
			final List<ViolationPattern> lines = ViolationPattern.parse(is);
			Assert.assertNotNull("List must not be null", lines);
			Assert.assertEquals("List must contain the exact number of entries", 1, lines.size());

			final ViolationPattern vp = lines.get(0);
			Assert.assertEquals("resourcePattern name must match", "resourcePattern", vp.getResourcePattern());
			Assert.assertEquals("rulePattern name must match", "rulePattern", vp.getRulePattern());

			final SortedSet<Integer> full = vp.getLines();
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
	public void parseLineValueCombined() {
		final ViolationPattern vp = ViolationPattern.parseLineValues("resourcePattern", "rulePattern", "[1,3,5-7]");
		Assert.assertNotNull("ViolationPattern must not be null", vp);
		Assert.assertEquals("resourcePattern name must match", "resourcePattern", vp.getResourcePattern());
		Assert.assertEquals("rulePattern name must match", "rulePattern", vp.getRulePattern());

		final SortedSet<Integer> lines = vp.getLines();
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
			final List<ViolationPattern> lines = ViolationPattern.parse(is);
			Assert.assertNotNull("List must not be null", lines);
			Assert.assertEquals("List must contain the exact number of entries", 0, lines.size());
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingBroken() {
		ViolationPattern.parseLine("rulePattern;[2-3]");
		Assert.fail("must not parse invalid input");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingLines() {
		ViolationPattern.parseLine("resourcePattern;rulePattern; ");
		Assert.fail("must not parse invalid input");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingResourcePattern() {
		ViolationPattern.parseLine(" ;rulePattern;[2-3]");
		Assert.fail("must not parse invalid input");
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseLineMissingRulePattern() {
		ViolationPattern.parseLine("resourcePattern; ;[2-3]");
		Assert.fail("must not parse invalid input");
	}

	@Test
	public void parseLineMixed() {
		final ViolationPattern vp = ViolationPattern.parseLine("resourcePattern;rulePattern;[2,4-6]");
		Assert.assertEquals("resourcePattern name must match", "resourcePattern", vp.getResourcePattern());
		Assert.assertEquals("rulePattern name must match", "rulePattern", vp.getRulePattern());

		final SortedSet<Integer> full = vp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
		Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
		Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
		Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
	}

	@Test
	public void parseLineRange() {
		final ViolationPattern vp = ViolationPattern.parseLine("resourcePattern;rulePattern;[2-6]");
		Assert.assertEquals("resourcePattern name must match", "resourcePattern", vp.getResourcePattern());
		Assert.assertEquals("rulePattern name must match", "rulePattern", vp.getRulePattern());

		final SortedSet<Integer> full = vp.getLines();
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
		final ViolationPattern vp = ViolationPattern.parseLine("resourcePattern;rulePattern;[2]");
		Assert.assertEquals("resourcePattern name must match", "resourcePattern", vp.getResourcePattern());
		Assert.assertEquals("rulePattern name must match", "rulePattern", vp.getRulePattern());

		final SortedSet<Integer> full = vp.getLines();
		Assert.assertNotNull("SortedSet must not be null", full);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 1, full.size());
		Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
	}

	@Test
	public void parseLineValuesRange() {
		final ViolationPattern linePattern = ViolationPattern.parseLineValues("resourcePattern", "rulePattern", "[2-6]");
		Assert.assertNotNull("ViolationPattern must not be null", linePattern);

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
		ViolationPattern.parseLineValues("resourcePattern", "rulePattern", "[6-2]");
		Assert.fail("must not allow to switch from and to");
	}

	@Test
	public void parseLineValuesSingle() {
		final ViolationPattern linePattern = ViolationPattern.parseLineValues("resourcePattern", "rulePattern", "[2]");
		Assert.assertNotNull("ViolationPattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 1, lines.size());
		Assert.assertTrue("SortedSet must contain line 2", lines.contains(2));
	}

	@Test
	public void parseLineValuesAllLines() {
		final ViolationPattern linePattern = ViolationPattern.parseLineValues("resourcePattern", "rulePattern", "*");
		Assert.assertNotNull("ViolationPattern must not be null", linePattern);

		final SortedSet<Integer> lines = linePattern.getLines();
		Assert.assertNotNull("SortedSet must not be null", lines);
		Assert.assertEquals("SortedSet must contain the exact number of entries", 0, lines.size());
	}
}
