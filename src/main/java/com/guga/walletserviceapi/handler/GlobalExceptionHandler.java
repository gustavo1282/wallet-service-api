package com.guga.walletserviceapi.handler;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import com.guga.walletserviceapi.exception.ResourceBadRequestException;
import com.guga.walletserviceapi.exception.ResourceNotFoundException;
import com.guga.walletserviceapi.model.enums.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LogManager.getLogger(GlobalExceptionHandler.class);

    // =====================================================
    // BUILDER PADRÃO
    // =====================================================

    private ErrorResponse buildError(HttpStatus status,
                                    ErrorCode code,
                                    String message,
                                    HttpServletRequest request) {

        String traceId = Optional.ofNullable(ThreadContext.get("traceId"))
                .orElse(UUID.randomUUID().toString());

        String path = resolvePath(request);      // ✅ novo
        String source = resolveSource(request);  // ✅ novo

        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code.getCode(),
                message,
                source,   // ✅ novo
                path,     // ✅ novo
                traceId,
                Instant.now()
        );
    }

    private String resolvePath(HttpServletRequest request) {
        if (request == null) return null;
        String method = request.getMethod();
        String uri = request.getRequestURI();
        return method + " " + uri;
    }

    /**
     * Tenta obter Controller.method quando o erro passou pelo DispatcherServlet (MVC).
     * Fallback: "METHOD URI".
     *
     * Obs: em 401/403 gerados por Security, muitas vezes não há HandlerMethod.
     */
    private String resolveSource(HttpServletRequest request) {
        if (request == null) return null;

        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (handler instanceof HandlerMethod hm) {
            return hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName();
        }

        // fallback (útil p/ errors que não passaram pelo HandlerMethod)
        return resolvePath(request);
    }

    // =====================================================
    // 400 - VALIDAÇÃO (@Valid)
    // =====================================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Validation error");

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(buildError(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    message,
                    request
            ));
    }


    // =====================================================
    // 400 - REGRA DE NEGÓCIO
    // =====================================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {

        // Você pode extrair uma mensagem mais amigável ou usar uma fixa
        String detail = "JSON mal formatado ou com tipos inválidos";

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST,
                        ex.getMessage(),
                        request
                ));

    }



    // =====================================================
    // 400 - REGRA DE NEGÓCIO
    // =====================================================

    @ExceptionHandler(ResourceBadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(ResourceBadRequestException ex,
                                                            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(
                        HttpStatus.BAD_REQUEST,
                        ErrorCode.BAD_REQUEST,
                        ex.getMessage(),
                        request
                ));
    }

    // =====================================================
    // 404 - NÃO ENCONTRADO
    // =====================================================

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex,
                                                        HttpServletRequest request) {

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(buildError(
                    HttpStatus.NOT_FOUND,
                    ErrorCode.NOT_FOUND,
                    ex.getMessage(),
                    request
            ));
    }

    // =====================================================
    // 401 - NÃO AUTENTICADO
    // =====================================================

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex,
                                                              HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(
                        HttpStatus.UNAUTHORIZED,
                        ErrorCode.UNAUTHORIZED,
                        ex.getMessage(),
                        request
                ));
    }

    // =====================================================
    // 403 - ACESSO NEGADO
    // =====================================================

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
                                                            HttpServletRequest request) {

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(
                        HttpStatus.FORBIDDEN,
                        ErrorCode.FORBIDDEN,
                        "Access denied.",
                        request
                ));
    }

    // =====================================================
    // 409 - CONFLITO (INTEGRIDADE)
    // =====================================================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {

        LOGGER.warn("Data integrity violation", ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(
                        HttpStatus.CONFLICT,
                        ErrorCode.CONFLICT,
                        "Data conflict. Please verify provided information.",
                        request
                ));
    }

    // =====================================================
    // 500 - ERRO INTERNO
    // =====================================================

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex,
                                                       HttpServletRequest request) {

        String messageEx = ex.getMessage().substring(0, Math.min(ex.getMessage().length(), 50));

        LOGGER.error("Unexpected internal error", messageEx);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        ErrorCode.INTERNAL_ERROR,
                        messageEx,
                        request
                ));
    }
}