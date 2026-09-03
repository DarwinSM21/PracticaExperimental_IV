package org.uteq.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.uteq.backend.common.exception.GlobalExceptionHandler;
import org.uteq.backend.common.exception.RecursoNoEncontradoException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleApiException maneja RecursoNoEncontradoException como 404")
    void handleRecursoNoEncontrado() {
        var ex = new RecursoNoEncontradoException("No existe el recurso 123");
        ProblemDetail pd = handler.handleApiException(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).isEqualTo("No existe el recurso 123");
        assertThat(pd.getProperties()).containsKey("timestamp");
    }

    @Test
    @DisplayName("handleBadCredentials maneja BadCredentialsException como 401")
    void handleBadCredentials() {
        var ex = new BadCredentialsException("Credenciales incorrectas");
        ProblemDetail pd = handler.handleBadCredentials(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(pd.getDetail()).isEqualTo("Credenciales invalidas");
    }

    @Test
    @DisplayName("handleAccessDenied maneja AccessDeniedException como 403")
    void handleAccessDenied() {
        var ex = new AccessDeniedException("Acceso denegado");
        ProblemDetail pd = handler.handleAccessDenied(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(pd.getDetail()).contains("permisos");
    }

    @Test
    @DisplayName("handleIllegalArgument maneja IllegalArgumentException como 400")
    void handleIllegalArgument() {
        var ex = new IllegalArgumentException("Parámetro inválido");
        ProblemDetail pd = handler.handleIllegalArgument(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("Parámetro inválido");
    }

    @Test
    @DisplayName("handleIllegalState maneja IllegalStateException como 409 Conflict")
    void handleIllegalState() {
        var ex = new IllegalStateException("Estado incompatible");
        ProblemDetail pd = handler.handleIllegalState(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getDetail()).isEqualTo("Estado incompatible");
    }

    @Test
    @DisplayName("handleDataIntegrity maneja DataIntegrityViolationException como 409 y no filtra SQL")
    void handleDataIntegrity() {
        var ex = new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint 'uk_usuario'");
        ProblemDetail pd = handler.handleDataIntegrity(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getDetail()).isEqualTo("Conflicto de integridad de datos o registro duplicado");
        assertThat(pd.getDetail()).doesNotContain("uk_usuario");
    }

    @Test
    @DisplayName("handleMethodNotSupported maneja HttpRequestMethodNotSupportedException como 405")
    void handleMethodNotSupported() {
        var ex = new HttpRequestMethodNotSupportedException("PATCH");
        ProblemDetail pd = handler.handleMethodNotSupported(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(pd.getProperties().get("metodo")).isEqualTo("PATCH");
    }

    @Test
    @DisplayName("handleGeneral maneja excepciones no controladas como 500 sin stack trace")
    void handleGeneral() {
        var ex = new NullPointerException("NullPointer imprevisto");
        ProblemDetail pd = handler.handleGeneral(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getDetail()).isEqualTo("Error interno del servidor");
        assertThat(pd.getProperties().get("timestamp")).isNotNull();
    }
}
