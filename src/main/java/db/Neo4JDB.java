package db;

import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.connectors.BoltConnector;
import org.neo4j.configuration.connectors.HttpConnector;
import org.neo4j.configuration.helpers.SocketAddress;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.dbms.api.DatabaseManagementServiceBuilder;
import org.neo4j.graphdb.GraphDatabaseService;

import utils.Logger;

import java.nio.file.Path;


public class Neo4JDB {

    public static GraphDatabaseService graphDb;
    public static  DatabaseManagementService managementService;

    // Starts Neo4J service
    public Neo4JDB(Path databaseDirectory) {
        managementService = new DatabaseManagementServiceBuilder(databaseDirectory)
                .setConfig(GraphDatabaseSettings.debug_log_enabled, true)
                .setConfig(BoltConnector.enabled, true)
                .setConfig(BoltConnector.listen_address, new SocketAddress("localhost", 5555))
                .setConfig(BoltConnector.encryption_level, BoltConnector.EncryptionLevel.DISABLED)
                .setConfig(HttpConnector.enabled, true)
                .setConfig(HttpConnector.listen_address, new SocketAddress("localhost", 7474))
                .build();

        graphDb = managementService.database("neo4j");
        registerShutdownHook(managementService);

    }

    public static void shutDown() {
        Logger.getInstance().logToOutput("Shutting down database ...");
        managementService.shutdown();
    }

    private static void registerShutdownHook(final DatabaseManagementService managementService) {
        // Registers a shutdown hook for the Neo4j instance so that it
        // shuts down nicely when the VM exits (even if you "Ctrl-C" the
        // running application).
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                managementService.shutdown();
            }
        });
    }
}

