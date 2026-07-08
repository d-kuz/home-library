package com.example.demo.testContainers;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import com.example.demo.util.BookDtoMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;

@Testcontainers
@Log4j2
@SpringBootTest
class MapperBookTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testmapperdb")
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

    public Author mainAuthor;
    public Author mainAuthor2;
    public CreateBookDto bookDto1;
    public CreateBookDto bookDto2;

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
        mainAuthor = Author.builder().name("author").build();
        mainAuthor2 = Author.builder().name("author2").build();
        bookDto1 = CreateBookDto.builder().title("book1").yearOfCreation("1994")
                .location("шкаф1").author("author").build();
        bookDto2 = CreateBookDto.builder().title("book2").author("author2").build();
    }


    @Test
    void testBookDtoMapperCreateBookEntity(){
        authorRepository.save(mainAuthor);
        Author author1 = authorRepository.getAuthorByName(mainAuthor.getName());

        try {
            Book book = BookDtoMapper.createBookEntity(bookDto1, author1);

            Assertions.assertTrue(!author1.getBooks().isEmpty());
            Assertions.assertEquals(bookDto1.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto1.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto1.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto1.getLocation(), book.getLocation());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateBooksEntity(){
        authorRepository.save(mainAuthor);

        Author author1 = authorRepository.getAuthorByName(mainAuthor.getName());

        try {
            Author author2 = author1;
            List<Book> books = BookDtoMapper.createBooksEntity(new ArrayList<>(List.of(bookDto1)), author2);

            Assertions.assertTrue(!author2.getBooks().isEmpty());
            Book book = books.get(0);
            Assertions.assertEquals(bookDto1.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto1.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto1.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto1.getLocation(), book.getLocation());

        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateBooksEntityWithBooks(){
        authorRepository.save(mainAuthor);

        Author author1 = authorRepository.getAuthorByName(mainAuthor.getName());
        //Hibernate.initialize(author1.getBooks()); только в транзакции

        try {
            Author author2 = author1;
            List<Book> books = BookDtoMapper.createBooksEntity(new ArrayList<>(List.of(bookDto1, bookDto2)), author2);
            Assertions.assertTrue(!author2.getBooks().isEmpty());
            Assertions.assertEquals(2, author2.getBooks().size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }




}
