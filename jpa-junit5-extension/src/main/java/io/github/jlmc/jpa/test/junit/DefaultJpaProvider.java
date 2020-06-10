package io.github.jlmc.jpa.test.junit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Function;

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
        final EntityManager em = em();
        try {

            consumer.accept(em);

        } finally {
            em.close();
        }
    }

    @Override
    public <T> T doIt(Function<EntityManager, T> function) {
        final EntityManager em = em();
        try {

            return function.apply(em);

        } finally {
            em.close();
        }
    }

    @Override
    public void doInTx(Consumer<EntityManager> consumer) {
        final EntityManager em = em();
        EntityTransaction tx = null;
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
    public <T> T doInTx(Function<EntityManager, T> consumer) {
        final EntityManager em = em();
        EntityTransaction tx = null;
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
        final EntityManager em = em();
        try {

            final Connection connection = unwrapConnection(em);
            return function.apply(connection);

        } finally {
            em.close();
        }
    }

    @Override
    public void doJDBCWork(Consumer<Connection> consumer) {
        final EntityManager em = em();
        try {

            final Connection connection = unwrapConnection(em);
            consumer.accept(connection);

        } finally {
            em.close();
        }
    }

    private Connection unwrapConnection(EntityManager em) {
        try {
            // try to unwarp from eclipseLink provider
            return em.unwrap(java.sql.Connection.class);
        } catch (Exception ignore) {
        }

        try {
            // try to unwarp from hibernate provider
            final Object delegate = em.getDelegate();
            final Class<?> aClass1 = delegate.getClass();
            final Method connectionMethod = aClass1.getDeclaredMethod("connection");
            return (java.sql.Connection) connectionMethod.invoke(delegate);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ignore) {
        }

        throw new UnsupportedOperationException("The JPA-Junit5-Extension can not extract a '"
                + java.sql.Connection.class.getName()
                + "' from your current JPA provider");
    }

}
