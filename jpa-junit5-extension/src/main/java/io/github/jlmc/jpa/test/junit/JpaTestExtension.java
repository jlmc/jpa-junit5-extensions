package io.github.jlmc.jpa.test.junit;

import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import io.github.jlmc.jpa.test.annotation.Sql;
import io.github.jlmc.jpa.test.annotation.SqlGroup;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JpaTestExtension implements
        BeforeAllCallback,
        AfterAllCallback,
        BeforeEachCallback,
        AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create("io", "github", "jlmc", "jpa", "test", "junit", "JpaTestExtension");
    private static final String JPA_CONTEXT_RESOURCES = "JPA_CONTEXT_RESOURCES";

    @Override
    public void beforeAll(final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        JpaTestConfiguration configuration = findJpaTestConfiguration(context);

        JpaContextResources contextResources = new JpaContextResources(configuration);
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
        final JpaContextResources contextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);

        final Class<?> requiredTestClass = context.getRequiredTestClass();
        final Method requiredTestMethod = context.getRequiredTestMethod();

        List<String> allScripts = new ArrayList<>();
        List<String> allStatements = new ArrayList<>();

        allScripts.addAll(getSqlConfigurations(requiredTestClass, Sql.Phase.BEFORE_TEST_METHOD, Sql::scripts));
        allScripts.addAll(getSqlConfigurations(requiredTestMethod, Sql.Phase.BEFORE_TEST_METHOD, Sql::scripts));

        allStatements.addAll(getSqlConfigurations(requiredTestClass, Sql.Phase.BEFORE_TEST_METHOD, Sql::statements));
        allStatements.addAll(getSqlConfigurations(requiredTestMethod, Sql.Phase.BEFORE_TEST_METHOD, Sql::statements));

        if (!allScripts.isEmpty() || !allStatements.isEmpty()) {
            executeScriptsAndStatements(contextResources, allScripts, allStatements);
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
        JpaContextResources contextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);

        final Class<?> requiredTestClass = context.getRequiredTestClass();
        final Method requiredTestMethod = context.getRequiredTestMethod();

        List<String> allScripts = new ArrayList<>();
        List<String> allStatements = new ArrayList<>();

        allScripts.addAll(getSqlConfigurations(requiredTestClass, Sql.Phase.AFTER_TEST_METHOD, Sql::scripts));
        allScripts.addAll(getSqlConfigurations(requiredTestMethod, Sql.Phase.AFTER_TEST_METHOD, Sql::scripts));

        allStatements.addAll(getSqlConfigurations(requiredTestClass, Sql.Phase.AFTER_TEST_METHOD, Sql::statements));
        allStatements.addAll(getSqlConfigurations(requiredTestMethod, Sql.Phase.AFTER_TEST_METHOD, Sql::statements));

        if (!allScripts.isEmpty() || !allStatements.isEmpty()) {
            executeScriptsAndStatements(contextResources, allScripts, allStatements);
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
        return context.getTestClass().flatMap(cs -> AnnotationSupport.findAnnotation(cs, JpaTest.class))
                .map(jpaTest -> new JpaTestConfiguration(jpaTest.persistenceUnit()))
                .orElseThrow(() -> new IllegalStateException("No Jpa Test configuration provider"));
    }

    private List<String> getSqlConfigurations(final AnnotatedElement annotatedElement,
                                              final Sql.Phase phase,
                                              final Function<Sql, String[]> resolver) {

        Objects.requireNonNull(annotatedElement);
        Objects.requireNonNull(phase);
        Objects.requireNonNull(resolver);

        final List<String> sqlGroupsValues = AnnotationSupport.findAnnotation(annotatedElement, SqlGroup.class)
                .stream()
                .map(SqlGroup::value)
                .flatMap(Arrays::stream)
                .filter(sql -> phase == sql.phase())
                .map(resolver)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        if (!sqlGroupsValues.isEmpty()) {
            return sqlGroupsValues;
        }

        return AnnotationSupport.findAnnotation(annotatedElement, Sql.class)
                .stream()
                .filter(sql -> phase == sql.phase())
                .map(resolver)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }


    private void executeScriptsAndStatements(final JpaContextResources contextResources, final List<String> scripts, final List<String> statements) {
        contextResources
                .jpaProvider()
                .doJDBCWork(connection -> {

                    try {
                        SqlPopulation.execute(connection, scripts, statements);
                    } catch (SQLException e) {
                        throw new IllegalStateException(e.getMessage(), e);
                    }

                });
    }

}
