package com.example.demo.service;

import com.example.demo.data.Author;
import com.example.demo.repository.AuthorRepository;
import com.example.demo.repository.BookRepository;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AuthorService {
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public AuthorService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository=authorRepository;
    }
    @CachePut(value = "author", key = "#author.name")
    public Author save(Author author){
        return authorRepository.save(author);
    }

    public List<Author> saveAll(List<Author> authors){
        List<Author> authorSave = new ArrayList<>();
        for (Author author: authors){
            authorSave.add(save(author));
        }
        return authorSave;
    }
}
