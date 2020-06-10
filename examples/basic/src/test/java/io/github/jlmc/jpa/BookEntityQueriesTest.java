package io.github.jlmc.jpa;

import io.github.jlmc.jpa.test.annotation.JpaContext;
import io.github.jlmc.jpa.test.annotation.JpaTest;
import io.github.jlmc.jpa.test.junit.JpaProvider;
import org.hibernate.annotations.QueryHints;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.persistence.EntityManager;
import java.util.List;

@JpaTest(
        persistenceUnit = "it",
        beforeEachQueries = {
                "insert into book (id, title) values (9901, 'Don Quixote by Miguel de Cervantes')",
                "insert into book (id, title) values (9902, 'n Search of Lost Time by Marcel Proust')",
        },
        afterEachQueries = {
                "delete from book where true"
        }
)
@DisplayNameGeneration(DisplayNameGenerator.Standard.class)
class BookEntityQueriesTest {

    @JpaContext
    private JpaProvider jpa;

    @ParameterizedTest
    @ValueSource(ints = {9901, 9902})
    void findBookById(int bookId) {
        final Book book = jpa.em().find(Book.class, bookId);
        Assertions.assertNotNull(book);
        Assertions.assertEquals(bookId, book.getId());
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
        Assertions.assertEquals(2, books.size());
    }

    @Test
    void createAndGetBook() {
        Book savedBook = jpa.doInTx(em -> {
            final Book newBook = new Book().setTitle("do In Tx");
            em.persist(newBook);
            return newBook;
        });

        final Book book = jpa.em().find(Book.class, savedBook.getId());

        Assertions.assertNotSame(savedBook, book);
        Assertions.assertEquals(savedBook.getId(), book.getId());
        Assertions.assertEquals(savedBook.getTitle(), book.getTitle());
    }

    @Test
    void updateBookTitle() {
        final int bookId = 9901;

        jpa.doInTx(em -> {

            final Book book = em.find(Book.class, bookId);
            book.setTitle(book.getTitle().toUpperCase());

        });

        final Book book = jpa
                .em()
                .createQuery("select b from Book b where b.id = :id", Book.class)
                .setParameter("id", bookId)
                .setHint(QueryHints.FETCH_SIZE, 1)
                .setHint(QueryHints.READ_ONLY, true)
                .getSingleResult();

        Assertions.assertEquals("Don Quixote by Miguel de Cervantes".toUpperCase(), book.getTitle());
    }
}