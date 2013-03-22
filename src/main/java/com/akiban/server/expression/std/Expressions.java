
package com.akiban.server.expression.std;

import com.akiban.ais.model.Column;
import com.akiban.qp.expression.IndexBound;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.row.RowBase;
import com.akiban.qp.rowtype.IndexRowType;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.api.dml.ColumnSelector;
import com.akiban.server.collation.AkCollator;
import com.akiban.server.expression.Expression;
import com.akiban.server.types.AkType;
import com.akiban.server.types.FromObjectValueSource;
import com.akiban.server.types.ValueSource;

public class Expressions
{
    public static Expression field(Column column, int position)
    {
        return new ColumnExpression(column, position);
    }

    public static Expression compare(Expression left, Comparison comparison, Expression right)
    {
        return new CompareExpression(left, comparison, right);
    }

    public static Expression collate(Expression left, Comparison comparison, Expression right, AkCollator collator)
    {
        return new CompareExpression(left, comparison, right, collator);
    }

    public static Expression field(RowType rowType, int position)
    {
        return new FieldExpression(rowType, position);
    }

    public static IndexBound indexBound(RowBase row, ColumnSelector columnSelector)
    {
        return new IndexBound(row, columnSelector);
    }

    public static IndexKeyRange indexKeyRange(IndexRowType indexRowType, IndexBound lo, boolean loInclusive, IndexBound hi, boolean hiInclusive)
    {
        return IndexKeyRange.bounded(indexRowType, lo, loInclusive, hi, hiInclusive);
    }

    public static Expression literal(Object value)
    {
        return new LiteralExpression(new FromObjectValueSource().setReflectively(value));
    }

    public static Expression literal(Object value, AkType type)
    {
        return new LiteralExpression(new FromObjectValueSource().setExplicitly(value, type));
    }

    public static Expression variable(AkType type, int position)
    {
        return new VariableExpression(type, position);
    }

    public static Expression valueSource(ValueSource valueSource)
    {
        return new ValueSourceExpression(valueSource);
    }

    public static Expression boundField(RowType rowType, int rowPosition, int fieldPosition)
    {
        return new BoundFieldExpression(rowPosition, new FieldExpression(rowType, fieldPosition));
    }
}
