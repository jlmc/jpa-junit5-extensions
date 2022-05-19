package io.github.jlmc.jpa.test.junit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jlmc.jpa.test.junit.ReflectionSupport.invokeMethod;

class DefaultJpaProvider implements JpaProvider {

    private final EntityManagerFactory entityManagerFactory;

    DefaultJpaProvider(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public EntityManagerFactory emf() {
        return entityManagerFactory;
    }

    @Override
    public EntityManager em() {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public void doIt(final Consumer<EntityManager> consumer) {
        try (EntityManager em = em()) {

            consumer.accept(em);

        }
    }

    @Override
    public <T> T doItWithReturn(Function<EntityManager, T> function) {
        try (EntityManager em = em()) {

            return function.apply(em);

        }
    }

    @Override
    public void doInTx(Consumer<EntityManager> consumer) {
        EntityTransaction tx = null;
        try (EntityManager em = em()) {
            tx = em.getTransaction();
            tx.begin();

            consumer.accept(em);

            em.flush();
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Override
    public <T> T doInTxWithReturn(Function<EntityManager, T> consumer) {
        EntityTransaction tx = null;
        try (EntityManager em = em()) {
            tx = em.getTransaction();
            tx.begin();

            final T result = consumer.apply(em);

            em.flush();
            tx.commit();

            return result;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    @Override
    public <T> T doJDBCReturningWork(Function<Connection, T> function) {
        try (EntityManager em = em()) {

            final Connection connection = unwrapConnection(em);
            return function.apply(connection);

        }
    }

    @Override
    public void doJDBCWork(Consumer<Connection> consumer) {
        try (EntityManager em = em()) {

            final Connection connection = unwrapConnection(em);
            consumer.accept(connection);

        }
    }

    private Connection unwrapConnection(EntityManager em) {
        try {
            // For hibernate greater or equals to 6.x.x
            return invokeMethod(em.getDelegate(), "getJdbcConnectionAccess")
                    .flatMap(jdbcConnectionAccess -> invokeMethod(jdbcConnectionAccess, "obtainConnection"))
                    .map(Connection.class::cast)
                    .orElseThrow();

        } catch (Exception ignore) {
        }

        try {
            // For hibernate versions less than 6.x.x
            return invokeMethod(em.getDelegate(), "connection")
                    .map(Connection.class::cast)
                    .orElseThrow();
        } catch (Exception ignore) {
        }

        try {
            // For eclipseLink provider
            return em.unwrap(java.sql.Connection.class);
        } catch (Exception ignore) {
        }

        throw new UnsupportedOperationException("The JPA-Junit5-Extension can not extract a '"
                + java.sql.Connection.class.getName()
                + "' from your current JPA provider");
    }
}
