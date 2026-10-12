package com.pulsepass.pulsepass.service;

import com.pulsepass.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.pulsepass.dto.response.UserResponse;
import com.pulsepass.pulsepass.entity.User;
import com.pulsepass.pulsepass.entity.UserProfile;
import com.pulsepass.pulsepass.exception.BusinessRuleException;
import com.pulsepass.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.pulsepass.mapper.UserMapper;
import com.pulsepass.pulsepass.repository.UserProfileRepository;
import com.pulsepass.pulsepass.repository.UserRepository;
import com.pulsepass.pulsepass.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserServiceImpl userService;

    private RegisterUserRequest validRequest;
    private User savedUser;
    private UserProfile savedProfile;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterUserRequest(
                "andrea", "andrea@email.com",
                "Andrea", "García",
                "3001234567", "Santa Marta",
                LocalDate.of(2001, 5, 15)
        );

        savedUser = new User();
        savedUser.setUsername("andrea");
        savedUser.setEmail("andrea@email.com");
        savedUser.setActive(true);

        savedProfile = new UserProfile();
        savedProfile.setFirstName("Andrea");
        savedProfile.setLastName("García");
        savedProfile.setBirthDate(LocalDate.of(2001, 5, 15));
        savedProfile.setUser(savedUser);

        userResponse = new UserResponse(
                1L, "andrea", "andrea@email.com", true,
                "Andrea", "García", "3001234567", "Santa Marta",
                LocalDate.of(2001, 5, 15)
        );
    }

    // TEST-USER-001: registrar usuario válido
    @Test
    void register_validRequest_createsUserAndProfile() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(savedProfile);
        when(userMapper.toResponse(savedUser, savedProfile)).thenReturn(userResponse);

        // ACT
        UserResponse result = userService.register(validRequest);

        // ASSERT
        assertThat(result).isNotNull();
        assertThat(result.username()).isEqualTo("andrea");
        assertThat(result.active()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(userProfileRepository).save(any(UserProfile.class));
    }

    // TEST-USER-002: username duplicado → DuplicateResourceException
    @Test
    void register_duplicateUsername_throwsDuplicateResourceException() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // TEST-USER-003: email duplicado → DuplicateResourceException
    @Test
    void register_duplicateEmail_throwsDuplicateResourceException() {
        // ARRANGE
        when(userRepository.existsByUsername("andrea")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("andrea@email.com")).thenReturn(true);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(validRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("andrea@email.com");

        verify(userRepository, never()).save(any());
        verify(userProfileRepository, never()).save(any());
    }

    // TEST-USER-004: birthDate futura → BusinessRuleException
    @Test
    void register_futureBirthDate_throwsBusinessRuleException() {
        // ARRANGE
        RegisterUserRequest futureRequest = new RegisterUserRequest(
                "carlos", "carlos@email.com",
                "Carlos", "López",
                "3009876543", "Bogotá",
                LocalDate.now().plusDays(1)
        );
        when(userRepository.existsByUsername("carlos")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("carlos@email.com")).thenReturn(false);

        // ACT & ASSERT
        assertThatThrownBy(() -> userService.register(futureRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("future");

        verify(userRepository, never()).save(any());
    }
}
