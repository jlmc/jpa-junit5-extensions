package io.github.jlmc.jpa.test.annotation;

import java.lang.annotation.*;

/**
 * @Sql is used to annotate a test class or test method to configure SQL scripts() and statements()
 * to be executed against a given database during integration tests.
 */
@Repeatable(SqlGroup.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Sql {

    /**
     * The paths to the SQL scripts to execute.
     */
    String[] scripts() default {};

    /**
     * Inlined SQL statements to execute.
     */
    String[] statements() default {};

    /**
     * When the SQL scripts and statements should be executed.
     */
    Phase phase() default Phase.BEFORE_TEST_METHOD;

    /**
     * When the SQL scripts and statements should be executed.
     */
    enum Phase {
        /**
         * The configured SQL scripts and statements will be executed before the corresponding test method.
         */
        BEFORE_TEST_METHOD,

        /**
         * The configured SQL scripts and statements will be executed after the corresponding test method.
         */
        AFTER_TEST_METHOD
    }


}
