
package com.akiban.server.types3.common.funcs;

import com.akiban.server.AkServer;
import com.akiban.server.types3.LazyList;
import com.akiban.server.types3.TClass;
import com.akiban.server.types3.TExecutionContext;
import com.akiban.server.types3.TInstance;
import com.akiban.server.types3.TInstanceGenerator;
import com.akiban.server.types3.TScalar;
import com.akiban.server.types3.TOverloadResult;
import com.akiban.server.types3.mcompat.mtypes.MApproximateNumber;
import com.akiban.server.types3.mcompat.mtypes.MDatetimes;
import com.akiban.server.types3.mcompat.mtypes.MNumeric;
import com.akiban.server.types3.mcompat.mtypes.MString;
import com.akiban.server.types3.pvalue.PValueSource;
import com.akiban.server.types3.pvalue.PValueTarget;
import com.akiban.server.types3.texpressions.TInputSetBuilder;
import com.akiban.server.types3.texpressions.TScalarBase;
import com.akiban.server.types3.texpressions.std.NoArgExpression;
import java.util.Date;

public class NoArgFuncs
{
    static final int USER_NAME_LENGTH = 77;
    static final int SCHEMA_NAME_LENGTH = 128;

    public static final TScalar SHORT_SERVER_VERSION = new NoArgExpression("version", true)
    {
        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putString(AkServer.SHORT_VERSION_STRING, null);
        }

        @Override
        protected TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { AkServer.SHORT_VERSION_STRING.length() };
        }
    };

    public static final TScalar SERVER_FULL_VERSION = new NoArgExpression("version_full", true)
    {

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putString(AkServer.VERSION_STRING, null);
        }

        @Override
        protected TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { AkServer.VERSION_STRING.length() };
        }
    };
    
    public static final TScalar PI = new TScalarBase()
    {
        @Override
        protected void buildInputSets(TInputSetBuilder builder)
        {
            // does nothing. doesn't take any arg
        }

        
        @Override
        protected void doEvaluate(TExecutionContext context, LazyList<? extends PValueSource> inputs, PValueTarget output)
        {
            output.putDouble(Math.PI);
        }

        @Override
        public String displayName()
        {
            return "PI";
        }

        @Override
        public TOverloadResult resultType()
        {
            return TOverloadResult.fixed(MApproximateNumber.DOUBLE);
        }
    };
 
    public static final TScalar CUR_DATE = new NoArgExpression("CURRENT_DATE", true)
    {
        @Override
        public String[] registeredNames()
        {
            return new String[] {"curdate", "current_date"};
        }
        
        @Override
        public TClass resultTClass()
        {
            return MDatetimes.DATE;
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt32(MDatetimes.encodeDate(context.getCurrentDate(), context.getCurrentTimezone()));
        }
    };

    public static final TScalar CUR_TIME = new NoArgExpression("CURRENT_TIME", true)
    {
        @Override
        public String[] registeredNames()
        {
            return new String[] {"curtime", "current_time"};
        }
        
        @Override
        public TClass resultTClass()
        {
            return MDatetimes.TIME;
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt32(MDatetimes.encodeTime(context.getCurrentDate(), context.getCurrentTimezone()));
        }   
    };

    public static final TScalar CUR_TIMESTAMP = new NoArgExpression("CURRENT_TIMESTAMP", true)
    {
        @Override
        public String[] registeredNames()
        {
            return new String[] {"current_timestamp", "now", "localtime", "localtimestamp"};
        }

        @Override
        public TClass resultTClass()
        {
            return MDatetimes.DATETIME;
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt64(MDatetimes.encodeDatetime(context.getCurrentDate(), context.getCurrentTimezone()));
        }
    };
    
    public static final TScalar UNIX_TIMESTAMP = new NoArgExpression("UNIX_TIMESTAMP", true)
    {
        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt32((int)MDatetimes.encodeTimetamp(context.getCurrentDate(), context));
        }

        @Override
        protected TClass resultTClass()
        {
            return MDatetimes.TIMESTAMP;
        }
        
    };
    
    public static final TScalar SYSDATE = new NoArgExpression("SYSDATE", false)
    {
        @Override
        public TClass resultTClass()
        {
            return MDatetimes.DATETIME;
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt64(MDatetimes.encodeDatetime(new Date().getTime(), context.getCurrentTimezone()));
        }
    };

    public static final TScalar CURRENT_USER = new NoArgExpression("CURRENT_USER", true) 
    {
        @Override
        public TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { USER_NAME_LENGTH };
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target) {
            target.putString(context.getCurrentUser(), null);
        }
    };

    public static final TScalar SESSION_USER = new NoArgExpression("SESSION_USER", true)
    {
        @Override
        public TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { USER_NAME_LENGTH };
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putString(context.getSessionUser(), null);
        }
    };
    
    public static final TScalar SYSTEM_USER = new NoArgExpression("SYSTEM_USER", true)
    {
        @Override
        public TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { USER_NAME_LENGTH };
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putString(context.getSystemUser(), null);
        }
    };
    
    public static final TScalar CURRENT_SCHEMA = new NoArgExpression("CURRENT_SCHEMA", true)
    {
        @Override
        public TClass resultTClass() {
            return MString.VARCHAR;
        }

        @Override
        protected int[] resultAttrs() {
            return new int[] { SCHEMA_NAME_LENGTH };
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putString(context.getCurrentSchema(), null);
        }
    };
    
    public static final TScalar CURRENT_SESSION_ID = new NoArgExpression("CURRENT_SESSION_ID", true)
    {
        @Override
        public TClass resultTClass() {
            return MNumeric.INT;
        }

        @Override
        public void evaluate(TExecutionContext context, PValueTarget target)
        {
            target.putInt32(context.getSessionId());
        }
    };
}
