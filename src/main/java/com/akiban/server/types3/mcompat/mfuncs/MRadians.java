
package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TScalar;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.mcompat.mtypes.MApproximateNumber;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public class MRadians extends TScalarBase {
    public static final TScalar INSTANCE = new MRadians();
    
    private MRadians(){}
    
    @Override
    protected void buildInputSets(TInputSetBuilder builder) {
        builder.covers(MApproximateNumber.DOUBLE, 0);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
        output.putDouble(Math.toRadians(inputs.get(0).getDouble()));
    }

    @Override
    public String displayName() {
        return "RADIANS";
    }

    @Override
    public TOverloadResult resultType() {
        return TOverloadResult.fixed(MApproximateNumber.DOUBLE);
    }
}
