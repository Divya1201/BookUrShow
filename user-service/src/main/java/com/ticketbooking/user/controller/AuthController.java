package com.ticketbooking.user.controller;

import com.ticketbooking.user.dto.AuthDtos.AuthResponse;
import com.ticketbooking.user.dto.AuthDtos.LoginRequest;
import com.ticketbooking.user.dto.AuthDtos.RegisterRequest;
import com.ticketbooking.user.dto.AuthDtos.UserProfile;
import com.ticketbooking.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** X-User-Id is set by the API gateway after it validates the JWT. */
    @GetMapping("/api/users/me")
    public UserProfile me(@RequestHeader("X-User-Id") Long userId) {
        return authService.profile(userId);
    }
}
