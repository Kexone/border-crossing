package com.prorocketeers.jakub.app.exception;

import com.prorocketeers.jakub.app.api.model.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoLandRouteException.class)
    ResponseEntity<ErrorResponse> handleNoLandRoute(NoLandRouteException e) {
        return ResponseEntity.badRequest().body(error(ErrorCode.NO_LAND_ROUTE, e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        var message = e.getConstraintViolations().stream()
                .map(violation -> parameterName(violation) + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(error(ErrorCode.INVALID_COUNTRY_CODE, message));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("Unhandled exception while processing request", e);
        return ResponseEntity.internalServerError()
                .body(error(ErrorCode.INTERNAL_ERROR, "Unexpected server error"));
    }

    private static ErrorResponse error(ErrorCode code, String message) {
        return new ErrorResponse(code.name(), message);
    }

    private static String parameterName(ConstraintViolation<?> violation) {
        String name = null;
        for (var node : violation.getPropertyPath()) {
            name = node.getName();
        }
        return name;
    }
}
