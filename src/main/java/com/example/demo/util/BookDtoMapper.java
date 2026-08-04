package com.example.demo.util;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import com.example.demo.dto.request.CreateBookDto;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@UtilityClass
public class BookDtoMapper {

    public static Book createBookEntity(CreateBookDto dto, Author author){
        Book book = Book.builder().title(dto.getTitle()).author(author)
                .yearOfCreation(dto.getYearOfCreation()!=null ? LocalDate.parse(dto.getYearOfCreation()): null)
                .location(dto.getLocation()).description(dto.getDescription()).build();
        author.addBook(book);
        return book;
    }
    public static List<Book> createBooksEntity(List<CreateBookDto> dtos, Author author){
        List<Book> books= new ArrayList<>(dtos.size());
        for (CreateBookDto dto: dtos){
            Book book = Book.builder().title(dto.getTitle()).author(author)
                    .yearOfCreation(dto.getYearOfCreation()!=null ? LocalDate.parse(dto.getYearOfCreation()): null)
                    .location(dto.getLocation()).description(dto.getDescription()).build();
            books.add(book);
        }

        author.addBookAll(books);

        return books;
    }
}
