package com.elkassimi.monitoring_v2_0.controller;

import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.dto.UserDto;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse> listUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean online) {
        if (role != null && !role.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(userService.findByRole(User.role.valueOf(role.toUpperCase()))));
        }
        if (online != null) {
            return ResponseEntity.ok(ApiResponse.ok(online ? userService.findOnlineUsers() : userService.findOfflineUsers()));
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> getUser(@PathVariable String id) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse> getUserByEmail(@RequestParam String email) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.getByEmail(email)));
    }

    @GetMapping("/exists")
    public ResponseEntity<ApiResponse> existsByEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(userService.existsByEmail(email)));
    }

    @GetMapping("/counts")
    public ResponseEntity<ApiResponse> userCounts() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "total", userService.userCount(),
                "online", userService.onlineUserCount(),
                "offline", userService.offlineUserCount()
        )));
    }

    @PostMapping
    public ResponseEntity<ApiResponse> register(@RequestBody UserDto userDto) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.register(userDto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> updateUser(
            @RequestBody UserDto userDto) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.updateUser(userDto)));
    }

    @PatchMapping("/{id}/online")
    public ResponseEntity<ApiResponse> setOnlineStatus(
            @PathVariable String id,
            @RequestParam boolean online) throws Exception {
        return ResponseEntity.ok(ApiResponse.ok(userService.setOnlineStatus(id, online)));
    }

    @PostMapping("/{id}/logout")
    public ResponseEntity<ApiResponse> logout(@PathVariable String id) throws Exception {
        userService.logout(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable String id) throws Exception {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.ok());
    }


}
