package com.example.demo.repository;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Book findBookByTitle(String title);

    List<Book> findByAuthorAndTitleIn(Author author, List<String> titles);

    List<Book> getBookByAuthorAndTitle(Author author, String title);

}
