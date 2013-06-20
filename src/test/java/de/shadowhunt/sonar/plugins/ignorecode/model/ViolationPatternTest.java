package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

public class ViolationPatternTest {

	@Test(expected = IllegalArgumentException.class)
	public void testAddLinesException() {
		ViolationPattern.addLines(new TreeSet<Integer>(), 3, 2);
	}

	@Test
	public void testAddLines() {
		final SortedSet<Integer> set = new TreeSet<Integer>();
		ViolationPattern.addLines(set, 4, 8);

		Assert.assertEquals("", 5, set.size());
		Assert.assertTrue("", set.contains(4));
		Assert.assertTrue("", set.contains(5));
		Assert.assertTrue("", set.contains(6));
		Assert.assertTrue("", set.contains(7));
		Assert.assertTrue("", set.contains(8));
	}
}
