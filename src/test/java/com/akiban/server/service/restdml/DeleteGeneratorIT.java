package com.akiban.server.service.restdml;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.akiban.ais.model.TableName;
import com.akiban.qp.operator.Operator;
import com.akiban.server.explain.ExplainContext;
import com.akiban.server.explain.format.DefaultFormatter;
import com.akiban.server.service.session.Session;
import com.akiban.server.t3expressions.T3RegistryService;
import com.akiban.server.test.it.ITBase;

public class DeleteGeneratorIT extends ITBase {

    private DeleteGenerator deleteGenerator; 
    public static final String SCHEMA = "test";

    @Before
    public void start() {
        Session session = this.session();
        this.txnService().beginTransaction(session);
    }
    
    @After
    public void commit() {
        this.txnService().commitTransaction(this.session());
    }

    @Test
    public void testCDelete() {
        createTable(SCHEMA, "c",
                "cid INT PRIMARY KEY NOT NULL",
                "name VARCHAR(32)");    

        TableName table = new TableName (SCHEMA, "c");
        this.deleteGenerator = new DeleteGenerator (this.ais());
        deleteGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator delete = deleteGenerator.create(table);
        
        assertEquals(
                getExplain(delete, table.getSchemaName()),
                "\n  Delete_Returning()\n"+
                "    AncestorLookup_Default(Index(c.PRIMARY) -> c)\n"+
                "      IndexScan_Default(Index(c.PRIMARY), cid = $1)");
    }

    @Test 
    public void testPKNotFirst() {
        createTable (SCHEMA, "c",
                "name varchar(32) not null",
                "address varchar(64) not null",
                "cid int not null primary key");
        TableName table = new TableName (SCHEMA, "c");
        this.deleteGenerator = new DeleteGenerator (this.ais());
        deleteGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator delete = deleteGenerator.create(table);
        assertEquals(
                getExplain(delete, table.getSchemaName()),
                "\n  Delete_Returning()\n"+
                "    AncestorLookup_Default(Index(c.PRIMARY) -> c)\n"+
                "      IndexScan_Default(Index(c.PRIMARY), cid = $1)");
    }

    @Test
    public void testPKMultiColumn() {
        createTable(SCHEMA, "o",
                "cid int not null",
                "oid int not null",
                "items int not null",
                "primary key (cid, oid)");
        TableName table = new TableName (SCHEMA, "o");
        this.deleteGenerator = new DeleteGenerator (this.ais());
        deleteGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator delete = deleteGenerator.create(table);
        assertEquals(
                getExplain(delete, table.getSchemaName()),
                "\n  Delete_Returning()\n"+
                "    AncestorLookup_Default(Index(o.PRIMARY) -> o)\n"+
                "      IndexScan_Default(Index(o.PRIMARY), cid = $1, oid = $2)");
    }
    
    @Test
    public void testNoPK() {
        createTable (SCHEMA, "c",
                "name varchar(32) not null",
                "address varchar(64) not null",
                "cid int not null");
        TableName table = new TableName (SCHEMA, "c");
        this.deleteGenerator = new DeleteGenerator (this.ais());
        deleteGenerator.setT3Registry(this.serviceManager().getServiceByClass(T3RegistryService.class));
        Operator delete = deleteGenerator.create(table);
        assertEquals (
                getExplain(delete, table.getSchemaName()),
                "\n  Delete_Returning()\n"+
                "    AncestorLookup_Default(Index(c.PRIMARY) -> c)\n"+
                "      IndexScan_Default(Index(c.PRIMARY), __akiban_pk = $1)");
    }

    protected String getExplain (Operator plannable, String defaultSchemaName) {
        StringBuilder str = new StringBuilder();
        ExplainContext context = new ExplainContext(); // Empty
        DefaultFormatter f = new DefaultFormatter(defaultSchemaName);
        for (String operator : f.format(plannable.getExplainer(context))) {
            str.append("\n  ");
            str.append(operator);
        }
        return str.toString();
    }
}
