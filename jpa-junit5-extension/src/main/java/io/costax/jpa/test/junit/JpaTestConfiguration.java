package io.costax.jpa.test.junit;

import java.util.Arrays;

class JpaTestConfiguration {
    private final String persistenceUnit;
    private final String[] beforeEachQueries;
    private final String[] afterEachQueries;

    JpaTestConfiguration(final String persistenceUnit, final String[] beforeEachQueries, final String[] afterEachQueries) {
        this.persistenceUnit = persistenceUnit;
        this.beforeEachQueries = beforeEachQueries;
        this.afterEachQueries = afterEachQueries;
    }

    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    public String[] getBeforeEachQueries() {
        return Arrays.copyOf(beforeEachQueries, beforeEachQueries.length);
    }

    public String[] getAfterEachQueries() {
        return Arrays.copyOf(afterEachQueries, afterEachQueries.length);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "persistenceUnit='" + persistenceUnit + '\'' +
                '}';
    }
}
