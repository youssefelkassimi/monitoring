package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.LogsRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Logs;
import com.elkassimi.monitoring_v2_0.repository.LogRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogRepository logRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;

    private LogService service() {
        return new LogService(logRepository, agentService, new ObjectMapper(), pushService);
    }

    @Test
    void save_withLogEntries_storesJsonArrayAndPushes() {
        LogService svc = service();
        Agent agent = Agent.builder().agentId("agt-1").build();
        when(agentService.touch("agt-1")).thenReturn(agent);
        when(logRepository.save(any(Logs.class))).thenAnswer(inv -> inv.getArgument(0));

        LogsRequestDto dto = new LogsRequestDto();
        dto.setAgentId("agt-1");
        dto.setLogs(List.of(Map.of("path", "/var/log/syslog", "status", "ok")));

        Logs saved = svc.save(dto);

        assertThat(saved.getAgent()).isSameAs(agent);
        assertThat(saved.getPayloadJson()).contains("/var/log/syslog");
        verify(pushService).pushLogs(saved, "agt-1");
    }

    @Test
    void save_nullLogs_defaultsToEmptyJsonArray() {
        LogService svc = service();
        when(agentService.touch("agt-2")).thenReturn(Agent.builder().agentId("agt-2").build());
        when(logRepository.save(any(Logs.class))).thenAnswer(inv -> inv.getArgument(0));

        LogsRequestDto dto = new LogsRequestDto();
        dto.setAgentId("agt-2");

        Logs saved = svc.save(dto);

        assertThat(saved.getPayloadJson()).isEqualTo("[]");
    }

    @Test
    void listForAgent_delegatesToRepository() {
        LogService svc = service();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<Logs>empty();
        when(logRepository.findByAgent_AgentIdOrderByTimestampDesc("agt-3", pageable))
                .thenReturn(page);

        assertThat(svc.listForAgent("agt-3", pageable)).isSameAs(page);
    }
}
