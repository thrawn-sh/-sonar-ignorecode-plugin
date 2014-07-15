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
package de.shadowhunt.sonar.plugins.ignorecode.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.batch.DecoratorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.utils.KeyValueFormat;

public class ModifyMeasuresTest {

    private static final KeyValueFormat.IntegerConverter CONVERTER = KeyValueFormat.newIntegerConverter();

    private static final double DELTA = 0.001;

    private static Map<Integer, Integer> parse(String data) {
        return KeyValueFormat.parse(data, CONVERTER, CONVERTER);
    }

    private Map<String, Map<Integer, Integer>> conditionData = new HashMap<>(2);

    private final ModifyMeasures modifyMeasures = new ModifyMeasures();

    @Before
    public void setUp() throws Exception {
        final MeasuresStorage measuresStorage = Mockito.mock(MeasuresStorage.class);
        modifyMeasures.setMeasuresStorage(measuresStorage);

        conditionData = new HashMap<>(2);
        final Map<Integer, Integer> total = new LinkedHashMap<>();
        total.put(1, 2);
        total.put(4, 4);
        conditionData.put(ModifyMeasures.TOTAL_CONDITIONS, total);

        final Map<Integer, Integer> covered = new LinkedHashMap<>();
        covered.put(1, 1);
        covered.put(4, 2);
        conditionData.put(ModifyMeasures.COVERED_CONDITIONS, covered);
    }

    @Test
    public void testFilterConditionsData() throws Exception {
        final Measure total = new Measure(CoreMetrics.CONDITIONS_BY_LINE, "1=2;2=0;3=2;4=4");
        final Measure covered = new Measure(CoreMetrics.COVERED_CONDITIONS_BY_LINE, "1=1;2=0;3=2;4=2");
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(total);
        Mockito.when(context.getMeasure(CoreMetrics.COVERED_CONDITIONS_BY_LINE)).thenReturn(covered);

        final Map<String, Map<Integer, Integer>> result = modifyMeasures.filterConditionsData(context, toSet(2, 3));
        Assert.assertEquals("CONDITIONS_BY_LINE", parse("1=2;4=4"), result.get(ModifyMeasures.TOTAL_CONDITIONS));
        Assert.assertEquals("CONDITIONS_BY_LINE", "1=2;4=4", total.getData());
        Assert.assertEquals("COVERED_CONDITIONS_BY_LINE", parse("1=1;4=2"), result.get(ModifyMeasures.COVERED_CONDITIONS));
        Assert.assertEquals("COVERED_CONDITIONS_BY_LINE", "1=1;4=2", covered.getData());
    }

    @Test
    public void testFilterConditionsDataNull() throws Exception {
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.CONDITIONS_BY_LINE)).thenReturn(null);

        final Map<String, Map<Integer, Integer>> conditionsData = modifyMeasures.filterConditionsData(context, toSet());
        Assert.assertNull("no CONDITIONS_BY_LINE", conditionsData);
    }

    @Test
    public void testFilterLinesData() throws Exception {
        final Measure measure = new Measure(CoreMetrics.COVERAGE_LINE_HITS_DATA, "1=2;2=2;3=0;4=2");
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(measure);

        final Map<Integer, Integer> result = modifyMeasures.filterLinesData(context, toSet(2, 3));
        Assert.assertEquals("LINE_COVERAGE", parse("1=2;4=2"), result);
        Assert.assertEquals("LINE_COVERAGE", "1=2;4=2", measure.getData());
    }

    @Test
    public void testFilterLinesDataNull() throws Exception {
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.COVERAGE_LINE_HITS_DATA)).thenReturn(null);

        final Map<Integer, Integer> lineData = modifyMeasures.filterLinesData(context, toSet());
        Assert.assertNull("no LINE_COVERAGE", lineData);
    }

    @Test
    public void testRewriteConditionsCountTotal() throws Exception {
        final Measure measure = new Measure(CoreMetrics.CONDITIONS_TO_COVER, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.CONDITIONS_TO_COVER)).thenReturn(measure);

        final long result = modifyMeasures.rewriteConditionsCountTotal(context, conditionData);
        Assert.assertEquals("CONDITIONS_TO_COVER", 6L, result);
        Assert.assertEquals("CONDITIONS_TO_COVER", 6.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteConditionsCountUncovered() throws Exception {
        final Measure measure = new Measure(CoreMetrics.UNCOVERED_CONDITIONS, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.UNCOVERED_CONDITIONS)).thenReturn(measure);

        final long result = modifyMeasures.rewriteConditionsCountUncovered(context, conditionData);
        Assert.assertEquals("CONDITIONS_TO_COVER", 3L, result);
        Assert.assertEquals("CONDITIONS_TO_COVER", 3.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteConditionsCoveragePercentage() throws Exception {
        final Measure measure = new Measure(CoreMetrics.BRANCH_COVERAGE, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.BRANCH_COVERAGE)).thenReturn(measure);

        final double result = modifyMeasures.rewriteConditionsCoveragePercentage(context, 200L, 30L);
        Assert.assertEquals("LINE_COVERAGE", 85.0, result, DELTA);
        Assert.assertEquals("LINE_COVERAGE", 85.0, measure.getValue(), DELTA);

        final double zero = modifyMeasures.rewriteConditionsCoveragePercentage(context, 0L, 30L);
        Assert.assertEquals("LINE_COVERAGE", 0.0, zero, DELTA);
        Assert.assertEquals("LINE_COVERAGE", 0.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteLinesCountTotal() throws Exception {
        final Measure measure = new Measure(CoreMetrics.LINES_TO_COVER, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.LINES_TO_COVER)).thenReturn(measure);

        final Map<Integer, Integer> lineData = parse("1=2;2=2;3=0;4=2");
        final long result = modifyMeasures.rewriteLinesCountTotal(context, lineData);
        Assert.assertEquals("LINE_COVERAGE", 4L, result);
        Assert.assertEquals("LINE_COVERAGE", 4.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteLinesCountUncovered() throws Exception {
        final Measure measure = new Measure(CoreMetrics.UNCOVERED_LINES, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.UNCOVERED_LINES)).thenReturn(measure);

        final Map<Integer, Integer> lineData = parse("1=1;2=1;3=0;4=1");
        final long result = modifyMeasures.rewriteLinesCountUncovered(context, lineData);
        Assert.assertEquals("UNCOVERED_LINES", 1L, result);
        Assert.assertEquals("UNCOVERED_LINES", 1.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteLinesCoveragePercentage() throws Exception {
        final Measure measure = new Measure(CoreMetrics.LINE_COVERAGE, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.LINE_COVERAGE)).thenReturn(measure);

        final double result = modifyMeasures.rewriteLinesCoveragePercentage(context, 200L, 30L);
        Assert.assertEquals("LINE_COVERAGE", 85.0, result, DELTA);
        Assert.assertEquals("LINE_COVERAGE", 85.0, measure.getValue(), DELTA);

        final double zero = modifyMeasures.rewriteLinesCoveragePercentage(context, 0L, 30L);
        Assert.assertEquals("LINE_COVERAGE", 0.0, zero, DELTA);
        Assert.assertEquals("LINE_COVERAGE", 0.0, measure.getValue(), DELTA);
    }

    @Test
    public void testRewriteOverallCoverage() throws Exception {
        final Measure measure = new Measure(CoreMetrics.COVERAGE, 42.0);
        DecoratorContext context = Mockito.mock(DecoratorContext.class);
        Mockito.when(context.getMeasure(CoreMetrics.COVERAGE)).thenReturn(measure);

        final double result = modifyMeasures.rewriteOverallCoverage(context, 200L, 30L, 50L, 20L);
        Assert.assertEquals("COVERAGE", 80.0, result, DELTA);
        Assert.assertEquals("COVERAGE", 80.0, measure.getValue(), DELTA);

        final double zeroBranch = modifyMeasures.rewriteOverallCoverage(context, 200L, 30L, 0L, 0L);
        Assert.assertEquals("COVERAGE", 85.0, zeroBranch, DELTA);
        Assert.assertEquals("COVERAGE", 85.0, measure.getValue(), DELTA);

        final double noCoverage = modifyMeasures.rewriteOverallCoverage(context, 0L, 0L, 0L, 0L);
        Assert.assertEquals("COVERAGE", 0.0, noCoverage, DELTA);
        Assert.assertEquals("COVERAGE", 0.0, measure.getValue(), DELTA);
    }

    private Set<Integer> toSet(final int... values) {
        Set<Integer> set = new HashSet<>(values.length);
        for (int value : values) {
            set.add(value);
        }
        return set;
    }
}
