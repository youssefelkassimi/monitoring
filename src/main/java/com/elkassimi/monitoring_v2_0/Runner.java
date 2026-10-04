package com.elkassimi.monitoring_v2_0;

import com.elkassimi.monitoring_v2_0.controller.QueryController;
import com.elkassimi.monitoring_v2_0.dto.AgentProvisionRequestDto;
import com.elkassimi.monitoring_v2_0.dto.AgentProvisionResponseDto;
import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.dto.UserDto;
import com.elkassimi.monitoring_v2_0.model.Metrics;
import com.elkassimi.monitoring_v2_0.repository.UserRepository;
import com.elkassimi.monitoring_v2_0.service.AgentService;
import com.elkassimi.monitoring_v2_0.service.MetricsService;
import com.elkassimi.monitoring_v2_0.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
@Slf4j
public class Runner implements CommandLineRunner {

    private final AgentService agentService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MetricsService metricsService;
    private final QueryController queryController;

    @Value("${spring.data.redis.host}") String host;
    @Value("${spring.data.redis.port}") int port;

    @Override
    public void run(String... args) throws Exception {

        log.error("REDIS TARGET = {}  :{}" , host, port);

        try {
            UserDto user = UserDto.builder()
                    .email("youssef@elkassimi.ma")
                    .role("ADMIN")
                    .fullName("yousser")
                    .password("12345678")
                    .isOnline(false)
                    .build();

            userService.register(user);
        } catch (Exception ignored) {

        }

        AgentProvisionRequestDto ag = new AgentProvisionRequestDto("youssef", 60 * 30 * 12);

        // System.out.println(agentService.provision(ag));

    }
}
