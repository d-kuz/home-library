package com.example.demo.data;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "authors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Author {
    @Id
    @GeneratedValue
    Long authorId;

    @Version
    private Long version;

    @Column(unique = true, nullable = false)
    String name;

    @OneToMany(mappedBy = "author", cascade = CascadeType.ALL)
    @Builder.Default
    private Set<Book> books = new HashSet<>();


    public boolean addBook(Book book) {
        boolean added = this.books.add(book);
        if (added) {
            book.setAuthor(this);
        }
        return added;
    }

    public boolean addBookAll(List<Book> books2) {
        boolean added = books.addAll(books2);
        return added;
    }


    public boolean removeBook(Book book) {
        boolean removed = this.books.remove(book);
        if (removed) {
            book.setAuthor(null);
        }
        return removed;
    }

}
