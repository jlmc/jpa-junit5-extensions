package io.github.jlmc.jpa.test.junit;


import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class JpaContextResources {

    private final JpaTestConfiguration jpaTestConfiguration;
    private final Set<FieldValuePair<?>> executionFieldObjects = new HashSet<>();
    private EntityManagerFactory entityManagerFactory;

    JpaContextResources(final JpaTestConfiguration configuration) {
        jpaTestConfiguration = configuration;
    }

    void startEntityManagerFactory() {
        if (entityManagerFactory != null) {
            throw new IllegalStateException("The EntityManagerFactory is already defined");
        }

        entityManagerFactory = Persistence.createEntityManagerFactory(jpaTestConfiguration.getPersistenceUnit());
    }

    void stopEntityManagerFactory() {
        if (entityManagerFactory == null) {
            throw new IllegalStateException("The EntityManagerFactory is not defined");
        }
        entityManagerFactory.close();
        this.entityManagerFactory = null;
    }

    JpaProvider jpaProvider() {
        return new DefaultJpaProvider(this.entityManagerFactory);
    }

    public <T> void addExecutionFieldValue(final Field field, Class<T> clazz, final T value) {
        this.executionFieldObjects.add(new FieldValuePair<>(field, clazz, value, (c) -> {
        }));
    }

    public <T> void addExecutionFieldValue(final Field field, Class<T> clazz, final T value, Consumer<T> afterEachCallback) {
        this.executionFieldObjects.add(new FieldValuePair<>(field, clazz, value, afterEachCallback));
    }

    public void afterEach() {
        this.executionFieldObjects.forEach(FieldValuePair::close);
        this.executionFieldObjects.clear();
    }

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static class FieldValuePair<T> {
        private final Field field;
        private final Class<T> clazz;
        private final T value;
        private final Consumer<T> afterEachCallback;

        FieldValuePair(final Field field, Class<T> clazz, final T value, Consumer<T> afterEachCallback) {
            this.field = field;
            this.clazz = clazz;
            this.value = value;
            this.afterEachCallback = afterEachCallback;
        }

        private void close() {
            this.afterEachCallback.accept(value);
        }

    }
}
