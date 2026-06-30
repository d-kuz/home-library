package com.example.demo.testContainers;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.exception.DeleteException;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import com.example.demo.service.AuthorService;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Log4j2
@Testcontainers
@SpringBootTest
class ServiceBookTests {

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

    @BeforeEach
    void setUp() {
        authorRepository.deleteAll();
        cacheManager.getCache("author").clear();
        mainAuthor = Author.builder().name("author").build();
        bookDto1 = CreateBookDto.builder().title("book1").author("author").build();
        bookDto2 = CreateBookDto.builder().title("book2").author("author").build();
    }


    @Test
    void testAddAuthor (){
        CreateAuthorDto createAuthorDto = CreateAuthorDto.builder().name("author").build();
        Author author = bookService.addAuthor(createAuthorDto);
        Assertions.assertNotNull(author);
        Assertions.assertEquals("author", author.getName());
    }

    @Test
    void testDeleteBook (){
        Book book1 = Book.builder().title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
        bookRepository.save(book1);
        try {
            Assertions.assertTrue(!bookRepository.findAll().isEmpty());
            bookService.deleteBook(book1.getBookId());
            Assertions.assertTrue(bookRepository.findAll().isEmpty());

        } catch (DeleteException e) {
            log.error(e.getMessage());
        }
    }
//    @Test
//    void testFilter (){
//        FilterBookDto filterBookDto = FilterBookDto.builder().nameAuthor("author1").build();
//        Book book1 = Book.builder().bookId(1L).title("book1").yearOfCreation(LocalDate.parse("1234-01-01")).build();
//        Book book2 = Book.builder().bookId(2L).title("book1").yearOfCreation(LocalDate.parse("1300-01-01")).build();
//        Author author1 = Author.builder().authorId(1L).name("author1").books(new HashSet<>(Set.of(book1, book2))).build();
//        authorRepository.save(author1);
//        bookRepository.save(book1);
//        bookRepository.save(book2);
//
//        try {
//            List<Book> findBooks = (List<Book>) bookService.filterBook(filterBookDto);
//            Assertions.assertTrue(!findBooks.isEmpty());
//        } catch (FilterException e) {
//            log.error(e.getMessage());
//        }
//
//    }
    @Test
    void testAddListBook(){
        Author author = Author.builder().name("author").build();
        Author author2 = Author.builder().name("author2").build();
        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto2 = CreateBookDto.builder().title("book2").author("author2").build();
        authorRepository.save(author);
        authorRepository.save(author2);
        try {
            List<Author> authors = authorRepository.findAll();
            List<Book> books = bookService.addBookList(List.of(bookDto,bookDto2));
            Assertions.assertTrue(!books.isEmpty());
            Assertions.assertEquals(2, books.size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testAddListBookWithOneAuthor(){
        Author author = Author.builder().name("author").build();
        Author author2 = Author.builder().name("author2").build();
        authorRepository.save(author);
        authorRepository.save(author2);
        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto1 = CreateBookDto.builder().title("book2").author("author").build();
        CreateBookDto bookDto2 = CreateBookDto.builder().title("book3").author("author2").build();
        try {
            List<Book> books = bookService.addBookList(List.of(bookDto, bookDto1,bookDto2));
            Assertions.assertTrue(!books.isEmpty());
            Assertions.assertEquals(3, books.size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }


    @Test
    void testAddListBookWithoutAuthor(){
        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto1 = CreateBookDto.builder().title("book2").author("author").build();
        try {
            List<Book> books = bookService.addBookList(List.of(bookDto,bookDto1));
            List<Author> authors = authorRepository.findAll();
            Assertions.assertTrue(!authors.isEmpty());
            Assertions.assertTrue(!books.isEmpty());
            Assertions.assertEquals(2, books.size());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void test2Thread(){
        Author author = Author.builder().name("author").build();
        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto1 = CreateBookDto.builder().title("book2").author("author").build();
        CreateBookDto bookDto2 = CreateBookDto.builder().title("book3").author("author").build();
        authorRepository.save(author);

        List<CreateBookDto> dto1 = List.of(bookDto, bookDto1);
        List<CreateBookDto> dto2 = List.of(bookDto, bookDto2);

        List<Book> books1 = new ArrayList<>();

        var a = CompletableFuture.supplyAsync(() -> bookService.addBookList(dto1))
                .handle((v,f) -> books1.addAll(v) );
        var b = CompletableFuture.supplyAsync(() -> bookService.addBookList(dto2))
                .handle((v,f) -> books1.addAll(v) );
        CompletableFuture.allOf(a, b).join();
        System.out.println(books1);
        Assertions.assertEquals(3, books1.size());

    }

    @Test
    void testCacheable(){
        Author author = Author.builder().name("author").build();
        Author authorSave = authorService.save(author);

        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto1 = CreateBookDto.builder().title("book2").author("author").build();
        CreateBookDto bookDto2 = CreateBookDto.builder().title("book3").author("author").build();
        CreateBookDto bookDto3 = CreateBookDto.builder().title("book4").author("author").build();

        List<Book> books = bookService.addBookList(List.of(bookDto,bookDto1));
        List<Book> books1 = bookService.addBookList(List.of(bookDto,bookDto2));
        List<Book> books2 = bookService.addBookList(List.of(bookDto1,bookDto3));

        var cache = cacheManager.getCache("author");
        Assertions.assertNotNull(cache);

        Author cachedAuthor = cache.get("author", Author.class);
        Assertions.assertNotNull(cachedAuthor);
        Assertions.assertEquals("author", cachedAuthor.getName());

        verify(authorRepository, times(0)).findByNameIn(Set.of("author"));
    }

    @Test
    void testOptLock(){
        Author author = Author.builder().name("author").build();
        CreateBookDto bookDto = CreateBookDto.builder().title("book1").author("author").build();
        CreateBookDto bookDto1 = CreateBookDto.builder().title("book2").author("author").build();
        authorRepository.save(author);
        Assertions.assertEquals(0, author.getVersion());
        try {
            List<Book> books = bookService.addBookList(List.of(bookDto,bookDto1));
            List<Author> authors = authorRepository.findAll();
            Assertions.assertNotNull(authors.getFirst().getBooks());
            System.out.println(authors.getFirst().getBooks());//меняется версия?
            Assertions.assertTrue(!authors.isEmpty());
            Assertions.assertEquals(1, authors.get(0).getVersion());
        }catch (Exception e){
            log.error(e.getMessage());
        }

    }

    @Test
    void testAddListBookMultiThread(){

        List<CreateBookDto> dtos1 = new ArrayList<>();
        List<CreateBookDto> dtos2 = new ArrayList<>();
        List<CreateBookDto> dtos3 = new ArrayList<>();
        Author author = Author.builder().name("author").build();
        Author author2 = Author.builder().name("author2").build();
        authorRepository.save(author);
        authorRepository.save(author2);
        int k = 50;
        int m = 150;
        for (int i = 0; i < k; i++) {
            dtos1.add(CreateBookDto.builder().title("book" + i).author(i%2==0 ? author.getName() : author2.getName()).build());
        }
        for (int i = k; i < m; i++) {
            dtos2.add(CreateBookDto.builder().title("book" + i).author(i%2==0 ? author.getName() : author2.getName()).build());
        }
        for (int i = m; i < 200; i++) {
            dtos3.add(CreateBookDto.builder().title("book" + i).author(i%2==0 ? author.getName() : author2.getName()).build());
        }

        List<Book> books1 = new ArrayList<>();
        List<Book> books2 = new ArrayList<>();
        List<Book> books3 = new ArrayList<>();

        var a = CompletableFuture.supplyAsync(() -> bookService.addBookList(dtos1))
                .handle((v,f) -> books1.addAll(v) );
        var b = CompletableFuture.supplyAsync(() -> bookService.addBookList(dtos2))
                .handle((v,f) -> books2.addAll(v) );
        var c = CompletableFuture.supplyAsync(() -> bookService.addBookList(dtos3))
                .handle((v,f) -> books3.addAll(v) );
        CompletableFuture.allOf(a, b, c).join();
        Assertions.assertEquals( k,books1.size());
        Assertions.assertEquals( m-k,books2.size());
        Assertions.assertEquals( 200-m,books3.size());

        List<Author> authors = authorRepository.findAll();
        Assertions.assertTrue(!authors.isEmpty());

    }

}
