
package com.akiban.server.types3.mcompat.mcasts;

import com.akiban.server.types3.TCast;
import com.akiban.server.types3.TCastBase;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.aksql.aktypes.AkBool;
import com.akiban.server.types3.common.BigDecimalWrapper;
import com.akiban.server.types3.mcompat.mtypes.MApproximateNumber;
import com.akiban.server.types3.mcompat.mtypes.MBigDecimal;
import com.akiban.server.types3.mcompat.mtypes.MDatetimes;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;

@SuppressWarnings("unused") // Used by reflected
public class Cast_From_Boolean {

    public static final TCast BOOL_TO_INTEGER = new TCastBase(AkBool.INSTANCE, MNumeric.INT) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putInt32(source.getBoolean() ? 1 : 0);
        }
    };

    // MApproximateNumber
    public static final TCast FLOAT_TO_BOOLEAN = new TCastBase(MApproximateNumber.FLOAT, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getFloat() != 0.0);
        }
    };
    public static final TCast FLOAT_UNSIGNED_TO_BOOLEAN = new TCastBase(MApproximateNumber.FLOAT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getFloat() != 0.0);
        }
    };
    public static final TCast DOUBLE_TO_BOOLEAN = new TCastBase(MApproximateNumber.DOUBLE, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getDouble() != 0.0);
        }
    };
    public static final TCast DOUBLE_UNSIGNED_TO_BOOLEAN = new TCastBase(MApproximateNumber.DOUBLE_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getDouble() != 0.0);
        }
    };


    // MBinary
    // TODO

    // MDatetimesfinal
    public static final TCast DATE_TO_BOOLEAN = new TCastBase(MDatetimes.DATE, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };
    public static final TCast DATETIME_TO_BOOLEAN = new TCastBase(MDatetimes.DATETIME, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt64() != 0);
        }
    };
    public static final TCast TIME_TO_BOOLEAN = new TCastBase(MDatetimes.TIME, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };
    public static final TCast YEAR_TO_BOOLEAN = new TCastBase(MDatetimes.YEAR, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt16() != 0);
        }
    };
    public static final TCast TIMESTAMP_TO_BOOLEAN = new TCastBase(MDatetimes.TIMESTAMP, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };

    // MNumeric
    public static final TCast TINYINT_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.TINYINT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt16() != 0);
        }
    };
    public static final TCast SMALLINT_TO_BOOLEAN = new TCastBase(MNumeric.SMALLINT, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt16() != 0);
        }
    };
    public static final TCast SMALLINT_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.SMALLINT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };
    public static final TCast MEDIUMINT_TO_BOOLEAN = new TCastBase(MNumeric.MEDIUMINT, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };
    public static final TCast MEDIUMINT_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.MEDIUMINT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt64() != 0);
        }
    };
    public static final TCast INT_TO_BOOLEAN = new TCastBase(MNumeric.INT, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt32() != 0);
        }
    };
    public static final TCast INT_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.INT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt64() != 0);
        }
    };
    public static final TCast BIGINT_TO_BOOLEAN = new TCastBase(MNumeric.BIGINT, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt64() != 0);
        }
    };
    public static final TCast BIGINT_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.BIGINT_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            target.putBool(source.getInt64() != 0);
        }
    };
    public static final TCast DECIMAL_TO_BOOLEAN = new TCastBase(MNumeric.DECIMAL, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            BigDecimalWrapper decimal = MBigDecimal.getWrapper(source, context.inputTInstanceAt(0));
            target.putBool(!decimal.isZero());
        }
    };
    public static final TCast DECIMAL_UNSIGNED_TO_BOOLEAN = new TCastBase(MNumeric.DECIMAL_UNSIGNED, AkBool.INSTANCE) {
        @Override
        protected void doEvaluate(TExecutionContext context, PValueSource source, PValueTarget target) {
            BigDecimalWrapper decimal = MBigDecimal.getWrapper(source, context.inputTInstanceAt(0));
            target.putBool(!decimal.isZero());
        }
    };

    // MString
    // Handled by string->boolean (i.e. TParsers.BOOLEAN)
}