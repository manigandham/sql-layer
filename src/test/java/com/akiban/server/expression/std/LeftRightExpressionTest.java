
package com.akiban.server.expression.std;

import com.akiban.junit.OnlyIf;
import com.akiban.junit.OnlyIfNot;
import com.akiban.junit.ParameterizationBuilder;
import com.akiban.junit.Parameterization;
import java.util.Collection;
import com.akiban.junit.NamedParameterizedRunner.TestParameters;
import com.akiban.junit.NamedParameterizedRunner;
import org.junit.runner.RunWith;
import com.akiban.server.error.WrongExpressionArityException;
import com.akiban.server.types.util.ValueHolder;
import com.akiban.server.types.NullValueSource;
import com.akiban.server.expression.Expression;
import com.akiban.server.expression.ExpressionComposer;
import com.akiban.server.types.AkType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.*;

@RunWith(NamedParameterizedRunner.class)
public class LeftRightExpressionTest extends ComposedExpressionTestBase
{
    private static boolean alreadyExc = false;
    
    private String st;
    private Integer len;
    private String expected;
    private Integer argc;
    private final ExpressionComposer composer;
    
    public LeftRightExpressionTest(String str, Integer length, String exp, Integer count, ExpressionComposer com)
    {
        st = str;
        len = length;
        expected = exp;
        argc = count;
        composer = com;
    }
    
    @TestParameters
    public static Collection<Parameterization> params()
    {
        ParameterizationBuilder pb = new ParameterizationBuilder();
        
        String name;
        testLeft(pb, name = "Test Shorter Length", "abc", 2, "ab", null);
        testLeft(pb, name, "abc", 0, "", null);
        testLeft(pb, name, "abc", -4, "", null);
        
        testLeft(pb, name = "Test Longer Length", "abc", 4, "abc", null);
        testLeft(pb, name, "abc", 3, "abc", null);
        
        testLeft(pb, name = "Test NULL", null, 3, null, null);
        testLeft(pb, name, "ab", null, null, null);
        
        testLeft(pb, name = "Test Wrong Arity", null, null, null, 0);
        testLeft(pb, name, null, null, null, 1);
        testLeft(pb, name, null, null, null, 3);
        testLeft(pb, name, null, null, null, 4);
        testLeft(pb, name, null, null, null, 5);
        
        return pb.asList();
    }
    
    private static void testLeft (ParameterizationBuilder pb, String name, String str, Integer length, String exp, Integer argc)
    {
        pb.add(name + " LEFT(" + str + ", " + length + "), argc = " + argc, str, length, exp, argc, LeftRightExpression.LEFT_COMPOSER);
    }
    
    private static void testRight(ParameterizationBuilder pb, String name, String str, Integer length, String exp, Integer argc)
    {
        pb.add(name + " RIGHT(" + str + ", " + length + "), argc = " + argc, str, length, exp, argc, LeftRightExpression.RIGHT_COMPOSER);
    }
    
    @OnlyIfNot("testArity()")
    @Test
    public void testRegularCases()
    {
        Expression str = new LiteralExpression(AkType.VARCHAR, st);
        Expression length = len == null? LiteralExpression.forNull():
                            new LiteralExpression(AkType.LONG,  len.intValue());
        
        Expression top = compose(composer, Arrays.asList(str, length));
        
        assertEquals("LEFT(" + st + ", " + len + ") ", 
                    expected == null? NullValueSource.only() : new ValueHolder(AkType.VARCHAR, expected),
                    top.evaluation().eval());
        alreadyExc = true;
    }
    
    @OnlyIf("testArity()")
    @Test(expected = WrongExpressionArityException.class)
    public void testFunctionArity()
    {
        List<Expression> args = new ArrayList<>();
        for (int n = 0; n < argc; ++n)
            args.add(LiteralExpression.forNull());
        compose(composer, args);
        alreadyExc = true;
    }
    
    public boolean testArity()
    {
        return argc != null;
    }
    
    @Override
    protected CompositionTestInfo getTestInfo()
    {
        return new CompositionTestInfo(2, AkType.VARCHAR, true);
    }

    @Override
    protected ExpressionComposer getComposer()
    {
        return composer;
    }

    @Override
    public boolean alreadyExc()
    {
        return alreadyExc;
    }
}
