package db;

import db.entities.*;
import gui.GettingStartedView;
import utils.Logger;

import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;

import java.util.*;

// "Connects" the java driver with the running neo4j db
// Used to interact with the database
public class DBModel {

    private static SessionFactory sessionFactory;
    private final Session session;
    private static int paramSaveCounter;
    public DBModel() {

        paramSaveCounter = 0;

        Configuration config = new Configuration.Builder()
                .useNativeTypes()
                .uri("bolt://localhost:5555")
                .build();
        sessionFactory = new SessionFactory(config, "db.entities");
        Session newSession;
        newSession = sessionFactory.openSession();
        this.session = newSession;

        if (this.session == null) {
            Logger.getInstance().logToError("Couldn't open a database session.");
            throw new RuntimeException("Session could not be opened!");
        }
    }

    public static void saveParameter(InputParameter inputParameter) {
        paramSaveCounter += 1;
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(inputParameter);
            tx.commit();
        }
        session.clear();
    }

    public static void saveURL(Url urlEntity) {
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(urlEntity);
            tx.commit();
        }
        session.clear();
    }

    public static void saveMatchEntity(ParameterMatch parameterMatchEntity) {
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(parameterMatchEntity);
            tx.commit();
        }
        session.clear();
    }

    public static Collection<InputParameter> loadAllParameters() {
        Session session = sessionFactory.openSession();
        Collection<InputParameter> collection = session.loadAll(InputParameter.class);
        paramSaveCounter = collection.size();
        GettingStartedView.numberOfParameters.setText(String.valueOf(paramSaveCounter));
        return collection;
    }

    public static Collection<Url> loadAllUrlEntities() {
        Session session = sessionFactory.openSession();
        Collection<Url> collection = session.loadAll(Url.class);
        GettingStartedView.numberOfUrls.setText(String.valueOf(collection.size()));
        session.clear();
        return collection;
    }

    public static Collection<ParameterMatch> loadAllMatchEntities() {
        Session session = sessionFactory.openSession();
        Collection<ParameterMatch> collection =  session.loadAll(ParameterMatch.class);
        GettingStartedView.numberOfParameterMatches.setText(String.valueOf(collection.size()));
        session.clear();
        return collection;
    }

    public static Collection<MatchValue> loadAllMatchEntryEntities() {
        Session session = sessionFactory.openSession();
        Collection<MatchValue> collection = session.loadAll(MatchValue.class);
        GettingStartedView.numberOfMatchValues.setText(String.valueOf(collection.size()));
        session.clear();
        return collection;
    }

    public static Collection<InputValue> loadAllParameterOccurrenceEntities() {
        Session session = sessionFactory.openSession();
        Collection<InputValue> collection = session.loadAll(InputValue.class);
        GettingStartedView.numberOfParameterValues.setText(String.valueOf(collection.size()));
        session.clear();
        return collection;
    }

   public static Result query(String cypherQuery, Map<String, String> params) {
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction(Transaction.Type.READ_ONLY)) {
            Result result = session.query(cypherQuery, params);
            tx.commit();
            session.clear();
            return result;
        }
   }

    public static void executeCypher(String cypherQuery, Map<String, String> values) {
        Session session = sessionFactory.openSession();
        try (Transaction transaction = session.beginTransaction(Transaction.Type.READ_WRITE)) {
            session.query(cypherQuery, values);
            transaction.commit();
        }
        session.clear();
    }

    public static void saveBulk(List<Object> entities) {
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.save(entities);
            tx.commit();
        }
        session.clear();
    }

    public void closeSession() {
        sessionFactory.close();
    }
}
