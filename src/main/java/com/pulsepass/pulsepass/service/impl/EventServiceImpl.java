package com.pulsepass.pulsepass.service.impl;

import com.pulsepass.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.pulsepass.dto.response.EventResponse;
import com.pulsepass.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.pulsepass.entity.Artist;
import com.pulsepass.pulsepass.entity.Event;
import com.pulsepass.pulsepass.entity.EventStatus;
import com.pulsepass.pulsepass.entity.Venue;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.mapper.EventMapper;
import com.pulsepass.pulsepass.repository.ArtistRepository;
import com.pulsepass.pulsepass.repository.EventRepository;
import com.pulsepass.pulsepass.repository.VenueRepository;
import com.pulsepass.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            ArtistRepository artistRepository,
            EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {
        // BR-EVENT-001: código único
        if (eventRepository.existsByEventCode(request.eventCode())) {
            throw new DuplicateResourceException("Event code already exists: " + request.eventCode());
        }

        // BR-EVENT-002: venue debe existir
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", request.venueCode()));

        // BR-EVENT-003: venue debe estar activo
        if (!venue.getActive()) {
            throw new BusinessRuleException("Venue is not active: " + request.venueCode());
        }

        // BR-EVENT-004: fecha debe ser futura
        if (!request.eventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Event date must be in the future.");
        }

        // BR-EVENT-006: edad mínima >= 0
        if (request.minimumAge() != null && request.minimumAge() < 0) {
            throw new BusinessRuleException("Minimum age must be 0 or greater.");
        }

        Event event = new Event();
        event.setEventCode(request.eventCode());
        event.setName(request.name());
        event.setDescription(request.description());
        event.setCategory(request.category());
        event.setEventDate(request.eventDate());
        event.setMinimumAge(request.minimumAge() != null ? request.minimumAge() : 0);
        event.setVenue(venue);
        // BR-EVENT-005: estado inicial DRAFT
        event.setStatus(EventStatus.DRAFT);

        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(eventMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventCode));

        // BR-EVENT-007: solo DRAFT puede publicarse
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published. Current status: " + event.getStatus());
        }

        // BR-EVENT-008: fecha todavía futura
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot publish an event with a past date.");
        }

        // BR-EVENT-009: venue sigue activo
        if (!event.getVenue().getActive()) {
            throw new BusinessRuleException("Cannot publish an event in an inactive venue.");
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {
        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventCode));

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist", String.valueOf(artistId)));

        // BR-EVENT-011: no se puede agregar artistas a eventos CANCELLED o FINISHED
        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "Cannot add artists to an event with status: " + event.getStatus());
        }

        // BR-EVENT-010: evitar duplicados
        if (event.getArtists().contains(artist)) {
            throw new DuplicateResourceException(
                    "Artist already associated with event: " + artist.getStageName());
        }

        event.getArtists().add(artist);
        Event saved = eventRepository.save(event);
        return eventMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository.findEventsByArtist(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }
}
