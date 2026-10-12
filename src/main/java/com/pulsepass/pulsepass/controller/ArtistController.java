package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.pulsepass.service.ArtistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    /**
     * FR-CTRL-ART-001: Consultar artista por ID.
     * GET /api/artists/{id} → 200 OK | 404 Not Found
     */
    @GetMapping("/{id}")
    public ResponseEntity<ArtistResponse> findById(@PathVariable Long id) {
        ArtistResponse artist = artistService.findById(id);
        return ResponseEntity.ok(artist);
    }

    /**
     * FR-CTRL-ART-002: Consultar artista por stageName.
     * GET /api/artists/by-stage-name?stageName=... → 200 OK | 404 Not Found
     */
    @GetMapping("/by-stage-name")
    public ResponseEntity<ArtistResponse> findByStageName(@RequestParam String stageName) {
        ArtistResponse artist = artistService.findByStageName(stageName);
        return ResponseEntity.ok(artist);
    }

    /**
     * FR-CTRL-ART-003: Consultar artistas activos.
     * GET /api/artists/active → 200 OK
     */
    @GetMapping("/active")
    public ResponseEntity<List<ArtistResponse>> findActiveArtists() {
        List<ArtistResponse> artists = artistService.findActiveArtists();
        return ResponseEntity.ok(artists);
    }
}
