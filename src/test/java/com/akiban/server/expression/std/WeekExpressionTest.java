
package com.akiban.server.expression.std;

import com.akiban.server.types.ValueSourceIsNullException;
import com.akiban.server.error.WrongExpressionArityException;
import com.akiban.server.expression.Expression;
import com.akiban.server.expression.ExpressionComposer;
import com.akiban.server.types.AkType;
import com.akiban.server.types.extract.Extractors;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;

public class WeekExpressionTest extends ComposedExpressionTestBase
{
    private static final CompositionTestInfo info = new CompositionTestInfo (1, AkType.DATE, true);

    @Test
    public void testFirstDay()
    {
        // test first day
        String st = "2009-1-1";
        test(st,0, 0);
        test(st,1,1);
        test(st,2,52);
        test(st,3,1);
        test(st,4,0);
        test(st,5,0);
        test(st,6,53);
        test(st,7,52);

        // test last day
        for (int i = 0; i < 8; ++i)
            test("2012-12-31",i,53);

        // test 2010-may-08
        test(st = "2011-5-8",7,18);
        test(st,6,19);
        test(st,5,18);
        test(st,4,19);
        test(st,3,18);
        test(st,2,19);
        test(st,1,18);
        test(st,0,19);
    }

    @Test
    public void testWeekOfYear_bug () // bug 905447 - unit-test passes
    {
        Expression week = compose(WeekExpression.WEEK_OF_YEAR_COMPOSER,
                                  Arrays.asList(new LiteralExpression(AkType.DATE,
                    Extractors.getLongExtractor(AkType.DATE).getLong("1864-02-28"))));
        int actual = (int)week.evaluation().eval().getInt();
        assertEquals("week of year: ", 8, actual);
    }

    @Test 
    public void testNullFirst ()
    {
        Expression week = new WeekExpression(Arrays.asList(LiteralExpression.forNull(),
                new LiteralExpression(AkType.LONG, 4)));
        assertEquals(AkType.INT,week.valueType());
        assertTrue(week.evaluation().eval().isNull());
    }

    @Test 
    public void testNullSecond ()
    {
        Expression week = new WeekExpression(Arrays.asList(new LiteralExpression(AkType.DATE, 12345L),
                LiteralExpression.forNull()));
        assertEquals(AkType.INT,week.valueType());
        assertTrue(week.evaluation().eval().isNull());
    }

    @Test (expected = WrongExpressionArityException.class)
    public void testWrongArity()
    {
        Expression week = new WeekExpression(Arrays.asList(new LiteralExpression(AkType.DATE, 12345L),
                new LiteralExpression(AkType.INT, 4),
                new LiteralExpression(AkType.INT, 4)));
    }
    
    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroYear()
    {
        test("0000-12-2", 0, 0);
    }

    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroMonth()
    {
        test("0001-00-02", 0, 0);
    }
    
    @Test (expected = ValueSourceIsNullException.class)
    public void testZeroDay()
    {
        test("0001-02-00", 0, 0);
    }
    
    @Test (expected = ValueSourceIsNullException.class)
    public void testInvalidMode()
    {
        test("2009-12-2", 10, 0);
    }

    private void test(String dateS, int mode, int exp)
    {
        long date = Extractors.getLongExtractor(AkType.DATE).getLong(dateS);

        Expression d = new LiteralExpression(AkType.DATE, date);
        Expression m = new LiteralExpression(AkType.INT, mode);
        Expression week = new WeekExpression(Arrays.asList(d, m));

        int actual = (int) week.evaluation().eval().getInt();
        assertEquals("assert topType is INT", AkType.INT, week.valueType());
        assertEquals("DATE: " + date + ", mode " + mode, actual, exp);
    }
    
    @Override
    protected boolean alreadyExc()
    {
        return false;
    }

    @Override
    protected CompositionTestInfo getTestInfo()
    {
        return info;
    }

    @Override
    protected ExpressionComposer getComposer()
    {
        return WeekExpression.WEEK_COMPOSER;
    }
}
