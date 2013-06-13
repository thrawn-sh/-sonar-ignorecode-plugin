package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.HashSet;

import org.junit.Assert;
import org.junit.Test;

public class IgnorePatternTest {

	@Test(expected = IllegalArgumentException.class)
	public void testAddLinesException() {
		IgnorePattern.addLines(new HashSet<Integer>(), 3, 2);
	}

	@Test
	public void testAddLines() {
		final HashSet<Integer> set = new HashSet<Integer>();
		IgnorePattern.addLines(set, 4, 8);

		Assert.assertEquals("", 5, set.size());
		Assert.assertTrue("", set.contains(4));
		Assert.assertTrue("", set.contains(5));
		Assert.assertTrue("", set.contains(6));
		Assert.assertTrue("", set.contains(7));
		Assert.assertTrue("", set.contains(8));
	}
}
