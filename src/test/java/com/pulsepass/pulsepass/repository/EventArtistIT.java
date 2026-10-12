package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Artist;
import com.pulsepass.pulsepass.entity.Event;
import com.pulsepass.pulsepass.entity.EventCategory;
import com.pulsepass.pulsepass.entity.EventStatus;
import com.pulsepass.pulsepass.entity.Venue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class EventArtistIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Test
    void shouldRelateEventWithArtists() {

        Venue venue = new Venue();
        venue.setCode("VEN-ARTIST-TEST-" + System.currentTimeMillis());
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(100);
        venue.setActive(true);

        venueRepository.save(venue);

        Artist artist = new Artist();
        artist.setStageName("Test Artist " + System.currentTimeMillis());
        artist.setCountry("Colombia");
        artist.setGenre("Music");
        artist.setActive(true);

        artistRepository.save(artist);

        Event event = new Event();
        event.setEventCode("EVT-ARTIST-TEST-" + System.currentTimeMillis());
        event.setName("Test Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now());
        event.setVenue(venue);

        event.getArtists().add(artist);

        eventRepository.saveAndFlush(event);

        Event found = eventRepository.findById(event.getId()).orElseThrow();

        assertEquals(1, found.getArtists().size());
        assertEquals(
                artist.getStageName(),
                found.getArtists().iterator().next().getStageName()
        );
    }
}