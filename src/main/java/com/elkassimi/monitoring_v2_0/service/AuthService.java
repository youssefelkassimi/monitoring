package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.LoginRequestDto;
import com.elkassimi.monitoring_v2_0.dto.LoginResponseDto;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.repository.UserRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RealTimePushService pushService;
    private final  UserService userService;

    public LoginResponseDto login(LoginRequestDto request) throws Exception {
        User user = userRepository.findByEmail(request.username())
                .filter(candidate -> candidate.getPassword() != null
                        && passwordEncoder.matches(request.password(), candidate.getPassword()))
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        userService.setOnlineStatus(user.getId(), true);
        return new LoginResponseDto(
                jwtService.createUserToken(user.getId(), user.getEmail(), user.getRole().name()),
                "Bearer");
    }
}
