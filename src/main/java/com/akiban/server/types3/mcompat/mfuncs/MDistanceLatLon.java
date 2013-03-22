
package com.akiban.server.types3.mcompat.mfuncs;

import com.akiban.server.error.InvalidParameterValueException;
import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TScalar;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.common.BigDecimalWrapper;
import com.akiban.server.types3.mcompat.mtypes.MApproximateNumber;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimal;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;

public class MDistanceLatLon extends TScalarBase
{
    public static final TScalar INSTANCE = new MDistanceLatLon();

    private MDistanceLatLon(){}
    
    static final double MAX_LAT = 90;
    static final double MIN_LAT = -90;
    static final double MAX_LON = 180;
    static final double MIN_LON = -180;
    
    static final double MAX_LON_DIS = MAX_LON * 2;
    
    @Override
    protected void buildInputSets(TInputSetBuilder builder)
    {
        builder.covers(MNumeric.DECIMAL, 0, 1, 2, 3);
    }

    @Override
    protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
    {
        double y1 = doubleInRange(MBigDecimal.getWrapper(inputs.get(0), context.inputTInstanceAt(0)), MIN_LAT, MAX_LAT);
        double x1 = doubleInRange(MBigDecimal.getWrapper(inputs.get(1), context.inputTInstanceAt(1)), MIN_LON, MAX_LON);
        double y2 = doubleInRange(MBigDecimal.getWrapper(inputs.get(2), context.inputTInstanceAt(2)), MIN_LAT, MAX_LAT);
        double x2 = doubleInRange(MBigDecimal.getWrapper(inputs.get(3), context.inputTInstanceAt(3)), MIN_LON, MAX_LON);
        
        double dx = Math.abs(x1 - x2);
        // we want the shorter distance of the two
        if (Double.compare(dx, MAX_LON) > 0)
            dx = MAX_LON_DIS - dx;
        
        double dy = y1 - y2;
        
        output.putDouble(Math.sqrt(dx * dx + dy * dy));
    }

    private static double doubleInRange(BigDecimalWrapper val, double min, double max)
    {
        double dVar = val.asBigDecimal().doubleValue();
        
        if (Double.compare(dVar, min) >= 0 && Double.compare(dVar, max) <= 0)
            return dVar;
        else
            throw new InvalidParameterValueException(String.format("Value out of range[%f, %f]: %f ", min, max, dVar));
    }
    @Override
    public String displayName()
    {
        return "DISTANCE_LAT_LON";
    }

    @Override
    public TOverloadResult resultType()
    {
        return TOverloadResult.fixed(MApproximateNumber.DOUBLE);
    }
}
