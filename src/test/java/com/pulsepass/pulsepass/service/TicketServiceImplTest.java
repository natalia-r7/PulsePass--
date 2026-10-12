package com.pulsepass.pulsepass.service;

import com.pulsepass.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
import com.pulsepass.pulsepass.entity.*;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.mapper.TicketMapper;
import com.pulsepass.pulsepass.repository.EventRepository;
import com.pulsepass.pulsepass.repository.TicketRepository;
import com.pulsepass.pulsepass.repository.UserProfileRepository;
import com.pulsepass.pulsepass.repository.UserRepository;
import com.pulsepass.pulsepass.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private EventRepository eventRepository;
    @Mock private TicketMapper ticketMapper;

    @InjectMocks private TicketServiceImpl ticketService;

    private User activeUser;
    private User inactiveUser;
    private UserProfile adultProfile;    // 25 años
    private UserProfile minorProfile;   // 17 años
    private Venue venue;
    private Event publishedEvent;
    private PurchaseTicketRequest validRequest;
    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        venue = new Venue();
        venue.setCode("VEN-SMR-01");
        venue.setCapacity(3);
        venue.setActive(true);

        // Evento en el futuro con edad mínima 18
        LocalDateTime eventDate = LocalDateTime.now().plusDays(30);

        publishedEvent = new Event();
        publishedEvent.setEventCode("CMF-2026");
        publishedEvent.setName("Caribbean Music Fest 2026");
        publishedEvent.setStatus(EventStatus.PUBLISHED);
        publishedEvent.setEventDate(eventDate);
        publishedEvent.setMinimumAge(18);
        publishedEvent.setVenue(venue);

        activeUser = new User();
        activeUser.setUsername("andrea");
        activeUser.setEmail("andrea@email.com");
        activeUser.setActive(true);

        inactiveUser = new User();
        inactiveUser.setUsername("miguel");
        inactiveUser.setEmail("miguel@email.com");
        inactiveUser.setActive(false);

        // Perfil adulto: 25 años en fecha del evento
        adultProfile = new UserProfile();
        adultProfile.setBirthDate(eventDate.toLocalDate().minusYears(25));
        adultProfile.setUser(activeUser);

        // Perfil menor: 17 años en fecha del evento
        minorProfile = new UserProfile();
        minorProfile.setBirthDate(eventDate.toLocalDate().minusYears(17));

        validRequest = new PurchaseTicketRequest("andrea@email.com", "CMF-2026", TicketType.GENERAL);

        ticketResponse = new TicketResponse(
                1L, "ticket-code-123", TicketType.GENERAL,
                new BigDecimal("50.00"), TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
    }

    // TEST-TICKET-001: compra válida → ticket PAID
    @Test
    void purchase_validRequest_returnsTicketPaid() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.of(adultProfile));
        when(ticketRepository.countByEventEventCodeAndStatus(eq("CMF-2026"), eq(TicketStatus.PAID))).thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse);

        // ACT
        TicketResponse result = ticketService.purchase(validRequest);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.status()).isEqualTo(TicketStatus.PAID);
        verify(ticketRepository).save(any(Ticket.class));
    }

    // TEST-TICKET-002: usuario inexistente → ResourceNotFoundException
    @Test
    void purchase_nonExistentUser_throwsResourceNotFoundException() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.empty());

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(validRequest))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-003: usuario inactivo → BusinessRuleException
    @Test
    void purchase_inactiveUser_throwsBusinessRuleException() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("miguel@email.com")).thenReturn(Optional.of(inactiveUser));
        PurchaseTicketRequest req = new PurchaseTicketRequest("miguel@email.com", "CMF-2026", TicketType.GENERAL);

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not active");

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-004: evento DRAFT → BusinessRuleException
    @Test
    void purchase_draftEvent_throwsBusinessRuleException() {
        // ARRANGE
        publishedEvent.setStatus(EventStatus.DRAFT);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(validRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PUBLISHED");

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-005: evento CANCELLED → BusinessRuleException
    @Test
    void purchase_cancelledEvent_throwsBusinessRuleException() {
        // ARRANGE
        publishedEvent.setStatus(EventStatus.CANCELLED);
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(validRequest))
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-006: usuario menor de edad → BusinessRuleException
    @Test
    void purchase_userBelowMinimumAge_throwsBusinessRuleException() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.of(minorProfile));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(validRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("minimum age");

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-007: evento sin capacidad → BusinessRuleException
    @Test
    void purchase_eventAtFullCapacity_throwsBusinessRuleException() {
        // ARRANGE
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.of(adultProfile));
        // capacity = 3, ya hay 3 tickets pagados
        when(ticketRepository.countByEventEventCodeAndStatus(eq("CMF-2026"), eq(TicketStatus.PAID))).thenReturn(3L);

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.purchase(validRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("capacity");

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-008: último ticket disponible → guardar ticket y cambiar evento a SOLD_OUT
    @Test
    void purchase_lastAvailableTicket_savesTicketAndSetsEventSoldOut() {
        // ARRANGE
        // capacity = 3, ya hay 2 → este es el último
        when(userRepository.findByEmailIgnoreCase("andrea@email.com")).thenReturn(Optional.of(activeUser));
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(publishedEvent));
        when(userProfileRepository.findByUserId(any())).thenReturn(Optional.of(adultProfile));
        when(ticketRepository.countByEventEventCodeAndStatus(eq("CMF-2026"), eq(TicketStatus.PAID))).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(ticketResponse);

        // ACT
        ticketService.purchase(validRequest);

        // ASSERT: ticket guardado
        verify(ticketRepository).save(any(Ticket.class));
        // ASSERT: evento guardado con status SOLD_OUT
        verify(eventRepository).save(argThat(e -> e.getStatus() == EventStatus.SOLD_OUT));
    }

    // TEST-TICKET-009: cancelar ticket PAID → CANCELLED
    @Test
    void cancel_paidTicket_becomeCancelled() {
        // ARRANGE
        Ticket paidTicket = buildPaidTicket();
        TicketResponse cancelledResponse = new TicketResponse(
                1L, "tc-001", TicketType.GENERAL, new BigDecimal("50.00"),
                TicketStatus.CANCELLED, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketRepository.findByTicketCode("tc-001")).thenReturn(Optional.of(paidTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(cancelledResponse);

        // ACT
        TicketResponse result = ticketService.cancel("tc-001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
        verify(ticketRepository).save(argThat(t -> t.getStatus() == TicketStatus.CANCELLED));
    }

    // TEST-TICKET-010: cancelar ticket USED → BusinessRuleException
    @Test
    void cancel_usedTicket_throwsBusinessRuleException() {
        // ARRANGE
        Ticket usedTicket = buildPaidTicket();
        usedTicket.setStatus(TicketStatus.USED);
        when(ticketRepository.findByTicketCode("tc-001")).thenReturn(Optional.of(usedTicket));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.cancel("tc-001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAID");

        verify(ticketRepository, never()).save(any());
    }

    // TEST-TICKET-011: marcar PAID como usado → USED
    @Test
    void markAsUsed_paidTicket_becomesUsed() {
        // ARRANGE
        Ticket paidTicket = buildPaidTicket();
        TicketResponse usedResponse = new TicketResponse(
                1L, "tc-001", TicketType.GENERAL, new BigDecimal("50.00"),
                TicketStatus.USED, LocalDateTime.now(),
                "andrea@email.com", "CMF-2026", "Caribbean Music Fest 2026"
        );
        when(ticketRepository.findByTicketCode("tc-001")).thenReturn(Optional.of(paidTicket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketMapper.toResponse(any(Ticket.class))).thenReturn(usedResponse);

        // ACT
        TicketResponse result = ticketService.markAsUsed("tc-001");

        // ASSERT
        assertThat(result.status()).isEqualTo(TicketStatus.USED);
        verify(ticketRepository).save(argThat(t -> t.getStatus() == TicketStatus.USED));
    }

    // TEST-TICKET-012: usar ticket CANCELLED → BusinessRuleException
    @Test
    void markAsUsed_cancelledTicket_throwsBusinessRuleException() {
        // ARRANGE
        Ticket cancelledTicket = buildPaidTicket();
        cancelledTicket.setStatus(TicketStatus.CANCELLED);
        when(ticketRepository.findByTicketCode("tc-001")).thenReturn(Optional.of(cancelledTicket));

        // ACT & ASSERT
        assertThatThrownBy(() -> ticketService.markAsUsed("tc-001"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PAID");

        verify(ticketRepository, never()).save(any());
    }

    // --- helper ---
    private Ticket buildPaidTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("tc-001");
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("50.00"));
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDateTime.now().minusHours(1));
        ticket.setUser(activeUser);
        ticket.setEvent(publishedEvent);
        return ticket;
    }
}
