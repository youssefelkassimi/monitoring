package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.dto.LoginRequestDto;
import com.elkassimi.monitoring_v2_0.service.AuthService;
import com.elkassimi.monitoring_v2_0.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequestDto request) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }
    @GetMapping("/logout")
    public ResponseEntity<ApiResponse> logout(Principal principal) throws Exception {
        userService.logout(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
