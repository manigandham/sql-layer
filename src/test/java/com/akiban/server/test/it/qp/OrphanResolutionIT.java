
package com.akiban.server.test.it.qp;

import com.akiban.ais.model.Group;
import com.akiban.qp.exec.UpdatePlannable;
import com.akiban.qp.row.BindableRow;
import com.akiban.qp.row.RowBase;
import com.akiban.qp.rowtype.Schema;
import com.akiban.qp.rowtype.UserTableRowType;
import com.akiban.server.api.dml.scan.NewRow;
import com.akiban.server.test.ExpressionGenerators;
import com.akiban.server.types.AkType;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.akiban.qp.operator.API.*;

public class OrphanResolutionIT extends OperatorITBase
{
    @Override
    protected void setupCreateSchema()
    {
        parent = createTable(
            "schema", "parent",
            "pid int not null",
            "px int",
            "primary key(pid)");
        child = createTable(
            "schema", "child",
            "pid int",
            "cx int",
            "grouping foreign key(pid) references parent(pid)");
    }

    @Override
    protected void setupPostCreateSchema()
    {
        schema = new Schema(ais());
        parentRowType = schema.userTableRowType(userTable(parent));
        childRowType = schema.userTableRowType(userTable(child));
        group = group(parent);
        adapter = persistitAdapter(schema);
        queryContext = queryContext(adapter);
        db = new NewRow[] {
            createNewRow(child, 1L, 100L),
            createNewRow(child, 1L, 101L),
        };
        use(db);
    }

    // Inspired by bug 1020342.

    @Test
    public void test()
    {
        UpdatePlannable insertPlan =
            insert_Default(
                valuesScan_Default(
                    Arrays.asList(parentRow(1, 10)), parentRowType));
        insertPlan.run(queryContext);
        // Execution of insertPlan used to hang before 1020342 was fixed.
        RowBase[] expected = new RowBase[] {
            row(parentRowType, 1L, 10L),
            // Last column of child rows is generated PK value
            row(childRowType, 1L, 100L, 1L),
            row(childRowType, 1L, 101L, 2L),
        };
        compareRows(expected, cursor(groupScan_Default(group), queryContext));
    }

    private BindableRow parentRow(int pid, int px)
    {
        return BindableRow.of(parentRowType, Arrays.asList(ExpressionGenerators.literal(pid, AkType.INT),
                                                           ExpressionGenerators.literal(px, AkType.INT)), null);
    }

    private int parent;
    private int child;
    private UserTableRowType parentRowType;
    private UserTableRowType childRowType;
    private Group group;
}
