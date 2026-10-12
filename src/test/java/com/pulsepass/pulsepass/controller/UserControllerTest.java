package com.pulsepass.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.pulsepass.dto.response.UserResponse;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest(
                "juanito99",
                "juan@example.com",
                "Juan",
                "Pérez",
                "3001234567",
                "Bogotá",
                LocalDate.of(1995, 6, 15)
        );
    }

    private UserResponse buildUser() {
        return new UserResponse(1L, "juanito99", "juan@example.com", true,
                "Juan", "Pérez", "3001234567", "Bogotá", LocalDate.of(1995, 6, 15));
    }

    // TEST-CTRL-USR-001: registrar usuario válido → 201
    @Test
    @DisplayName("POST /api/users con request válido retorna 201 Created")
    void register_validRequest_returns201() throws Exception {
        when(userService.register(any())).thenReturn(buildUser());

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("juanito99"))
                .andExpect(jsonPath("$.email").value("juan@example.com"));

        verify(userService).register(any());
    }

    // TEST-CTRL-USR-002: email inválido → 400
    @Test
    @DisplayName("POST /api/users con email inválido retorna 400 Bad Request")
    void register_invalidEmail_returns400() throws Exception {
        String invalidJson = """
                {
                    "username": "juanito99",
                    "email": "no-es-email",
                    "firstName": "Juan",
                    "lastName": "Pérez",
                    "birthDate": "1995-06-15"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isMap());

        verify(userService, never()).register(any());
    }

    // TEST-CTRL-USR-003: username duplicado → 409
    @Test
    @DisplayName("POST /api/users con username duplicado retorna 409 Conflict")
    void register_duplicateUsername_returns409() throws Exception {
        when(userService.register(any()))
                .thenThrow(new DuplicateResourceException("Username already exists: juanito99"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));

        verify(userService).register(any());
    }

    // TEST-CTRL-USR-004: consultar por email → 200
    @Test
    @DisplayName("GET /api/users/by-email?email=... con usuario existente retorna 200 OK")
    void findByEmail_existingUser_returns200() throws Exception {
        when(userService.findByEmail("juan@example.com")).thenReturn(buildUser());

        mockMvc.perform(get("/api/users/by-email").param("email", "juan@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("juan@example.com"))
                .andExpect(jsonPath("$.username").value("juanito99"));

        verify(userService).findByEmail("juan@example.com");
    }

    // TEST-CTRL-USR-005: consultar por username → 200
    @Test
    @DisplayName("GET /api/users/by-username?username=... con usuario existente retorna 200 OK")
    void findByUsername_existingUser_returns200() throws Exception {
        when(userService.findByUsername("juanito99")).thenReturn(buildUser());

        mockMvc.perform(get("/api/users/by-username").param("username", "juanito99"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("juanito99"))
                .andExpect(jsonPath("$.active").value(true));

        verify(userService).findByUsername("juanito99");
    }
}
