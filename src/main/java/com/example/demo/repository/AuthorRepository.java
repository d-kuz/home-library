package com.example.demo.repository;

import com.example.demo.data.Author;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    Author getAuthorByName(String author);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"books"})
    List<Author> findByNameIn(Set<String> authorsName);

    @Lock(LockModeType.OPTIMISTIC)
    @EntityGraph(attributePaths = {"books"})
    List<Author> getByNameIn(Set<String> authorsName);

    @Query(value = "SELECT * FROM authors a LEFT JOIN books b ON a.author_id = b.book_id FOR UPDATE OF a SKIP LOCKED", nativeQuery = true)
    List<Author> findAuthors(Set<String> authorsName);

    @Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
    @Query("SELECT a FROM Author a LEFT JOIN FETCH a.books b WHERE a.name IN (:authorsName)")
    List<Author> findByNamesIn (Set<String> authorsName);

}
