package io.github.jlmc.jpa;

import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import io.github.jlmc.jpa.test.annotation.Sql;
import io.github.jlmc.jpa.test.junit.JpaProvider;
import jakarta.persistence.EntityManager;
import org.hibernate.annotations.QueryHints;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static io.github.jlmc.jpa.test.annotation.Sql.Phase.AFTER_TEST_METHOD;
import static io.github.jlmc.jpa.test.annotation.Sql.Phase.BEFORE_TEST_METHOD;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JpaTest(persistenceUnit = "it")
@Sql(
        statements = {
                "insert into book (id, title) values (9901, 'Mastering Java 11')",
                "insert into book (id, title) values (9902, 'Refactoring. Improving the Design of Existing Code')"
        },
        phase = BEFORE_TEST_METHOD
)
@Sql(
        statements = "delete from book where true",
        phase = AFTER_TEST_METHOD)
@DisplayNameGeneration(DisplayNameGenerator.Standard.class)
class BookEntityQueriesTest {

    @JpaContext
    JpaProvider jpa;

    @ParameterizedTest
    @ValueSource(ints = {9901, 9902})
    void findBookById(int bookId) {
        final Book book = jpa.em().find(Book.class, bookId);
        Assertions.assertNotNull(book);
        assertEquals(bookId, book.getId());
    }

    @Test
    void createBook() {
        EntityManager em = jpa.em();
        em.getTransaction().begin();

        em.persist(new Book().setTitle("The Great Gatsby"));

        em.getTransaction().commit();

        em.close();
    }

    @Test
    void findAllBooks() {
        List<Book> books = jpa.em()
                .createQuery("select b from Book b", Book.class)
                .getResultList();
        assertEquals(2, books.size());
    }

    @Test
    void createAndGetBook() {
        Book savedBook = jpa.doInTxWithReturn(em -> {
            final Book newBook = new Book().setTitle("do In Tx");
            em.persist(newBook);
            return newBook;
        });

        final Book book = jpa.em().find(Book.class, savedBook.getId());

        Assertions.assertNotSame(savedBook, book);
        assertEquals(savedBook.getId(), book.getId());
        assertEquals(savedBook.getTitle(), book.getTitle());
    }

    @Test
    void updateBookTitle() {
        final int bookId = 9901;

        jpa.doInTx(em -> {

            final Book book = em.find(Book.class, bookId);
            book.setTitle(book.getTitle().toUpperCase());

        });

        //@formatter:off
        final Book book = jpa
                .em()
                    .createQuery("select b from Book b where b.id = :id", Book.class)
                    .setParameter("id", bookId)
                    .setHint(QueryHints.FETCH_SIZE, 1)
                    .setHint(QueryHints.READ_ONLY, true)
                    .getSingleResult();
        //@formatter:on

        assertEquals("Mastering Java 11".toUpperCase(), book.getTitle());
    }
}
