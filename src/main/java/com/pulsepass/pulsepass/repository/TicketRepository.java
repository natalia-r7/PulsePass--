package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Ticket;
import com.pulsepass.pulsepass.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketCode(String ticketCode);

    List<Ticket> findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(String email);

    List<Ticket> findByUserEmailAndStatus(String email, TicketStatus status);

    List<Ticket> findByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    long countByEventEventCodeAndStatus(String eventCode, TicketStatus status);

    @Query("""
        SELECT t
        FROM Ticket t
        WHERE t.event.eventDate > :date
        ORDER BY t.event.eventDate ASC
    """)
    List<Ticket> findTicketsForEventsAfterDate(@Param("date") LocalDateTime date);
}
