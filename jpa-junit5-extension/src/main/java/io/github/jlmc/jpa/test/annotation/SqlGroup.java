package io.github.jlmc.jpa.test.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation that aggregates several @Sql annotations.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SqlGroup {

    /** (Required) An array of <code>Sql</code> annotations. */
    Sql[] value();
}
