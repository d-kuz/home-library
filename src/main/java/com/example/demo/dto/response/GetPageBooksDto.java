package com.example.demo.dto.response;

import com.example.demo.data.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetPageBooksDto {
    Page<Book> books;
}
