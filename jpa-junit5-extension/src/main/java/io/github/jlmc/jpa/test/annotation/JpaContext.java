package io.github.jlmc.jpa.test.annotation;


import io.github.jlmc.jpa.test.junit.JpaProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to inject information into a class field of Jpa Test Case Class.
 *
 * @see JpaProvider
 * @see javax.persistence.EntityManager
 * @see javax.persistence.EntityManagerFactory
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JpaContext {
}
