package io.github.jlmc.jpa.test.junit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jlmc.jpa.test.support.ReflectionSupport.invokeMethod;

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
        EntityManager em = em();
        try {

            consumer.accept(em);

        } finally {
            em.close();
        }
    }

    @Override
    public <T> T doItWithReturn(Function<EntityManager, T> function) {
        EntityManager em = em();
        try {

            return function.apply(em);

        } finally {
            em.close();
        }
    }

    @Override
    public void doInTx(Consumer<EntityManager> consumer) {
        EntityTransaction tx = null;
        EntityManager em = em();

        try {
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
        } finally {
            em.close();
        }
    }

    @Override
    public <T> T doInTxWithReturn(Function<EntityManager, T> consumer) {
        EntityTransaction tx = null;

        EntityManager em = em();

        try {
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
        } finally {
            em.close();
        }
    }

    @Override
    public <T> T doJDBCReturningWork(Function<Connection, T> function) {
        EntityManager em = em();
        try {

            final Connection connection = unwrapConnection(em);
            return function.apply(connection);

        } finally {
            em.close();
        }
    }

    @Override
    public void doJDBCWork(Consumer<Connection> consumer) {
        EntityManager em = em();
        try {

            final Connection connection = unwrapConnection(em);
            consumer.accept(connection);

        } finally {
            em.close();
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
