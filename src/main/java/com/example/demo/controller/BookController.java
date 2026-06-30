package com.example.demo.controller;

import com.example.demo.dto.request.CreateAuthorDto;
import com.example.demo.dto.request.CreateBookDto;
import com.example.demo.dto.response.GetBookDto;
import com.example.demo.exception.CreateException;
import com.example.demo.exception.DeleteException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.service.BookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

    @PostMapping("/author")
    public ResponseEntity createAuthor(@RequestBody CreateAuthorDto author) throws CreateException {
        bookService.addAuthor(author);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity createBooks(@RequestBody List<CreateBookDto> book){
        bookService.addBookList(book);
        return  ResponseEntity.ok().build();
    }


    @GetMapping("/{bookId}")
    public ResponseEntity<GetBookDto> getBook(@PathVariable Long bookId) throws NotFoundException {
        return ResponseEntity.ok(bookService.findById(bookId));
    }

    @DeleteMapping("/delete/{bookId}")
    public ResponseEntity deleteBook(@PathVariable Long bookId) throws DeleteException {
        bookService.deleteBook(bookId);
        return ResponseEntity.ok().build();
    }

}
