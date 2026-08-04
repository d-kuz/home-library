package com.example.demo.testContainers;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdatajpadb")
            .withUsername("testuser")
            .withPassword("testpass");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    AuthorRepository authorRepository;

    @Autowired
    BookRepository bookRepository;


    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
    }

	@Test
	void testAddBookToAuthors() {
        Book book1 = Book.builder().title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
        Book book2 = Book.builder().title("book2").yearOfCreation(LocalDate.parse("1300-01-01")).build();
        Author author = authorRepository.save(Author.builder().name("author1").books(Set.of(book1, book2)).build());
        Assertions.assertFalse(author.getBooks().isEmpty());
        Assertions.assertEquals("author1", author.getName());

	}

    @Test
    void testAddAuthors() {
        Book book1 = Book.builder().title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
        Book book2 = Book.builder().title("book2").yearOfCreation(LocalDate.parse("1300-01-01")).build();
        Author author1 = authorRepository.save(Author.builder().name("author1").books(Set.of(book1, book2)).build());

        Author author2 = authorRepository.findById(author1.getAuthorId()).orElseThrow();
        Assertions.assertEquals(author2, author1);


    }

    @Test
    void testAddSetBookToAuthors() {
        Book book1 = Book.builder().title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
        Book book2 = Book.builder().title("book2").yearOfCreation(LocalDate.parse("1300-01-01")).build();
        Author author1 = authorRepository.save(Author.builder().name("author1").books(Set.of(book1, book2)).build());

        Author author2 = authorRepository.findById(author1.getAuthorId()).orElseThrow();
        Assertions.assertFalse(author2.getBooks().isEmpty());

    }

    @Test
    void testDeleteBook(){
        Book book1 = Book.builder().title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
        Book saveBook = bookRepository.save(book1);
        Assertions.assertFalse(bookRepository.findAll().isEmpty());
        bookRepository.deleteById(saveBook.getBookId());
        Assertions.assertTrue(bookRepository.findAll().isEmpty());
    }

}
