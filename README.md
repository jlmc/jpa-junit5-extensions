![Java CI with Maven](https://github.com/jlmc/jpa-junit5-extensions/workflows/Java%20CI%20with%20Maven/badge.svg?branch=master)

# JPA JUnit 5 Extensions

This extension allow you to run Jpa integration tests without having to start any EntityManagerFactory for each test class.

## How to use it?

Add the following dependency to your project:

```xml
<dependency>
  <groupId>io.github.jlmc</groupId>
  <artifactId>jpa-junit5-extension</artifactId>
  <version>1.0</version>
  <scope>test</scope>
</dependency>
```

---
**NOTE**: 

This project depends on:
- javax.persistence-api 2.2
- JUnit Jupiter 5.6.2

---

Add the `@JpaTest` annotation to your test class. By default, Jpa `EntityManagerFactory` will be started in the callback `BeforeAllCallback#beforeAll`.
Using the `@JpaTest` annotation to your test class, allows you to inject an instance of `EntityManager`, `EntityManagerFactory` or `JpaProvider` for each execution of the test methods:

```java
@JpaTest( persistenceUnit = "it" )
class JUnit5Test {

    @JpaContext
    private JpaProvider jpa;

    @Test
    void createBook() {
        EntityManager em = jpa.em();
        em.getTransaction().begin();

        em.persist(new Book().setTitle("The Great Gatsby"));

        em.getTransaction().commit();

        em.close();
    }
}
```