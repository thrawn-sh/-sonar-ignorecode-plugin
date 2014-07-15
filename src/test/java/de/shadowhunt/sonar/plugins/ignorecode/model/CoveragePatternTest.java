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
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class CoveragePatternTest {

    @Test
    public void testAddLine() throws Exception {
        final SortedSet<Integer> lines = new TreeSet<>();
        lines.add(1);
        lines.add(3);
        lines.add(5);
        final CoveragePattern pattern = new CoveragePattern("resourcePattern", lines);

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 3, full.size());
        Assert.assertTrue("SortedSet must contain line 1", full.contains(1));
        Assert.assertTrue("SortedSet must contain line 3", full.contains(3));
        Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
    }

    @Test
    public void testAddLines() throws Exception {
        final SortedSet<Integer> lines = new TreeSet<>();
        CoveragePattern.addLines(lines, 2, 2);
        CoveragePattern.addLines(lines, 4, 6);
        final CoveragePattern pattern = new CoveragePattern("resourcePattern", lines);

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
        Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
        Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
        Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
        Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddLinesException() throws Exception {
        final SortedSet<Integer> lines = new TreeSet<>();
        CoveragePattern.addLines(lines, 6, 4);

        Assert.fail("must not allow to switch from and to");
    }

    @Test
    public void testEmpty() throws Exception {
        final CoveragePattern pattern = new CoveragePattern("resourcePattern", new TreeSet<Integer>());

        final SortedSet<Integer> empty = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", empty);
        Assert.assertEquals("SortedSet must not contain any entries", 0, empty.size());
    }

    @Test
    public void testParse() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintWriter writer = new PrintWriter(baos);
        writer.println("# comment");
        writer.println();
        writer.println("resourcePattern;[2,4-6]");
        writer.close();

        final InputStream is = new ByteArrayInputStream(baos.toByteArray());
        try {
            final List<CoveragePattern> lines = CoveragePattern.parse(is);
            Assert.assertNotNull("List must not be null", lines);
            Assert.assertEquals("List must contain the exact number of entries", 1, lines.size());

            final CoveragePattern pattern = lines.get(0);
            final SortedSet<Integer> full = pattern.getLines();
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
    public void testParseEmpty() throws IOException {
        final InputStream is = new ByteArrayInputStream(new byte[0]);
        try {
            final List<CoveragePattern> lines = CoveragePattern.parse(is);
            Assert.assertNotNull("List must not be null", lines);
            Assert.assertEquals("List must contain the exact number of entries", 0, lines.size());
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLineMissingLines() throws Exception {
        CoveragePattern.parseLine("resourcePattern; ");
        Assert.fail("must not parse invalid input");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLineMissingResourcePattern() throws Exception {
        CoveragePattern.parseLine(" ;[2-3]");
        Assert.fail("must not parse invalid input");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLineMissingRulePattern() throws Exception {
        CoveragePattern.parseLine("resourcePattern; ;[2-3]");
        Assert.fail("must not parse invalid input");
    }

    @Test
    public void testParseLineMixed() throws Exception {
        final CoveragePattern pattern = CoveragePattern.parseLine("resourcePattern;[2,4-6]");
        Assert.assertEquals("resourcePattern name must match", "resourcePattern", pattern.getResourcePattern());

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 4, full.size());
        Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
        Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
        Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
        Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
    }

    @Test
    public void testParseLineRange() throws Exception {
        final CoveragePattern pattern = CoveragePattern.parseLine("resourcePattern;[2-6]");
        Assert.assertEquals("resourcePattern name must match", "resourcePattern", pattern.getResourcePattern());

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 5, full.size());
        Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
        Assert.assertTrue("SortedSet must contain line 3", full.contains(3));
        Assert.assertTrue("SortedSet must contain line 4", full.contains(4));
        Assert.assertTrue("SortedSet must contain line 5", full.contains(5));
        Assert.assertTrue("SortedSet must contain line 6", full.contains(6));
    }

    @Test
    public void testParseLineRangeAllLines() throws Exception {
        final CoveragePattern pattern = CoveragePattern.parseLine("resourcePattern;*");
        Assert.assertEquals("resourcePattern name must match", "resourcePattern", pattern.getResourcePattern());

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 0, full.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseLineRangeException() throws Exception {
        CoveragePattern.parseLineValues("resourcePattern;[6-2]");
        Assert.fail("must not allow to switch from and to");
    }

    @Test
    public void testParseLineSingle() throws Exception {
        final CoveragePattern pattern = CoveragePattern.parseLine("resourcePattern;[2]");
        Assert.assertEquals("resourcePattern name must match", "resourcePattern", pattern.getResourcePattern());

        final SortedSet<Integer> full = pattern.getLines();
        Assert.assertNotNull("SortedSet must not be null", full);
        Assert.assertEquals("SortedSet must contain the exact number of entries", 1, full.size());
        Assert.assertTrue("SortedSet must contain line 2", full.contains(2));
    }
}

