package com.pulsepass.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.pulsepass.dto.response.VenueResponse;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.service.VenueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenueService venueService;

    private VenueResponse buildVenue() {
        return new VenueResponse(1L, "VEN-SMR-01", "Marina Convention Center",
                "Santa Marta", "Calle 1 #1-1", 3, true);
    }

    // TEST-CTRL-VEN-001: venue existente → 200
    @Test
    @DisplayName("GET /api/venues/{code} con venue existente retorna 200 y VenueResponse")
    void findByCode_existingVenue_returns200() throws Exception {
        when(venueService.findByCode("VEN-SMR-01")).thenReturn(buildVenue());

        mockMvc.perform(get("/api/venues/VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$.name").value("Marina Convention Center"));

        verify(venueService).findByCode("VEN-SMR-01");
    }

    // TEST-CTRL-VEN-002: venue inexistente → 404
    @Test
    @DisplayName("GET /api/venues/{code} con venue inexistente retorna 404 y ErrorResponse")
    void findByCode_nonExistingVenue_returns404() throws Exception {
        when(venueService.findByCode("NOPE-99")).thenThrow(new ResourceNotFoundException("Venue not found: NOPE-99"));

        mockMvc.perform(get("/api/venues/NOPE-99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));

        verify(venueService).findByCode("NOPE-99");
    }

    // TEST-CTRL-VEN-003: venues activos → 200
    @Test
    @DisplayName("GET /api/venues/active retorna 200 y lista de venues activos")
    void findActiveVenues_returns200WithList() throws Exception {
        when(venueService.findActiveVenues()).thenReturn(List.of(buildVenue()));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"))
                .andExpect(jsonPath("$[0].active").value(true));

        verify(venueService).findActiveVenues();
    }
}
