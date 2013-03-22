
package com.akiban.sql.pg;

import com.akiban.server.test.it.ITBase;
import com.akiban.server.service.servicemanager.GuicedServiceManager;

import static org.junit.Assert.fail;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Ignore
public class PostgresServerITBase extends ITBase
{
    private static final Logger LOG = LoggerFactory.getLogger(PostgresServerITBase.class);
    public static final File RESOURCE_DIR = 
        new File("src/test/resources/"
                 + PostgresServerITBase.class.getPackage().getName().replace('.', '/'));

    public static final String SCHEMA_NAME = "test";
    public static final String DRIVER_NAME = "org.postgresql.Driver";
    public static final String CONNECTION_URL = "jdbc:postgresql://localhost:%d/"+SCHEMA_NAME;
    public static final String USER_NAME = "auser";
    public static final String USER_PASSWORD = "apassword";

    @Override
    protected GuicedServiceManager.BindingsConfigurationProvider serviceBindingsProvider() {
        return super.serviceBindingsProvider()
                .bindAndRequire(PostgresService.class, PostgresServerManager.class);
    }

    @Override
    protected Map<String, String> startupConfigProperties() {
        return uniqueStartupConfigProperties(PostgresServerITBase.class);
    }

    protected Connection openConnection() throws Exception {
        int port = getPostgresService().getPort();
        if (port <= 0) {
            throw new Exception("akserver.postgres.port is not set.");
        }
        String url = String.format(CONNECTION_URL, port);
        Class.forName(DRIVER_NAME);
        return DriverManager.getConnection(url, USER_NAME, USER_PASSWORD);
    }

    protected static void closeConnection(Connection connection) throws Exception {
        if (!connection.isClosed())
            connection.close();
    }

    protected PostgresService getPostgresService() {
        return serviceManager().getServiceByClass(PostgresService.class);
    }

    protected PostgresServer server() {
        return getPostgresService().getServer();
    }

    private static final Callable<Void> forgetOnStopServices = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                forgetConnection();
                return null;
            }
        };

    // One element connection pool.
    private static final ThreadLocal<Connection> connectionRef = new ThreadLocal<>();

    protected Connection getConnection() throws Exception {
        Connection connection = connectionRef.get();
        if (connection == null || connection.isClosed()) {
            beforeStopServices.add(forgetOnStopServices);
            for (int i = 0; i < 6; i++) {
                if (server().isListening())
                    break;
                if (i == 1)
                    LOG.warn("Postgres server not listening. Waiting...");
                else if (i == 5)
                    fail("Postgres server still not listening. Giving up.");
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ex) {
                    LOG.warn("caught an interrupted exception; re-interrupting", ex);
                    Thread.currentThread().interrupt();
                }
            }
            connection = openConnection();
            connectionRef.set(connection);
        }
        return connection;
    }

    public static void forgetConnection() throws Exception {
        Connection connection = connectionRef.get();
        if (connection != null) {
            closeConnection(connection);
            connectionRef.remove();
            beforeStopServices.remove(forgetOnStopServices);
        }
    }

    protected PostgresServerITBase() {
    }

    protected List<List<?>> sql(String sql) {
        try {
            Connection conn = getConnection();
            try {
                try (Statement statement = conn.createStatement()) {
                    if (!statement.execute(sql))
                        return null;
                    List<List<?>> results = new ArrayList<>();
                    try (ResultSet rs = statement.getResultSet()) {
                        int ncols = rs.getMetaData().getColumnCount();
                        while (rs.next()) {
                            List<Object> row = new ArrayList<>(ncols);
                            for (int i = 0; i < ncols; ++i)
                                row.add(rs.getObject(i+1));
                        }
                    }
                    if (statement.getMoreResults())
                        throw new RuntimeException("multiple ResultSets for SQL: " + sql);
                    return results;
                }
            }
            finally {
                forgetConnection();
            }
        } catch (Exception e) {
            throw new RuntimeException("while executing SQL: " + sql, e);
        }
    }

}
