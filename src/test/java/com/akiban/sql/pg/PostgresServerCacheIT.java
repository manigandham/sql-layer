
package com.akiban.sql.pg;

import com.akiban.server.api.dml.scan.NewRow;

import org.junit.Before;
import org.junit.Test;
import static junit.framework.Assert.*;

import java.sql.ResultSet;
import java.sql.Statement;

import java.util.Collections;
import java.util.Map;

public class PostgresServerCacheIT extends PostgresServerFilesITBase
{
    public static final String QUERY = "SELECT id FROM t1 WHERE id = %d";
    public static final int NROWS = 100;
    public static final String CAPACITY = "10";

    private int hitsBase;
    private int missesBase;
    
    @Override
    protected Map<String, String> startupConfigProperties() {
        return Collections.singletonMap("akserver.postgres.statementCacheCapacity", CAPACITY);
    }

    @Before
    public void createData() throws Exception {
        int tid = createTable(SCHEMA_NAME, "t1", "id int not null primary key");
        NewRow[] rows = new NewRow[NROWS];
        for (int i = 0; i < NROWS; i++) {
            rows[i] = createNewRow(tid, i);
        }
        writeRows(rows);
        hitsBase = server().getStatementCacheHits();
        missesBase = server().getStatementCacheMisses();
    }

    @Test
    public void testRepeated() throws Exception {
        Statement stmt = getConnection().createStatement();
        for (int i = 0; i < 1000; i++) {
            query(stmt, i / NROWS);
        }
        stmt.close();
        assertEquals("Cache hits matches", 990, server().getStatementCacheHits() - hitsBase);
        assertEquals("Cache misses matches", 10, server().getStatementCacheMisses() - missesBase);
    }

    @Test
    public void testSequential() throws Exception {
        Statement stmt = getConnection().createStatement();
        for (int i = 0; i < 1000; i++) {
            query(stmt, i % NROWS);
        }
        stmt.close();
        assertEquals("Cache hits matches", 0, server().getStatementCacheHits() - hitsBase);
        assertEquals("Cache misses matches", 1000, server().getStatementCacheMisses() - missesBase);
    }

    protected void query(Statement stmt, int n) throws Exception {
        ResultSet rs = stmt.executeQuery(String.format(QUERY, n));
        if (rs.next()) {
            assertEquals("Query result matches", n, rs.getInt(1));
        }
        else {
            fail("No query results");
        }
    }

}
