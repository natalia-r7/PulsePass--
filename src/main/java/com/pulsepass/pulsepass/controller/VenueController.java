package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.dto.response.VenueResponse;
import com.pulsepass.pulsepass.service.VenueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    /**
     * FR-CTRL-VEN-001: Consultar venue por código de negocio.
     * GET /api/venues/{code} → 200 OK | 404 Not Found
     */
    @GetMapping("/{code}")
    public ResponseEntity<VenueResponse> findByCode(@PathVariable String code) {
        VenueResponse venue = venueService.findByCode(code);
        return ResponseEntity.ok(venue);
    }

    /**
     * FR-CTRL-VEN-002: Consultar venues activos.
     * GET /api/venues/active → 200 OK
     */
    @GetMapping("/active")
    public ResponseEntity<List<VenueResponse>> findActiveVenues() {
        List<VenueResponse> venues = venueService.findActiveVenues();
        return ResponseEntity.ok(venues);
    }
}
