package de.shadowhunt.sonar.plugins.ignorecode.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

public class LineValuePairTest {

	@Test
	public void parseDataString() {
		final List<LineValuePair> pairs = LineValuePair.parseDataString("1=5;2=4;5=6");
		Assert.assertNotNull("List must not be null", pairs);
		Assert.assertEquals("List must contain the exact number of entries", 3, pairs.size());

		final LineValuePair pair1 = pairs.get(0);
		Assert.assertEquals("LineValuePair must contain line 1", 1, pair1.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 5", 5, pair1.getValue());

		final LineValuePair pair2 = pairs.get(1);
		Assert.assertEquals("LineValuePair must contain line 2", 2, pair2.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 4", 4, pair2.getValue());

		final LineValuePair pair3 = pairs.get(2);
		Assert.assertEquals("LineValuePair must contain line 5", 5, pair3.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 6", 6, pair3.getValue());
	}

	@Test
	public void parseDataStringBlanks() {
		final List<LineValuePair> pairs = LineValuePair.parseDataString(";1=5;;2=4; ;   ;5=6;");
		Assert.assertNotNull("List must not be null", pairs);
		Assert.assertEquals("List must contain the exact number of entries", 3, pairs.size());

		final LineValuePair pair1 = pairs.get(0);
		Assert.assertEquals("LineValuePair must contain line 1", 1, pair1.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 5", 5, pair1.getValue());

		final LineValuePair pair2 = pairs.get(1);
		Assert.assertEquals("LineValuePair must contain line 2", 2, pair2.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 4", 4, pair2.getValue());

		final LineValuePair pair3 = pairs.get(2);
		Assert.assertEquals("LineValuePair must contain line 5", 5, pair3.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 6", 6, pair3.getValue());
	}

	@Test
	public void parseDataStringBroken() {
		final List<LineValuePair> pairs = LineValuePair.parseDataString("1=5;2=4;5=6;6=;=7");
		Assert.assertNotNull("List must not be null", pairs);
		Assert.assertEquals("List must contain the exact number of entries", 3, pairs.size());

		final LineValuePair pair1 = pairs.get(0);
		Assert.assertEquals("LineValuePair must contain line 1", 1, pair1.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 5", 5, pair1.getValue());

		final LineValuePair pair2 = pairs.get(1);
		Assert.assertEquals("LineValuePair must contain line 2", 2, pair2.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 4", 4, pair2.getValue());

		final LineValuePair pair3 = pairs.get(2);
		Assert.assertEquals("LineValuePair must contain line 5", 5, pair3.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 6", 6, pair3.getValue());
	}

	@Test
	public void parseDataStringEmpty() {
		final List<LineValuePair> pairs = LineValuePair.parseDataString("");
		Assert.assertNotNull("List must not be null", pairs);
	}

	@Test
	public void sumValuesEmpty() {
		final List<LineValuePair> pairs = Collections.emptyList();
		Assert.assertEquals("sum must match", 0, LineValuePair.sumValues(pairs));
	}

	@Test
	public void sumValuesSingle() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		Assert.assertEquals("sum must match", 5, LineValuePair.sumValues(pairs));
	}

	@Test
	public void toDataStringEmpty() {
		final List<LineValuePair> pairs = Collections.emptyList();
		Assert.assertEquals("data sting must match", "", LineValuePair.toDataString(pairs));
	}

	@Test
	public void toDataStringSingle() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		Assert.assertEquals("data sting must match", "1=5", LineValuePair.toDataString(pairs));
	}

	@Test
	public void removeIgnoresEmptyParis() {
		final List<LineValuePair> pairs = Collections.emptyList();
		final Set<Integer> ignoreLines = new HashSet<Integer>();
		ignoreLines.add(2);

		LineValuePair.removeIgnores(pairs, ignoreLines);
		Assert.assertEquals("List must contain the exact number of entries", 0, pairs.size());
	}

	@Test
	public void removeIgnoresEmptyIgnores() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		pairs.add(new LineValuePair(2, 7));
		pairs.add(new LineValuePair(3, -4));
		final Set<Integer> ignoreLines = Collections.emptySet();

		LineValuePair.removeIgnores(pairs, ignoreLines);
		Assert.assertEquals("List must contain the exact number of entries", 0, pairs.size());
	}

	@Test
	public void removeIgnores() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		pairs.add(new LineValuePair(2, 7));
		pairs.add(new LineValuePair(3, -4));
		final Set<Integer> ignoreLines = new HashSet<Integer>();
		ignoreLines.add(1);
		ignoreLines.add(3);

		LineValuePair.removeIgnores(pairs, ignoreLines);
		Assert.assertEquals("List must contain the exact number of entries", 1, pairs.size());

		final LineValuePair pair1 = pairs.get(0);
		Assert.assertEquals("LineValuePair must contain line 2", 2, pair1.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 7", 7, pair1.getValue());
	}

	@Test
	public void toDataString() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		pairs.add(new LineValuePair(2, 6));
		Assert.assertEquals("data sting must match", "1=5;2=6", LineValuePair.toDataString(pairs));
	}

	@Test
	public void sumValues() {
		final List<LineValuePair> pairs = new ArrayList<LineValuePair>();
		pairs.add(new LineValuePair(1, 5));
		pairs.add(new LineValuePair(2, 7));
		pairs.add(new LineValuePair(3, -4));
		Assert.assertEquals("sum must match", 8, LineValuePair.sumValues(pairs));
	}

	@Test
	public void parseDataStringSinglePair() {
		final List<LineValuePair> pairs = LineValuePair.parseDataString("1=5");
		Assert.assertNotNull("List must not be null", pairs);
		Assert.assertEquals("List must contain the exact number of entries", 1, pairs.size());

		final LineValuePair pair = pairs.get(0);
		Assert.assertEquals("LineValuePair must contain line 1", 1, pair.getLineNumber());
		Assert.assertEquals("LineValuePair must contain value 5", 5, pair.getValue());
	}
}
