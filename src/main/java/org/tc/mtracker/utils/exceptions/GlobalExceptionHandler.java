package org.tc.mtracker.utils.exceptions;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return buildProblem(ex.getStatus(), ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "bad_credentials", request);
    }

    @ExceptionHandler({JwtException.class, UsernameNotFoundException.class})
    public ProblemDetail handleJwtException(Exception ex, HttpServletRequest request) {
        log.warn("Token processing failed for {} {}: {}", request.getMethod(), request.getRequestURI(), ex.getMessage());
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid or expired token.", "invalid_token", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildValidationProblem(request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(resolveConstraintViolationName(violation), violation.getMessage());
        }
        return buildValidationProblem(request, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (ParameterErrors beanResult : ex.getBeanResults()) {
            addFieldErrors(errors, beanResult.getFieldErrors());
            addGlobalErrors(errors, resolveParameterName(beanResult), beanResult.getGlobalErrors());
        }

        for (ParameterValidationResult valueResult : ex.getValueResults()) {
            addResolvableErrors(errors, resolveParameterName(valueResult), valueResult.getResolvableErrors());
        }

        for (MessageSourceResolvable crossParameterError : ex.getCrossParameterValidationResults()) {
            errors.put("parameters", crossParameterError.getDefaultMessage());
        }

        return buildValidationProblem(request, errors);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class
    })
    public ProblemDetail handleMalformedRequest(Exception ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Malformed request.", "malformed_request", request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildProblem(
                HttpStatus.CONTENT_TOO_LARGE,
                "Uploaded file is too large.",
                "payload_too_large",
                request
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(MultipartException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid multipart request.", "invalid_multipart_request", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String causeMessage = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation for {} {}: {}", request.getMethod(), request.getRequestURI(), causeMessage);
        return buildProblem(HttpStatus.CONFLICT, "Request conflicts with existing data.", "data_conflict", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.FORBIDDEN, "Access denied.", "access_denied", request);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildProblem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                "internal_server_error",
                request
        );
    }

    @ExceptionHandler(UserUpdateProfileException.class)
    public ProblemDetail handleUserUpdateProfileException(UserUpdateProfileException ex, HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, ex.getMessage(), "user_update_profile_failed", request);
    }

    private ProblemDetail buildValidationProblem(HttpServletRequest request, Map<String, String> errors) {
        ProblemDetail problemDetail = buildProblem(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                "validation_failed",
                request
        );
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    private String resolveConstraintViolationName(ConstraintViolation<?> violation) {
        String name = "request";
        for (Path.Node node : violation.getPropertyPath()) {
            if (isNamedNode(node)) {
                name = node.getName();
            }
        }
        return name;
    }

    private void addFieldErrors(Map<String, String> errors, Iterable<FieldError> fieldErrors) {
        for (FieldError fieldError : fieldErrors) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }

    private void addGlobalErrors(Map<String, String> errors, String key, Iterable<ObjectError> globalErrors) {
        for (ObjectError globalError : globalErrors) {
            errors.put(key, globalError.getDefaultMessage());
        }
    }

    private void addResolvableErrors(
            Map<String, String> errors,
            String key,
            Iterable<? extends MessageSourceResolvable> resolvableErrors
    ) {
        for (MessageSourceResolvable resolvableError : resolvableErrors) {
            errors.put(key, resolvableError.getDefaultMessage());
        }
    }

    private String resolveParameterName(ParameterValidationResult validationResult) {
        String parameterName = validationResult.getMethodParameter().getParameterName();
        if (parameterName == null || parameterName.isBlank()) {
            return "arg" + validationResult.getMethodParameter().getParameterIndex();
        }
        return parameterName;
    }

    private boolean isNamedNode(Path.Node node) {
        return node.getName() != null
                && !node.getName().isBlank()
                && !node.getName().startsWith("<");
    }

    private ProblemDetail buildProblem(HttpStatus status, String detail, String code, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(status.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty("code", code);
        problemDetail.setProperty("timestamp", Instant.now().toString());
        return problemDetail;
    }
}
