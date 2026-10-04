package com.elkassimi.monitoring_v2_0.service;

import com.elkassimi.monitoring_v2_0.dto.CommandCreateRequestDto;
import com.elkassimi.monitoring_v2_0.dto.PendingCommandDto;
import com.elkassimi.monitoring_v2_0.dto.RemoteCommandResultDto;
import com.elkassimi.monitoring_v2_0.dto.RemoteCommandResultInnerDto;
import com.elkassimi.monitoring_v2_0.model.Agent;
import com.elkassimi.monitoring_v2_0.model.RemoteCommand;
import com.elkassimi.monitoring_v2_0.model.User;
import com.elkassimi.monitoring_v2_0.repository.RemoteCommandRepository;
import com.elkassimi.monitoring_v2_0.websocket.RealTimePushService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoteCommandServiceTest {

    @Mock
    private RemoteCommandRepository remoteCommandRepository;
    @Mock
    private AgentService agentService;
    @Mock
    private RealTimePushService pushService;
    @Mock
    private UserService userService;

    private RemoteCommandService service() {
        return new RemoteCommandService(remoteCommandRepository, agentService, new ObjectMapper(), userService, pushService);
    }

    @Test
    void queue_createsPendingCommandWithSerializedArgsAndPushes() throws Exception {
        RemoteCommandService svc = service();
        Agent agent = Agent.builder().agentId("agt-1").build();
        when(agentService.getByAgentId("agt-1")).thenReturn(agent);
        when(remoteCommandRepository.save(any(RemoteCommand.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandCreateRequestDto dto = new CommandCreateRequestDto();
        dto.setCommand("uptime");
        dto.setArgs(List.of("-p"));
        dto.setTimeout(45);

        RemoteCommand result = svc.queue("agt-1", dto);

        assertThat(result.getAgent()).isSameAs(agent);
        assertThat(result.getCommand()).isEqualTo("uptime");
        assertThat(result.getArgsJson()).contains("-p");
        assertThat(result.getTimeout()).isEqualTo(45);
        assertThat(result.getStatus()).isEqualTo(RemoteCommand.CommandStatus.PENDING);
        verify(pushService).pushCommand(result, "agt-1");
    }

    @Test
    void queue_nullTimeout_defaultsTo30() throws Exception {
        RemoteCommandService svc = service();
        when(agentService.getByAgentId("agt-2")).thenReturn(Agent.builder().agentId("agt-2").build());
        when(remoteCommandRepository.save(any(RemoteCommand.class))).thenAnswer(inv -> inv.getArgument(0));

        CommandCreateRequestDto dto = new CommandCreateRequestDto();
        dto.setCommand("free");

        RemoteCommand result = svc.queue("agt-2", dto);

        assertThat(result.getTimeout()).isEqualTo(30);
    }

    @Test
    void fetchPending_marksCommandsSentAndReturnsThem() {
        RemoteCommandService svc = service();
        RemoteCommand cmd = RemoteCommand.builder()
                .id(UUID.randomUUID())
                .command("hostname")
                .argsJson(null)
                .timeout(30)
                .status(RemoteCommand.CommandStatus.PENDING)
                .build();
        when(remoteCommandRepository.findByAgent_AgentIdAndStatus("agt-3", RemoteCommand.CommandStatus.PENDING))
                .thenReturn(List.of(cmd));
        when(remoteCommandRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<PendingCommandDto> result = svc.fetchPending("agt-3");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommand()).isEqualTo("hostname");
        assertThat(result.get(0).getId()).isEqualTo(cmd.getId().toString());
        assertThat(cmd.getStatus()).isEqualTo(RemoteCommand.CommandStatus.SENT);
        assertThat(cmd.getSentAt()).isNotNull();
    }

    @Test
    void fetchPending_noneQueued_returnsEmptyListWithoutSaving() {
        RemoteCommandService svc = service();
        when(remoteCommandRepository.findByAgent_AgentIdAndStatus("agt-4", RemoteCommand.CommandStatus.PENDING))
                .thenReturn(List.of());
        when(remoteCommandRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.fetchPending("agt-4")).isEmpty();
    }

    @Test
    void recordResult_knownCommand_updatesFieldsAndPushes() {
        RemoteCommandService svc = service();
        Agent agent = Agent.builder().agentId("agt-5").build();
        User user = User.builder().id("use-1").build();
        UUID id = UUID.randomUUID();
        RemoteCommand command = RemoteCommand.builder().id(id).agent(agent).user(user).build();
        when(remoteCommandRepository.findById(id)).thenReturn(Optional.of(command));
        when(remoteCommandRepository.save(any(RemoteCommand.class))).thenAnswer(inv -> inv.getArgument(0));

        RemoteCommandResultInnerDto inner = new RemoteCommandResultInnerDto();
        inner.setStatus("success");
        inner.setStdout("out");
        inner.setStderr("");
        inner.setExitCode(0);
        inner.setExecutedAt(1_700_000_000.0);

        RemoteCommandResultDto dto = new RemoteCommandResultDto();
        dto.setAgentId("agt-5");
        dto.setResult(inner);

        RemoteCommand result = svc.recordResult(id.toString(), dto);

        assertThat(result.getStatus()).isEqualTo(RemoteCommand.CommandStatus.SUCCESS);
        assertThat(result.getStdout()).isEqualTo("out");
        assertThat(result.getExitCode()).isEqualTo(0);
        assertThat(result.getExecutedAt()).isNotNull();
        verify(pushService).pushCommandResult(result, "agt-5");
    }

    @Test
    void recordResult_unknownStatusString_mapsToError() {
        RemoteCommandService svc = service();
        UUID id = UUID.randomUUID();
        RemoteCommand command = RemoteCommand.builder().id(id).agent(Agent.builder().agentId("agt-6").build()).user(User.builder().id("use-1").build()).build();
        when(remoteCommandRepository.findById(id)).thenReturn(Optional.of(command));
        when(remoteCommandRepository.save(any(RemoteCommand.class))).thenAnswer(inv -> inv.getArgument(0));

        RemoteCommandResultInnerDto inner = new RemoteCommandResultInnerDto();
        inner.setStatus("whatever");

        RemoteCommandResultDto dto = new RemoteCommandResultDto();
        dto.setResult(inner);

        assertThat(svc.recordResult(id.toString(), dto).getStatus()).isEqualTo(RemoteCommand.CommandStatus.ERROR);
    }

    @Test
    void recordResult_unknownCommandId_throwsNoSuchElementException() {
        RemoteCommandService svc = service();
        UUID id = UUID.randomUUID();
        when(remoteCommandRepository.findById(id)).thenReturn(Optional.empty());

        RemoteCommandResultDto dto = new RemoteCommandResultDto();

        assertThatThrownBy(() -> svc.recordResult(id.toString(), dto))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void listForAgent_delegatesToRepository() {
        RemoteCommandService svc = service();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        var page = org.springframework.data.domain.Page.<RemoteCommand>empty();
        when(remoteCommandRepository.findByAgent_AgentIdOrderByCreatedAtDesc("agt-7", pageable)).thenReturn(page);

        assertThat(svc.listForAgent("agt-7", pageable)).isSameAs(page);
    }
}
