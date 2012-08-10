/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.test.it.qp;

import java.util.ArrayList;
import java.lang.Long;
import com.akiban.qp.expression.IndexBound;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.operator.API;
import com.akiban.qp.operator.Cursor;
import com.akiban.qp.operator.Operator;
import com.akiban.qp.row.OverlayingRow;
import com.akiban.qp.row.Row;
import com.akiban.qp.rowtype.IndexRowType;
import com.akiban.qp.rowtype.RowType;
import com.akiban.qp.rowtype.Schema;
import com.akiban.server.api.dml.SetColumnSelector;
import com.akiban.server.api.dml.scan.NewRow;
import com.akiban.server.expression.std.FieldExpression;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.akiban.qp.operator.API.cursor;
import static com.akiban.qp.operator.API.indexScan_Default;
import static org.junit.Assert.assertEquals;

public class IndexScanJumpBoundedIT extends OperatorITBase
{
    @Before
    public void before()
    {
        t = createTable(
            "schema", "t",
            "id int not null primary key",
            "a int",
            "b int",
            "c int");
        createIndex("schema", "t", "idx", "a", "b", "c", "id");
        schema = new Schema(rowDefCache().ais());
        tRowType = schema.userTableRowType(userTable(t));
        idxRowType = indexType(t, "a", "b", "c", "id");
        db = new NewRow[] {
            createNewRow(t, 1010L, 1L, 11L, 111L),
            createNewRow(t, 1011L, 1L, 11L, 112L),
            createNewRow(t, 1012L, 1L, 12L, 121L),
            createNewRow(t, 1013L, 1L, 12L, 122L),
            createNewRow(t, 1014L, 1L, 13L, 131L),
            createNewRow(t, 1015L, 1L, 13L, 132L),
        };
        adapter = persistitAdapter(schema);
        queryContext = queryContext(adapter);
        use(db);
        for (NewRow row : db) {
            indexRowMap.put((Long) row.get(0),
                            new TestRow(tRowType,
                                        new Object[] {row.get(1),     // a
                                                      row.get(2),     // b
                                                      row.get(3),     // c
                                                      row.get(0)}));  // id
        }
    }

    @Test
    public void testAAAA()
    {
        API.Ordering ordering = ordering(A, ASC, B, ASC, C, ASC, ID, ASC);
        long[] idOrdering = longs(1010, 1011, 1012, 1013, 1014, 1015);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testAAAD()
    {
        API.Ordering ordering = ordering(A, ASC, B, ASC, C, ASC, ID, DESC);
        long[] idOrdering = longs(1010, 1011, 1012, 1013, 1014, 1015);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }
    }

    @Test
    public void testAADA()
    {
        API.Ordering ordering = ordering(A, ASC, B, ASC, C, DESC, ID, ASC);
        long[] idOrdering = longs(1011, 1010, 1013, 1012, 1015, 1014);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testAADD()
    {
        API.Ordering ordering = ordering(A, ASC, B, ASC, C, DESC, ID, DESC);
        long[] idOrdering = longs(1011, 1010, 1013, 1012, 1015, 1014);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }

    }

    @Test
    public void testADAA()
    {
        API.Ordering ordering = ordering(A, ASC, B, DESC, C, ASC, ID, ASC);
        long[] idOrdering = longs(1014, 1015, 1012, 1013, 1010, 1011);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testADAD()
    {
        API.Ordering ordering = ordering(A, ASC, B, DESC, C, ASC, ID, DESC);
        long[] idOrdering = longs(1014, 1015, 1012, 1013, 1010, 1011);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }

    }

    @Test
    public void testADDA()
    {
        API.Ordering ordering = ordering(A, ASC, B, DESC, C, DESC, ID, ASC);
        long[] idOrdering = longs(1015, 1014, 1013, 1012, 1011, 1010);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testADDD()
    {
        API.Ordering ordering = ordering(A, ASC, B, DESC, C, DESC, ID, DESC);
        long[] idOrdering = longs(1015, 1014, 1013, 1012, 1011, 1010);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }
    }

    @Test
    public void testDAAA()
    {
        API.Ordering ordering = ordering(A, DESC, B, ASC, C, ASC, ID, ASC);
        long[] idOrdering = longs(1010, 1011, 1012, 1013, 1014, 1015);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testDAAD()
    {
        API.Ordering ordering = ordering(A, DESC, B, ASC, C, ASC, ID, DESC);
        long[] idOrdering = longs(1010, 1011, 1012, 1013, 1014, 1015);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }
    }

    @Test
    public void testDADA()
    {
        API.Ordering ordering = ordering(A, DESC, B, ASC, C, DESC, ID, ASC);
        long[] idOrdering = longs(1011, 1010, 1013, 1012, 1015, 1014);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testDADD()
    {
        API.Ordering ordering = ordering(A, DESC, B, ASC, C, DESC, ID, DESC);
        long[] idOrdering = longs(1011, 1010, 1013, 1012, 1015, 1014);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }

    }

    @Test
    public void testDDAA()
    {
        API.Ordering ordering = ordering(A, DESC, B, DESC, C, ASC, ID, ASC);
        long[] idOrdering = longs(1014, 1015, 1012, 1013, 1010, 1011);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testDDAD()
    {
        API.Ordering ordering = ordering(A, DESC, B, DESC, C, ASC, ID, DESC);
        long[] idOrdering = longs(1014, 1015, 1012, 1013, 1010, 1011);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }

    }

    @Test
    public void testDDDA()
    {
        API.Ordering ordering = ordering(A, DESC, B, DESC, C, DESC, ID, ASC);
        long[] idOrdering = longs(1015, 1014, 1013, 1012, 1011, 1010);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), -1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), -1);
            cursor.close();
        }
    }

    @Test
    public void testDDDD()
    {
        API.Ordering ordering = ordering(A, DESC, B, DESC, C, DESC, ID, DESC);
        long[] idOrdering = longs(1015, 1014, 1013, 1012, 1011, 1010);
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, idOrdering, 0);
            testJump(cursor, idOrdering, 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, true, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, last4(idOrdering), 0);
            testJump(cursor, last4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, true), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, first4(idOrdering), 0);
            testJump(cursor, first4(idOrdering), 1);
            cursor.close();
        }
        {
            Operator plan = indexScan_Default(idxRowType, bounded(1, 11, false, 13, false), ordering);
            Cursor cursor = cursor(plan, queryContext);
            cursor.open();
            testJump(cursor, middle2(idOrdering), 0);
            testJump(cursor, middle2(idOrdering), 1);
            cursor.close();
        }
    }

    @Test
    public void testDDDDLegalRange()
    {
        testRange(0,
                  13, true,
                  15, true,
                  new long [][]     // range is: [1015 - 1013]
                  {
                     {1015, 1014, 1013}, 
                     {1014, 1013},
                     {1013},
                     {},
                     {},
                     {}
                  });
    }

    @Test
    public void testDDDDOutOfRangeUpperBound()
    {
        testRange(0,
                  13, true,
                  21, true,
                  new long [][]     // specified range: [21-13],
                  {                 // but it should still only see  [15 - 13]
                     {1015, 1014, 1013}, 
                     {1014, 1013},
                     {1013},
                     {},
                     {},
                     {}
                  });
    }

    @Test
    public void testDDDDOutOfRangeLowerBound()
    {
        testRange(0,
                  3, true,
                  14, true,
                  new long [][]     // specified range: [14-3],
                  {                 // but it should still only see  [15 - 10]
                     {1015, 1014, 1013, 1012, 1011, 1010}, 
                     {1014, 1013, 1012, 1011, 1010},
                     {1013, 1012, 1011, 1010},
                     {1012, 1011, 1010},
                     {1011, 1010},
                     {1010}
                  });
    }

    @Test
    public void testDDDDOutOfRange() // both upper and lower bound
    {
        testRange(0,
                  8, true,
                  20, true,
                  new long [][]     // specified range: [20 - 8],
                  {                 // but it should still only see [15 - 10]
                     {1015, 1014, 1013, 1012, 1011, 1010}, 
                     {1014, 1013, 1012, 1011, 1010},
                     {1013, 1012, 1011, 1010},
                     {1012, 1011, 1010},
                     {1011, 1010},
                     {1010}
                  });
    }

    private void testRange(int nudge, int lo, boolean loInclusive, int hi, boolean hiInclusive, long expectedArs[][])
    {
        API.Ordering ordering = ordering(A, DESC, B, DESC, C, DESC, ID, DESC);
        long idOrdering[] = longs(1015, 1014, 1013, 1012, 1011, 1010);
        Operator plan = indexScan_Default(idxRowType, bounded(1, lo, loInclusive, hi, hiInclusive), ordering);
        Cursor cursor = cursor(plan, queryContext);
        cursor.open();
        testJump(cursor,
                 idOrdering,
                 nudge,
                 expectedArs);
        cursor.close();
    }

    private void testJump(Cursor cursor, long idOrdering[], int nudge, long expectedsAr[][])
    {
        List<List<Long>> expecteds = new ArrayList<List<Long>>(expectedsAr.length);
        
        for (long expectedAr[] : expectedsAr)
        {
            List<Long> expected = new ArrayList();
            for (long val : expectedAr)
                expected.add(val);
            expecteds.add(expected);
        }
       
        doTestJump(cursor, idOrdering, nudge, expecteds);
    }

    private void testJump(Cursor cursor, long idOrdering[], int nudge)
    {
        List<List<Long>> expecteds = new ArrayList<List<Long>>();
        
        
        for (int idIndex = 0; idIndex < idOrdering.length; ++idIndex)
        {
            List<Long> expected = new ArrayList<Long>();
            for (int i = idIndex; i < idOrdering.length; i++)
                expected.add(idOrdering[i]);
            expecteds.add(expected);
        }
        
        doTestJump(cursor, idOrdering, nudge, expecteds);
    }

    private void doTestJump(Cursor cursor, long idOrdering[], int nudge, List<List<Long>> expecteds)
    {
        for (int start = 0; start < idOrdering.length; ++start)
        {
            TestRow target = indexRow(idOrdering[start]);
            OverlayingRow nudgedTarget = new OverlayingRow(target);
            nudgedTarget.overlay(3, target.eval(3).getLong() + nudge);
            cursor.jump(nudgedTarget, INDEX_ROW_SELECTOR);
            Row row;
            List<Long> actualIds = new ArrayList<Long>();
            while ((row = cursor.next()) != null)
                actualIds.add(row.eval(3).getInt());

            assertEquals(expecteds.get(start), actualIds);
        }
    }

    private TestRow indexRow(long id)
    {
        return indexRowMap.get(id);
    }

    private long[] longs(long... longs)
    {
        return longs;
    }

    private IndexKeyRange bounded(long a, long bLo, boolean loInclusive, long bHi, boolean hiInclusive)
    {
        IndexBound lo = new IndexBound(new TestRow(tRowType, new Object[] {a, bLo}), new SetColumnSelector(0, 1));
        IndexBound hi = new IndexBound(new TestRow(tRowType, new Object[] {a, bHi}), new SetColumnSelector(0, 1));
        return IndexKeyRange.bounded(idxRowType, lo, loInclusive, hi, hiInclusive);
    }

    private API.Ordering ordering(Object... ord) // alternating column positions and asc/desc
    {
        API.Ordering ordering = API.ordering();
        int i = 0;
        while (i < ord.length) {
            int column = (Integer) ord[i++];
            boolean asc = (Boolean) ord[i++];
            ordering.append(new FieldExpression(idxRowType, column), asc);
        }
        return ordering;
    }

    private long[] first4(long ... x)
    {
        long[] y = new long[4];
        System.arraycopy(x, 0, y, 0, 4);
        return y;
    }

    private long[] last4(long ... x)
    {
        long[] y = new long[4];
        System.arraycopy(x, 2, y, 0, 4);
        return y;
    }

    private long[] middle2(long ... x)
    {
        long[] y = new long[2];
        System.arraycopy(x, 2, y, 0, 2);
        return y;
    }

    // Positions of fields within the index row
    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;
    private static final int ID = 3;
    private static final boolean ASC = true;
    private static final boolean DESC = false;
    private static final SetColumnSelector INDEX_ROW_SELECTOR = new SetColumnSelector(0, 1, 2, 3);

    private int t;
    private RowType tRowType;
    private IndexRowType idxRowType;
    private Map<Long, TestRow> indexRowMap = new HashMap<Long, TestRow>();
}
