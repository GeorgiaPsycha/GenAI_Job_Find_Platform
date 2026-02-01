package dev.genai.genaibe.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleAnyException(
            Exception ex,
            HttpServletRequest request
    ) {
        Instant now = Instant.now();
        if (ex instanceof GenAiException genAiEx) {
            HttpStatus status = genAiEx.getStatus();
            ApiErrorResponse body = new ApiErrorResponse(
                    now,
                    status.value(),
                    genAiEx.getErrorCode(),
                    genAiEx.getErrorMessage()
            );
            logger.warn("Handled GenAiException at {} {}: {}",
                    request.getMethod(), request.getRequestURI(), body, ex);
            return ResponseEntity.status(status).body(body);
        }
        logger.error("Unhandled exception at {} {}", request.getMethod(), request.getRequestURI(), ex);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ApiErrorResponse body = new ApiErrorResponse(
                now,
                status.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected error occurred. Please contact support if the problem persists."
        );
        return ResponseEntity.status(status).body(body);
    }
}
