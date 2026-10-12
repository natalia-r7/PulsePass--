package com.pulsepass.pulsepass.controller;

import com.pulsepass.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.pulsepass.dto.response.UserResponse;
import com.pulsepass.pulsepass.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * FR-CTRL-USR-001: Registrar usuario.
     * POST /api/users → 201 Created
     */
    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        UserResponse user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * FR-CTRL-USR-002: Consultar usuario por email.
     * GET /api/users/by-email?email=... → 200 OK | 404 Not Found
     */
    @GetMapping("/by-email")
    public ResponseEntity<UserResponse> findByEmail(@RequestParam String email) {
        UserResponse user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * FR-CTRL-USR-003: Consultar usuario por username.
     * GET /api/users/by-username?username=... → 200 OK | 404 Not Found
     */
    @GetMapping("/by-username")
    public ResponseEntity<UserResponse> findByUsername(@RequestParam String username) {
        UserResponse user = userService.findByUsername(username);
        return ResponseEntity.ok(user);
    }
}
