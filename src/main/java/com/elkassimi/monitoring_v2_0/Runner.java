package com.elkassimi.monitoring_v2_0;

import com.elkassimi.monitoring_v2_0.dto.AgentProvisionRequestDto;
import com.elkassimi.monitoring_v2_0.service.AgentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class Runner implements CommandLineRunner {

    private final AgentService agentService;

    public Runner(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) throws Exception {
        AgentProvisionRequestDto ap = new AgentProvisionRequestDto("agent-01", 70);
//        System.out.println(agentService.provision(ap));
    }
}
