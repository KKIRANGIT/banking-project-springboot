package com.example.bankingpaymentservice.exception;

import com.example.bankingpaymentservice.dto.ErrorResponse;
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

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleTransactionNotFound(TransactionNotFoundException exception) {
        return buildErrorResponse("TRANSACTION_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(InvalidTransactionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidTransaction(InvalidTransactionException exception) {
        return buildErrorResponse("INVALID_TRANSACTION", exception.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInsufficientFunds(InsufficientFundsException exception) {
        return buildErrorResponse("INSUFFICIENT_FUNDS", exception.getMessage());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExists(UserAlreadyExistsException exception) {
        return buildErrorResponse("USER_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException exception) {
        return buildErrorResponse("INVALID_CREDENTIALS", exception.getMessage());
    }

    @ExceptionHandler(TransactionProcessingException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleTransactionProcessing(TransactionProcessingException exception) {
        return buildErrorResponse("TRANSACTION_PROCESSING_ERROR", exception.getMessage());
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
