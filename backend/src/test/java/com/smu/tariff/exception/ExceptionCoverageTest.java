package com.smu.tariff.exception;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExceptionCoverageTest {
    @Test
    void testHandleDataIntegrityViolation_nullCause() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail");
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("data integrity constraint");
    }

    @Test
    void testHandleDataIntegrityViolation_unknownCause() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail", new RuntimeException("something else"));
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("data integrity constraint");
    }

    @Test
    void testHandleDataIntegrityViolation_duplicate() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail", new RuntimeException("duplicate value error"));
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("Duplicate value");
    }

    @Test
    void testHandleDataIntegrityViolation_username() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail", new RuntimeException("users_username_key"));
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("Username is already taken");
    }

    @Test
    void testHandleDataIntegrityViolation_email() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail", new RuntimeException("users_email_key"));
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("Email is already taken");
    }
    @Test
    void testGlobalExceptionHandlerHandlesValidationException() throws NoSuchMethodException {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.validation.BindException bindEx = new org.springframework.validation.BindException(new Object(), "obj");
        org.springframework.validation.FieldError fieldError = new org.springframework.validation.FieldError("obj", "field", "bad");
        bindEx.addError(fieldError);
        // Use a real MethodParameter to avoid NPE
        class Dummy { public void foo(String arg) {} }
        java.lang.reflect.Method m = Dummy.class.getMethod("foo", String.class);
        org.springframework.core.MethodParameter param = new org.springframework.core.MethodParameter(m, 0);
        org.springframework.web.bind.MethodArgumentNotValidException ex =
            new org.springframework.web.bind.MethodArgumentNotValidException(param, bindEx);
        var resp = handler.handleValidationExceptions(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.get("validationErrors")).isInstanceOf(Map.class);
        assertThat(((Map<?,?>)resp.get("validationErrors")).get("field")).isEqualTo("bad");
    }

    @Test
    void testGlobalExceptionHandlerHandlesDataIntegrityViolation() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("fail", new RuntimeException("users_username_key"));
        ErrorResponse resp = handler.handleDataIntegrityViolation(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("Username is already taken");
    }

    @Test
    void testGlobalExceptionHandlerHandlesAuthenticationException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.security.authentication.BadCredentialsException ex = new org.springframework.security.authentication.BadCredentialsException("bad creds");
        ErrorResponse resp = handler.handleAuthenticationException(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("Invalid username or password");
    }

    @Test
    void testGlobalExceptionHandlerHandlesAccessDenied() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        org.springframework.security.access.AccessDeniedException ex = new org.springframework.security.access.AccessDeniedException("denied");
        ErrorResponse resp = handler.handleAccessDenied(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("do not have permission");
    }

    @Test
    void testGlobalExceptionHandlerHandlesRuntimeException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        RuntimeException ex = new RuntimeException("fail");
        ErrorResponse resp = handler.handleRuntimeException(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("unexpected error");
    }

    @Test
    void testGlobalExceptionHandlerHandlesGenericException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        Exception ex = new Exception("fail");
        ErrorResponse resp = handler.handleGenericException(ex, webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).contains("unexpected error");
    }
    @Test
    void testTariffNotFoundExceptionConstructors() {
        TariffNotFoundException ex1 = new TariffNotFoundException("msg");
        assertThat(ex1.getMessage()).isEqualTo("msg");
        Throwable cause = new RuntimeException("cause");
        TariffNotFoundException ex2 = new TariffNotFoundException("msg2", cause);
        assertThat(ex2.getMessage()).isEqualTo("msg2");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    void testInvalidTariffRequestExceptionConstructors() {
        InvalidTariffRequestException ex1 = new InvalidTariffRequestException("bad");
        assertThat(ex1.getMessage()).isEqualTo("bad");
        Throwable cause = new RuntimeException("fail");
        InvalidTariffRequestException ex2 = new InvalidTariffRequestException("bad2", cause);
        assertThat(ex2.getMessage()).isEqualTo("bad2");
        assertThat(ex2.getCause()).isSameAs(cause);
    }

    @Test
    void testErrorResponseGettersSetters() {
        ErrorResponse er = new ErrorResponse();
        Instant now = Instant.now();
        er.setTimestamp(now);
        er.setStatus(400);
        er.setError("err");
        er.setMessage("msg");
        er.setPath("/api");
        assertThat(er.getTimestamp()).isEqualTo(now);
        assertThat(er.getStatus()).isEqualTo(400);
        assertThat(er.getError()).isEqualTo("err");
        assertThat(er.getMessage()).isEqualTo("msg");
        assertThat(er.getPath()).isEqualTo("/api");
    }

    @Test
    void testValidationErrorResponseGettersSetters() {
        ValidationErrorResponse ver = new ValidationErrorResponse();
        Instant now = Instant.now();
        ver.setTimestamp(now);
        ver.setStatus(422);
        ver.setError("validation");
        ver.setMessage("fail");
        ver.setPath("/api/validate");
        ver.setValidationErrors(Map.of("field", "error"));
        assertThat(ver.getTimestamp()).isEqualTo(now);
        assertThat(ver.getStatus()).isEqualTo(422);
        assertThat(ver.getError()).isEqualTo("validation");
        assertThat(ver.getMessage()).isEqualTo("fail");
        assertThat(ver.getPath()).isEqualTo("/api/validate");
        assertThat(ver.getValidationErrors().get("field")).isEqualTo("error");
    }

    @Test
    void testGlobalExceptionHandlerHandlesIllegalArgument() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        ErrorResponse resp = handler.handleIllegalArgument(new IllegalArgumentException("bad arg"), webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).isEqualTo("bad arg");
    }

    @Test
    void testGlobalExceptionHandlerHandlesTariffNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        ErrorResponse resp = handler.handleTariffNotFound(new TariffNotFoundException("not found"), webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).isEqualTo("not found");
    }

    @Test
    void testGlobalExceptionHandlerHandlesInvalidTariffRequest() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        org.springframework.mock.web.MockHttpServletRequest req = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.web.context.request.ServletWebRequest webRequest = new org.springframework.web.context.request.ServletWebRequest(req);
        ErrorResponse resp = handler.handleInvalidTariffRequest(new InvalidTariffRequestException("bad req"), webRequest).getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.getMessage()).isEqualTo("bad req");
    }
}
