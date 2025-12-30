package com.guga.walletserviceapi.handler;

import java.time.Instant;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;

//@ControllerAdvice  // O @ControllerAdvice foi feito originalmente para tratar exceções em aplicações que retornavam Views (Model and View).

/***
 * @RestControllerAdvice é a especialização que deve ser usada quando a classe de tratamento de exceções lida 
 *                       com Controllers REST que retornam dados (JSON/XML).
                         A principal diferença é que o @RestControllerAdvice aplica o @ResponseBody 
                         automaticamente, o que significa que o valor de retorno (seu ResponseEntity) será 
                         escrito diretamente no corpo da resposta HTTP (JSON), exatamente o que você quer.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ErrorResponse buildError(HttpStatus status, String message) {
        return new ErrorResponse(
            status.value(),
            status.getReasonPhrase(),
            message,
            ThreadContext.get("traceId"),
            Instant.now()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Retorna 404
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(buildError(HttpStatus.NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(ResourceBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Retorna 400
    public ResponseEntity<ErrorResponse> handleBusinessRule(ResourceBadRequestException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildError(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    @ResponseStatus(HttpStatus.CONFLICT) // Retorna 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        //String message = "Data integrity violation: " + ex.getMostSpecificCause().getMessage();
        String message = "Data integrity violation. ";

        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            message = "The value provided for the reference ID of other tables is invalid or missing."
                    .concat(ex.getCause().getMessage());
        }

        if (ex.getMostSpecificCause() != null) {
            message = ex.getMostSpecificCause().getMessage();
        }

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(buildError(HttpStatus.CONFLICT, message));        
    }

   // 🔥 Catch-all (ESSENCIAL)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(buildError(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unexpected error occurred. " + ex.getMessage()
                )
            );
    }

    /*
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Regra de Negócio Violada",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    */

    /*
    @ExceptionHandler(WalletException.class)
    public ResponseEntity<ErrorResponse> handleWallet(WalletException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Regra de Negócio Violada",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
    */

    /*
    @ExceptionHandler(CustomerException.class)
    public ResponseEntity<ErrorResponse> handleWallet(CustomerException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Regra de Negócio Violada",
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
     */

    // 💡 DTO Simples para a resposta de erro
    //record ErrorResponse(int status, String error, String message) {}

}