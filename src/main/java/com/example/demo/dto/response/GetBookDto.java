package com.example.demo.dto.response;

import com.example.demo.data.Book;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetBookDto {
    Book book;
}
