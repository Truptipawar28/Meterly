package com.meterly.api;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns exceptions into JSON error responses (RFC 9457 "problem details"), e.g.
 * {"status":404,"title":"Not Found","detail":"Account 7 not found"}.
 * Extending ResponseEntityExceptionHandler also handles Spring's own errors
 * (validation failures, missing headers, bad JSON) in the same format.
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
    }
}
