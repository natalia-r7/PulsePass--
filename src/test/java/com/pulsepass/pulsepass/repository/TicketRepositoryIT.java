
package com.pulsepass.pulsepass.repository;

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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class TicketRepositoryIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldFindTicketsByUserEmailAndStatus() {

        String id = UUID.randomUUID().toString().substring(0, 8);

        User user = new User();
        user.setUsername("ticket-user-" + id);
        user.setEmail("ticket.user." + id + "@test.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        Venue venue = new Venue();
        venue.setCode("VEN-TICKET-01-" + id);
        venue.setName("Ticket Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Carrera 1 #1-01");
        venue.setCapacity(100);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        Event event = new Event();
        event.setEventCode("EVENT-TICKET-01-" + id);
        event.setName("Ticket Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);
        event = eventRepository.saveAndFlush(event);

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TICKET-USER-01-" + id);
        ticket.setType(TicketType.GENERAL);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPrice(new BigDecimal("120000.00"));
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        ticketRepository.saveAndFlush(ticket);

        List<Ticket> result =
                ticketRepository.findByUserEmailAndStatus(
                        user.getEmail(),
                        TicketStatus.PAID
                );

        assertEquals(1, result.size());
        assertEquals(ticket.getTicketCode(), result.get(0).getTicketCode());
    }

    @Test
    void shouldFindPaidTicketsByEventCode() {

        String id = UUID.randomUUID().toString().substring(0, 8);

        User user = new User();
        user.setUsername("paid-user-" + id);
        user.setEmail("paid.user." + id + "@test.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        Venue venue = new Venue();
        venue.setCode("VEN-TICKET-02-" + id);
        venue.setName("Paid Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Carrera 2 #2-02");
        venue.setCapacity(200);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        Event event = new Event();
        event.setEventCode("EVENT-PAID-01-" + id);
        event.setName("Paid Event");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(20));
        event.setVenue(venue);
        event = eventRepository.saveAndFlush(event);

        Ticket paidTicket = new Ticket();
        paidTicket.setTicketCode("TICKET-PAID-01-" + id);
        paidTicket.setType(TicketType.VIP);
        paidTicket.setStatus(TicketStatus.PAID);
        paidTicket.setPrice(new BigDecimal("250000.00"));
        paidTicket.setPurchaseDate(LocalDateTime.now());
        paidTicket.setUser(user);
        paidTicket.setEvent(event);

        ticketRepository.saveAndFlush(paidTicket);

        Ticket reservedTicket = new Ticket();
        reservedTicket.setTicketCode("TICKET-RESERVED-01-" + id);
        reservedTicket.setType(TicketType.GENERAL);
        reservedTicket.setStatus(TicketStatus.RESERVED);
        reservedTicket.setPrice(new BigDecimal("120000.00"));
        reservedTicket.setPurchaseDate(LocalDateTime.now());
        reservedTicket.setUser(user);
        reservedTicket.setEvent(event);

        ticketRepository.saveAndFlush(reservedTicket);

        List<Ticket> result =
                ticketRepository.findByEventEventCodeAndStatus(
                        event.getEventCode(),
                        TicketStatus.PAID
                );

        assertEquals(1, result.size());
        assertEquals(
                paidTicket.getTicketCode(),
                result.get(0).getTicketCode()
        );
    }

    @Test
    @Transactional
    void shouldFindTicketsForEventsAfterDateOrderedByEventDate() {

        ticketRepository.deleteAll();
        ticketRepository.flush();

        String id = UUID.randomUUID().toString().substring(0, 8);

        User user = new User();
        user.setUsername("future-user-" + id);
        user.setEmail("future.user." + id + "@test.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        Venue venue = new Venue();
        venue.setCode("VEN-TICKET-03-" + id);
        venue.setName("Future Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Carrera 3 #3-03");
        venue.setCapacity(300);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        Event laterEvent = new Event();
        laterEvent.setEventCode("EVENT-FUTURE-LATER-" + id);
        laterEvent.setName("Later Event");
        laterEvent.setCategory(EventCategory.MUSIC);
        laterEvent.setStatus(EventStatus.PUBLISHED);
        laterEvent.setEventDate(LocalDateTime.now().plusDays(120));
        laterEvent.setVenue(venue);
        laterEvent = eventRepository.saveAndFlush(laterEvent);

        Event earlierEvent = new Event();
        earlierEvent.setEventCode("EVENT-FUTURE-EARLIER-" + id);
        earlierEvent.setName("Earlier Event");
        earlierEvent.setCategory(EventCategory.MUSIC);
        earlierEvent.setStatus(EventStatus.PUBLISHED);
        earlierEvent.setEventDate(LocalDateTime.now().plusDays(110));
        earlierEvent.setVenue(venue);
        earlierEvent = eventRepository.saveAndFlush(earlierEvent);

        Ticket laterTicket = new Ticket();
        laterTicket.setTicketCode("TICKET-FUTURE-LATER-" + id);
        laterTicket.setType(TicketType.GENERAL);
        laterTicket.setStatus(TicketStatus.PAID);
        laterTicket.setPrice(new BigDecimal("120000.00"));
        laterTicket.setPurchaseDate(LocalDateTime.now());
        laterTicket.setUser(user);
        laterTicket.setEvent(laterEvent);

        ticketRepository.saveAndFlush(laterTicket);

        Ticket earlierTicket = new Ticket();
        earlierTicket.setTicketCode("TICKET-FUTURE-EARLIER-" + id);
        earlierTicket.setType(TicketType.VIP);
        earlierTicket.setStatus(TicketStatus.PAID);
        earlierTicket.setPrice(new BigDecimal("250000.00"));
        earlierTicket.setPurchaseDate(LocalDateTime.now());
        earlierTicket.setUser(user);
        earlierTicket.setEvent(earlierEvent);

        ticketRepository.saveAndFlush(earlierTicket);

        LocalDateTime searchDate =
                LocalDateTime.now().plusDays(100);

        List<Ticket> result =
                ticketRepository.findTicketsForEventsAfterDate(searchDate);

        assertEquals(2, result.size());

        assertEquals(
                earlierTicket.getTicketCode(),
                result.get(0).getTicketCode()
        );

        assertEquals(
                laterTicket.getTicketCode(),
                result.get(1).getTicketCode()
        );

        assertTrue(
                result.get(0).getEvent().getEventDate()
                        .isBefore(
                                result.get(1).getEvent().getEventDate()
                        )
        );
    }
}

