package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.DiscoveryRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Discovery;
import com.elkassimi.monitoring_v2_0.repository.DiscoveryRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryServiceTest {

    @Mock
    private DiscoveryRepository discoveryRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;

    private DiscoveryService service() {
        return new DiscoveryService(discoveryRepository, agentService, new ObjectMapper(), pushService);
    }

    @Test
    void save_withDiscoveryData_storesJsonAndPushes() {
        DiscoveryService svc = service();
        Agent agent = Agent.builder().agentId("agt-1").build();
        when(agentService.touch("agt-1")).thenReturn(agent);
        when(discoveryRepository.save(any(Discovery.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscoveryRequestDto dto = new DiscoveryRequestDto();
        dto.setAgentId("agt-1");
        dto.setTimestamp(1_700_000_000.0);
        dto.setDiscovery(Map.of("hosts_up", 12));

        Discovery saved = svc.save(dto);

        assertThat(saved.getAgent()).isSameAs(agent);
        assertThat(saved.getPayloadJson()).contains("hosts_up");
        verify(pushService).pushDiscovery(saved, "agt-1");
    }

    @Test
    void save_nullDiscovery_defaultsToEmptyJsonObject() {
        DiscoveryService svc = service();
        when(agentService.touch("agt-2")).thenReturn(Agent.builder().agentId("agt-2").build());
        when(discoveryRepository.save(any(Discovery.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscoveryRequestDto dto = new DiscoveryRequestDto();
        dto.setAgentId("agt-2");

        Discovery saved = svc.save(dto);

        assertThat(saved.getPayloadJson()).isEqualTo("{}");
    }

    @Test
    void listForAgent_delegatesToRepository() {
        DiscoveryService svc = service();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Discovery>empty();
        when(discoveryRepository.findByAgent_AgentIdOrderByTimestampDesc("agt-3", pageable))
                .thenReturn(page);

        assertThat(svc.listForAgent("agt-3", pageable)).isSameAs(page);
    }
}
