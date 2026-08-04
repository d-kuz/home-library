package com.example.demo.service;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.dto.request.FilterBookDto;
import com.example.demo.dto.request.UpdateBookDto;
import com.example.demo.dto.response.AuthorDto;
import com.example.demo.dto.response.GetFilterBookDto;
import com.example.demo.dto.response.GetBookDto;
import com.example.demo.exception.DeleteException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import com.example.demo.util.BookDtoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BookService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CacheManager cacheManager;
    private final AuthorService authorService;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository, CacheManager cacheManager, AuthorService authorService) {
        this.bookRepository = bookRepository;
        this.authorRepository=authorRepository;
        this.cacheManager=cacheManager;
        this.authorService = authorService;
    }

    public AuthorDto addAuthor(CreateAuthorDto authorDto){
        Objects.requireNonNull(authorDto);
        Objects.requireNonNull(authorDto.getName());
        Author author = Author.builder().name(authorDto.getName()).build();
        return AuthorDto.builder().author(authorRepository.save(author)).build();
    }

    public GetBookDto findById(Long bookId) throws NotFoundException {
            Objects.requireNonNull(bookId);
            Book book = bookRepository.findById(bookId).orElseThrow(() -> new NotFoundException("Книга с ID " + bookId + " не найдена"));
            return GetBookDto.builder().book(book).build();
    }

    public GetBookDto updateBook(Long bookId, UpdateBookDto dto) throws NotFoundException {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Книга с ID " + bookId + " не найдена"));
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()){
            book.setTitle(dto.getTitle().trim());
        }
        if (dto.getAuthor() != null && !dto.getAuthor().trim().isEmpty()){
            book.setAuthor(allAuthorToListFromDBAndNew(Set.of(dto.getAuthor().trim())).getFirst());
        }
        if (dto.getLocation() != null && !dto.getLocation().trim().isEmpty()){
            book.setLocation(dto.getLocation().trim());
        }
        if (dto.getDescription() != null && !dto.getDescription().trim().isEmpty()){
            book.setDescription(dto.getDescription().trim());
        }

        return GetBookDto.builder().book(bookRepository.save(book)).build();

    }


    public void deleteBook(Long bookId) throws DeleteException {
        try {
            Objects.requireNonNull(bookId);
            bookRepository.deleteById(bookId);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new DeleteException("Не удалена книга с ID "+ bookId + " "+ e.getMessage());
        }
    }


    public GetFilterBookDto filterBook(FilterBookDto dto) {
        String nameAuthor = Optional.ofNullable(dto.getNameAuthor())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);

        String title = Optional.ofNullable(dto.getTitle())
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .orElse(null);

        if (nameAuthor == null && title == null) {
            throw new IllegalArgumentException("Необходимо указать имя автора или название книги");
        }

        Author author = null;
        if (nameAuthor != null) {
            author = authorRepository.getAuthorByName(dto.getNameAuthor()).orElseThrow();
        }

        List<Book> books = bookRepository.findBooksFlexible(author, dto.getTitle());

        return GetFilterBookDto.builder()
                .books(books != null ? books : Collections.emptyList())
                .build();
    }

    @Transactional
    public List<Book> addBookList(List<CreateBookDto>  bookDtos){
        HashMap<String, List<CreateBookDto>> authorsName = new HashMap<>();
        for (CreateBookDto dto: bookDtos){
            if (authorsName.containsKey(dto.getAuthor())) {
                authorsName.compute(dto.getAuthor(), (k, v) -> {
                    v.add(dto);
                    return v;
                });////compute вычисляет новое значение для ключа и значения на основе текущего значения и заменяет его
            } else {
                authorsName.put(dto.getAuthor(), new ArrayList<>(List.of(dto)));
            }
        }

        try {
            List<Author>authors = allAuthorToListFromDBAndNew(authorsName.keySet());

            List<Book> h = authors.stream().flatMap(x ->
                    getBooksToAuthor(x, authorsName.get(x.getName())).stream()).collect(Collectors.toList());

            return authorService.saveAll(authors).stream().flatMap(x -> x.getBooks().stream()).collect(Collectors.toList());
        }catch (Exception e){
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Author> allAuthorToListFromDBAndNew(Set <String> authorsName){
        List<Author> authors = new ArrayList<>(authorsName.size());
        var cache = cacheManager.getCache("author");
        Set<String> authorsNameNoCache = new HashSet<>(authorsName);

        if (cache != null){
            authors.addAll(authorsNameNoCache.stream().map(name -> cache.get(name, Author.class)).filter(Objects::nonNull).toList());
            authorsNameNoCache.removeAll(authors.stream().map(Author::getName).collect(Collectors.toSet()));
        }

        if(!authorsNameNoCache.isEmpty()) {
            List<Author> authorsFromDB = authorRepository.findByNameIn(authorsNameNoCache);// v1
            //List<Author> authors = authorRepository.getByNameIn(authorsNameNoCache);//v2
            authors.addAll(authorsFromDB);
            authorsNameNoCache.removeAll(authorsFromDB.stream().map(Author::getName).toList());

            authors.addAll(authorsNameNoCache.stream().map(this::createAuthorForBook).toList());

        }
        return authors;
    }

    private Author createAuthorForBook(String authorName){
        return authorService.save( Author.builder().name(authorName).books(new HashSet<>()).build());
    }

    public List<Book> getBooksToAuthor(Author author, List<CreateBookDto> dtos){

        List<Book> authorBooks = bookRepository.findByAuthorAndTitleIn(author, dtos.stream().map(CreateBookDto::getTitle).toList());
        Set<String> findBookTitles = authorBooks.stream().map(Book::getTitle).collect(Collectors.toSet());

        List<CreateBookDto> newDtos = dtos.stream()
                .filter(dto -> !findBookTitles.contains(dto.getTitle()))
                .collect(Collectors.toList());
        if (newDtos.isEmpty()) {
            return new ArrayList<>();
        }
        List<Book> newBooks = BookDtoMapper.createBooksEntity(newDtos, author);

        return  newBooks;
    }


}
