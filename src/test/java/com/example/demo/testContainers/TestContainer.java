package com.example.demo.testContainers;


import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import com.example.demo.service.BookService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
@SpringBootTest
@Log4j2
public class TestContainer {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
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
    private BookRepository bookRepository;
    @Autowired
    private AuthorRepository authorRepository;

    @Autowired
    BookService bookService;

    @Autowired
    CacheManager cacheManager;

    public Author mainAuthor;
    public Author mainAuthor2;
    public CreateBookDto bookDto1;
    public CreateBookDto bookDto2;

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
        cacheManager.getCache("author").clear();
        mainAuthor = Author.builder().name("author").build();
        mainAuthor2 = Author.builder().name("author2").build();
        bookDto1 = CreateBookDto.builder().title("book1").author("author").build();
        bookDto2 = CreateBookDto.builder().title("book2").author("author2").build();
    }

    @Test
    void testFindAllEmpty() {
        // Проверяем, что репозиторий пуст перед добавлением
        List<Book> books = bookRepository.findAll();
        Assertions.assertTrue(books.isEmpty());
    }

    @Test
    void testAddListBook(){
        try {
            List<Book> books = bookService.addBookList(new ArrayList<>(List.of(bookDto1,bookDto2)));
            List<Author> authors = authorRepository.findAll();
            Assertions.assertEquals(2, authors.size());
            Assertions.assertEquals(2, books.size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

}
