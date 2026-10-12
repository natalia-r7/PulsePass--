package com.pulsepass.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.pulsepass.dto.response.EventResponse;
import com.pulsepass.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
import com.pulsepass.pulsepass.entity.EventCategory;
import com.pulsepass.pulsepass.entity.EventStatus;
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
import java.util.Set;

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

@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    private CreateEventRequest validRequest() {
        return new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival de música",
                EventCategory.MUSIC,
                LocalDateTime.now().plusMonths(3),
                18,
                "VEN-SMR-01"
        );
    }

    private EventResponse buildEventResponse(EventStatus status) {
        return new EventResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                "Festival de música", EventCategory.MUSIC, status,
                LocalDateTime.now().plusMonths(3), 18, "VEN-SMR-01",
                "Marina Convention Center", Set.of());
    }

    private EventSummaryResponse buildSummary() {
        return new EventSummaryResponse(1L, "CMF-2026", "Caribbean Music Fest 2026",
                EventCategory.MUSIC, EventStatus.PUBLISHED, LocalDateTime.now().plusMonths(3),
                "Marina Convention Center");
    }

    // TEST-CTRL-EVT-001: crear válido → 201
    @Test
    @DisplayName("POST /api/events con request válido retorna 201 Created")
    void create_validRequest_returns201() throws Exception {
        when(eventService.create(any())).thenReturn(buildEventResponse(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(eventService).create(any());
    }

    // TEST-CTRL-EVT-002: request inválido → 400
    @Test
    @DisplayName("POST /api/events con request inválido retorna 400 Bad Request")
    void create_invalidRequest_returns400() throws Exception {
        // eventCode y name vacíos, category y eventDate nulos
        String invalidJson = """
                {
                    "eventCode": "",
                    "name": "",
                    "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isMap());

        verify(eventService, never()).create(any());
    }

    // TEST-CTRL-EVT-003: consultar existente → 200
    @Test
    @DisplayName("GET /api/events/{eventCode} con evento existente retorna 200 OK")
    void findByCode_existingEvent_returns200() throws Exception {
        when(eventService.findByCode("CMF-2026")).thenReturn(buildEventResponse(EventStatus.DRAFT));

        mockMvc.perform(get("/api/events/CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004: inexistente → 404
    @Test
    @DisplayName("GET /api/events/{eventCode} con evento inexistente retorna 404 Not Found")
    void findByCode_nonExistingEvent_returns404() throws Exception {
        when(eventService.findByCode("NO-EVT")).thenThrow(new ResourceNotFoundException("Event not found: NO-EVT"));

        mockMvc.perform(get("/api/events/NO-EVT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(eventService).findByCode("NO-EVT");
    }

    // TEST-CTRL-EVT-005: listar publicados → 200
    @Test
    @DisplayName("GET /api/events/published retorna 200 OK con lista de publicados")
    void findPublishedEvents_returns200() throws Exception {
        when(eventService.findPublishedEvents()).thenReturn(List.of(buildSummary()));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006: publicar válido → 200
    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish con transición válida retorna 200 OK")
    void publish_validTransition_returns200() throws Exception {
        when(eventService.publish("CMF-2026")).thenReturn(buildEventResponse(EventStatus.PUBLISHED));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007: publicar inválido (regla negocio) → 409
    @Test
    @DisplayName("PATCH /api/events/{eventCode}/publish con conflicto de negocio retorna 409 Conflict")
    void publish_businessRuleViolation_returns409() throws Exception {
        when(eventService.publish("CMF-2026")).thenThrow(new BusinessRuleException("Event cannot be published in its current state"));

        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        verify(eventService).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-008: asociar artista → 200
    @Test
    @DisplayName("POST /api/events/{eventCode}/artists/{artistId} retorna 200 OK")
    void addArtist_validAssociation_returns200() throws Exception {
        when(eventService.addArtist("CMF-2026", 1L)).thenReturn(buildEventResponse(EventStatus.DRAFT));

        mockMvc.perform(post("/api/events/CMF-2026/artists/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(eventService).addArtist("CMF-2026", 1L);
    }

    // TEST-CTRL-EVT-009: buscar por artista → 200
    @Test
    @DisplayName("GET /api/events/by-artist?stageName=... retorna 200 OK con lista de eventos")
    void findByArtist_returns200() throws Exception {
        when(eventService.findByArtist("Solar Beat")).thenReturn(List.of(buildSummary()));

        mockMvc.perform(get("/api/events/by-artist").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(eventService).findByArtist("Solar Beat");
    }
}
