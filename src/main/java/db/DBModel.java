package db;

import db.entities.*;
import gui.GettingStartedView;
import org.neo4j.driver.exceptions.TransientException;
import org.neo4j.graphdb.TransactionFailureException;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import utils.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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
            RuntimeException exception = new RuntimeException("Session could not be opened!");
            Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
            throw exception;
        }
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

    public static Collection<db.entities.Session> loadAllSessionEntities() {
        Session session = sessionFactory.openSession();
        Collection<db.entities.Session> collection = session.loadAll(db.entities.Session.class);
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

    public static void saveEntity(Object entity) {
        if (entity instanceof InputParameter)
            paramSaveCounter += 1;
        Throwable txEx = null;
        int RETRIES = 20;
        int BACKOFF = 3000;
        Session session = sessionFactory.openSession();
        for ( int i = 0; i < RETRIES; i++ )
        {
            try ( Transaction tx = session.beginTransaction() )
            {
                session.save(entity);
                tx.commit();
                session.clear();
                return;
            }
            catch ( Throwable ex )
            {
                txEx = ex;

                // Add whatever exceptions to retry on here
                if ( !(ex instanceof DeadlockDetectedException || ex instanceof TransientException) )
                {
                    break;
                }
            }

            // Wait so that we don't immediately get into the same deadlock
            if ( i < RETRIES - 1 )
            {
                try
                {
                    Thread.sleep( BACKOFF );
                }
                catch ( InterruptedException e )
                {
                    TransactionFailureException exception = new TransactionFailureException( "Interrupted", e );
                    Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
                    throw exception;
                }
            }
        }

        session.clear();

        if ( txEx instanceof TransactionFailureException )
        {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((TransactionFailureException) txEx);
        }
        else if ( txEx instanceof Error )
        {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((Error) txEx);
        }
        else
        {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((RuntimeException) txEx);
        }
    }

    public static void saveBulk(List<Object> entities) {
        Throwable txEx = null;
        int RETRIES = 20;
        int BACKOFF = 3000;
        Session session = sessionFactory.openSession();
        for ( int i = 0; i < RETRIES; i++ ) {
            try ( Transaction tx = session.beginTransaction() ) {
                session.save(entities);
                tx.commit();
                session.clear();
                return;
            } catch ( Throwable ex ) {
                txEx = ex;

                // Add whatever exceptions to retry on here
                if ( !(ex instanceof DeadlockDetectedException || ex instanceof TransientException) ) {
                    break;
                }
            }

            // Wait so that we don't immediately get into the same deadlock
            if ( i < RETRIES - 1 ) {
                try {
                    Thread.sleep( BACKOFF );
                } catch ( InterruptedException e ) {
                    TransactionFailureException exception = new TransactionFailureException( "Interrupted", e );
                    Logger.getInstance().logToError(Arrays.toString(exception.getStackTrace()));
                    throw exception;
                }
            }
        }

        session.clear();

        if ( txEx instanceof TransactionFailureException ) {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((TransactionFailureException) txEx);
        } else if ( txEx instanceof Error ) {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((Error) txEx);
        } else {
            Logger.getInstance().logToError(Arrays.toString(txEx.getStackTrace()));
            throw ((RuntimeException) txEx);
        }
    }

    public static void purgeDatabase() {
        Session session = sessionFactory.openSession();
        try (Transaction tx = session.beginTransaction()) {
            session.purgeDatabase();
            tx.commit();
        }
        session.clear();
    }

    public void closeSession() {
        sessionFactory.close();
    }
}
