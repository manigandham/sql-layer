
package com.akiban.sql.embedded;

import com.akiban.sql.embedded.JDBCParameterMetaData.ParameterType;

import com.akiban.ais.model.Parameter;
import com.akiban.ais.model.TableName;
import com.akiban.server.error.UnsupportedSQLException;
import com.akiban.sql.parser.CallStatementNode;
import com.akiban.sql.parser.ParameterNode;
import com.akiban.sql.parser.StaticMethodCallNode;
import com.akiban.sql.server.ServerCallInvocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

abstract class ExecutableCallStatement extends ExecutableStatement
{
    protected ServerCallInvocation invocation;
    protected JDBCParameterMetaData parameterMetaData;

    protected ExecutableCallStatement(ServerCallInvocation invocation,
                                      JDBCParameterMetaData parameterMetaData) {
        this.invocation = invocation;
        this.parameterMetaData = parameterMetaData;
    }

    public static ExecutableStatement executableStatement(CallStatementNode call,
                                                          List<ParameterNode> sqlParams,
                                                          EmbeddedQueryContext context) {
        StaticMethodCallNode methodCall = (StaticMethodCallNode)call.methodCall().getJavaValueNode();
        ServerCallInvocation invocation =
            ServerCallInvocation.of(context.getServer(), methodCall);
        return executableStatement(invocation, call, sqlParams, context);
    }

    public static ExecutableStatement executableStatement(TableName routineName,
                                                          EmbeddedQueryContext context) {
        ServerCallInvocation invocation =
            ServerCallInvocation.of(context.getServer(), routineName);
        return executableStatement(invocation, null, null, context);
    }

    protected static ExecutableStatement executableStatement(ServerCallInvocation invocation,
                                                             CallStatementNode call,
                                                             List<ParameterNode> sqlParams,
                                                             EmbeddedQueryContext context) {
        int nparams = (sqlParams == null) ? invocation.size() : sqlParams.size();
        JDBCParameterMetaData parameterMetaData = parameterMetaData(invocation, nparams);
        switch (invocation.getCallingConvention()) {
        case LOADABLE_PLAN:
            return ExecutableLoadableOperator.executableStatement(invocation, parameterMetaData, call, context);
        case JAVA:
            return ExecutableJavaMethod.executableStatement(invocation, parameterMetaData, context);
        case SCRIPT_FUNCTION_JAVA:
        case SCRIPT_FUNCTION_JSON:
            return ExecutableScriptFunctionJavaRoutine.executableStatement(invocation, parameterMetaData, context);
        case SCRIPT_BINDINGS:
        case SCRIPT_BINDINGS_JSON:
            return ExecutableScriptBindingsRoutine.executableStatement(invocation, parameterMetaData, context);
        default:
            throw new UnsupportedSQLException("Unknown routine", call);
        }
    }

    protected static JDBCParameterMetaData parameterMetaData(ServerCallInvocation invocation,
                                                             int nparams) {
        ParameterType[] ptypes = new ParameterType[nparams];
        for (int i = 0; i < nparams; i++) {
            int usage = invocation.parameterUsage(i);
            if (usage < 0) continue;
            ptypes[i] = new ParameterType(invocation.getRoutineParameter(usage));
        }
        return new JDBCParameterMetaData(Arrays.asList(ptypes));
    }

    public ServerCallInvocation getInvocation() {
        return invocation;
    }

    @Override
    public JDBCParameterMetaData getParameterMetaData() {
        return parameterMetaData;
    }
    
}
