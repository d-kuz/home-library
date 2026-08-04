package com.example.demo.controller;

import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.dto.request.FilterBookDto;
import com.example.demo.dto.request.UpdateBookDto;
import com.example.demo.dto.response.AuthorDto;
import com.example.demo.dto.response.GetBookDto;
import com.example.demo.dto.response.GetBooksDto;
import com.example.demo.dto.response.GetFilterBookDto;
import com.example.demo.exception.DeleteException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.BookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping("/authors")
    public ResponseEntity<AuthorDto> createAuthor(@Valid @RequestBody CreateAuthorDto authorDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addAuthor(authorDto));
    }

    @PostMapping
    public ResponseEntity<GetBooksDto> createBooks(@Valid @RequestBody List<CreateBookDto> bookDtos) {
        return ResponseEntity.status(HttpStatus.CREATED).body((GetBooksDto) bookService.addBookList(bookDtos));
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<GetBookDto> getBook(@PathVariable Long bookId) throws NotFoundException {
        return ResponseEntity.ok(bookService.findById(bookId));
    }

    @PatchMapping("/{bookId}")
    public ResponseEntity<GetBookDto> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody UpdateBookDto updateBookDto) throws NotFoundException {
        return ResponseEntity.ok(bookService.updateBook(bookId, updateBookDto));
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long bookId) throws DeleteException {
        bookService.deleteBook(bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/filter")
    public ResponseEntity<GetFilterBookDto> filterBooks(@ModelAttribute @Valid FilterBookDto filterBookDto) {
        return ResponseEntity.ok(bookService.filterBook(filterBookDto));
    }

}
