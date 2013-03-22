
package com.akiban.server.api.dml.scan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.EnumSet;
import java.util.Map;

import org.junit.Test;

public final class SimplePredicateTest {
    @Test
    public void testEquals() throws Exception {
        final SimplePredicate predicate = new SimplePredicate(0, SimplePredicate.Comparison.EQ);
        final Object col0Val = new Object();

        predicate.addColumn(0, col0Val);

        final NewRow start = predicate.getStartRow();
        final NewRow end = predicate.getEndRow();

        assertSame("start and end", start, end);

        Map<Integer,Object> actualFields = start.getFields();
        assertEquals("fields size", 1, actualFields.size());
        assertSame("value[0]", col0Val, actualFields.get(0));

        assertEquals("scan flags",
                EnumSet.of(
                    ScanFlag.START_RANGE_EXCLUSIVE,
                    ScanFlag.END_RANGE_EXCLUSIVE
                ),
                predicate.getScanFlags());
    }
    
    public void testGeneralNE(SimplePredicate.Comparison comparison) {
        final SimplePredicate predicate = new SimplePredicate(0, comparison);
        final Object col0Val = new Object();

        predicate.addColumn(0, col0Val);

        final NewRow start = predicate.getStartRow();
        final NewRow end = predicate.getEndRow();

        EnumSet<ScanFlag> expectedFlags = EnumSet.noneOf(ScanFlag.class);
        final NewRow rowToCheck;
        switch (comparison) {
            case LT:
                expectedFlags.add(ScanFlag.END_RANGE_EXCLUSIVE);
            case LTE:
                expectedFlags.add(ScanFlag.START_AT_BEGINNING);
                assertNull("start isn't null", start);
                assertNotNull("end isn't null", end);
                rowToCheck = end;
                break;
            case GT:
                expectedFlags.add(ScanFlag.START_RANGE_EXCLUSIVE);
            case GTE:
                expectedFlags.add(ScanFlag.END_AT_END);
                assertNotNull("start is null", start);
                assertNull("end isn't null", end);
                rowToCheck = start;
                break;
            default:
                throw new RuntimeException("inapplicable comparison: " + comparison);
        }


        Map<Integer,Object> actualFields = rowToCheck.getFields();
        assertEquals("fields size", 1, actualFields.size());
        assertSame("value[0]", col0Val, actualFields.get(0));

        assertEquals("scan flags", expectedFlags, predicate.getScanFlags());
    }

    @Test
    public void testLT() {
        testGeneralNE(SimplePredicate.Comparison.LT);
    }


    @Test
    public void testLTE() {
        testGeneralNE(SimplePredicate.Comparison.LTE);
    }


    @Test
    public void testGT() {
        testGeneralNE(SimplePredicate.Comparison.GT);
    }


    @Test
    public void testGTE() {
        testGeneralNE(SimplePredicate.Comparison.GTE);
    }

}
