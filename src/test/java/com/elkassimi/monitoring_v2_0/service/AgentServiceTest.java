package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.AgentProvisionRequestDto;
import com.elkassimi.monitoring_v2_0.dto.AgentProvisionResponseDto;
import com.elkassimi.monitoring_v2_0.dto.HeartbeatRequestDto;
import com.elkassimi.monitoring_v2_0.dto.RegistrationRequestDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.Heartbeat;
import com.elkassimi.monitoring_v2_0.repository.AgentRepository;
import com.elkassimi.monitoring_v2_0.repository.HeartbeatRepository;
import com.elkassimi.monitoring_v2_0.scheduler.AgentLivenessCache;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;
    @Mock
    private HeartbeatRepository heartbeatRepository;
    @Mock
    private RealTimePushService pushService;
    @Mock
    private AgentLivenessCache livenessCache;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AgentService agentService;

    // --- provision ---

    @Test
    void provision_createsAgentAndReturnsIdAndToken() {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.createAgentToken(any(String.class), any())).thenReturn("agent-jwt");

        AgentProvisionRequestDto dto = new AgentProvisionRequestDto("prod-db-primary-01",30);

        AgentProvisionResponseDto response = agentService.provision(dto);

        assertThat(response.getId()).isNotBlank();
        assertThat(response.getLabel()).isEqualTo("prod-db-primary-01");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getTokenExpiresAt()).isNotNull();

        ArgumentCaptor<Agent> captor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(captor.capture());
        Agent saved = captor.getValue();
        assertThat(saved.getLabel()).isEqualTo("prod-db-primary-01");
        assertThat(saved.getStatus()).isEqualTo(Agent.AgentStatus.OFFLINE);
        assertThat(saved.getProvisioningStatus()).isEqualTo(Agent.ProvisioningStatus.PENDING);
        verify(pushService).pushAgents(saved);
    }

    // --- register ---

    @Test
    void register_newAgent_createsAndActivatesProvisioning() throws Exception {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setAgentId("agt-1");
        dto.setHostname("host-1");
        dto.setOs("Linux");
        dto.setOsVersion("22.04");
        dto.setArchitecture("x86_64");
        dto.setPythonVersion("3.11");
        dto.setRegisteredAt(1_700_000_000.0);

        when(agentRepository.findByAgentId("agt-1")).thenReturn(Optional.empty());

        Agent result = agentService.register(dto);

        assertThat(result.getAgentId()).isEqualTo("agt-1");
        assertThat(result.getHostname()).isEqualTo("host-1");
        assertThat(result.getStatus()).isEqualTo(Agent.AgentStatus.ONLINE);
        assertThat(result.getProvisioningStatus()).isEqualTo(Agent.ProvisioningStatus.ACTIVE);
        verify(pushService).pushAgents(result);
    }

    @Test
    void register_existingAgent_refreshesFieldsAndKeepsActiveProvisioning() throws Exception {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent existing = Agent.builder()
                .agentId("agt-2")
                .provisioningStatus(Agent.ProvisioningStatus.ACTIVE)
                .build();
        when(agentRepository.findByAgentId("agt-2")).thenReturn(Optional.of(existing));

        RegistrationRequestDto dto = new RegistrationRequestDto();
        dto.setAgentId("agt-2");
        dto.setHostname("host-2");

        Agent result = agentService.register(dto);

        assertThat(result).isSameAs(existing);
        assertThat(result.getHostname()).isEqualTo("host-2");
        assertThat(result.getProvisioningStatus()).isEqualTo(Agent.ProvisioningStatus.ACTIVE);
    }

    // --- findOrCreate ---

    @Test
    void findOrCreate_existingAgent_returnsIt() {
        Agent existing = Agent.builder().agentId("agt-3").build();
        when(agentRepository.findByAgentId("agt-3")).thenReturn(Optional.of(existing));

        Agent result = agentService.findOrCreate("agt-3");

        assertThat(result).isSameAs(existing);
        verify(agentRepository, never()).save(any());
    }

    @Test
    void findOrCreate_unknownAgent_createsOnlineAgent() {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        when(agentRepository.findByAgentId("agt-4")).thenReturn(Optional.empty());

        Agent result = agentService.findOrCreate("agt-4");

        assertThat(result.getAgentId()).isEqualTo("agt-4");
        assertThat(result.getStatus()).isEqualTo(Agent.AgentStatus.ONLINE);
        verify(agentRepository).save(any(Agent.class));
    }

    // --- recordHeartbeat ---

    @Test
    void recordHeartbeat_aliveStatus_marksOnlineAndPersistsHeartbeat() {
        when(agentRepository.findByAgentId("agt-5")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(heartbeatRepository.save(any(Heartbeat.class))).thenAnswer(inv -> inv.getArgument(0));

        HeartbeatRequestDto dto = new HeartbeatRequestDto();
        dto.setAgentId("agt-5");
        dto.setHostname("host-5");
        dto.setStatus("alive");
        dto.setTimestamp(1_700_000_000.0);

        agentService.recordHeartbeat(dto);

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getStatus()).isEqualTo(Agent.AgentStatus.ONLINE);

        verify(livenessCache).markAlive("agt-5");

        ArgumentCaptor<Heartbeat> hbCaptor = ArgumentCaptor.forClass(Heartbeat.class);
        verify(heartbeatRepository).save(hbCaptor.capture());
        assertThat(hbCaptor.getValue().getStatus()).isEqualTo(Agent.AgentStatus.ONLINE);
        verify(pushService).pushHeartbeat(any(Heartbeat.class), eq("agt-5"));
    }

    @Test
    void recordHeartbeat_nonAliveStatus_marksOffline() {
        when(agentRepository.findByAgentId("agt-6")).thenReturn(Optional.empty());
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(heartbeatRepository.save(any(Heartbeat.class))).thenAnswer(inv -> inv.getArgument(0));

        HeartbeatRequestDto dto = new HeartbeatRequestDto();
        dto.setAgentId("agt-6");
        dto.setHostname("host-6");
        dto.setStatus("dead");

        agentService.recordHeartbeat(dto);

        ArgumentCaptor<Agent> agentCaptor = ArgumentCaptor.forClass(Agent.class);
        verify(agentRepository).save(agentCaptor.capture());
        assertThat(agentCaptor.getValue().getStatus()).isEqualTo(Agent.AgentStatus.OFFLINE);
    }

    // --- touch ---

    @Test
    void touch_updatesLastSeenAndMarksAliveInCache() {
        when(agentRepository.save(any(Agent.class))).thenAnswer(inv -> inv.getArgument(0));

        Agent existing = Agent.builder().agentId("agt-7").build();
        when(agentRepository.findByAgentId("agt-7")).thenReturn(Optional.of(existing));

        Agent result = agentService.touch("agt-7");

        assertThat(result.getLastSeenAt()).isNotNull();
        verify(livenessCache).markAlive("agt-7");
        verify(agentRepository).save(existing);
    }

    // --- reads ---

    @Test
    void findAll_delegatesToRepository() {
        List<Agent> agents = List.of(Agent.builder().agentId("a1").build());
        when(agentRepository.findAll()).thenReturn(agents);

        assertThat(agentService.findAll()).isEqualTo(agents);
    }

    @Test
    void getByAgentId_found_returnsAgent() {
        Agent agent = Agent.builder().agentId("agt-8").build();
        when(agentRepository.findByAgentId("agt-8")).thenReturn(Optional.of(agent));

        assertThat(agentService.getByAgentId("agt-8")).isSameAs(agent);
    }

    @Test
    void getByAgentId_notFound_throwsNoSuchElementException() {
        when(agentRepository.findByAgentId("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.getByAgentId("missing"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void getById_found_returnsAgent() {
        Agent agent = Agent.builder().agentId("agt-9").build();
        when(agentRepository.findById("agt-9")).thenReturn(Optional.of(agent));

        assertThat(agentService.getById("agt-9")).isSameAs(agent);
    }

    @Test
    void getById_notFound_throwsNoSuchElementException() {
        when(agentRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> agentService.getById("missing"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void findByStatus_delegatesToRepository() {
        List<Agent> online = List.of(Agent.builder().agentId("a1").status(Agent.AgentStatus.ONLINE).build());
        when(agentRepository.findByStatus(Agent.AgentStatus.ONLINE)).thenReturn(online);

        assertThat(agentService.findByStatus(Agent.AgentStatus.ONLINE)).isEqualTo(online);
    }

    @Test
    void agentCount_delegatesToRepositoryCount() {
        when(agentRepository.count()).thenReturn(5L);
        assertThat(agentService.agentCount()).isEqualTo(5L);
    }

    @Test
    void onlineAgentCount_countsOnlyOnlineAgents() {
        when(agentRepository.findByStatus(Agent.AgentStatus.ONLINE))
                .thenReturn(List.of(Agent.builder().agentId("a1").build(), Agent.builder().agentId("a2").build()));

        assertThat(agentService.onlineAgentCount()).isEqualTo(2);
    }
}
