package io.github.jlmc.jpa.test.junit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.EntityManagerFactory;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An injectable interface providing runtime methods to execute test statements.
 */
public interface JpaProvider {

    /**
     * Get the current instance of the {@link EntityManagerFactory} that has been created for the given current tests class.
     * @return the current {@link EntityManagerFactory}
     */
    EntityManagerFactory emf();

    /**
     * Create a new Instance of {@link EntityManager}, it is a delegator method for {@code emf().createEntityManager()}.
     * Each invocation of this method will create a new instance of {@link EntityManager}.
     * @return new instance of {@link EntityManager}
     */
    EntityManager em();

    /**
     * Execute an EntityManager consumer function.
     * @param consumer functions that should be executed.
     */
    void doIt(Consumer<EntityManager> consumer);

    /**
     * Execute an EntityManager function function.
     * @param function code statement that should be executed.
     * @param <T> class type of the object that should be returned.
     * @return object of the generic type T
     */
    <T> T doItWithReturn(Function<EntityManager, T> function);

    /**
     * Execute an EntityManager consumer function under an JPA {@link EntityTransaction}.
     * The Transaction begins before the invocations of the {@link Consumer#accept} method
     * and is committed right after the execution of the method {@link Consumer#accept}.
     * If any things goes wrong the Transaction will be rollback.
     * @param consumer function that should be executed.
     */
    void doInTx(Consumer<EntityManager> consumer);

    /**
     * Execute an EntityManager consumer function under an JPA {@link EntityTransaction}.
     * Transaction begins before the invocations of the {@link Consumer#accept} method
     * and is committed right after the execution of the method {@link Consumer#accept}.
     * If any things goes wrong the Transaction will be rollback.
     * @param function that should be executed
     * @param <T> class type of the object that should be returned.
     * @return object of the generic type T
     */
    <T> T doInTxWithReturn(Function<EntityManager, T> function);

    /**
     * Execute a Jdbc connection consumer statement with a return object.
     * @param function code statement that should be executed.
     * @param <T> class type of the object that should be returned.
     * @return object of the generic type T
     */
    <T> T doJDBCReturningWork(Function<Connection, T> function);

    /**
     * Execute a Jdbc connection consumer statement.
     * @param consumer function code statement that should be executed.
     */
    void doJDBCWork(Consumer<Connection> consumer);

}
