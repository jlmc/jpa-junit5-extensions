package io.github.jlmc.jpa.test.annotation;

import io.github.jlmc.jpa.test.junit.JpaTestExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a JPA test that focuses only on JPA components.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(JpaTestExtension.class)
public @interface JpaTest {

    String persistenceUnit();

}
