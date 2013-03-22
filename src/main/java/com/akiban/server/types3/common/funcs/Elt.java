
package com.akiban.server.types3.common.funcs;

import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.TPreptimeValue;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.pvalue.PValueTargets;
import com.akiban.server.types3.texpressions.Constantness;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public class Elt extends TScalarBase
{

    private final TClass stringType;
    private final TClass intType;
    
    public Elt(TClass intType, TClass stringType)
    {
        this.intType = intType;
        this.stringType = stringType;
    }
    
    @Override
    protected void buildInputSets(TInputSetBuilder builder)
    {
        // ELT(<INT>, <T> ....)
        // argc >= 2
        builder.covers(intType, 0).pickingVararg(stringType, 1);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
    {
        int index = inputs.get(0).getInt32();
        int nvarargs = inputs.size();
        if (index < 1 || index >= nvarargs)
            output.putNull();
        else
            PValueTargets.copyFrom(inputs.get(index), output);
    }
     
    @Override
    protected boolean constnessMatters(int inputIndex) 
    {
        return true;
    }

    @Override
    protected Constantness constness(int inputIndex, LazyList<? extends TPreptimeValue> values) {
        assert inputIndex == 0 : inputIndex + " for " + values; // 0 should be enough to fully answer the question
        PValueSource indexVal = constSource(values, 0);
        if (indexVal == null)
            return Constantness.NOT_CONST;
        if (indexVal.isNull())
            return Constantness.CONST;
        int answerIndex = indexVal.getInt32();
        if (answerIndex < 1 || answerIndex >= values.size())
            return Constantness.CONST; // answer is null
        PValueSource answer = constSource(values, answerIndex);
        return answer == null ? Constantness.NOT_CONST : Constantness.CONST;
    }

    @Override
    protected boolean nullContaminates(int inputIndex) 
    {
        return inputIndex == 0;
    }

    @Override
    public String displayName()
    {
        return "ELT";
    }

    @Override
    public TOverloadResult resultType()
    {
        return TOverloadResult.picking();
    }
}
