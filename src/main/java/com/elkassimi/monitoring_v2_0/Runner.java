package com.elkassimi.monitoring_v2_0;

import com.elkassimi.monitoring_v2_0.controller.QueryController;
import com.elkassimi.monitoring_v2_0.dto.AgentProvisionRequestDto;
import com.elkassimi.monitoring_v2_0.dto.ApiResponse;
import com.elkassimi.monitoring_v2_0.model.Metrics;
import com.elkassimi.monitoring_v2_0.repository.UserRepository;
import com.elkassimi.monitoring_v2_0.service.AgentService;
import com.elkassimi.monitoring_v2_0.service.MetricsService;
import com.elkassimi.monitoring_v2_0.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@AllArgsConstructor
public class Runner implements CommandLineRunner {

    private final AgentService agentService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final MetricsService metricsService;
    private final QueryController queryController;


    @Override
    public void run(String... args) throws Exception {
        AgentProvisionRequestDto ap = new AgentProvisionRequestDto("agent-01", 70);
        userRepository.findById("a1b2c3d4-1111-4aaa-8bbb-000000000001")
                .ifPresent(user -> {
                    user.setOnline(true);
                    userRepository.save(user);
                });
//        ResponseEntity<ApiResponse> response = queryController.listMetrics("9d51ee29-e506-45c2-bc8e-95d41906f8e9", Pageable.ofSize(20));
//        System.out.println(response.getBody().getData());





    }
}
