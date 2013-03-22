
package com.akiban.server.types3.common.funcs;

import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TScalar;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public abstract class Locate extends TScalarBase
{
    public static TScalar create2ArgOverload(final TClass stringType, final TClass intType, String name)
    {
        return new Locate(intType, name)
        {
            @Override
            protected void buildInputSets(TInputSetBuilder builder)
            {
                builder.covers(stringType, 0, 1);
            }
        };
    }
    
    public static TScalar create3ArgOverload(final TClass stringType, final TClass intType, String name)
    {
        return new Locate(intType, name)
        {
            @Override
            protected void buildInputSets(TInputSetBuilder builder)
            {
                builder.covers(stringType, 0, 1).covers(intType, 2);
            }
        };
    }
    
    private final TClass intType;
    private final String name;
    
    Locate(TClass intType, String name)
    {
        this.intType = intType;
        this.name = name;
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
    {
        String str = inputs.get(1).getString();
        String substr = inputs.get(0).getString();

        int index = 0;
        if (inputs.size() == 3)
        {
            index = inputs.get(2).getInt32() - 1; // mysql uses 1-based indexing
            // invalid index => return 0 as the result
            if (index < 0 || index > str.length())
            {
                output.putInt32(0);
                return;
            }
        }
        output.putInt32(1 + str.indexOf(substr, index));
    }

    @Override
    public String displayName()
    {
        return name;
    }

    @Override
    public TOverloadResult resultType()
    {
        return TOverloadResult.fixed(intType);
    }
}

