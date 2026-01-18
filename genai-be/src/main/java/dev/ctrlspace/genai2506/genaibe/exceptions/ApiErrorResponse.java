package dev.ctrlspace.genai2506.genaibe.exceptions;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorResponse {
    private Instant timestamp;
    private int status;
    private String errorCode;
    private String errorMessage;
}
