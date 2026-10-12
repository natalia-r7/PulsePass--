package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.pulsepass.dto.response.EventResponse;
import com.pulsepass.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
import com.pulsepass.pulsepass.service.EventService;
import com.pulsepass.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;
    private final TicketService ticketService;

    public EventController(EventService eventService, TicketService ticketService) {
        this.eventService = eventService;
        this.ticketService = ticketService;
    }

    /**
     * FR-CTRL-EVT-001: Crear evento.
     * POST /api/events → 201 Created
     */
    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        EventResponse event = eventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    /**
     * FR-CTRL-EVT-002: Consultar evento por código.
     * GET /api/events/{eventCode} → 200 OK | 404 Not Found
     */
    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        EventResponse event = eventService.findByCode(eventCode);
        return ResponseEntity.ok(event);
    }

    /**
     * FR-CTRL-EVT-003: Consultar eventos publicados.
     * GET /api/events/published → 200 OK
     */
    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublishedEvents() {
        List<EventSummaryResponse> events = eventService.findPublishedEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * FR-CTRL-EVT-004: Publicar evento.
     * PATCH /api/events/{eventCode}/publish → 200 OK | 409 Conflict
     */
    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        EventResponse event = eventService.publish(eventCode);
        return ResponseEntity.ok(event);
    }

    /**
     * FR-CTRL-EVT-005: Asociar artista a evento.
     * POST /api/events/{eventCode}/artists/{artistId} → 200 OK | 409 Conflict
     */
    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(
            @PathVariable String eventCode,
            @PathVariable Long artistId) {
        EventResponse event = eventService.addArtist(eventCode, artistId);
        return ResponseEntity.ok(event);
    }

    /**
     * FR-CTRL-EVT-006: Buscar eventos por artista.
     * GET /api/events/by-artist?stageName=... → 200 OK
     */
    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        List<EventSummaryResponse> events = eventService.findByArtist(stageName);
        return ResponseEntity.ok(events);
    }

    /**
     * FR-CTRL-TKT-004: Consultar tickets PAID de un evento.
     * GET /api/events/{eventCode}/tickets/paid → 200 OK
     */
    @GetMapping("/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        List<TicketResponse> tickets = ticketService.findPaidTicketsByEvent(eventCode);
        return ResponseEntity.ok(tickets);
    }
}
