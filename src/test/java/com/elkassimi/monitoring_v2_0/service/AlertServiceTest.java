package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AlertEventDto;
import com.elkassimi.monitoring_v2_0.dto.AlertsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Alert;
import com.elkassimi.monitoring_v2_0.repository.AlertRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.elkassimi.monitoring_v2_0.websocket.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;

    @InjectMocks
    private AlertService alertService;

    @Test
    void save_nullAlerts_returnsEmptyListWithoutTouchingAgent() {
        AlertsRequestDto dto = new AlertsRequestDto();
        dto.setAgentId("agt-1");
        dto.setAlerts(null);

        List<Alert> result = alertService.save(dto);

        assertThat(result).isEmpty();
        verify(agentService, never()).touch(anyString());
    }

    @Test
    void save_emptyAlerts_returnsEmptyListWithoutTouchingAgent() {
        AlertsRequestDto dto = new AlertsRequestDto();
        dto.setAgentId("agt-1");
        dto.setAlerts(List.of());

        assertThat(alertService.save(dto)).isEmpty();
        verify(agentService, never()).touch(anyString());
    }

    @Test
    void save_problemAndRecoveryEvents_mapStatusCorrectlyAndPushBoth() {
        Agent agent = Agent.builder().agentId("agt-2").build();
        when(agentService.touch("agt-2")).thenReturn(agent);
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        AlertEventDto problem = new AlertEventDto();
        problem.setTriggerName("cpu_high");
        problem.setStatus("problem");
        problem.setSeverity("critical");

        AlertEventDto recovery = new AlertEventDto();
        recovery.setTriggerName("cpu_high");
        recovery.setStatus("recovery");

        AlertsRequestDto dto = new AlertsRequestDto();
        dto.setAgentId("agt-2");
        dto.setTimestamp(1_700_000_000.0);
        dto.setAlerts(List.of(problem, recovery));

        List<Alert> result = alertService.save(dto);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStatus()).isEqualTo(Alert.AlertStatus.PROBLEM);
        assertThat(result.get(1).getStatus()).isEqualTo(Alert.AlertStatus.RECOVERY);
        assertThat(result).allSatisfy(a -> assertThat(a.getAgent()).isSameAs(agent));

        verify(pushService, times(2)).pushAlert(org.mockito.ArgumentMatchers.any(Alert.class));
        verify(pushService, times(2)).pushAgentAlert(org.mockito.ArgumentMatchers.any(Alert.class), eq("agt-2"));
    }

    @Test
    void save_unknownStatusString_defaultsToProblem() {
        when(agentService.touch("agt-3")).thenReturn(Agent.builder().agentId("agt-3").build());
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        AlertEventDto event = new AlertEventDto();
        event.setTriggerName("weird");
        event.setStatus("not-a-real-status");

        AlertsRequestDto dto = new AlertsRequestDto();
        dto.setAgentId("agt-3");
        dto.setAlerts(List.of(event));

        List<Alert> result = alertService.save(dto);

        assertThat(result.get(0).getStatus()).isEqualTo(Alert.AlertStatus.PROBLEM);
    }

    @Test
    void save_missingStatusString_defaultsToProblem() {
        when(agentService.touch("agt-4")).thenReturn(Agent.builder().agentId("agt-4").build());
        when(alertRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        AlertEventDto event = new AlertEventDto();
        event.setTriggerName("no_status");

        AlertsRequestDto dto = new AlertsRequestDto();
        dto.setAgentId("agt-4");
        dto.setAlerts(List.of(event));

        assertThat(alertService.save(dto).get(0).getStatus()).isEqualTo(Alert.AlertStatus.PROBLEM);
    }

    @Test
    void listForAgent_delegatesToRepository() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Alert>empty();
        when(alertRepository.findByAgent_AgentIdOrderByReceivedAtDesc("agt-5", pageable)).thenReturn(page);

        assertThat(alertService.listForAgent("agt-5", pageable)).isSameAs(page);
    }

    @Test
    void listAll_delegatesToRepository() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Alert>empty();
        when(alertRepository.findAllByOrderByReceivedAtDesc(pageable)).thenReturn(page);

        assertThat(alertService.listAll(pageable)).isSameAs(page);
    }

    @Test
    void listByStatus_delegatesToRepository() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Alert>empty();
        when(alertRepository.findByStatusOrderByReceivedAtDesc(Alert.AlertStatus.RECOVERY, pageable))
                .thenReturn(page);

        assertThat(alertService.listByStatus(Alert.AlertStatus.RECOVERY, pageable)).isSameAs(page);
    }
}
