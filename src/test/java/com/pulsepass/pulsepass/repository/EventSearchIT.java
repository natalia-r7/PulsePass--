package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Artist;
import com.pulsepass.pulsepass.entity.Event;
import com.pulsepass.pulsepass.entity.EventCategory;
import com.pulsepass.pulsepass.entity.EventStatus;
import com.pulsepass.pulsepass.entity.Ticket;
import com.pulsepass.pulsepass.entity.TicketStatus;
import com.pulsepass.pulsepass.entity.TicketType;
import com.pulsepass.pulsepass.entity.User;
import com.pulsepass.pulsepass.entity.Venue;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EventSearchIT {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldFindPublishedEventsOrderedByDate() {

        Venue venue = new Venue();
        venue.setCode("VEN-SEARCH-" + System.currentTimeMillis());
        venue.setName("Search Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);
        venueRepository.save(venue);

        Event laterEvent = new Event();
        laterEvent.setEventCode("EVT-LATER-" + System.currentTimeMillis());
        laterEvent.setName("Later Event");
        laterEvent.setCategory(EventCategory.MUSIC);
        laterEvent.setStatus(EventStatus.PUBLISHED);
        laterEvent.setEventDate(LocalDateTime.now().plusDays(10));
        laterEvent.setVenue(venue);
        eventRepository.save(laterEvent);

        Event earlierEvent = new Event();
        earlierEvent.setEventCode("EVT-EARLIER-" + System.currentTimeMillis());
        earlierEvent.setName("Earlier Event");
        earlierEvent.setCategory(EventCategory.MUSIC);
        earlierEvent.setStatus(EventStatus.PUBLISHED);
        earlierEvent.setEventDate(LocalDateTime.now().plusDays(5));
        earlierEvent.setVenue(venue);
        eventRepository.save(earlierEvent);

        List<Event> found =
                eventRepository.findByStatusOrderByEventDateAsc(
                        EventStatus.PUBLISHED
                );

        assertFalse(found.isEmpty());
        assertEquals("Earlier Event", found.get(0).getName());
    }

    @Test
    void shouldFindEventsByCityAndArtist() {

        Venue venue = new Venue();
        venue.setCode("VEN-CITY-" + System.currentTimeMillis());
        venue.setName("City Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);
        venueRepository.save(venue);

        Artist artist = new Artist();
        artist.setStageName("City Artist " + System.currentTimeMillis());
        artist.setCountry("Colombia");
        artist.setGenre("Music");
        artist.setActive(true);
        artistRepository.save(artist);

        Event event = new Event();
        event.setEventCode("EVT-CITY-" + System.currentTimeMillis());
        event.setName("City Artist Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setVenue(venue);
        event.getArtists().add(artist);
        eventRepository.saveAndFlush(event);

        List<Event> found =
                eventRepository.findEventsByCityAndArtist(
                        "Santa Marta",
                        artist.getStageName()
                );

        assertEquals(1, found.size());
        assertEquals("City Artist Event", found.get(0).getName());
    }

    @Test
    void shouldFindRecommendedEvents() {

        Venue venue = new Venue();
        venue.setCode("VEN-RECOMMENDED-" + System.currentTimeMillis());
        venue.setName("Recommended Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);
        venueRepository.save(venue);

        Artist artist = new Artist();
        artist.setStageName("Solar Test Artist " + System.currentTimeMillis());
        artist.setCountry("Colombia");
        artist.setGenre("Music");
        artist.setActive(true);
        artistRepository.save(artist);

        Event event = new Event();
        event.setEventCode("EVT-RECOMMENDED-" + System.currentTimeMillis());
        event.setName("Recommended Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);
        event.getArtists().add(artist);
        eventRepository.saveAndFlush(event);

        List<Event> found =
                eventRepository.findRecommendedEvents(
                        EventStatus.PUBLISHED,
                        "Santa Marta",
                        LocalDateTime.now(),
                        "solar test"
                );

        assertFalse(found.isEmpty());
        assertEquals("Recommended Event", found.get(0).getName());
    }

    @Test
    void shouldFindEventsByArtist() {

        Venue venue = new Venue();
        venue.setCode("VEN-ARTIST-" + System.currentTimeMillis());
        venue.setName("Artist Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);
        venueRepository.save(venue);

        Artist artist = new Artist();
        artist.setStageName("Artist Search " + System.currentTimeMillis());
        artist.setCountry("Colombia");
        artist.setGenre("Music");
        artist.setActive(true);
        artistRepository.save(artist);

        Event event = new Event();
        event.setEventCode("EVT-ARTIST-" + System.currentTimeMillis());
        event.setName("Artist Search Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setVenue(venue);
        event.getArtists().add(artist);
        eventRepository.saveAndFlush(event);

        List<Event> found =
                eventRepository.findEventsByArtist(
                        artist.getStageName()
                );

        assertEquals(1, found.size());
        assertEquals("Artist Search Event", found.get(0).getName());
    }

    @Test
    void shouldCountPaidTicketsByEventCode() {

        Venue venue = new Venue();
        venue.setCode("VEN-COUNT-" + System.currentTimeMillis());
        venue.setName("Count Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(500);
        venue.setActive(true);
        venueRepository.save(venue);

        Event event = new Event();
        event.setEventCode("EVT-COUNT-" + System.currentTimeMillis());
        event.setName("Count Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);
        eventRepository.saveAndFlush(event);

        User user = new User();
        user.setUsername("count-user-" + System.currentTimeMillis());
        user.setEmail("count-" + System.currentTimeMillis() + "@example.com");
        user.setActive(true);
        userRepository.saveAndFlush(user);

        Ticket paidTicket = new Ticket();
        paidTicket.setTicketCode("TKT-PAID-" + System.currentTimeMillis());
        paidTicket.setType(TicketType.GENERAL);
        paidTicket.setStatus(TicketStatus.PAID);
        paidTicket.setPrice(new BigDecimal("50000.00"));
        paidTicket.setPurchaseDate(LocalDateTime.now());
        paidTicket.setUser(user);
        paidTicket.setEvent(event);
        ticketRepository.save(paidTicket);

        Ticket reservedTicket = new Ticket();
        reservedTicket.setTicketCode("TKT-RESERVED-" + System.currentTimeMillis());
        reservedTicket.setType(TicketType.GENERAL);
        reservedTicket.setStatus(TicketStatus.RESERVED);
        reservedTicket.setPrice(new BigDecimal("50000.00"));
        reservedTicket.setPurchaseDate(LocalDateTime.now());
        reservedTicket.setUser(user);
        reservedTicket.setEvent(event);
        ticketRepository.saveAndFlush(reservedTicket);

        long count =
                eventRepository.countPaidTicketsByEventCode(
                        event.getEventCode()
                );

        assertEquals(1, count);
    }
}