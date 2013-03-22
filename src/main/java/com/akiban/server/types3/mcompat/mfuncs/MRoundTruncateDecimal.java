package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.types3.*;
import com.akiban.server.types3.common.BigDecimalWrapper;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimal;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MRoundTruncateDecimal extends TScalarBase {

    public static final Collection<TScalar> overloads = createAll();

    private static final int DEC_INDEX = 0;

    private enum RoundingStrategy {
        ROUND {
            @Override
            protected void apply(BigDecimalWrapper io, int scale) {
                io.round(scale);
            }
        },
        TRUNCATE {
            @Override
            protected void apply(BigDecimalWrapper io, int scale) {
                io.truncate(scale);
            }
        };

        protected abstract void apply(BigDecimalWrapper io, int scale);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output) {
        BigDecimalWrapper result = MBigDecimal.getWrapper(context, DEC_INDEX);
        result.set(MBigDecimal.getWrapper(inputs.get(0), context.inputTInstanceAt(0)));
        int scale = signatureStrategy.roundToScale(inputs);
        roundingStrategy.apply(result, scale);
        output.putObject(result);
    }

    @Override
    public TOverloadResult resultType() {
        return TOverloadResult.custom(new TCustomOverloadResult() {
            @Override
            public TInstance resultInstance(List<TPreptimeValue> inputs, TPreptimeContext context) {
                TPreptimeValue valueToRound = inputs.get(0);
                PValueSource roundToPVal = signatureStrategy.getScaleOperand(inputs);
                int precision, scale;
                if ((roundToPVal == null) || roundToPVal.isNull()) {
                    precision = 17;
                    int incomingScale = valueToRound.instance().attribute(MBigDecimal.Attrs.SCALE);
                    if (incomingScale > 1)
                        precision += (incomingScale - 1);
                    scale = incomingScale;
                } else {
                    scale = roundToPVal.getInt32();

                    TInstance incomingInstance = valueToRound.instance();
                    int incomingPrecision = incomingInstance.attribute(MBigDecimal.Attrs.PRECISION);
                    int incomingScale = incomingInstance.attribute(MBigDecimal.Attrs.SCALE);

                    precision = incomingPrecision;
                    if (incomingScale > 1)
                        precision -= (incomingScale - 1);
                    precision += scale;

                }
                return MNumeric.DECIMAL.instance(precision, scale, anyContaminatingNulls(inputs));
            }
        });
    }

    @Override
    protected void buildInputSets(TInputSetBuilder builder) {
        signatureStrategy.buildInputSets(MNumeric.DECIMAL, builder);
    }

    @Override
    public String displayName() {
        return roundingStrategy.name();
    }

    protected MRoundTruncateDecimal(RoundingOverloadSignature signatureStrategy,
                                    RoundingStrategy roundingStrategy)
    {
        this.signatureStrategy = signatureStrategy;
        this.roundingStrategy = roundingStrategy;
    }

    private static Collection<TScalar> createAll() {
        List<TScalar> results = new ArrayList<>();
        for (RoundingOverloadSignature signature : RoundingOverloadSignature.values()) {
            for (RoundingStrategy rounding : RoundingStrategy.values()) {
                results.add(new MRoundTruncateDecimal(signature, rounding));
            }
        }
        return Collections.unmodifiableCollection(results);
    }

    private final RoundingOverloadSignature signatureStrategy;
    private final RoundingStrategy roundingStrategy;
}