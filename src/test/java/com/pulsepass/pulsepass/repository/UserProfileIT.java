
package com.pulsepass.pulsepass.repository;

import com.pulsepass.pulsepass.entity.User;
import com.pulsepass.pulsepass.entity.UserProfile;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserProfileIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Test
    void shouldRelateUserWithUserProfile() {

        User user = new User();

        user.setUsername("testuser-" + System.currentTimeMillis());

        user.setEmail("test-" + System.currentTimeMillis() + "@example.com");

        user.setActive(true);

        userRepository.save(user);

        UserProfile profile = new UserProfile();

        profile.setFirstName("Angela");

        profile.setLastName("Test");

        profile.setPhone("3001234567");

        profile.setCity("Santa Marta");

        profile.setBirthDate(LocalDate.of(2005, 1, 1));

        profile.setUser(user);

        userProfileRepository.saveAndFlush(profile);

        UserProfile found =
                userProfileRepository.findById(profile.getId()).orElseThrow();

        assertEquals("Angela", found.getFirstName());

        assertEquals("Test", found.getLastName());

        assertEquals(user.getId(), found.getUser().getId());
    }

    @Test
    void shouldFindUserByEmailIgnoringCase() {

        User user = new User();

        user.setUsername("emailtest-" + System.currentTimeMillis());

        user.setEmail("Angela.Test@example.com");

        user.setActive(true);

        userRepository.saveAndFlush(user);

        User found =
                userRepository.findByEmailIgnoreCase("ANGELA.TEST@EXAMPLE.COM")
                        .orElseThrow();

        assertEquals(user.getId(), found.getId());

        assertEquals("Angela.Test@example.com", found.getEmail());
    }
}

