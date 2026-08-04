package com.example.demo.repository;

import com.example.demo.data.Author;
import com.example.demo.data.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findBookByTitle(String title);

    List<Book> findByAuthorAndTitleIn(Author author, List<String> titles);

    @Query("SELECT b FROM Book b WHERE " +
            "(:author IS NULL OR b.author = :author) AND " +
            "(:title IS NULL OR b.title = :title)")
    List<Book> findBooksFlexible(@Param("author") Author author, @Param("title") String title);

}
