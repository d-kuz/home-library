package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilterBookDto {
    String title;
    String nameAuthor;
    String yearOfCreation;
}