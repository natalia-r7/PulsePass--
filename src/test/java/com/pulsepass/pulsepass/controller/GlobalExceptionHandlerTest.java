package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del contrato de ErrorResponse según QT-CTRL-011 y NFR-CTRL-003.
 *
 * Verifica que GlobalExceptionHandler produce una respuesta uniforme con los
 * campos timestamp, status, error y message en todos los casos de error,
 * y que no expone detalles internos del stack trace.
 */
@WebMvcTest(UserController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // QT-CTRL-011-A: ErrorResponse completo en 404 — todos los campos presentes
    @Test
    @DisplayName("404 retorna ErrorResponse con timestamp, status, error y message")
    void notFound_returnsCompleteErrorResponse() throws Exception {
        when(userService.findByEmail("noexiste@test.com"))
                .thenThrow(new ResourceNotFoundException("User not found: noexiste@test.com"));

        mockMvc.perform(get("/api/users/by-email").param("email", "noexiste@test.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found: noexiste@test.com"));
    }

    // QT-CTRL-011-B: JSON malformado → 400 con ErrorResponse (NFR-CTRL-003)
    @Test
    @DisplayName("JSON malformado retorna 400 con ErrorResponse uniforme")
    void malformedJson_returns400WithErrorResponse() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ esto no es JSON válido }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    // QT-CTRL-011-C: validación de campos → 400 con details no vacío
    @Test
    @DisplayName("Validación fallida retorna 400 con campo details que contiene los errores por campo")
    void validationFailed_returns400WithDetails() throws Exception {
        String bodyConErrores = """
                {
                    "username": "",
                    "email": "no-es-email",
                    "firstName": "",
                    "lastName": "Pérez",
                    "birthDate": "1995-06-15"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyConErrores))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.details").isNotEmpty());
    }

    // QT-CTRL-011-D: excepción inesperada → 500 sin exponer detalles internos (NFR-CTRL-003)
    @Test
    @DisplayName("Excepción inesperada retorna 500 con ErrorResponse y sin stack trace expuesto")
    void unexpectedException_returns500WithoutStackTrace() throws Exception {
        when(userService.findByEmail(any()))
                .thenThrow(new RuntimeException("Error interno inesperado"));

        mockMvc.perform(get("/api/users/by-email").param("email", "alguien@test.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                // El mensaje genérico no expone la causa interna
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
