package com.pulsepass.pulsepass.service;

import com.pulsepass.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.pulsepass.dto.response.EventResponse;
import com.pulsepass.pulsepass.entity.*;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.mapper.EventMapper;
import com.pulsepass.pulsepass.repository.ArtistRepository;
import com.pulsepass.pulsepass.repository.EventRepository;
import com.pulsepass.pulsepass.repository.VenueRepository;
import com.pulsepass.pulsepass.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock private EventRepository eventRepository;
    @Mock private VenueRepository venueRepository;
    @Mock private ArtistRepository artistRepository;
    @Mock private EventMapper eventMapper;

    @InjectMocks private EventServiceImpl eventService;

    private Venue activeVenue;
    private Event draftEvent;
    private EventResponse eventResponse;

    @BeforeEach
    void setUp() {
        activeVenue = new Venue();
        activeVenue.setCode("VEN-SMR-01");
        activeVenue.setName("Marina Convention Center");
        activeVenue.setCity("Santa Marta");
        activeVenue.setCapacity(3);
        activeVenue.setActive(true);

        draftEvent = new Event();
        draftEvent.setEventCode("CMF-2026");
        draftEvent.setName("Caribbean Music Fest 2026");
        draftEvent.setStatus(EventStatus.DRAFT);
        draftEvent.setEventDate(LocalDateTime.now().plusDays(30));
        draftEvent.setMinimumAge(18);
        draftEvent.setVenue(activeVenue);
        draftEvent.setArtists(new java.util.HashSet<>());

        eventResponse = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026",
                null, EventCategory.MUSIC, EventStatus.DRAFT,
                LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", Set.of()
        );
    }

    // TEST-EVENT-001: evento existente → retorna DTO
    @Test
    void findByCode_existingEvent_returnsDto() {
        // ARRANGE
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draftEvent));
        when(eventMapper.toResponse(draftEvent)).thenReturn(eventResponse);

        // ACT
        EventResponse result = eventService.findByCode("CMF-2026");

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.eventCode()).isEqualTo("CMF-2026");
    }

    // TEST-EVENT-002: evento inexistente → ResourceNotFoundException
    @Test
    void findByCode_nonExistentEvent_throwsResourceNotFoundException() {
        // ARRANGE
        when(eventRepository.findByEventCode("INEXISTENTE")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.findByCode("INEXISTENTE"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("INEXISTENTE");
    }

    // TEST-EVENT-003: crear evento válido → save() ejecutado
    @Test
    void create_validRequest_saveIsExecuted() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest 2026", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(30),
                18, "VEN-SMR-01"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue));
        when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);
        when(eventMapper.toResponse(draftEvent)).thenReturn(eventResponse);

        // ACT
        EventResponse result = eventService.create(request);

        // ASSERT
        assertThat(result).isNotNull();
        verify(eventRepository).save(any(Event.class));
    }

    // TEST-EVENT-004: venue inexistente → error y save() nunca ejecutado
    @Test
    void create_nonExistentVenue_throwsAndSaveNeverCalled() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest 2026", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(30),
                18, "VEN-NOEXISTE"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-NOEXISTE")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-005: venue inactivo → BusinessRuleException
    @Test
    void create_inactiveVenue_throwsBusinessRuleException() {
        // ARRANGE
        activeVenue.setActive(false);
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest 2026", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(30),
                18, "VEN-SMR-01"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue));

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-006: fecha pasada → BusinessRuleException
    @Test
    void create_pastDate_throwsBusinessRuleException() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Caribbean Music Fest 2026", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().minusDays(1),
                18, "VEN-SMR-01"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(activeVenue));

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(eventRepository, never()).save(any());
    }

    // TEST-EVENT-007: publicar DRAFT válido → PUBLISHED
    @Test
    void publish_draftEvent_becomesPublished() {
        // ARRANGE
        EventResponse publishedResponse = new EventResponse(
                1L, "CMF-2026", "Caribbean Music Fest 2026",
                null, EventCategory.MUSIC, EventStatus.PUBLISHED,
                LocalDateTime.now().plusDays(30), 18,
                "VEN-SMR-01", "Marina Convention Center", Set.of()
        );
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draftEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(draftEvent);
        when(eventMapper.toResponse(any(Event.class))).thenReturn(publishedResponse);

        // ACT
        EventResponse result = eventService.publish("CMF-2026");

        // ASSERT
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.PUBLISHED));
    }

    // TEST-EVENT-008: publicar CANCELLED → BusinessRuleException y no persistir
    @Test
    void publish_cancelledEvent_throwsBusinessRuleExceptionAndNotSaved() {
        // ARRANGE
        draftEvent.setStatus(EventStatus.CANCELLED);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(draftEvent));

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.publish("CMF-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DRAFT");

        verify(eventRepository, never()).save(any());
    }

    // TEST adicional: código de evento duplicado → DuplicateResourceException
    @Test
    void create_duplicateEventCode_throwsDuplicateResourceException() {
        // ARRANGE
        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026", "Otro evento", "Desc",
                EventCategory.MUSIC, LocalDateTime.now().plusDays(30),
                0, "VEN-SMR-01"
        );
        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> eventService.create(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(eventRepository, never()).save(any());
    }
}
