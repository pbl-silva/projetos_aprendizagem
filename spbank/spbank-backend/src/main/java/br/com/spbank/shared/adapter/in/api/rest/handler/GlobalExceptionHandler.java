package br.com.spbank.shared.adapter.in.api.rest.handler;

import br.com.spbank.shared.application.exception.BusinessException;
import br.com.spbank.shared.application.exception.NotFoundException;
import br.com.spbank.shared.application.exception.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static final Logger LOG =
            LoggerFactory.getLogger(
                    GlobalExceptionHandler.class
            );

    private final MessageSource messages;

    public GlobalExceptionHandler(
            MessageSource messages
    ) {
        this.messages = messages;
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ProblemDetail> business(
            BusinessException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        HttpStatus status =
                ex instanceof UnauthorizedException
                        ? HttpStatus.UNAUTHORIZED
                        : ex instanceof NotFoundException
                                ? HttpStatus.NOT_FOUND
                                : HttpStatus.UNPROCESSABLE_CONTENT;

        String domain =
                domain(
                        ex,
                        request
                );

        LOG.info(
                "businessEvent=REQUEST_REJECTED "
                        + "domain={} errorCode={} path={} correlationId={}",
                domain,
                ex.getCode(),
                request.getRequestURI(),
                MDC.get("correlationId")
        );

        return response(
                ex.getCode(),
                ex.getMessageKey(),
                status,
                request,
                locale,
                domain,
                Map.of()
        );
    }

    @ExceptionHandler(
            MethodArgumentNotValidException.class
    )
    ResponseEntity<ProblemDetail> validation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        List<Map<String, String>> fields =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(error ->
                                Map.of(
                                        "field",
                                        error.getField(),
                                        "message",
                                        Objects.requireNonNullElse(
                                                error.getDefaultMessage(),
                                                "Inválido"
                                        )
                                )
                        )
                        .toList();

        return response(
                "INVALID_REQUEST",
                "request.invalid",
                HttpStatus.BAD_REQUEST,
                request,
                locale,
                domain(null, request),
                Map.of(
                        "fieldErrors",
                        fields
                )
        );
    }

    @ExceptionHandler(
            HttpMessageNotReadableException.class
    )
    ResponseEntity<ProblemDetail> malformed(
            HttpMessageNotReadableException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        return response(
                "MALFORMED_JSON",
                "request.malformed",
                HttpStatus.BAD_REQUEST,
                request,
                locale,
                domain(null, request),
                Map.of()
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingRequestHeaderException.class
    })
    ResponseEntity<ProblemDetail> invalidParameter(
            Exception ex,
            HttpServletRequest request,
            Locale locale
    ) {
        return response(
                "INVALID_PARAMETER",
                "request.invalid",
                HttpStatus.BAD_REQUEST,
                request,
                locale,
                domain(null, request),
                Map.of()
        );
    }

    @ExceptionHandler(
            DataIntegrityViolationException.class
    )
    ResponseEntity<ProblemDetail> conflict(
            DataIntegrityViolationException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        return response(
                "DATA_CONFLICT",
                "request.conflict",
                HttpStatus.CONFLICT,
                request,
                locale,
                domain(null, request),
                Map.of()
        );
    }

    @ExceptionHandler(
            NoResourceFoundException.class
    )
    ResponseEntity<ProblemDetail> resourceNotFound(
            NoResourceFoundException ex,
            HttpServletRequest request,
            Locale locale
    ) {
        return response(
                "RESOURCE_NOT_FOUND",
                "resource.not-found",
                HttpStatus.NOT_FOUND,
                request,
                locale,
                domain(null, request),
                Map.of()
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> unexpected(
            Exception ex,
            HttpServletRequest request,
            Locale locale
    ) {
        LOG.error(
                "Erro inesperado correlationId={}",
                MDC.get("correlationId"),
                ex
        );

        return response(
                "INTERNAL_ERROR",
                "request.internal",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                locale,
                domain(null, request),
                Map.of()
        );
    }

    private ResponseEntity<ProblemDetail> response(
            String code,
            String key,
            HttpStatus status,
            HttpServletRequest request,
            Locale locale,
            String domain,
            Map<String, Object> extensions
    ) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(
                        status,
                        messages.getMessage(
                                key,
                                null,
                                locale
                        )
                );

        problem.setType(
                URI.create(
                        "/problems/"
                                + code.toLowerCase(
                                        Locale.ROOT
                                )
                                .replace(
                                        '_',
                                        '-'
                                )
                )
        );

        problem.setTitle(
                messages.getMessage(
                        "problem.title",
                        null,
                        locale
                )
        );

        problem.setInstance(
                URI.create(
                        request.getRequestURI()
                )
        );

        problem.setProperty(
                "errorCode",
                code
        );

        problem.setProperty(
                "correlationId",
                MDC.get("correlationId")
        );

        problem.setProperty(
                "domain",
                domain
        );

        extensions.forEach(
                problem::setProperty
        );

        return ResponseEntity
                .status(status)
                .contentType(
                        MediaType.APPLICATION_PROBLEM_JSON
                )
                .body(problem);
    }

    private static String domain(
        Exception ex,
        HttpServletRequest request
) {

    String path =
            request.getRequestURI();

    if (path.startsWith("/api/v1/admin")) {
        return "ADMINISTRATION";
    }

    if (ex instanceof UnauthorizedException) {
        return "AUTHENTICATION";
    }

    if (path.startsWith("/api/v1/auth")) {
        return "AUTHENTICATION";
    }

    if (path.startsWith("/api/v1/accounts")
            || path.startsWith("/api/v1/banks")) {

        return "ACCOUNT";
    }

    if (path.startsWith("/api/v1/transfers")) {
        return "TRANSFER";
    }

    if (path.startsWith("/api/v1/pix")) {
        return "PIX";
    }

    return "SPBANK";
}
}