
package com.akiban.server.test.it.dxl;

import com.akiban.ais.AISCloner;
import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.ais.model.Index;
import com.akiban.ais.model.IndexColumn;
import com.akiban.ais.model.TableIndex;
import com.akiban.ais.model.UserTable;
import com.akiban.server.api.dml.scan.CursorId;
import com.akiban.server.api.dml.scan.NewRow;
import com.akiban.server.error.InvalidOperationException;
import com.akiban.server.error.TableDefinitionChangedException;
import com.akiban.server.test.it.ITBase;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public final class DDLInvalidatesScansIT extends ITBase {
    private final static String SCHEMA = "mycoolschema";
    private static final String CUSTOMERS = "customers";
    private static final String ORDERS = "orders";
    private static final String SNOWMEN = "snowmen";

    @Test(expected=TableDefinitionChangedException.class)
    public void dropScannedTable() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, ORDERS, "date");
            ddl().dropTable(session(), tableName(SCHEMA, ORDERS));
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        scanExpectingException(cursor);
    }

    @Test
    public void addGrandChildTable() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "PRIMARY");
            createItems();
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        ListRowOutput output = new ListRowOutput();
        dml().scanSome(session(), cursor, output);
        dml().closeCursor(session(), cursor);
        assertEquals("rows scanned", expectedCustomers(), output.getRows());
    }

    @Test
    public void addChildTable() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "PRIMARY");
            createAddresses();
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        ListRowOutput output = new ListRowOutput();
        dml().scanSome(session(), cursor, output);
        dml().closeCursor(session(), cursor);
        assertEquals("rows scanned", expectedCustomers(), output.getRows());
    }

    @Test
    public void dropDifferentTableInGroup() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "PRIMARY");
            ddl().dropTable(session(), tableName(SCHEMA, ORDERS));
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        ListRowOutput output = new ListRowOutput();
        dml().scanSome(session(), cursor, output);
        dml().closeCursor(session(), cursor);
        assertEquals("rows scanned", expectedCustomers(), output.getRows());
    }

    @Test(expected=TableDefinitionChangedException.class)
    public void dropScannedIndex() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, ORDERS, "date");
            ddl().dropTableIndexes(session(), tableName(SCHEMA, ORDERS), Collections.singleton("date"));
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        scanExpectingException(cursor);
    }

    @Test(expected=TableDefinitionChangedException.class)
    public void dropScannedIndexButAnotherReplaces() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "name");
            indexId(SCHEMA, CUSTOMERS, "name");
            ddl().dropTableIndexes(session(), tableName(SCHEMA, CUSTOMERS), Collections.singleton("name"));
            indexId(SCHEMA, CUSTOMERS, "position");
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        scanExpectingException(cursor);
    }

    @Test(expected=TableDefinitionChangedException.class)
    public void dropDifferentIndex() throws InvalidOperationException {
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "name");
            ddl().dropTableIndexes(session(), tableName(SCHEMA, CUSTOMERS), Collections.singleton("position"));
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        scanExpectingException(cursor);
    }

    @Test(expected=TableDefinitionChangedException.class)
    public void addNewIndex() throws InvalidOperationException {
        
        final CursorId cursor;
        try {
            cursor = openFullScan(SCHEMA, CUSTOMERS, "name");
            ddl().createIndexes(session(), Collections.singleton(createIndex()));
        } catch (InvalidOperationException e) {
            throw new TestException(e);
        }

        scanExpectingException(cursor);
    }

    private void scanExpectingException(CursorId cursorId) throws InvalidOperationException {
        ListRowOutput output = new ListRowOutput();
        try {
            dml().scanSome(session(), cursorId, output);
            fail("Expected exception, but scanned: " + output.getRows().toString());
        } finally {
            dml().closeCursor(session(), cursorId);
        }
    }

    private Index createIndex() throws InvalidOperationException {
        AkibanInformationSchema aisCopy = AISCloner.clone(ddl().getAIS(session()));
        UserTable customers = aisCopy.getUserTable(SCHEMA, CUSTOMERS);
        Index addIndex = TableIndex.create(
                aisCopy,
                customers,
                "played_for_Bs",
                2,
                false,
                "KEY"
        );
        IndexColumn.create(
                addIndex,
                customers.getColumn("has_played_for_bruins"),
                0,
                true,
                null
        );
        return addIndex;
    }

    @Before
    public void setUpTables() throws InvalidOperationException{
        createTable(
                SCHEMA, CUSTOMERS,
                "id int not null primary key",
                "name varchar(32)",
                "position char(1)",
                "has_played_for_bruins char(1)"
        );
        createIndex(SCHEMA, CUSTOMERS, "name", "name");
        createIndex(SCHEMA, CUSTOMERS, "position", "position");
        int orders = createTable(
                SCHEMA, ORDERS,
                "id int not null primary key",
                "cid int",
                "date varchar(32)",
                "GROUPING FOREIGN KEY (cid) REFERENCES " + CUSTOMERS + " (id)"
        );
        createIndex(SCHEMA, ORDERS, "date", "date");
        int snowmen = createTable(
                SCHEMA, SNOWMEN,
                "id int not null primary key",
                "melt_at int"
        );

        List<NewRow> customerRows = expectedCustomers();
        writeRows(customerRows.toArray(new NewRow[customerRows.size()]));

        writeRows(
                createNewRow(orders, 31, 27, "today"),
                createNewRow(orders, 32, 27, "yesterday"),

                createNewRow(orders, 33, 29, "tomorrow"),
                createNewRow(orders, 34, 29, "never"),

                createNewRow(snowmen, 1, 32),
                createNewRow(snowmen, 2, -40) // C or F?!
        );
    }

    private List<NewRow> expectedCustomers() {
        int customers = tableId(SCHEMA, CUSTOMERS);
        return Arrays.asList(
                createNewRow(customers, 27L, "Knoepfli", "C", "N"), // I forget if he actually played center...
                createNewRow(customers, 29L, "Bitz", "W", "Y")

        );
    }

    private int createItems() throws InvalidOperationException {
        int tid = createTable(
                SCHEMA, "items",
                "id int not null primary key",
                "oid int",
                "sku int",
                "GROUPING FOREIGN KEY (oid) REFERENCES " + ORDERS + " (id)"
        );
        createIndex(SCHEMA, "items", "sku", "sku");
        return tid;
    }

    private int createAddresses() throws InvalidOperationException {
        return createTable(
                SCHEMA, "addresses",
                "id int not null primary key",
                "cid int",
                "street varchar(32)",
                "GROUPING FOREIGN KEY (cid) REFERENCES " + CUSTOMERS + " (id)"
        );
    }
}
