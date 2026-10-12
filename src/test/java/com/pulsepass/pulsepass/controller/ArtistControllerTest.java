package com.pulsepass.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.service.ArtistService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ArtistService artistService;

    private ArtistResponse buildArtist() {
        return new ArtistResponse(1L, "Solar Beat", "Colombia", "Electrónica", true);
    }

    // TEST-CTRL-ART-001: buscar por ID → 200
    @Test
    @DisplayName("GET /api/artists/{id} con artista existente retorna 200 OK")
    void findById_existingArtist_returns200() throws Exception {
        when(artistService.findById(1L)).thenReturn(buildArtist());

        mockMvc.perform(get("/api/artists/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findById(1L);
    }

    // TEST-CTRL-ART-002: ID inexistente → 404
    @Test
    @DisplayName("GET /api/artists/{id} con artista inexistente retorna 404 Not Found")
    void findById_nonExistingArtist_returns404() throws Exception {
        when(artistService.findById(99L)).thenThrow(new ResourceNotFoundException("Artist not found: 99"));

        mockMvc.perform(get("/api/artists/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(artistService).findById(99L);
    }

    // TEST-CTRL-ART-003: buscar por stageName → 200
    @Test
    @DisplayName("GET /api/artists/by-stage-name?stageName=... retorna 200 OK")
    void findByStageName_existingArtist_returns200() throws Exception {
        when(artistService.findByStageName("Solar Beat")).thenReturn(buildArtist());

        mockMvc.perform(get("/api/artists/by-stage-name").param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Solar Beat"));

        verify(artistService).findByStageName("Solar Beat");
    }

    // TEST-CTRL-ART-004: listar activos → 200
    @Test
    @DisplayName("GET /api/artists/active retorna 200 OK con lista de artistas activos")
    void findActiveArtists_returns200() throws Exception {
        when(artistService.findActiveArtists()).thenReturn(List.of(buildArtist()));

        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(artistService).findActiveArtists();
    }
}
