
package com.akiban.server.expression.subquery;

import com.akiban.qp.operator.Operator;
import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.explain.*;
import com.akiban.server.expression.Expression;
import com.akiban.server.expression.ExpressionEvaluation;
import com.akiban.server.types.AkType;
import com.akiban.server.types.ValueSource;
import com.akiban.server.types.extract.BooleanExtractor;
import com.akiban.server.types.extract.Extractors;
import com.akiban.server.types.util.BoolValueSource;

public final class AnySubqueryExpression extends SubqueryExpression {

    @Override
    public ExpressionEvaluation evaluation() {
        return new InnerEvaluation(subquery(), expression.evaluation(),
                                   outerRowType(), innerRowType(),
                                   bindingPosition());
    }

    @Override
    public AkType valueType() {
        return AkType.BOOL;
    }

    @Override
    public String toString() {
        return "ANY(" + subquery() + ")";
    }

    @Override
    public CompoundExplainer getExplainer(ExplainContext context) {
        CompoundExplainer explainer = super.getExplainer(context);
        explainer.addAttribute(Label.EXPRESSIONS, expression.getExplainer(context));
        return explainer;
    }

    public AnySubqueryExpression(Operator subquery, Expression expression,
                                 RowType outerRowType, RowType innerRowType, 
                                 int bindingPosition) {
        super(subquery, outerRowType, innerRowType, bindingPosition);
        this.expression = expression;
    }
                                 
    private final Expression expression;

    @Override
    public String name()
    {
        return "ANY";
    }

    private static final class InnerEvaluation extends SubqueryExpressionEvaluation {
        @Override
        public ValueSource doEval() {
            expressionEvaluation.of(queryContext());
            Boolean result = Boolean.FALSE;
            BooleanExtractor extractor = Extractors.getBooleanExtractor();
            while (true) {
                Row row = next();
                if (row == null) break;
                expressionEvaluation.of(row);
                Boolean value = extractor.getBoolean(expressionEvaluation.eval(), null);
                if (value == Boolean.TRUE) {
                    result = value;
                    break;
                }
                else if (value == null) {
                    result = value;
                }
            }
            return BoolValueSource.of(result);
        }

        private InnerEvaluation(Operator subquery,
                                ExpressionEvaluation expressionEvaluation, 
                                RowType outerRowType, RowType innerRowType, 
                                int bindingPosition) {
            super(subquery, outerRowType, innerRowType, bindingPosition);
            this.expressionEvaluation = expressionEvaluation;
        }

        private final ExpressionEvaluation expressionEvaluation;
    }

}
