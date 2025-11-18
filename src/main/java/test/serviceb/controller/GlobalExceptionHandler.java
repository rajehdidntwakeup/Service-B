package test.serviceb.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * GlobalExceptionHandler is a centralized exception handling component that
 * handles exceptions globally for REST controllers. It provides specific methods
 * to capture and process commonly encountered exceptions during request execution and
 * return structured error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles MethodArgumentNotValidException and returns an appropriate response entity
   * with detailed validation error messages.
   *
   * @param exception the MethodArgumentNotValidException thrown when validation fails
   * @return a ResponseEntity with a body containing the HTTP status code, error message,
   * and a map of field-specific validation errors
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException exception) {
    // Collect all field errors
    Map<String, String> errors = exception.getBindingResult().getFieldErrors()
        .stream()
        .collect(Collectors.toMap(
            FieldError::getField,
            DefaultMessageSourceResolvable::getDefaultMessage,
            (a, b) -> a));

    Map<String, Object> body = new HashMap<>();
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
    body.put("message", "Validation failed");
    body.put("errors", errors);

    return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
  }
}
