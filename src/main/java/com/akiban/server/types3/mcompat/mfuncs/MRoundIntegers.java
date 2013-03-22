
package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.TScalar;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public class MRoundIntegers extends TScalarBase {

    public static final TScalar[] roundInts = new TScalar[] {
            new MRoundIntegers(MNumeric.TINYINT),
            new MRoundIntegers(MNumeric.SMALLINT),
            new MRoundIntegers(MNumeric.INT),
            new MRoundIntegers(MNumeric.MEDIUMINT),
            new MRoundIntegers(MNumeric.BIGINT),
            new MRoundIntegers(MNumeric.TINYINT_UNSIGNED),
            new MRoundIntegers(MNumeric.SMALLINT_UNSIGNED),
            new MRoundIntegers(MNumeric.INT_UNSIGNED),
            new MRoundIntegers(MNumeric.MEDIUMINT_UNSIGNED),
            new MRoundIntegers(MNumeric.BIGINT_UNSIGNED),
    };

    @Override
    protected void buildInputSets(TInputSetBuilder builder) {
        builder.pickingCovers(targetClass, 0);
        builder.covers(MNumeric.INT, 1);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
        long numToRound = MNumeric.getAsLong(targetClass, inputs.get(0));
        long roundBy = inputs.get(1).getInt32();
        if (roundBy < 0) {
            // We'll use simple mods, division and multiplication to get two values:
            // 1) places: a multiple of 10 that represents the places to round to -- 10^roundBy.
            // 2) roundedComponent a number (0 <= n < places) which represents just the portion of the original number
            //    which is to be rounded up/down
            // Once we have those two, we either subtract roundedComponent if it's less than half of places, or else
            // add (places - roundedComponent).
            // For instance, in ROUND(1234, 2), we have places=100, roundedComponent=34, places/2 = 50, so we do
            // 1234 - 34.
            // For ROUND(5678, 2) we have roundedComponent = 78, so we do 5678 + (100 - 78) = 5678 + 22 = 5700.
            roundBy = -roundBy;
            long roundedComponent = 0;
            long places = 1;
            long numCopy = numToRound;
            for (int i = 0; i < roundBy; ++i) {
                long digit = numCopy % 10;
                numCopy /= 10;
                roundedComponent += (digit * places);
                places *= 10;
            }
            long halfway = places / 2;
            if (roundedComponent < halfway) {
                numToRound -= roundedComponent;
            }
            else {
                numToRound += (places - roundedComponent);
            }
        }
        MNumeric.putAsLong(targetClass, output, numToRound);
    }

    @Override
    public String displayName() {
        return "ROUND";
    }

    @Override
    public TOverloadResult resultType() {
        return TOverloadResult.picking();
    }

    private MRoundIntegers(TClass targetClass) {
        this.targetClass = targetClass;
    }

    private final TClass targetClass;
}
