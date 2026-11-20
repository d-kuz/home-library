package com.example.demo.testContainers;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.dto.request.FilterBookDto;
import com.example.demo.dto.request.UpdateBookDto;
import com.example.demo.exception.DeleteException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import com.example.demo.service.AuthorService;
import com.example.demo.service.BookService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Log4j2
@Testcontainers
@SpringBootTest
class ServiceBookNegTests {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testsevrisedb")
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
    @MockitoSpyBean
    AuthorRepository authorRepository;

    @Autowired
    BookRepository bookRepository;

    @Autowired
    BookService bookService;

    @Autowired
    CacheManager cacheManager;

    @Autowired
    private AuthorService authorService;

    public Author mainAuthor;
    public CreateBookDto bookDto1;
    public CreateBookDto bookDto2;
    public Book testBook;

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
        cacheManager.getCache("author").clear();
        mainAuthor = Author.builder().name("author").build();
        bookDto1 = CreateBookDto.builder().title("book1").author("author")
                .description("description").location("location").build();
        bookDto2 = CreateBookDto.builder().title("book2").author("author").build();
        testBook = Book.builder().title("book1").author(mainAuthor).description("description")
                .location("location").build();
        mainAuthor.getBooks().add(testBook);

    }

    @Test
    void testAddAuthor_WithNullDto() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            bookService.addAuthor(null);
        });

        Assertions.assertTrue(authorRepository.findAll().isEmpty());
    }

    @Test
    void testAddAuthor_WithNullName() {
        CreateAuthorDto invalidDto = CreateAuthorDto.builder().name(null).build();

        Assertions.assertThrows(NullPointerException.class, () -> {
            bookService.addAuthor(invalidDto);
        });

        Assertions.assertTrue(authorRepository.findAll().isEmpty());
    }


    @Test
    void testFindById_WithNullId() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            bookService.findById(null);
        });
    }

    @Test
    void testFindById_WhenBookDoesNotExist() {
        Long nonExistentId = 5L;

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () -> {
            bookService.findById(nonExistentId);
        });

        Assertions.assertEquals("Книга с ID " + nonExistentId + " не найдена", exception.getMessage());
    }

    @Test
    void testUpdateBook_WhenBookDoesNotExist() {
        Long nonExistentId = 50L;
        UpdateBookDto dto = UpdateBookDto.builder().title("New Title").build();

        NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () -> {
            bookService.updateBook(nonExistentId, dto);
        });

        Assertions.assertEquals("Книга с ID " + nonExistentId + " не найдена", exception.getMessage());
    }

    @Test
    void testUpdateBook_WithNullDto() {
        Author savedAuthor = authorRepository.save(mainAuthor);
        Book savedBook = savedAuthor.getBooks().iterator().next();

        Assertions.assertThrows(NullPointerException.class, () -> {
            bookService.updateBook(savedBook.getBookId(), null);
        });

        Book untouchedBook = bookRepository.findById(savedBook.getBookId()).get();
        Assertions.assertEquals("book1", untouchedBook.getTitle());
        Assertions.assertEquals("author", untouchedBook.getAuthor().getName());
    }

    @Test
    void testUpdateBook_WithBlankFields() throws NotFoundException {
        authorRepository.save(mainAuthor);

        UpdateBookDto dto = UpdateBookDto.builder()
                .title("    ")
                .description("")
                .location("New Location")
                .build();

        bookService.updateBook(testBook.getBookId(), dto);
        Book updatedBook = bookRepository.findById(testBook.getBookId()).get();

        Assertions.assertEquals(testBook.getTitle(), updatedBook.getTitle());
        Assertions.assertEquals(testBook.getDescription(), updatedBook.getDescription());
        Assertions.assertEquals(updatedBook.getLocation(), updatedBook.getLocation());
    }

    @Test
    void testFilterBook_WithNullOrEmptyFields() {
        FilterBookDto nullFieldsDto = FilterBookDto.builder()
                .nameAuthor(null)
                .title(null)
                .build();

        IllegalArgumentException exception1 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            bookService.filterBook(nullFieldsDto);
        });
        Assertions.assertEquals("Необходимо указать имя автора или название книги", exception1.getMessage());

        FilterBookDto emptyFieldsDto = FilterBookDto.builder()
                .nameAuthor("   ")
                .title("")
                .build();

        IllegalArgumentException exception2 = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            bookService.filterBook(emptyFieldsDto);
        });
        Assertions.assertEquals("Необходимо указать имя автора или название книги", exception2.getMessage());
    }

    @Test
    void testFilterBook_WithNonExistentAuthor() {
        FilterBookDto unknownAuthorDto = FilterBookDto.builder()
                .nameAuthor("Unknown Author")
                .build();

        Assertions.assertThrows(NoSuchElementException.class, () -> {
            bookService.filterBook(unknownAuthorDto);
        });
    }

    @Test
    void testFilterBook_WithNullDto() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            bookService.filterBook(null);
        });
    }

    @Test
    void testAddListBook_WithNullList() {

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            bookService.addBookList(null);
        });

        Assertions.assertNotNull(exception.getMessage());

        Assertions.assertTrue(authorRepository.findAll().isEmpty());
        Assertions.assertTrue(bookRepository.findAll().isEmpty());
    }

    @Test
    void testAddListBook_WhenDatabaseFails() {
        List<CreateBookDto> dtos = List.of(bookDto1, bookDto2);

        Mockito.doThrow(new RuntimeException("Simulated Database Error"))
                .when(authorRepository).findByNameIn(Mockito.anySet());

        RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> {
            bookService.addBookList(dtos);
        });
        Assertions.assertTrue(exception.getMessage().contains("Simulated Database Error"));

        Assertions.assertTrue(authorRepository.findAll().isEmpty());
        Assertions.assertTrue(bookRepository.findAll().isEmpty());
    }

    @Test
    void testAddListBook_WithNullAuthorName() {
        CreateBookDto invalidDto = CreateBookDto.builder().title("book3").author(null).build();

        List<CreateBookDto> dtos = List.of(bookDto1, invalidDto);

        Assertions.assertThrows(RuntimeException.class, () -> {
            bookService.addBookList(dtos);
        });

        Assertions.assertTrue(authorRepository.findAll().isEmpty());
        Assertions.assertTrue(bookRepository.findAll().isEmpty());
    }

}
