package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Event;
import com.pulsepass.pulsepass.entity.EventCategory;
import com.pulsepass.pulsepass.entity.EventStatus;
import com.pulsepass.pulsepass.entity.Venue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventRepositoryIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldFindEventByEventCode() {

        Venue venue = new Venue();
        venue.setCode("VEN-EVENT-TEST-" + System.currentTimeMillis());
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(100);
        venue.setActive(true);
        venueRepository.save(venue);

        Event event = new Event();
        event.setEventCode("EVT-TEST-01");
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now());
        event.setVenue(venue);

        eventRepository.save(event);

        Event found =
                eventRepository.findByEventCode("EVT-TEST-01")
                        .orElseThrow();

        assertEquals("Test Event", found.getName());
        assertEquals(EventStatus.PUBLISHED, found.getStatus());
    }

    @Test
    void shouldFindEventsByVenueCode() {

        Venue venue = new Venue();
        venue.setCode("VEN-VENUE-TEST-" + System.currentTimeMillis());
        venue.setName("Venue Search Test");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);

        venueRepository.saveAndFlush(venue);

        Event event = new Event();
        event.setEventCode("EVT-VENUE-TEST-" + System.currentTimeMillis());
        event.setName("Venue Search Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setVenue(venue);

        eventRepository.saveAndFlush(event);

        List<Event> found =
                eventRepository.findByVenueCode(venue.getCode());

        assertEquals(1, found.size());
        assertEquals("Venue Search Event", found.get(0).getName());
        assertEquals(venue.getCode(), found.get(0).getVenue().getCode());
    }
}