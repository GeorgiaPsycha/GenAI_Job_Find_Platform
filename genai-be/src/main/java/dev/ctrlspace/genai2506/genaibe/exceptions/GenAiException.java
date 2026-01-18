package dev.ctrlspace.genai2506.genaibe.exceptions;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@EqualsAndHashCode(callSuper = true)
public class GenAiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final String errorMessage;

    public GenAiException(HttpStatus status, String errorCode, String errorMessage) {
        super(errorMessage);
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public GenAiException(HttpStatus status, String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
