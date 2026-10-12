package com.pulsepass.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
import com.pulsepass.pulsepass.entity.TicketStatus;
import com.pulsepass.pulsepass.entity.TicketType;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.service.EventService;
import com.pulsepass.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// EventController se incluye para cubrir el endpoint FR-CTRL-TKT-004:
// GET /api/events/{eventCode}/tickets/paid (TEST-CTRL-TKT-007)
@WebMvcTest({TicketController.class, EventController.class})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private EventService eventService;

    private PurchaseTicketRequest validRequest() {
        return new PurchaseTicketRequest("juan@example.com", "CMF-2026", TicketType.GENERAL);
    }

    private TicketResponse buildTicket(TicketStatus status) {
        return new TicketResponse(1L, "TKT-0001", TicketType.GENERAL,
                new BigDecimal("150000"), status, LocalDateTime.now(),
                "juan@example.com", "CMF-2026", "Caribbean Music Fest 2026");
    }

    // TEST-CTRL-TKT-001: comprar ticket válido → 201
    @Test
    @DisplayName("POST /api/tickets con request válido retorna 201 Created")
    void purchase_validRequest_returns201() throws Exception {
        when(ticketService.purchase(any())).thenReturn(buildTicket(TicketStatus.PAID));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"));

        verify(ticketService).purchase(any());
    }

    // TEST-CTRL-TKT-002: request inválido (email y eventCode) → 400
    @Test
    @DisplayName("POST /api/tickets con request inválido retorna 400 Bad Request")
    void purchase_invalidRequest_returns400() throws Exception {
        String invalidJson = """
                {
                    "userEmail": "no-es-email",
                    "eventCode": ""
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isMap());

        verify(ticketService, never()).purchase(any());
    }

    // TEST-CTRL-TKT-003: usuario o evento inexistente → 404
    @Test
    @DisplayName("POST /api/tickets con usuario inexistente retorna 404 Not Found")
    void purchase_nonExistingUser_returns404() throws Exception {
        when(ticketService.purchase(any()))
                .thenThrow(new ResourceNotFoundException("User not found: juan@example.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(ticketService).purchase(any());
    }

    // TEST-CTRL-TKT-004: evento no publicado o sin cupos → 409
    @Test
    @DisplayName("POST /api/tickets con regla de negocio violada retorna 409 Conflict")
    void purchase_businessRuleViolation_returns409() throws Exception {
        when(ticketService.purchase(any()))
                .thenThrow(new BusinessRuleException("Event is not published or has no available capacity"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(ticketService).purchase(any());
    }

    // TEST-CTRL-TKT-005: consultar ticket por código → 200
    @Test
    @DisplayName("GET /api/tickets/{ticketCode} con ticket existente retorna 200 OK")
    void findByCode_existingTicket_returns200() throws Exception {
        when(ticketService.findByCode("TKT-0001")).thenReturn(buildTicket(TicketStatus.PAID));

        mockMvc.perform(get("/api/tickets/TKT-0001"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.userEmail").value("juan@example.com"));

        verify(ticketService).findByCode("TKT-0001");
    }

    // TEST-CTRL-TKT-006: consultar tickets por usuario → 200
    @Test
    @DisplayName("GET /api/tickets/by-user?email=... retorna 200 OK con lista de tickets del usuario")
    void findByUserEmail_returns200() throws Exception {
        when(ticketService.findByUserEmail("juan@example.com"))
                .thenReturn(List.of(buildTicket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/tickets/by-user").param("email", "juan@example.com"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$[0].userEmail").value("juan@example.com"));

        verify(ticketService).findByUserEmail("juan@example.com");
    }

    // TEST-CTRL-TKT-007: consultar tickets PAID de un evento → 200
    @Test
    @DisplayName("GET /api/events/{eventCode}/tickets/paid retorna 200 OK con tickets pagados del evento")
    void findPaidTicketsByEvent_returns200() throws Exception {
        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(buildTicket(TicketStatus.PAID)));

        mockMvc.perform(get("/api/events/CMF-2026/tickets/paid"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService).findPaidTicketsByEvent("CMF-2026");
    }

    // TEST-CTRL-TKT-008: cancelar ticket válido → 200
    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel con ticket PAID retorna 200 OK con estado CANCELLED")
    void cancel_validTicket_returns200() throws Exception {
        when(ticketService.cancel("TKT-0001")).thenReturn(buildTicket(TicketStatus.CANCELLED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService).cancel("TKT-0001");
    }

    // TEST-CTRL-TKT-009: cancelar ticket en estado inválido → 409
    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/cancel con ticket no cancelable retorna 409 Conflict")
    void cancel_businessRuleViolation_returns409() throws Exception {
        when(ticketService.cancel("TKT-0001"))
                .thenThrow(new BusinessRuleException("Ticket cannot be cancelled in its current state"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(ticketService).cancel("TKT-0001");
    }

    // TEST-CTRL-TKT-010: marcar ticket como usado → 200
    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use con ticket PAID retorna 200 OK con estado USED")
    void markAsUsed_validTicket_returns200() throws Exception {
        when(ticketService.markAsUsed("TKT-0001")).thenReturn(buildTicket(TicketStatus.USED));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService).markAsUsed("TKT-0001");
    }

    // TEST-CTRL-TKT-011: marcar como usado ticket en estado inválido → 409
    @Test
    @DisplayName("PATCH /api/tickets/{ticketCode}/use con ticket no utilizable retorna 409 Conflict")
    void markAsUsed_businessRuleViolation_returns409() throws Exception {
        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(new BusinessRuleException("Ticket cannot be marked as used in its current state"));

        mockMvc.perform(patch("/api/tickets/TKT-0001/use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(ticketService).markAsUsed("TKT-0001");
    }
}
