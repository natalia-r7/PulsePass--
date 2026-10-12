package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Event;
import com.pulsepass.pulsepass.entity.EventStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByEventCode(String eventCode);

    boolean existsByEventCode(String eventCode);

    List<Event> findByStatusOrderByEventDateAsc(EventStatus status);

    List<Event> findByVenueCode(String venueCode);

    @Query("""
        SELECT DISTINCT e
        FROM Event e
        JOIN e.artists a
        WHERE a.stageName = :stageName
    """)
    List<Event> findEventsByArtist(@Param("stageName") String stageName);

    @Query("""
        SELECT COUNT(t)
        FROM Ticket t
        WHERE t.event.eventCode = :eventCode
          AND t.status = com.pulsepass.pulsepass.entity.TicketStatus.PAID
    """)
    long countPaidTicketsByEventCode(@Param("eventCode") String eventCode);

    @Query("""
        SELECT DISTINCT e
        FROM Event e
        JOIN e.artists a
        WHERE e.venue.city = :city
          AND a.stageName = :stageName
    """)
    List<Event> findEventsByCityAndArtist(
            @Param("city") String city,
            @Param("stageName") String stageName
    );

    @Query("""
        SELECT DISTINCT e
        FROM Event e
        JOIN e.artists a
        WHERE e.status = :status
          AND e.venue.city = :city
          AND e.eventDate >= :date
          AND LOWER(a.stageName) LIKE LOWER(CONCAT('%', :artistText, '%'))
        ORDER BY e.eventDate ASC
    """)
    List<Event> findRecommendedEvents(
            @Param("status") EventStatus status,
            @Param("city") String city,
            @Param("date") LocalDateTime date,
            @Param("artistText") String artistText
    );
}
