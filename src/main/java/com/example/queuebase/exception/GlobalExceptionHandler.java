package com.example.queuebase.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(InvalidTokenOperationException.class)
	public ResponseEntity<ApiError> handleInvalidTokenOperation(InvalidTokenOperationException exception) {
		return error(HttpStatus.CONFLICT, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleRequestValidation(MethodArgumentNotValidException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(fieldError -> details.put(fieldError.getField(), fieldError.getDefaultMessage()));
		return error(HttpStatus.BAD_REQUEST, "Request validation failed.", details);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiError> handleConstraintValidation(ConstraintViolationException exception) {
		Map<String, String> details = new LinkedHashMap<>();
		exception.getConstraintViolations().forEach(violation ->
			details.put(violation.getPropertyPath().toString(), violation.getMessage()));
		return error(HttpStatus.BAD_REQUEST, "Request validation failed.", details);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleInvalidArgument(IllegalArgumentException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiError> handleDataConflict() {
		return error(HttpStatus.CONFLICT, "The requested change conflicts with existing records.", Map.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpectedError() {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", Map.of());
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> details) {
		return ResponseEntity.status(status)
				.body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, details));
	}

	public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> details) {
	}
}