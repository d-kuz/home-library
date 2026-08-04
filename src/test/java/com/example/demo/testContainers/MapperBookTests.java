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
    public CreateBookDto bookDto1;
    public CreateBookDto bookDto2;

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
        mainAuthor = Author.builder().name("author").build();
        bookDto1 = CreateBookDto.builder().title("book1").yearOfCreation("1994")
                .location("шкаф1").author("author").description("ok").build();
        bookDto2 = CreateBookDto.builder().title("book2").author("author").build();
    }


    @Test
    void testBookDtoMapperCreateBookEntity(){
        authorRepository.save(mainAuthor);
        try {
            Book book = BookDtoMapper.createBookEntity(bookDto1, mainAuthor);

            Assertions.assertFalse(mainAuthor.getBooks().isEmpty());
            Assertions.assertEquals(bookDto1.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto1.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto1.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto1.getLocation(), book.getLocation());
            Assertions.assertEquals(bookDto1.getDescription(), book.getDescription());

        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateBookEntityWithEmpty(){
        authorRepository.save(mainAuthor);

        try {
            Book book = BookDtoMapper.createBookEntity(bookDto2, mainAuthor);

            Assertions.assertFalse(mainAuthor.getBooks().isEmpty());
            Assertions.assertEquals(bookDto2.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto2.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto2.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto2.getLocation(), book.getLocation());
            Assertions.assertEquals(bookDto2.getDescription(), book.getDescription());

        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateListBookEntity(){
        authorRepository.save(mainAuthor);

        try {
            List<Book> books = BookDtoMapper.createBooksEntity(new ArrayList<>(List.of(bookDto1)), mainAuthor);

            Assertions.assertFalse(mainAuthor.getBooks().isEmpty());
            Book book = books.getFirst();
            Assertions.assertEquals(bookDto1.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto1.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto1.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto1.getLocation(), book.getLocation());
            Assertions.assertEquals(bookDto1.getDescription(), book.getDescription());

        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateListBookEntityWithEmpty(){
        authorRepository.save(mainAuthor);

        try {
            List<Book> books = BookDtoMapper.createBooksEntity(new ArrayList<>(List.of(bookDto2)), mainAuthor);

            Assertions.assertFalse(mainAuthor.getBooks().isEmpty());
            Book book = books.getFirst();
            Assertions.assertEquals(bookDto2.getTitle(), book.getTitle());
            Assertions.assertEquals(bookDto2.getAuthor(), book.getAuthor().getName());
            Assertions.assertEquals(bookDto2.getYearOfCreation(), book.getYearOfCreation());
            Assertions.assertEquals(bookDto2.getLocation(), book.getLocation());

        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testBookDtoMapperCreateBooksEntityWithBooks(){
        authorRepository.save(mainAuthor);

        try {
            BookDtoMapper.createBooksEntity(new ArrayList<>(List.of(bookDto1, bookDto2)), mainAuthor);
            Assertions.assertFalse(mainAuthor.getBooks().isEmpty());
            Assertions.assertEquals(2, mainAuthor.getBooks().size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }


}
