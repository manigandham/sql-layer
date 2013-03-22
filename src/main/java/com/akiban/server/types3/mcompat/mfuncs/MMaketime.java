package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.types3.*;
import com.akiban.server.types3.mcompat.mtypes.MDatetimes;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public class MMaketime extends TScalarBase {

    public static final TScalar INSTANCE = new MMaketime() {};
    
    private MMaketime() {}

    @Override
    protected void buildInputSets(TInputSetBuilder builder)
    {
        builder.covers(MNumeric.INT, 0, 1, 2);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
    {
        // Time input format HHMMSS
        int hours = inputs.get(0).getInt32();
        int minutes = inputs.get(1).getInt32();
        int seconds = inputs.get(2).getInt32();
        
        // Check for invalid input
        if (minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
            output.putNull();
            return;
        }
        
        int mul;
        if (hours < 0)
            hours *= mul = -1;
        else
            mul = 1;

        output.putInt32(MDatetimes.encodeTime(hours, minutes, seconds, context));
    }

    @Override
    public String displayName() {
        return "MAKETIME";
    }

    @Override
    public TOverloadResult resultType() {
        return TOverloadResult.fixed(MDatetimes.TIME);
    }
}
