package com.example.bankingaccountservice.exception;

import com.example.bankingaccountservice.dto.ErrorResponse;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@ResponseBody
public class GlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleAccountNotFound(AccountNotFoundException exception) {
        return buildErrorResponse("ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidAccountException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidAccount(InvalidAccountException exception) {
        return buildErrorResponse("INVALID_ACCOUNT", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return buildErrorResponse("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException exception) {
        return buildErrorResponse("INVALID_REQUEST_BODY", "Request body is invalid or contains unsupported values");
    }

    private ErrorResponse buildErrorResponse(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
