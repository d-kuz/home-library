package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<String> handleException(NotFoundException e) {
        return new ResponseEntity<>("Не найдено " + e.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(CreateException.class)
    public ResponseEntity<String> handleException(CreateException e) {
        return new ResponseEntity<>("Ошибка добавления " + e.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DeleteException.class)
    public ResponseEntity<String> handleException(DeleteException e) {
        return new ResponseEntity<>("Ошибка удаления " + e.getMessage(), HttpStatus.NOT_FOUND);
    }

}
