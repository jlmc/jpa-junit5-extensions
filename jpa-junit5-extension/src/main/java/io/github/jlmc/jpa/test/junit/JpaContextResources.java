package io.github.jlmc.jpa.test.junit;


import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class JpaContextResources {

    private final JpaTestConfiguration jpaTestConfiguration;
    private final List<Field> providesFields;
    private final Set<FieldValuePair<?>> executionFieldObjects = new HashSet<>();
    private EntityManagerFactory entityManagerFactory;

    JpaContextResources(final JpaTestConfiguration configuration,
                        final List<Field> providesFields) {

        jpaTestConfiguration = configuration;
        this.providesFields = providesFields;
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

    JpaTestConfiguration getJpaTestConfiguration() {
        return jpaTestConfiguration;
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
