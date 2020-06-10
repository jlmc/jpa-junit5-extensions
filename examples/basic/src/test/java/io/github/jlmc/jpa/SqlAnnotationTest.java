package io.github.jlmc.jpa;


import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import io.github.jlmc.jpa.test.annotation.Sql;
import io.github.jlmc.jpa.test.junit.JpaProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@JpaTest(persistenceUnit = "it")
@Sql(
        statements = {
                "insert into book (id, title) values (9901, 'Effective Java')",
        },
        phase = Sql.Phase.BEFORE_TEST_METHOD
)
@Sql(
        statements = "delete from book where id in (9901)",
        phase = Sql.Phase.AFTER_TEST_METHOD)
public class SqlAnnotationTest {

    @JpaContext
    JpaProvider jpa;

    @Test
    @Sql(
            statements = {
                    "insert into book (id, title) values (9902, 'Clean Code')",
                    "insert into book (id, title) values (9903, 'Patterns of Enterprise Application Architecture')",
            },
            phase = Sql.Phase.BEFORE_TEST_METHOD
    )
    @Sql(
            statements = "delete from book where id in (9901, 9902, 9903)",
            phase = Sql.Phase.AFTER_TEST_METHOD)
    void executeSqlStatementsBeforeEachMethod() {

        //@formatter:off
        List<Book> books =
                jpa
                  .em()
                      .createQuery("select b from Book b where b.id in ( :id ) order by b.title", Book.class)
                      .setParameter("id", List.of(9901, 9902, 9903))
                      .getResultList();
        //@formatter:on

        assertNotNull(books);
        assertEquals(3, books.size());
        assertEquals("Clean Code", books.get(0).getTitle());
        assertEquals("Effective Java", books.get(1).getTitle());
        assertEquals("Patterns of Enterprise Application Architecture", books.get(2).getTitle());
    }

    @Test
    @Sql(scripts = "/insert-data.sql", phase = Sql.Phase.BEFORE_TEST_METHOD)
    @Sql(scripts = "/delete-data.sql", phase = Sql.Phase.AFTER_TEST_METHOD)
    void executeSqlScriptsBeforeEachMethod() {
        //@formatter:off
        List<Book> books =
                jpa
                   .em()
                   .createQuery("select b from Book b order by b.title", Book.class)
                   .getResultList();
        //@formatter:on

        assertNotNull(books);
        assertEquals(4, books.size());
        assertEquals("Effective Java", books.get(0).getTitle());
        assertEquals("Java Concurrency in Practice", books.get(1).getTitle());
        assertEquals("Java EE 8 High Performance", books.get(2).getTitle());
        assertEquals("The Definitive Guide to Java Performance", books.get(3).getTitle());
    }
}
