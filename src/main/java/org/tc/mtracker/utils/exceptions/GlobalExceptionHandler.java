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
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex, HttpServletRequest request) {
        return buildProblem(ex.getStatus(), ex.getMessage(), ex.getErrorCode(), request);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(HttpServletRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "bad_credentials", request);
    }

    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex, HttpServletRequest request) {
        log.warn("JWT processing error: {}", ex.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid or expired token.", "invalid_reset_token", request);
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ProblemDetail handleJwtAuthenticationException(JwtAuthenticationException ex, HttpServletRequest request) {
        log.warn("JWT authentication failed: {} - {}", ex.getErrorCode(), ex.getMessage());
        return buildProblem(
                HttpStatus.UNAUTHORIZED,
                ex.getMessage(),
                ex.getErrorCode(),
                request
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ProblemDetail handleUsernameNotFound(HttpServletRequest request) {
        return buildProblem(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "bad_credentials", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return buildValidationProblem(request, errors);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Method {} not supported for {}", ex.getMethod(), request.getRequestURI());

        return buildProblem(
                HttpStatus.METHOD_NOT_ALLOWED,
                "HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.",
                "method_not_allowed",
                request
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(resolveConstraintViolationPath(violation), violation.getMessage());
        }
        return buildValidationProblem(request, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (ParameterValidationResult validationResult : ex.getParameterValidationResults()) {
            if (validationResult instanceof ParameterErrors parameterErrors) {
                for (FieldError fieldError : parameterErrors.getFieldErrors()) {
                    errors.put(fieldError.getField(), fieldError.getDefaultMessage());
                }
                for (ObjectError globalError : parameterErrors.getGlobalErrors()) {
                    errors.put(resolveParameterPath(validationResult), globalError.getDefaultMessage());
                }
                continue;
            }

            String parameterPath = resolveParameterPath(validationResult);
            for (MessageSourceResolvable resolvableError : validationResult.getResolvableErrors()) {
                errors.put(parameterPath, resolvableError.getDefaultMessage());
            }
        }

        for (MessageSourceResolvable crossParameterError : ex.getCrossParameterValidationResults()) {
            errors.put("parameters", crossParameterError.getDefaultMessage());
        }

        return buildValidationProblem(request, errors);
    }


    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
    })
    public ProblemDetail handleMalformedRequest(HttpServletRequest request) {
        return buildProblem(
                HttpStatus.BAD_REQUEST,
                "Malformed request.",
                "malformed_request", request);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message = ex.getMessage();
        String cleanedMessage = cleanEnumErrorMessage(message);

        return buildProblem(
                HttpStatus.BAD_REQUEST,
                cleanedMessage,
                "invalid_value",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildProblem(
                HttpStatus.NOT_FOUND,
                "Endpoint not found.",
                "endpoint_not_found",
                request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSizeExceeded(HttpServletRequest request) {
        return buildProblem(
                HttpStatus.CONTENT_TOO_LARGE,
                "Uploaded file is too large.",
                "payload_too_large",
                request
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ProblemDetail handleMultipartException(HttpServletRequest request) {
        return buildProblem(HttpStatus.BAD_REQUEST, "Invalid multipart request.", "invalid_multipart_request", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String causeMessage = ex.getMostSpecificCause().getMessage();
        log.warn("Data integrity violation for {} {}: {}", request.getMethod(), request.getRequestURI(), causeMessage);
        return buildProblem(HttpStatus.CONFLICT, "Request conflicts with existing data.", "data_conflict", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(HttpServletRequest request) {
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

    private String resolveConstraintViolationPath(ConstraintViolation<?> violation) {
        StringBuilder path = new StringBuilder();

        for (Path.Node node : violation.getPropertyPath()) {
            if (!isValidationPathNode(node)) {
                continue;
            }

            appendPathSegment(path, node.getName());
            appendContainerReference(path, node);
        }

        return !path.isEmpty() ? path.toString() : "request";
    }

    private boolean isValidationPathNode(Path.Node node) {
        return switch (node.getKind()) {
            case PARAMETER, PROPERTY, CONTAINER_ELEMENT, CROSS_PARAMETER, RETURN_VALUE -> true;
            default -> false;
        };
    }

    private String resolveParameterPath(ParameterValidationResult validationResult) {
        String parameterName = validationResult.getMethodParameter().getParameterName();
        if (parameterName == null || parameterName.isBlank()) {
            parameterName = "arg" + validationResult.getMethodParameter().getParameterIndex();
        }

        StringBuilder path = new StringBuilder(parameterName);
        appendIndexedReference(path, validationResult.getContainerKey(), validationResult.getContainerIndex());
        return path.toString();
    }

    private void appendPathSegment(StringBuilder path, String segment) {
        if (segment == null || segment.isBlank() || segment.startsWith("<")) {
            return;
        }

        if (!path.isEmpty()) {
            path.append('.');
        }
        path.append(segment);
    }

    private void appendContainerReference(StringBuilder path, Path.Node node) {
        appendIndexedReference(path, node.getKey(), node.getIndex());
    }

    private void appendIndexedReference(StringBuilder path, Object key, Integer index) {
        if (index != null) {
            path.append('[').append(index).append(']');
            return;
        }

        if (key != null) {
            path.append('[').append(key).append(']');
        }
    }

    private String cleanEnumErrorMessage(String originalMessage) {
        if (originalMessage == null) {
            return "Malformed request.";
        }

        Pattern enumPattern = Pattern.compile("Cannot deserialize value of type `(.*?)`");
        Matcher enumMatcher = enumPattern.matcher(originalMessage);
        String enumName = "field";

        if (enumMatcher.find()) {
            String fullName = enumMatcher.group(1);
            enumName = fullName.substring(fullName.lastIndexOf('.') + 1);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < enumName.length(); i++) {
                if (enumName.charAt(i) == Character.toUpperCase(enumName.charAt(i))) {
                    sb.append(" ");
                }
                sb.append(enumName.charAt(i));
            }
            enumName = sb.toString();

        }

        if (originalMessage.contains("empty String") || originalMessage.contains("from String \"\"")) {
            return String.format("%s cannot be empty.", enumName);
        }

        if (originalMessage.contains("null")) {
            return String.format("%s cannot be null.", enumName);
        }

        Pattern valuesPattern = Pattern.compile("accepted for Enum class: \\[(.*?)]");
        Matcher valuesMatcher = valuesPattern.matcher(originalMessage);

        if (valuesMatcher.find()) {
            String invalidValue = extractInvalidValue(originalMessage);
            String allowedValues = valuesMatcher.group(1);
            return String.format("Invalid %s '%s'. Allowed values: [%s]",
                    enumName, invalidValue, allowedValues);
        }

        return "Malformed request.";
    }

    private String extractInvalidValue(String message) {
        Pattern pattern = Pattern.compile("from String \"(.*?)\"");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
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
