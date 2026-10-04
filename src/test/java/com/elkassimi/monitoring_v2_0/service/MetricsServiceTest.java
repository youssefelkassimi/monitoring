package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.MetricsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Metrics;
import com.elkassimi.monitoring_v2_0.repository.MetricsRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** ObjectMapper is real (not mocked) - JsonUtil needs actual serialization,
 * and mocking it would just re-test Jackson itself. */
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock
    private MetricsRepository metricsRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;
    @Mock
    private ApplicationEventPublisher publisher;


    private final ObjectMapper objectMapper = new ObjectMapper();

    private MetricsService metricsService;

    private MetricsService service() {
        return new MetricsService(metricsRepository, agentService, objectMapper, pushService,publisher);
    }

    @Test
    void save_extractsCpuMemoryProcessCountAndTopLevelLoadAvg() {
        metricsService = service();
        Agent agent = Agent.builder().agentId("agt-1").build();
        when(agentService.touch("agt-1")).thenReturn(agent);
        when(metricsRepository.save(any(Metrics.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricsRequestDto dto = new MetricsRequestDto();
        dto.setAgentId("agt-1");
        dto.setSentAt(1_700_000_000.0);
        dto.setSystem(Map.of(
                "cpu", Map.of("cpu_percent", 55.5),
                "memory", Map.of("percent", 70.2),
                "processes", Map.of("total_count", 128),
                "load_avg", List.of(1.1, 2.2, 3.3)
        ));
        dto.setChecks(Map.of());

        Metrics saved = metricsService.save(dto);

        assertThat(saved.getCpuPercent()).isEqualTo(55.5);
        assertThat(saved.getMemoryPercent()).isEqualTo(70.2);
        assertThat(saved.getProcessCount()).isEqualTo(128);
        assertThat(saved.getLoadAvg()).isEqualTo(1.1);
        assertThat(saved.getAgent()).isSameAs(agent);
        verify(pushService).pushMetrics(saved, "agt-1");
        verify(pushService).pushSystem(saved.getSystem(), "agt-1");
    }

    @Test
    void save_loadAvgFallsBackToNestedCpuBlockWhenTopLevelMissing() {
        metricsService = service();
        when(agentService.touch("agt-2")).thenReturn(Agent.builder().agentId("agt-2").build());
        when(metricsRepository.save(any(Metrics.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricsRequestDto dto = new MetricsRequestDto();
        dto.setAgentId("agt-2");
        // no top-level "load_avg" key; the nested "cpu" block also has no
        // "load_avg" entry, so nestedDouble's fallback lookup finds nothing
        // and toDouble(null) yields null rather than throwing.
        dto.setSystem(Map.of("cpu", Map.of("cpu_percent", 10.0)));
        dto.setChecks(Map.of());

        Metrics saved = metricsService.save(dto);

        assertThat(saved.getLoadAvg()).isNull();
    }

    @Test
    void save_nullSystem_defaultsToEmptyJsonObject() {
        metricsService = service();
        when(agentService.touch("agt-3")).thenReturn(Agent.builder().agentId("agt-3").build());
        when(metricsRepository.save(any(Metrics.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricsRequestDto dto = new MetricsRequestDto();
        dto.setAgentId("agt-3");
        dto.setChecks(Map.of()); // checks must be non-null - see save_nullChecks_throwsNpe

        Metrics saved = metricsService.save(dto);

        assertThat(saved.getSystem()).isEqualTo("{}");
        assertThat(saved.getCpuPercent()).isNull();
    }

    @Test
    void save_nullChecks_throwsNpe() {
        metricsService = service();
        Agent agent = Agent.builder().agentId("agt-4").build();
        when(agentService.touch("agt-4")).thenReturn(agent);
        when(metricsRepository.save(any(Metrics.class))).thenAnswer(inv -> inv.getArgument(0));

        MetricsRequestDto dto = new MetricsRequestDto();
        dto.setAgentId("agt-4");
        // dto.checks left null
        Metrics saved = metricsService.save(dto);

        assertThat(saved).isNotNull();
        assertThat(saved.getAgent()).isSameAs(agent);
    }

    @Test
    void listForAgent_delegatesToRepository() {
        metricsService = service();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Metrics>empty();
        when(metricsRepository.findByAgent_AgentIdOrderByReceivedAtDesc("agt-5", pageable))
                .thenReturn(page);

        assertThat(metricsService.listForAgent("agt-5", pageable)).isSameAs(page);
    }

    @Test
    void latestForAgent_delegatesToRepository() {
        metricsService = service();
        Metrics latest = Metrics.builder().build();
        when(metricsRepository.findFirstByAgent_AgentIdOrderByReceivedAtDesc("agt-6"))
                .thenReturn(Optional.of(latest));

        assertThat(metricsService.latestForAgent("agt-6")).contains(latest);
    }
}
