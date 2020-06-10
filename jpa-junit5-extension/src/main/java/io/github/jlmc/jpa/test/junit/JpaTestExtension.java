package io.github.jlmc.jpa.test.junit;

import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import io.github.jlmc.jpa.test.annotation.Sql;
import io.github.jlmc.jpa.test.annotation.SqlGroup;
import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.support.AnnotationSupport;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

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

        JpaContextResources contextResources = store.get(JPA_CONTEXT_RESOURCES, JpaContextResources.class);

        Object targetInstance = context.getRequiredTestInstance();

        final Map<Class<? extends Annotation>, List<Field>> injectableFields = providesFieldsMap(context);
        if (!injectableFields.isEmpty()) {
            final JpaProvider provider = contextResources.jpaProvider();

            final List<Field> persistenceUnitFields = injectableFields.get(PersistenceUnit.class);
            InjectionsSupport.injectPersistenceUnitFields(targetInstance, persistenceUnitFields, provider, contextResources);

            final List<Field> persistenceContextFields = injectableFields.get(PersistenceContext.class);
            InjectionsSupport.injectPersistenceContextFields(targetInstance, persistenceContextFields, provider, contextResources);

            final List<Field> contextFields = injectableFields.get(JpaContext.class);
            InjectionsSupport.injectContextFields(targetInstance, contextFields, provider, contextResources);
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

    private Map<Class<? extends Annotation>, List<Field>> providesFieldsMap(ExtensionContext context) {
        final List<Field> jpaContexts = context.getTestClass()
                .map(cs -> AnnotationSupport.findAnnotatedFields(cs, JpaContext.class))
                .orElse(Collections.emptyList());

        final List<Field> persistenceContexts = context.getTestClass()
                .map(cs -> AnnotationSupport.findAnnotatedFields(cs, PersistenceContext.class))
                .orElse(Collections.emptyList());

        final List<Field> persistenceUnits = context.getTestClass()
                .map(cs -> AnnotationSupport.findAnnotatedFields(cs, PersistenceUnit.class))
                .orElse(Collections.emptyList());

        return Map.of(
                JpaContext.class, jpaContexts,
                PersistenceContext.class, persistenceContexts,
                PersistenceUnit.class, persistenceUnits);
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
                .collect(toList());

        if (!sqlGroupsValues.isEmpty()) {
            return sqlGroupsValues;
        }

        return AnnotationSupport.findAnnotation(annotatedElement, Sql.class)
                .stream()
                .filter(sql -> phase == sql.phase())
                .map(resolver)
                .flatMap(Arrays::stream)
                .collect(toList());
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
