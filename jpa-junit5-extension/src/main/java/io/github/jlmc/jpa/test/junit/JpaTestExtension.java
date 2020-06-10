package io.github.jlmc.jpa.test.junit;

import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.AnnotationUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

public class JpaTestExtension implements
        BeforeAllCallback,
        AfterAllCallback,
        BeforeEachCallback,
        AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("io", "costax", "jpa", "test", "junit", "JpaTestExtension");
    private static final String JPA_CONTEXT_RESOURCES = "JPA_CONTEXT_RESOURCES";

    @Override
    public void beforeAll(final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        JpaTestConfiguration configuration = findJpaTestConfiguration(context);
        List<Field> providesFields = providesFields(context);

        JpaContextResources contextResources = new JpaContextResources(configuration, providesFields);
        contextResources.startEntityManagerFactory();

        store.put(JPA_CONTEXT_RESOURCES, contextResources);
    }

    @Override
    public void afterAll(final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        JpaContextResources jpaContextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);

        jpaContextResources.stopEntityManagerFactory();
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        injectJpaContextFields(context);

        executeBeforeEachQueries(context);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        executeAfterEachQueries(context);
        disposeJpaContextFields(context);
    }

    private void executeBeforeEachQueries(final ExtensionContext context) {
        final ExtensionContext.Store store = context.getStore(NAMESPACE);
        final JpaContextResources jpaContextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);

        if (jpaContextResources.getJpaTestConfiguration().getBeforeEachQueries().length > 0) {
            jpaContextResources
                    .jpaProvider()
                    .doJDBCWork(connection -> executeBatch(jpaContextResources.getJpaTestConfiguration().getBeforeEachQueries(), connection));
        }
    }

    private void injectJpaContextFields(final ExtensionContext context) throws IllegalAccessException {
        ExtensionContext.Store store = context.getStore(NAMESPACE);

        JpaContextResources jpaContextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);
        List<Field> providesFields = providesFields(context);
        Object testClassInstance = context.getRequiredTestInstance();

        if (!providesFields.isEmpty()) {
            final JpaProvider jpaProvider = jpaContextResources.jpaProvider();

            for (final Field providesField : providesFields) {
                //AnnotatedType annotatedType = providesField.getAnnotatedType();
                Class<?> type = providesField.getType();

                if (type.isAssignableFrom(JpaProvider.class)) {
                    providesField.setAccessible(true);
                    providesField.set(testClassInstance, jpaProvider);

                    jpaContextResources.addExecutionFieldValue(providesField, JpaProvider.class, jpaProvider);

                } else if (type.isAssignableFrom(EntityManager.class)) {
                    providesField.setAccessible(true);
                    final EntityManager em = jpaProvider.em();
                    providesField.set(testClassInstance, em);

                    jpaContextResources.addExecutionFieldValue(providesField, EntityManager.class, em, x -> {
                        if (x.isOpen()) {
                            x.close();
                        }
                    });
                } else if (type.isAssignableFrom(EntityManagerFactory.class)) {
                    providesField.setAccessible(true);
                    final EntityManagerFactory emf = jpaProvider.emf();
                    providesField.set(testClassInstance, emf);
                    jpaContextResources.addExecutionFieldValue(providesField, EntityManagerFactory.class, emf);
                }
            }
        }
    }


    private void executeAfterEachQueries(final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        JpaContextResources jpaContextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);


        if (jpaContextResources.getJpaTestConfiguration().getAfterEachQueries().length > 0) {
            jpaContextResources.jpaProvider()
                    .doJDBCWork(connection -> executeBatch(jpaContextResources.getJpaTestConfiguration().getAfterEachQueries(), connection));
        }
    }

    private void disposeJpaContextFields(final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        JpaContextResources jpaContextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);
        jpaContextResources.afterEach();
    }

    public List<Field> providesFields(ExtensionContext context) {
        return context.getTestClass()
                .map(cs -> AnnotationSupport.findAnnotatedFields(cs, JpaContext.class))
                .orElse(Collections.emptyList());
    }

    private JpaTestConfiguration findJpaTestConfiguration(final ExtensionContext context) {
        return context.getTestClass()
                .flatMap(cs -> AnnotationUtils.findAnnotation(cs, JpaTest.class))
                .map(jpaTest -> new JpaTestConfiguration(jpaTest.persistenceUnit(), jpaTest.beforeEachQueries(), jpaTest.afterEachQueries()))
                .orElseThrow(() -> new IllegalStateException("No Jpa Test configuration provider"));
    }

    private void executeBatch(final String[] queries, final Connection connection) {
        if (queries != null && queries.length > 0) {
            try {
                connection.setAutoCommit(false);
                try (Statement stmt = connection.createStatement()) {
                    for (String s : queries) {
                        stmt.addBatch(s);
                    }
                    stmt.executeBatch();
                }

                connection.commit();
            } catch (SQLException e) {
                throw new IllegalStateException("Can not execute the Batch SQL Statements ", e);
            }
        }
    }

}
