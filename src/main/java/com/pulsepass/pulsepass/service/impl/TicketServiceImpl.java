package com.pulsepass.pulsepass.service.impl;

import com.pulsepass.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.pulsepass.dto.response.TicketResponse;
import com.pulsepass.pulsepass.entity.*;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.pulsepass.mapper.TicketMapper;
import com.pulsepass.pulsepass.repository.*;
import com.pulsepass.pulsepass.service.TicketService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            EventRepository eventRepository,
            TicketMapper ticketMapper) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
    }

    @Override
    @Transactional
    public TicketResponse purchase(PurchaseTicketRequest request) {
        // BR-TICKET-001: usuario debe existir
        User user = userRepository.findByEmailIgnoreCase(request.userEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userEmail()));

        // BR-TICKET-002: usuario debe estar activo
        if (!user.getActive()) {
            throw new BusinessRuleException("User is not active: " + request.userEmail());
        }

        // BR-TICKET-003: evento debe existir
        Event event = eventRepository.findByEventCode(request.eventCode())
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.eventCode()));

        // BR-TICKET-004: evento debe estar PUBLISHED
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events. Current status: " + event.getStatus());
        }

        // BR-TICKET-005: fecha futura
        if (!event.getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot purchase tickets for an event that has already occurred.");
        }

        // BR-TICKET-006: validar edad mínima
        if (event.getMinimumAge() != null && event.getMinimumAge() > 0) {
            UserProfile profile = userProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("UserProfile for user", request.userEmail()));

            int ageAtEvent = calculateAgeAt(profile.getBirthDate(), event.getEventDate().toLocalDate());
            if (ageAtEvent < event.getMinimumAge()) {
                throw new BusinessRuleException(
                        "User does not meet minimum age requirement. Required: "
                        + event.getMinimumAge() + ", user age at event: " + ageAtEvent);
            }
        }

        // BR-TICKET-007: validar capacidad
        long paidTickets = ticketRepository.countByEventEventCodeAndStatus(
                request.eventCode(), TicketStatus.PAID);
        if (paidTickets >= event.getVenue().getCapacity()) {
            throw new BusinessRuleException("Event has reached maximum capacity.");
        }

        // Calcular precio (BR-TICKET-009)
        BigDecimal price = calculatePrice(request.type());

        // Crear ticket con estado PAID
        Ticket ticket = new Ticket();
        ticket.setTicketCode(UUID.randomUUID().toString());
        ticket.setType(request.type());
        ticket.setPrice(price);
        ticket.setStatus(TicketStatus.PAID);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        Ticket saved = ticketRepository.save(ticket);

        // BR-TICKET-008: si esta compra agota la capacidad → SOLD_OUT
        long newPaidCount = paidTickets + 1;
        if (newPaidCount >= event.getVenue().getCapacity()) {
            event.setStatus(EventStatus.SOLD_OUT);
            eventRepository.save(event);
        }

        return ticketMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(ticketMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(String email) {
        return ticketRepository.findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(email)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(String eventCode) {
        return ticketRepository.findByEventEventCodeAndStatus(eventCode, TicketStatus.PAID)
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketCode));

        // BR-TICKET-010 / BR-TICKET-011: solo PAID puede cancelarse
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled. Current status: " + ticket.getStatus());
        }

        // BR-TICKET-012: no puede cancelarse después del evento
        if (!ticket.getEvent().getEventDate().isAfter(LocalDateTime.now())) {
            throw new BusinessRuleException("Cannot cancel a ticket after the event date.");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketCode));

        // BR-TICKET-013 / BR-TICKET-014: solo PAID puede marcarse como USED
        if (ticket.getStatus() != TicketStatus.PAID) {
            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.USED);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    // --- helpers ---

    private int calculateAgeAt(LocalDate birthDate, LocalDate referenceDate) {
        return Period.between(birthDate, referenceDate).getYears();
    }

    /**
     * Estrategia de precios encapsulada (BR-TICKET-009).
     * GENERAL  → $50.00 (precio base)
     * STUDENT  → $35.00 (30% descuento)
     * VIP      → $150.00 (3× base)
     * BACKSTAGE→ $250.00 (5× base)
     */
    private BigDecimal calculatePrice(TicketType type) {
        return switch (type) {
            case GENERAL   -> new BigDecimal("50.00");
            case STUDENT   -> new BigDecimal("35.00");
            case VIP       -> new BigDecimal("150.00");
            case BACKSTAGE -> new BigDecimal("250.00");
        };
    }
}
