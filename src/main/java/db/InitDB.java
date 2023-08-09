package db;

import scheduling.Presenter;
import utils.Logger;

import java.nio.file.Path;

public class InitDB {

    private DBModel dbModel;

    public DBModel start(Path dbPath) {
        Logger.getInstance().logToOutput("Initializing Database...");
        // Set ContextClassLoader to load the neo4j stuff on the right classpath
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        // Not sure if needed
        Presenter.init();
        new Neo4JDB(dbPath);
        this.dbModel = new DBModel();
        Logger.getInstance().logToOutput("Initializing Database finished");
        return this.dbModel;
    }
}
