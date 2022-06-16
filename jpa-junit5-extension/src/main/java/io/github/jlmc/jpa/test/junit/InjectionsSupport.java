package io.github.jlmc.jpa.test.junit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

final class InjectionsSupport {

    static void injectPersistenceUnitFields(final Object targetInstance,
                                            final Collection<Field> fields,
                                            final JpaProvider provider,
                                            final JpaContextResources contextResources) throws IllegalAccessException {

        for (final Field field : fields) {
            injectPersistenceUnitField(targetInstance, field, provider, contextResources);
        }
    }

    static void injectPersistenceUnitField(final Object targetInstance,
                                           final Field field,
                                           final JpaProvider provider,
                                           final JpaContextResources contextResources) throws IllegalAccessException {
        injectEntityManagerFactoryField(targetInstance, field, provider, contextResources);
    }

    static void injectEntityManagerFactoryField(final Object targetInstance,
                                                final Field field,
                                                final JpaProvider provider,
                                                final JpaContextResources contextResources) throws IllegalAccessException {

        field.setAccessible(true);
        final EntityManagerFactory emf = provider.emf();
        field.set(targetInstance, emf);
        contextResources.addExecutionFieldValue(field, EntityManagerFactory.class, emf);
    }

    public static void injectPersistenceContextFields(final Object targetInstance,
                                                      final List<Field> fields,
                                                      final JpaProvider provider,
                                                      final JpaContextResources contextResources) throws IllegalAccessException {

        for (final Field field : fields) {
            injectPersistenceContextField(targetInstance, field, provider, contextResources);
        }
    }

    public static void injectPersistenceContextField(final Object targetInstance,
                                                     final Field field,
                                                     final JpaProvider provider,
                                                     final JpaContextResources contextResources) throws IllegalAccessException {
        injectEntityManagerField(targetInstance, field, provider, contextResources);
    }

    public static void injectEntityManagerField(final Object targetInstance,
                                                final Field field,
                                                final JpaProvider provider,
                                                final JpaContextResources contextResources) throws IllegalAccessException {

        field.setAccessible(true);
        final EntityManager em = provider.em();

        field.set(targetInstance, em);

        contextResources.addExecutionFieldValue(field, EntityManager.class, em, x -> {
            if (x.isOpen()) {
                x.close();
            }
        });

    }

    public static void injectContextFields(final Object targetInstance,
                                           final List<Field> fields,
                                           final JpaProvider provider,
                                           final JpaContextResources contextResources) throws IllegalAccessException {

        if (fields.isEmpty()) {
            return;
        }

        for (final Field field : fields) {

            Class<?> type = field.getType();

            if (type.isAssignableFrom(JpaProvider.class)) {
                injectJpaProviderField(targetInstance, provider, contextResources, field);

            } else if (type.isAssignableFrom(EntityManager.class)) {
                injectEntityManagerField(targetInstance, field, provider, contextResources);

            } else if (type.isAssignableFrom(EntityManagerFactory.class)) {
                injectEntityManagerFactoryField(targetInstance, field, provider, contextResources);
            }
        }

    }

    private static void injectJpaProviderField(final Object targetInstance, final JpaProvider provider, final JpaContextResources contextResources, final Field field) throws IllegalAccessException {
        field.setAccessible(true);
        field.set(targetInstance, provider);
        contextResources.addExecutionFieldValue(field, JpaProvider.class, provider);
    }
}
