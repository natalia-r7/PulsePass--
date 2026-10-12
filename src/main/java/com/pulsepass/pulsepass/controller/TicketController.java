package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
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
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * FR-CTRL-TKT-001: Comprar ticket.
     * POST /api/tickets → 201 Created
     */
    @PostMapping
    public ResponseEntity<TicketResponse> purchase(@Valid @RequestBody PurchaseTicketRequest request) {
        TicketResponse ticket = ticketService.purchase(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /**
     * FR-CTRL-TKT-002: Consultar ticket por código.
     * GET /api/tickets/{ticketCode} → 200 OK | 404 Not Found
     */
    @GetMapping("/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        TicketResponse ticket = ticketService.findByCode(ticketCode);
        return ResponseEntity.ok(ticket);
    }

    /**
     * FR-CTRL-TKT-003: Consultar tickets por usuario (email).
     * GET /api/tickets/by-user?email=... → 200 OK
     */
    @GetMapping("/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(@RequestParam String email) {
        List<TicketResponse> tickets = ticketService.findByUserEmail(email);
        return ResponseEntity.ok(tickets);
    }

    /**
     * FR-CTRL-TKT-005: Cancelar ticket.
     * PATCH /api/tickets/{ticketCode}/cancel → 200 OK | 409 Conflict
     */
    @PatchMapping("/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        TicketResponse ticket = ticketService.cancel(ticketCode);
        return ResponseEntity.ok(ticket);
    }

    /**
     * FR-CTRL-TKT-006: Marcar ticket como usado.
     * PATCH /api/tickets/{ticketCode}/use → 200 OK | 409 Conflict
     */
    @PatchMapping("/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        TicketResponse ticket = ticketService.markAsUsed(ticketCode);
        return ResponseEntity.ok(ticket);
    }
}
