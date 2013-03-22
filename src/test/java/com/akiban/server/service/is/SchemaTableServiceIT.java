
package com.akiban.server.service.is;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;

import com.akiban.ais.model.AkibanInformationSchema;
import com.akiban.server.service.servicemanager.GuicedServiceManager;

import com.akiban.server.test.it.ITBase;

public final class SchemaTableServiceIT extends ITBase {

    @Override
    protected GuicedServiceManager.BindingsConfigurationProvider serviceBindingsProvider() {
        return super.serviceBindingsProvider()
                .bindAndRequire(BasicInfoSchemaTablesService.class, BasicInfoSchemaTablesServiceImpl.class)
                .bindAndRequire(StorageSchemaTablesService.class, StorageSchemaTablesServiceImpl.class)
                .bindAndRequire(ServerSchemaTablesService.class, ServerSchemaTablesServiceImpl.class);
    }

    @Override
    protected Map<String, String> startupConfigProperties() {
        return uniqueStartupConfigProperties(getClass());
    }

    private AkibanInformationSchema ais;
    
    @Before
    public void getISTables () throws Exception {
        ais = transactionally(new Callable<AkibanInformationSchema>() {
            @Override
            public AkibanInformationSchema call() {
                return serviceManager().getSchemaManager().getAis(session());
            }
        });
    }
    
    @Test
    public void baseInfoExamine() {
        assertEquals ("Table count", 19, BasicInfoSchemaTablesServiceImpl.createTablesToRegister().getUserTables().size());
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.SCHEMATA));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.TABLES));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.COLUMNS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.TABLE_CONSTRAINTS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.REFERENTIAL_CONSTRAINTS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.GROUPING_CONSTRAINTS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.KEY_COLUMN_USAGE));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.INDEXES));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.INDEX_COLUMNS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.SEQUENCES));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.VIEWS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.VIEW_TABLE_USAGE));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.VIEW_COLUMN_USAGE));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.ROUTINES));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.PARAMETERS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.JARS));
        assertNotNull (ais.getUserTable(BasicInfoSchemaTablesServiceImpl.ROUTINE_JAR_USAGE));
    }
    
    @Test
    public void storageExamine() {
        assertEquals ("Table count", 11, StorageSchemaTablesServiceImpl.createTablesToRegister().getUserTables().size());
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_ALERTS_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_BUFFER_POOLS));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_CHECKPOINT_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_CLEANUP_MANAGER_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_IO_METER_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_IO_METERS));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_JOURNAL_MANAGER_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_MANAGEMENT_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_TRANSACTION_SUMMARY));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_TREES));
        assertNotNull (ais.getUserTable(StorageSchemaTablesServiceImpl.STORAGE_VOLUMES));
    }
    
    @Test
    public void serverExamine() {
        assertEquals ("Table count", 10, ServerSchemaTablesServiceImpl.createTablesToRegister().getUserTables().size());
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.ERROR_CODES));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_INSTANCE_SUMMARY));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_SERVERS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_SESSIONS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_PARAMETERS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_MEMORY_POOLS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_GARBAGE_COLLECTORS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_TAPS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_PREPARED_STATEMENTS));
        assertNotNull (ais.getUserTable(ServerSchemaTablesServiceImpl.SERVER_CURSORS));
    }
}
