/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.test.it.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import com.foundationdb.ais.model.Index;
import com.foundationdb.ais.model.Routine;
import com.foundationdb.ais.model.Schema;
import com.foundationdb.ais.model.Sequence;
import com.foundationdb.ais.model.Table;
import com.foundationdb.ais.model.TableName;
import com.foundationdb.ais.model.aisb2.AISBBasedBuilder;
import com.foundationdb.ais.model.aisb2.NewAISBuilder;
import com.foundationdb.ais.util.DDLGenerator;
import com.foundationdb.qp.expression.IndexKeyRange;
import com.foundationdb.qp.memoryadapter.MemoryAdapter;
import com.foundationdb.qp.memoryadapter.MemoryGroupCursor;
import com.foundationdb.qp.operator.API;
import com.foundationdb.qp.operator.Cursor;
import com.foundationdb.qp.operator.IndexScanSelector;
import com.foundationdb.qp.memoryadapter.MemoryTableFactory;
import com.foundationdb.server.error.DuplicateTableNameException;
import com.foundationdb.server.error.ErrorCode;
import com.foundationdb.server.error.ISTableVersionMismatchException;
import com.foundationdb.server.error.InvalidOperationException;
import com.foundationdb.server.error.JoinToProtectedTableException;
import com.foundationdb.server.error.NoSuchTableException;
import com.foundationdb.server.service.session.Session;
import com.foundationdb.server.store.SchemaManager;
import com.foundationdb.server.store.statistics.IndexStatistics;
import com.foundationdb.server.test.it.ITBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.foundationdb.ais.model.AkibanInformationSchema;
import com.foundationdb.ais.model.UserTable;

public final class SchemaManagerIT extends ITBase {
    final static String SCHEMA = "my_schema";
    final static String VOL2_PREFIX = "foo_schema";
    final static String VOL3_PREFIX = "bar_schema";

    final static String T1_NAME = "t1";
    final static String T1_DDL = "id int NOT NULL, PRIMARY KEY(id)";
    final static String T2_NAME = "t2";
    final static String T2_DDL = "id int NOT NULL, PRIMARY KEY(id)";
    final static String T3_CHILD_T1_NAME = "t3";
    final static String T3_CHILD_T1_DDL = "id int NOT NULL, t1id int, PRIMARY KEY(id), "+
                                          "grouping foreign key(t1id) references t1(id)";

    private SchemaManager schemaManager;

    private void createTableDef(final String schema, final String tableName, final String ddl) throws Exception {
        transactionally(new Callable<Void>() {
            public Void call() throws Exception {
                createTable(schema, tableName, ddl);
                return null;
            }
        });
    }

    private void registerISTable(final UserTable table, final MemoryTableFactory factory) throws Exception {
        schemaManager.registerMemoryInformationSchemaTable(table, factory);
    }

    private void registerISTable(final UserTable table, final int version) throws Exception {
        schemaManager.registerStoredInformationSchemaTable(table, version);
    }

    private void unRegisterISTable(final TableName name) throws Exception {
        schemaManager.unRegisterMemoryInformationSchemaTable(name);
    }

    private void deleteTableDef(final String schema, final String table) throws Exception {
        transactionally(new Callable<Void>() {
            public Void call() throws Exception {
                schemaManager.dropTableDefinition(session(), schema, table, SchemaManager.DropBehavior.RESTRICT);
                return null;
            }
        });
    }

    private void safeRestart() throws Exception {
        safeRestartTestServices();
        schemaManager = serviceManager().getSchemaManager();
    }
    
    @Override
    protected Map<String, String> startupConfigProperties() {
        // Set up multi-volume treespace policy so we can be sure schema is properly distributed.
        final Map<String, String> properties = new HashMap<>();
        properties.put("fdbsql.treespace.a",
                                    VOL2_PREFIX + "*:${datapath}/${schema}.v0,create,pageSize:${buffersize},"
                                    + "initialSize:10K,extensionSize:1K,maximumSize:10G");
        properties.put("fdbsql.treespace.b",
                                    VOL3_PREFIX + "*:${datapath}/${schema}.v0,create,pageSize:${buffersize},"
                                    + "initialSize:10K,extensionSize:1K,maximumSize:10G");
        return properties;
    }

    @Before
    public void setUp() throws Exception {
        schemaManager = serviceManager().getSchemaManager();
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void createOneDefinition() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);
    }

    @Test
    public void deleteOneDefinition() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);
        deleteTableDef(SCHEMA, T1_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test(expected=NoSuchTableException.class)
    public void deleteUnknownDefinition() throws Exception {
        assertTablesInSchema(SCHEMA);
        deleteTableDef("schema1", "table1");
    }

    @Test
    public void deleteDefinitionTwice() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);
        
        deleteTableDef(SCHEMA, T1_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void deleteTwoDefinitions() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);

        createTableDef(SCHEMA, T2_NAME, T2_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME, T2_NAME);

        deleteTableDef(SCHEMA, T1_NAME);
        assertTablesInSchema(SCHEMA, T2_NAME);

        deleteTableDef(SCHEMA, T2_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void deleteChildDefinition() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);

        createTableDef(SCHEMA, T3_CHILD_T1_NAME, T3_CHILD_T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME, T3_CHILD_T1_NAME);

        // Deleting child should not delete parent
        deleteTableDef(SCHEMA, T3_CHILD_T1_NAME);
        assertTablesInSchema(SCHEMA, T1_NAME);

        deleteTableDef(SCHEMA, T1_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void deleteParentDefinitionFirst() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME);

        createTableDef(SCHEMA, T3_CHILD_T1_NAME, T3_CHILD_T1_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME, T3_CHILD_T1_NAME);

        final AkibanInformationSchema ais = ddl().getAIS(session());
        final UserTable t1 = ais.getUserTable(SCHEMA, T1_NAME);
        assertNotNull("t1 exists", t1);
        final UserTable t3 = ais.getUserTable(SCHEMA, T3_CHILD_T1_NAME);
        assertNotNull("t3 exists", t3);

        // Double check grouping we are expecting
        assertNotNull("t3 has parent", t3.getParentJoin());
        assertSame("t1 is t3 parent", t1, t3.getParentJoin().getParent());
        assertNotNull("t1 has children", t1.getCandidateChildJoins());
        assertEquals("t1 has 1 child", 1, t1.getCandidateChildJoins().size());
        assertSame("t3 is t1 child", t3, t1.getCandidateChildJoins().get(0).getChild());
        
        try {
            deleteTableDef(SCHEMA, T1_NAME);
            Assert.fail("Exception expected!");
        } catch(InvalidOperationException e) {
            assertEquals("error code", ErrorCode.REFERENCED_TABLE, e.getCode());
        }

        assertTablesInSchema(SCHEMA, T1_NAME, T3_CHILD_T1_NAME);
        deleteTableDef(SCHEMA, T3_CHILD_T1_NAME);
        assertTablesInSchema(SCHEMA, T1_NAME);
        deleteTableDef(SCHEMA, T1_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void createTwoDefinitionsTwoVolumes() throws Exception {
        final String SCHEMA_VOL2_A = VOL2_PREFIX + "_a";
        final String SCHEMA_VOL2_B = VOL2_PREFIX + "_b";

        assertTablesInSchema(SCHEMA_VOL2_A);
        assertTablesInSchema(SCHEMA_VOL2_B);
        assertTablesInSchema(SCHEMA);

        createTableDef(SCHEMA_VOL2_A, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA_VOL2_A, T1_NAME);
        assertTablesInSchema(SCHEMA);

        createTableDef(SCHEMA_VOL2_B, T2_NAME, T2_DDL);
        assertTablesInSchema(SCHEMA_VOL2_B, T2_NAME);
        assertTablesInSchema(SCHEMA_VOL2_A, T1_NAME);
        assertTablesInSchema(SCHEMA);
    }

    @Test
    public void deleteTwoDefinitionsTwoVolumes() throws Exception {
        final String SCHEMA_VOL2_A = VOL2_PREFIX + "_a";
        final String SCHEMA_VOL2_B = VOL2_PREFIX + "_b";

        createTableDef(SCHEMA_VOL2_A, T1_NAME, T1_DDL);
        createTableDef(SCHEMA_VOL2_B, T2_NAME, T2_DDL);
        assertTablesInSchema(SCHEMA_VOL2_A, T1_NAME);
        assertTablesInSchema(SCHEMA_VOL2_B, T2_NAME);

        deleteTableDef(SCHEMA_VOL2_A, T1_NAME);
        assertTablesInSchema(SCHEMA_VOL2_A);
        assertTablesInSchema(SCHEMA_VOL2_B, T2_NAME);

        deleteTableDef(SCHEMA_VOL2_B, T2_NAME);
        assertTablesInSchema(SCHEMA_VOL2_A);
        assertTablesInSchema(SCHEMA_VOL2_B);
    }


    @Test
    public void updateTimestampChangesWithCreate() throws Exception {
        final long first = ais().getGeneration();
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        final long second = ais().getGeneration();
        assertTrue("timestamp changed", first != second);
    }

    @Test
    public void updateTimestampChangesWithDelete() throws Exception {
        createTableDef(SCHEMA, T1_NAME, T1_DDL);
        final long first = ais().getGeneration();
        deleteTableDef(SCHEMA, T1_NAME);
        final long second = ais().getGeneration();
        assertTrue("timestamp changed", first != second);
    }

    @Test
    public void manyTablesAndRestart() throws Exception {
        final int TABLE_COUNT = 50;
        final int UT_COUNT = ais().getUserTables().size();

        String tableNames[] = new String[TABLE_COUNT];
        for(int i = 0; i < TABLE_COUNT; ++i) {
            tableNames[i] = "t" + i;
            createTable(SCHEMA, tableNames[i], "id int not null primary key");
        }

        AkibanInformationSchema ais = ais();
        Collection<UserTable> before = new ArrayList<>(ais.getUserTables().values());
        assertEquals("user tables count before", TABLE_COUNT + UT_COUNT, ais.getUserTables().size());
        assertTablesInSchema(SCHEMA, tableNames);

        safeRestart();
        ais = ais();
        assertNotNull(ais);
        Collection<UserTable> after = ais.getUserTables().values();
        // Diagnostics for occasional assertion violation of user table count
        if (ais.getUserTables().size() != TABLE_COUNT + UT_COUNT) {
            System.out.println("BEFORE");
            for (UserTable userTable : before) {
                System.out.println(String.format("    %s", userTable));
            }
            System.out.println("AFTER");
            for (UserTable userTable : after) {
                System.out.println(String.format("    %s", userTable));
            }
        }
        assertEquals("user tables count after", TABLE_COUNT + UT_COUNT, ais.getUserTables().size());
        assertTablesInSchema(SCHEMA, tableNames);
    }

    @Test
    public void multipleSchemasAndRestart() throws Exception {
        final int TABLE_COUNT = 3;
        AkibanInformationSchema ais = ais();
        final int UT_COUNT = ais.getUserTables().size();

        createTable(SCHEMA+"1", "t1", "id int not null primary key");
        createTable(SCHEMA+"2", "t2", "id int not null primary key");
        createTable(SCHEMA+"3", "t3", "id int not null primary key");

        ais = ais();
        assertEquals("user tables count", TABLE_COUNT + UT_COUNT, ais.getUserTables().size());
        assertTablesInSchema(SCHEMA+"1", "t1");
        assertTablesInSchema(SCHEMA+"2", "t2");
        assertTablesInSchema(SCHEMA+"3", "t3");

        safeRestart();
        ais = ais();
        assertNotNull("ais exists", ais);

        assertEquals("user tables count", TABLE_COUNT + UT_COUNT, ais.getUserTables().size());
        assertTablesInSchema(SCHEMA+"1", "t1");
        assertTablesInSchema(SCHEMA+"2", "t2");
        assertTablesInSchema(SCHEMA+"3", "t3");
    }

    @Test
    public void treeNamesAreUnique() {
        TableName testNames[][] = {
                // These broke simple concat(s,'.',t) that was in RowDefCache
                {new TableName("foo.bar", "baz"), new TableName("foo", "bar.baz")},
                // These broke actual tree name generation
                {new TableName("foo$$_akiban_bar", "baz"), new TableName("foo", "bar$$_akiban_baz")},
                // New tree name separator
                {new TableName("tes.", "tt1"), new TableName("tes", ".tt1")}
        };

        for(TableName pair[] : testNames) {
            createTable(pair[0].getSchemaName(), pair[0].getTableName(), "id int not null primary key");
            createTable(pair[1].getSchemaName(), pair[1].getTableName(), "id int not null primary key");
            String treeName1 = ddl().getAIS(session()).getUserTable(pair[0]).getGroup().getTreeName();
            String treeName2 = ddl().getAIS(session()).getUserTable(pair[1]).getGroup().getTreeName();
            assertFalse("Non unique tree name: " + treeName1, treeName1.equals(treeName2));
        }
    }

    @Test
    public void crossSchemaGroups() throws Exception {
        final String SCHEMA1 = "schema1";
        final String SCHEMA2 = "schema2";
        final TableName PARENT1 = new TableName(SCHEMA1, "parent1");
        final TableName CHILD1 = new TableName(SCHEMA2, "child1");
        final TableName PARENT2 = new TableName(SCHEMA2, "parent2");
        final TableName CHILD2 = new TableName(SCHEMA1, "child2");
        final String T2_CHILD_DDL = T2_DDL + ", t1id int, grouping foreign key(t1id) references %s";

        // parent in schema1, child in schema2
        createTable(PARENT1.getSchemaName(), PARENT1.getTableName(), T1_DDL);
        createTable(CHILD1.getSchemaName(), CHILD1.getTableName(), String.format(T2_CHILD_DDL, PARENT1));
        // child in schema1, child in schema2
        createTable(PARENT2.getSchemaName(), PARENT2.getTableName(), T1_DDL);
        createTable(CHILD2.getSchemaName(), CHILD2.getTableName(), String.format(T2_CHILD_DDL, PARENT2));

        safeRestart();

        assertTablesInSchema(SCHEMA1, PARENT1.getTableName(), CHILD2.getTableName());
        assertTablesInSchema(SCHEMA2, PARENT2.getTableName(), CHILD1.getTableName());
        UserTable parent1 = ddl().getUserTable(session(), PARENT1);
        assertEquals("parent1 and child1 group", parent1.getGroup(), ddl().getUserTable(session(), CHILD1).getGroup());
        UserTable parent2 = ddl().getUserTable(session(), PARENT2);
        assertEquals("parent2 and child2 group", parent2.getGroup(), ddl().getUserTable(session(), CHILD2).getGroup());
    }

    @Test
    public void changeInAISTableIsUpgradeIssue() throws Exception {
        /*
         * Simple sanity check. Change as needed but remember it is an UPGRADE ISSUE.
         */
        final String SCHEMA = "information_schema";
        final String STATS_TABLE = "index_statistics";
        final String ENTRY_TABLE = "index_statistics_entry";
        final String STATS_DDL = "create table `information_schema`.`index_statistics`("+
            "`table_id` bigint NOT NULL, `index_id` bigint NOT NULL, `analysis_timestamp` timestamp NULL, "+
            "`row_count` bigint NULL, `sampled_count` bigint NULL, "+
            "PRIMARY KEY(`table_id`, `index_id`)"+
        ") engine=akibandb DEFAULT CHARSET=utf8 COLLATE=utf8_bin";
        final String ENTRY_DDL = "create table `information_schema`.`index_statistics_entry`("+
            "`table_id` bigint NOT NULL, `index_id` bigint NOT NULL, `column_count` int NOT NULL, "+
            "`item_number` int NOT NULL, `key_string` varchar(2048) CHARACTER SET latin1 NULL, `key_bytes` varbinary(4096) NULL, "+
            "`eq_count` bigint NULL, `lt_count` bigint NULL, `distinct_count` bigint NULL, "+
            "PRIMARY KEY(`table_id`, `index_id`, `column_count`, `item_number`), "+
            "CONSTRAINT `__akiban_fk_0` FOREIGN KEY `__akiban_fk_0`(`table_id`, `index_id`) "+
                "REFERENCES `index_statistics`(`table_id`, `index_id`)"+
        ") engine=akibandb DEFAULT CHARSET=utf8 COLLATE=utf8_bin";

        DDLGenerator generator = new DDLGenerator();
        UserTable statsTable = ais().getUserTable(SCHEMA, STATS_TABLE);
        assertNotNull("Stats table present", statsTable);
        assertEquals("Stats DDL", STATS_DDL, generator.createTable(statsTable));

        UserTable entryTable = ais().getUserTable(SCHEMA, ENTRY_TABLE);
        assertNotNull("Entry table present", entryTable);
        assertEquals("Entry DDL", ENTRY_DDL, generator.createTable(entryTable));
    }

    @Test
    public void renameAndRecreate() throws Exception {
        createTable(SCHEMA, T1_NAME, T1_DDL);
        ddl().renameTable(session(), tableName(SCHEMA, T1_NAME), tableName("foo", "bar"));
        createTable(SCHEMA, T1_NAME, T1_DDL);

        String originalTreeName = getUserTable(SCHEMA, T1_NAME).getGroup().getTreeName();
        String newTreeName = getUserTable("foo", "bar").getGroup().getTreeName();
        assertTrue("Unique tree names", !originalTreeName.equals(newTreeName));
    }

    @Test
    public void createRestartAndCreateMore() throws Exception {
        createTable(SCHEMA, T1_NAME, T1_DDL);
        createTable(SCHEMA, T3_CHILD_T1_NAME, T3_CHILD_T1_DDL);
        createTable(SCHEMA, T2_NAME, T2_DDL);
        assertTablesInSchema(SCHEMA, T1_NAME, T2_NAME, T3_CHILD_T1_NAME);
        safeRestart();
        assertTablesInSchema(SCHEMA, T1_NAME, T2_NAME, T3_CHILD_T1_NAME);
        createIndex(SCHEMA, T2_NAME, "id_2", "id");
    }

    @Test
    public void registerMemoryTableBasic() throws Exception {
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");
        MemoryTableFactory factory = new MemoryTableFactoryMock();
        registerISTable(makeSimpleISTable(tableName), factory);

        {
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNotNull("New table exists", testTable);
            assertEquals("Is memoryTable", true, testTable.hasMemoryTableFactory());
            assertSame("Exact factory preserved", factory, testTable.getMemoryTableFactory());
        }

        createTable(SCHEMA, T1_NAME, T1_DDL);
        {
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNotNull("New table exists after DDL", testTable);
            assertEquals("Is memoryTable after more DDL", true, testTable.hasMemoryTableFactory());
            assertSame("Exact factory preserved after more DDL", factory, testTable.getMemoryTableFactory());
        }

        {
            safeRestart();
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNull("Table did not survive restart", testTable);
        }
    }

    @Test
    public void noDuplicateMemoryTables() throws Exception {
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");
        final UserTable sourceTable = makeSimpleISTable(tableName);
        MemoryTableFactory factory = new MemoryTableFactoryMock();
        registerISTable(sourceTable, factory);
        try {
            registerISTable(sourceTable, factory);
            fail("Expected DuplicateTableNameException");
        } catch(DuplicateTableNameException e) {
            // expected
        } finally {
            schemaManager.unRegisterMemoryInformationSchemaTable(tableName);
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void noNullMemoryTableFactory() throws Exception {
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");
        registerISTable(makeSimpleISTable(tableName), null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void noMemoryTableOutsideAISSchema() throws Exception {
        final TableName tableName = new TableName("foo", "test_table");
        registerISTable(makeSimpleISTable(tableName), null);
    }

    @Test
    public void registerStoredTableBasic() throws Exception {
        final Integer VERSION = 5;
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");

        registerISTable(makeSimpleISTable(tableName), VERSION);
        {
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNotNull("New table exists", testTable);
            assertEquals("Exact version is preserved", VERSION, testTable.getVersion());
        }

        createTable(SCHEMA, T1_NAME, T1_DDL);
        {
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNotNull("New table exists after DDL", testTable);
            assertEquals("Exact version preserved after more DDL", VERSION, testTable.getVersion());
        }

        {
            safeRestart();
            UserTable testTable = ddl().getAIS(session()).getUserTable(tableName);
            assertNotNull("Table survived restart", testTable);
            assertEquals("Exact version preserved after more DDL", VERSION, testTable.getVersion());
        }
    }

    @Test
    public void canRegisterStoredTableWithSameVersion() throws Exception {
        final Integer VERSION = 5;
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");
        final UserTable sourceTable = makeSimpleISTable(tableName);
        registerISTable(sourceTable, VERSION);
        registerISTable(sourceTable, VERSION);
    }

    @Test(expected=ISTableVersionMismatchException.class)
    public void cannotRegisterStoredTableWithDifferentVersion() throws Exception {
        final Integer VERSION = 5;
        final TableName tableName = new TableName(TableName.INFORMATION_SCHEMA, "test_table");
        final UserTable sourceTable = makeSimpleISTable(tableName);
        registerISTable(sourceTable, VERSION);
        registerISTable(sourceTable, VERSION + 1);
    }

    @Test(expected=IllegalArgumentException.class)
    public void noStoredTableOutsideAISSchema() throws Exception {
        final int VERSION = 5;
        final TableName tableName = new TableName("foo", "test_table");
        registerISTable(makeSimpleISTable(tableName), VERSION);
    }

    @Test
    public void sameRootNameMultipleSchemasAndRestart() throws Exception {
        final String SCHEMA1 = SCHEMA + "1";
        final String SCHEMA2 = SCHEMA + "2";
        createTable(SCHEMA1, T1_NAME, T1_DDL);
        createTable(SCHEMA2, T1_NAME, T1_DDL);
        assertTablesInSchema(SCHEMA1, T1_NAME);
        assertTablesInSchema(SCHEMA2, T1_NAME);

        safeRestart();
        assertTablesInSchema(SCHEMA1, T1_NAME);
        assertTablesInSchema(SCHEMA2, T1_NAME);
    }

    @Test(expected=JoinToProtectedTableException.class)
    public void joinToISTable() throws Exception {
        TableName name = new TableName(TableName.INFORMATION_SCHEMA, "p");
        NewAISBuilder builder = AISBBasedBuilder.create(SCHEMA);
        builder.userTable(name).colLong("id", false).pk("id");
        try {
            builder.userTable(T1_NAME).colLong("id", false).colLong("pid", true).pk("id").joinTo("information_schema", "p").on("pid", "id");
            registerISTable(builder.unvalidatedAIS().getUserTable(name), new MemoryTableFactoryMock());
            ddl().createTable(session(), builder.unvalidatedAIS().getUserTable(SCHEMA, T1_NAME));
        } finally {
            // ApiTestBase#tearDownAllTables skips IS tables 
            unRegisterISTable(name);
        }
    }

    @Test
    public void userAndSystemRoutines() {
        final TableName sysName = new TableName(TableName.SYS_SCHEMA, "sys");
        final TableName userName = new TableName(SCHEMA, "user");
        AkibanInformationSchema temp = new AkibanInformationSchema();
        final Routine sysR = Routine.create(temp, sysName.getSchemaName(), sysName.getTableName(), "other", Routine.CallingConvention.SQL_ROW);
        final Routine userR = Routine.create(temp, userName.getSchemaName(), userName.getTableName(), "java", Routine.CallingConvention.JAVA);

        schemaManager.registerSystemRoutine(sysR);
        assertNotNull("Found sys routine after register", ais().getRoutine(sysName));

        transactionallyUnchecked(new Runnable() {
            @Override
            public void run() {
                schemaManager.createRoutine(session(), userR, false);
            }
        });

        assertNotNull("Found user routine after create", ais().getRoutine(userName));
        assertNotNull("Found sys routine after user create", ais().getRoutine(sysName));
    }

    @Test(expected=UnsupportedOperationException.class)
    public void alterSequenceName() {
        transactionallyUnchecked(new Runnable()
        {
            @Override
            public void run() {
                AkibanInformationSchema ais = new AkibanInformationSchema();
                Sequence s1 = Sequence.create(ais, SCHEMA, "s1", 1, 1, 1, 10, false);
                schemaManager.createSequence(session(), s1);
            }
        });
        transactionallyUnchecked(new Runnable()
        {
            @Override
            public void run() {
                AkibanInformationSchema ais = new AkibanInformationSchema();
                Sequence s2 = Sequence.create(ais, SCHEMA, "s2", 1, 1, 1, 10, false);
                schemaManager.alterSequence(session(), new TableName(SCHEMA, "s1"), s2);
            }
        });
    }

    @Test
    public void alterSequenceParameters() {
        final TableName name = new TableName(SCHEMA, "s");
        transactionallyUnchecked(new Runnable()
        {
            @Override
            public void run() {
                AkibanInformationSchema ais = new AkibanInformationSchema();
                Sequence s = Sequence.create(ais, name.getSchemaName(), name.getTableName(), 3, 4, 1, 10, false);
                schemaManager.createSequence(session(), s);
            }
        });
        final String origTreeName = ais().getSequence(name).getTreeName();
        transactionallyUnchecked(new Runnable()
        {
            @Override
            public void run() {
                AkibanInformationSchema ais = new AkibanInformationSchema();
                Sequence s = Sequence.create(ais, name.getSchemaName(), name.getTableName(), 5, 6, 2, 20, true);
                schemaManager.alterSequence(session(), s.getSequenceName(), s);
            }
        });
        Sequence s = ais().getSequence(name);
        assertEquals("startsWith", 5, s.getStartsWith());
        assertEquals("increment", 6, s.getIncrement());
        assertEquals("minValue", 2, s.getMinValue());
        assertEquals("maxValue", 20, s.getMaxValue());
        assertEquals("cycle", true, s.isCycle());
        assertEquals("Tree names different", schemaManager.treeRemovalIsDelayed(), !origTreeName.equals(s.getTreeName()));
    }

    @Test
    public void alterIdentitySequence() {
        createTable(SCHEMA, T1_NAME, "id INT NOT NULL GENERATED BY DEFAULT AS IDENTITY");
        final UserTable origTable = getUserTable(SCHEMA, T1_NAME);
        final Sequence origSequence = origTable.getColumn("id").getIdentityGenerator();
        final TableName name = origSequence.getSequenceName();
        assertNotNull("identity sequence", origSequence);
        transactionallyUnchecked(new Runnable()
        {
            @Override
            public void run() {
                AkibanInformationSchema ais = new AkibanInformationSchema();
                Sequence s = Sequence.create(ais, name.getSchemaName(), name.getTableName(), 5, 6, 2, 20, true);
                schemaManager.alterSequence(session(), s.getSequenceName(), s);
            }
        });

        final UserTable newTable = getUserTable(SCHEMA, T1_NAME);
        final Sequence newSequence = newTable.getColumn("id").getIdentityGenerator();
        assertNotNull("has identity sequence after alter", newSequence);
        assertEquals("startsWith", 5, newSequence.getStartsWith());
        assertEquals("increment", 6, newSequence.getIncrement());
        assertEquals("minValue", 2, newSequence.getMinValue());
        assertEquals("maxValue", 20, newSequence.getMaxValue());
        assertEquals("cycle", true, newSequence.isCycle());
    }


    /**
     * Assert that the given tables in the given schema has the, and only the, given tables. Also
     * confirm each table exists in the AIS and has a definition.
     * @param schema Name of schema to check.
     * @param tableNames List of table names to check.
     * @throws Exception For any internal error.
     */
    private void assertTablesInSchema(String schema, String... tableNames) {
        final SortedSet<String> expected = new TreeSet<>();
        final AkibanInformationSchema ais = ddl().getAIS(session());
        for (String name : tableNames) {
            final Table table = ais.getTable(schema, name);
            assertNotNull(schema + "." + name + " in AIS", table);
            expected.add(name);
        }
        final SortedSet<String> actual = new TreeSet<>();
        Schema schemaObj = ais.getSchema(schema);
        if(schemaObj != null) {
            actual.addAll(schemaObj.getUserTables().keySet());
        }
        assertEquals("tables in: " + schema, expected, actual);
    }

    private static UserTable makeSimpleISTable(TableName name) {
        NewAISBuilder builder = AISBBasedBuilder.create(name.getSchemaName());
        builder.userTable(name.getTableName()).colLong("id", false).pk("id");
        return builder.ais().getUserTable(name);
    }

    private static class MemoryTableFactoryMock implements MemoryTableFactory {
        @Override
        public TableName getName() {
            throw new UnsupportedOperationException();
        }

        @Override
        public MemoryGroupCursor.GroupScan getGroupScan(MemoryAdapter adapter) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cursor getIndexCursor(Index index, Session session, IndexKeyRange keyRange, API.Ordering ordering, IndexScanSelector scanSelector) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long rowCount() {
            throw new UnsupportedOperationException();
        }

        @Override
        public IndexStatistics computeIndexStatistics(Session session, Index index) {
            throw new UnsupportedOperationException();
        }
    }
}