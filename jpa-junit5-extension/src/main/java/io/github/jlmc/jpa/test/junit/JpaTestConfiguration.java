package io.github.jlmc.jpa.test.junit;

class JpaTestConfiguration {
    private final String persistenceUnit;

    JpaTestConfiguration(final String persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }

    public String getPersistenceUnit() {
        return persistenceUnit;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "persistenceUnit='" + persistenceUnit + '\'' +
                '}';
    }
}
