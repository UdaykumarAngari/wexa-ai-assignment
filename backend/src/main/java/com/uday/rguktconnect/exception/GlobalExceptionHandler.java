package com.uday.rguktconnect.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) 
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(org.neo4j.driver.exceptions.Neo4jException.class)
    public ResponseEntity<?> handleNeo4jException(org.neo4j.driver.exceptions.Neo4jException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "Database service is temporarily unavailable. Please try again later."));
    }

}