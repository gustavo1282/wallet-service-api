package com.guga.walletserviceapi.handler;

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

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Retorna 404
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceBadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Retorna 400
    public ResponseEntity<String> handleBusinessRule(ResourceBadRequestException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.CONFLICT) // Retorna 409
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<String> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Data integrity violation: " + ex.getMostSpecificCause().getMessage();

        if (ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException) {
            message = "The value provided for the reference ID of other tables is invalid or missing."
                    .concat(ex.getCause().getMessage());
        }
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
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