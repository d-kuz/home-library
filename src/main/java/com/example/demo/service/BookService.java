package com.example.demo.service;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.dto.request.FilterBookDto;
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

    public Author addAuthor(CreateAuthorDto authorDto){
        Objects.requireNonNull(authorDto);
        Objects.requireNonNull(authorDto.getName());
        Author author = Author.builder().name(authorDto.getName()).build();
        return authorRepository.save(author);
    }

    public GetBookDto findById(Long bookId) throws NotFoundException {
        try {
            Objects.requireNonNull(bookId);
            Optional<Book> getBook = bookRepository.findById(bookId);
            return GetBookDto.builder().book(getBook.get()).build();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new NotFoundException("книга по id: " + e.getMessage());
        }
    }


    public void deleteBook(Long bookId) throws DeleteException {
        try {
            Objects.requireNonNull(bookId);
            bookRepository.deleteById(bookId);
        }catch (Exception e){
            log.error(e.getMessage());
            throw new DeleteException("книги: " + e.getMessage());
        }
    }


    public GetFilterBookDto filterBook(FilterBookDto filterBookDto) throws NotFoundException {
        try {
            Objects.requireNonNull(filterBookDto);
            Author author = authorRepository.getAuthorByName(filterBookDto.getNameAuthor());
            if (author != null){
            List<Book> books= bookRepository.getBookByAuthorAndTitle(author,filterBookDto.getTitle());
            return GetFilterBookDto.builder().books(books).build();}
        }catch (Exception e){
            throw new  NotFoundException("страницы книги: " + e.getMessage());
        }
        return null;
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
            List<Author>authors = allAuthorToListFromDBAndNew(authorsName);

            List<Book> h = authors.stream().flatMap(x ->
                    getBooksToAuthor(x, authorsName.get(x.getName())).stream()).collect(Collectors.toList());

            List<Book> booksAuthors = authorService.saveAll(authors).stream().flatMap(x -> x.getBooks().stream()).collect(Collectors.toList());
            return booksAuthors;
        }catch (Exception e){
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    public List<Author> allAuthorToListFromDBAndNew(HashMap<String, List<CreateBookDto>> authorsName){
        List<Author> authors = new ArrayList<>(authorsName.size());
        var cache = cacheManager.getCache("author");
        Set<String> authorsNameNoCache = new HashSet<>(authorsName.keySet());

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
