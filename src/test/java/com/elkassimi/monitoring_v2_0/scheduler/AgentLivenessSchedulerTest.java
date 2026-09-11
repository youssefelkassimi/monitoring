package com.elkassimi.monitoring_v2_0.scheduler;

import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.repository.AgentRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentLivenessSchedulerTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private AgentLivenessCache livenessCache;
    @Mock
    private RealTimePushService pushService;

    @InjectMocks
    private AgentLivenessScheduler scheduler;

    @Test
    void markStaleAgentsOffline_noTrackedAgents_doesNothing() {
        when(livenessCache.onlineAgentIds()).thenReturn(Set.of());

        scheduler.markStaleAgentsOffline();

        verify(agentRepository, never()).findByAgentId(any());
        verify(pushService, never()).pushAgents(any());
    }

    @Test
    void markStaleAgentsOffline_agentStillWithinTtl_isLeftAlone() {
        when(livenessCache.onlineAgentIds()).thenReturn(Set.of("agt-1"));
        when(livenessCache.isAlive("agt-1")).thenReturn(true);

        scheduler.markStaleAgentsOffline();

        verify(agentRepository, never()).findByAgentId(any());
        verify(livenessCache, never()).markOffline(any());
        verify(pushService, never()).pushAgents(any());
    }

    @Test
    void markStaleAgentsOffline_staleOnlineAgent_flipsToOfflineAndPushes() {
        Agent agent = Agent.builder().agentId("agt-2").status(Agent.AgentStatus.ONLINE).build();
        when(livenessCache.onlineAgentIds()).thenReturn(Set.of("agt-2"));
        when(livenessCache.isAlive("agt-2")).thenReturn(false);
        when(agentRepository.findByAgentId("agt-2")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        scheduler.markStaleAgentsOffline();

        org.junit.jupiter.api.Assertions.assertEquals(Agent.AgentStatus.OFFLINE, agent.getStatus());
        verify(agentRepository).save(agent);
        verify(livenessCache).markOffline("agt-2");
        verify(pushService).pushAgentUpdate(agent);
        verify(pushService).pushAgents(List.of(agent));
    }

    @Test
    void markStaleAgentsOffline_staleAgentAlreadyOffline_doesNotResaveOrPush() {
        Agent agent = Agent.builder().agentId("agt-3").status(Agent.AgentStatus.OFFLINE).build();
        when(livenessCache.onlineAgentIds()).thenReturn(Set.of("agt-3"));
        when(livenessCache.isAlive("agt-3")).thenReturn(false);
        when(agentRepository.findByAgentId("agt-3")).thenReturn(Optional.of(agent));

        scheduler.markStaleAgentsOffline();

        verify(agentRepository, never()).save(any());
        verify(livenessCache).markOffline("agt-3");
        // nothing newly went offline, so no push at all
        verify(pushService, never()).pushAgents(any());
        verify(pushService, never()).pushAgentUpdate(any());
    }

    @Test
    void markStaleAgentsOffline_agentNoLongerInDb_stillClearsCacheEntry() {
        when(livenessCache.onlineAgentIds()).thenReturn(Set.of("agt-4"));
        when(livenessCache.isAlive("agt-4")).thenReturn(false);
        when(agentRepository.findByAgentId("agt-4")).thenReturn(Optional.empty());

        scheduler.markStaleAgentsOffline();

        verify(agentRepository, never()).save(any());
        verify(livenessCache).markOffline("agt-4");
        verify(pushService, never()).pushAgents(any());
    }
}
