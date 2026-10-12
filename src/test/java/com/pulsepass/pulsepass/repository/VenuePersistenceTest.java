package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VenuePersistenceTest {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    void shouldSaveAndFindVenue() {

        Venue venue = new Venue();
        venue.setCode("VEN-TEST-" + System.currentTimeMillis());
        venue.setName("Test Venue");
        venue.setCity("Santa Marta");
        venue.setAddress("Test Address");
        venue.setCapacity(100);
        venue.setActive(true);

        Venue saved = venueRepository.saveAndFlush(venue);

        assertNotNull(saved.getId());

        Venue found = venueRepository.findById(saved.getId()).orElseThrow();

        assertEquals(venue.getCode(), found.getCode());
        assertEquals("Test Venue", found.getName());
        assertEquals("Santa Marta", found.getCity());
    }

    @Test
    void shouldRejectDuplicateVenueCode() {

        String code = "VEN-DUPLICATE-" + System.currentTimeMillis();

        Venue venue1 = new Venue();
        venue1.setCode(code);
        venue1.setName("First Venue");
        venue1.setCity("Santa Marta");
        venue1.setAddress("Address 1");
        venue1.setCapacity(100);
        venue1.setActive(true);

        venueRepository.saveAndFlush(venue1);

        Venue venue2 = new Venue();
        venue2.setCode(code);
        venue2.setName("Second Venue");
        venue2.setCity("Santa Marta");
        venue2.setAddress("Address 2");
        venue2.setCapacity(200);
        venue2.setActive(true);

        assertThrows(
                Exception.class,
                () -> venueRepository.saveAndFlush(venue2)
        );
    }
}
    